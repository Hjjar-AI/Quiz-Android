package com.mukhtabir.android.ui.masterexams

import com.mukhtabir.android.core.has
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.GroupService
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.MasterExamAuthorDto
import com.mukhtabir.android.data.remote.MasterExamMetadataWrite
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionPageDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.UserDto
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
internal data class ExamForm(
    val name: String = "", val description: String = "", val instructions: String = "", val topic: String = "",
    val opens: String = "", val closes: String = "", val duration: String = "30",
    val allDoctors: Boolean = false, val groups: String = "", val users: String = "", val coAuthors: String = "",
    val easy: String = "1.0", val medium: String = "1.0", val hard: String = "1.0",
    val shuffleQuestions: Boolean = true, val shuffleChoices: Boolean = false, val makeup: Boolean = true,
) {
    fun payload(primary: Long): MasterExamMetadataWrite? {
        val start = runCatching { OffsetDateTime.parse(opens) }.getOrNull() ?: return null
        val end = runCatching { OffsetDateTime.parse(closes) }.getOrNull() ?: return null
        val minutes = duration.asciiDigits().trim().toIntOrNull()?.takeIf { it in 1..600 } ?: return null
        if (!start.isBefore(end) || Duration.between(start, end).toMinutes() < minutes) return null
        fun weight(value: String) = value.asciiDigits().trim().replace('٫', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 0.1..10.0 }
        val groupIds = parseExamTargetIds(groups) ?: return null
        val userIds = parseExamTargetIds(users)?.filter { it != primary } ?: return null
        val authorIds = parseExamTargetIds(coAuthors)?.filter { it != primary } ?: return null
        if (name.trim().isEmpty() || name.trim().codePointCount(0, name.trim().length) > 200 ||
            topic.trim().codePointCount(0, topic.trim().length) > 100) return null
        return MasterExamMetadataWrite(name.trim(), description, instructions,
            start.withOffsetSameInstant(ZoneOffset.UTC).toString(), end.withOffsetSameInstant(ZoneOffset.UTC).toString(),
            minutes, allDoctors, groupIds, userIds, authorIds,
            weight(easy) ?: return null, weight(medium) ?: return null, weight(hard) ?: return null,
            shuffleQuestions, shuffleChoices, makeup, topic.trim())
    }
}

private fun String.asciiDigits(): String = map { char ->
    val digit = Character.digit(char, 10)
    if (digit >= 0) ('0'.code + digit).toChar() else char
}.joinToString("")

internal fun parseExamTargetIds(value: String): List<Long>? {
    if (value.isBlank()) return emptyList()
    val parts = value.asciiDigits().trim().split(Regex("[\\s,،]+"))
    val ids = parts.map { it.toLongOrNull()?.takeIf { id -> id > 0 } ?: return null }
    return ids.distinct().sorted()
}

internal fun MasterExamAuthorDto.form() = ExamForm(name, description.orEmpty(), instructions.orEmpty(), examTopicTag.orEmpty(),
    opensAt, closesAt, durationMinutes.toString(), audienceAllDoctors,
    audienceGroups.map { it.id }.sorted().joinToString(", "), audienceUsers.map { it.id }.sorted().joinToString(", "),
    coAttendings.map { it.id }.sorted().joinToString(", "), weightEasy.toString(), weightMedium.toString(), weightHard.toString(),
    shuffleQuestions, shuffleChoices, allowMakeup)

internal data class ExamEditorState(
    val exam: MasterExamAuthorDto? = null,
    val form: ExamForm = ExamForm(), val baseline: ExamForm = ExamForm(),
    val loaded: Boolean = false, val busy: Boolean = false, val denied: Boolean = false,
    val error: Throwable? = null, val message: Int? = null, val validation: Boolean = false,
    val needsSync: Boolean = false, val conflict: Boolean = false, val latest: MasterExamAuthorDto? = null,
    val pendingMetadata: MasterExamMetadataWrite? = null, val plannedIds: List<Long>? = null,
    val deleting: Boolean = false, val deleted: Boolean = false,
    val questions: Map<Long, QuestionDto> = emptyMap(), val questionError: Throwable? = null,
    val pickerOpen: Boolean = false, val pickerBusy: Boolean = false, val pickerError: Throwable? = null,
    val pickerPage: QuestionPageDto = QuestionPageDto(), val pickerRequestedPage: Int = 1, val query: String = "", val selected: Set<Long> = emptySet(),
    val targets: ExamTargetPickerState? = null,
    val embedded: EmbeddedExamDraftState? = null,
    val draftCategories: List<CategoryDto> = emptyList(), val draftCategoriesBusy: Boolean = false,
    val draftCategoriesError: Throwable? = null,
    val targetNames: Map<ExamTargetKind, Map<Long, String>> = emptyMap(),
) { val dirty: Boolean get() = form != baseline }

internal class MasterExamEditorViewModel(
    private val initialId: Long?, private val user: UserDto, private val service: MasterExamService,
    private val questions: QuestionService, private val pageSize: Int,
    private val admin: AdminService, private val groups: GroupService,
    private val categories: CategoryService, val maxChoices: Int,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ExamEditorState(loaded = initialId == null))
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var hydrateJob: Job? = null
    private var targetJob: Job? = null
    private var categoryJob: Job? = null
    private val recoveryMutex = Mutex()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = "exam-${initialId ?: "new"}"
    private val pendingRecovery = MutableStateFlow<StoredExamEditor?>(null)
    val recovery = pendingRecovery.asStateFlow()
    private val storageFailure = MutableStateFlow<Throwable?>(null)
    val recoveryError = storageFailure.asStateFlow()
    private var unreadableRecovery = false
    private var recoveryLoading = true
    init {
        viewModelScope.launch {
            try {
                val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.readEditor(user.id, storageKey).getOrThrow() }
                ensureActive()
                pendingRecovery.value = raw?.let { json.decodeFromString(StoredExamEditor.serializer(), it) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { unreadableRecovery = true; storageFailure.value = failure }
            recoveryLoading = false
            if (initialId != null) refresh()
        }
        viewModelScope.launch {
            state.collect { s ->
                if (!recoveryLoading && pendingRecovery.value == null && s.loaded && !s.busy && !s.denied && (!unreadableRecovery || s.dirty || s.embedded != null || s.needsSync)) persistSnapshot(s)
            }
        }
    }
    private suspend fun persistSnapshot(s: ExamEditorState): Boolean = recoveryMutex.withLock {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (s.deleted || (!s.dirty && s.embedded == null && !s.needsSync && s.plannedIds == null && s.targets == null && s.selected.isEmpty())) store.clearEditor(user.id, storageKey)
            else store.writeEditor(user.id, storageKey, json.encodeToString(StoredExamEditor.serializer(), StoredExamEditor(
                s.form, s.baseline, s.exam?.version, s.embedded?.form, s.embedded?.baseline,
                s.embedded?.beforeIds.orEmpty(), s.embedded?.uncertain == true, s.embedded?.createdId,
                s.needsSync, s.pendingMetadata, s.plannedIds, s.deleting, s.targets?.kind?.name,
                s.targets?.selected?.toList().orEmpty(), s.targets?.query.orEmpty(), s.query, s.selected.toList())))
        }
        kotlin.coroutines.coroutineContext.ensureActive()
        storageFailure.value = result.exceptionOrNull()
        if (result.isSuccess) unreadableRecovery = false
        result.isSuccess
    }
    fun restoreDraft() {
        val saved = pendingRecovery.value ?: return
        val s = _state.value
        if (!s.loaded || s.busy || s.denied) return
        pendingRecovery.value = null
        _state.update { it.copy(form = saved.form, baseline = saved.baseline, needsSync = saved.needsSync,
            pendingMetadata = saved.pendingMetadata, plannedIds = saved.plannedIds, deleting = saved.deleting,
            conflict = s.exam != null && saved.version != s.exam.version, latest = s.exam.takeIf { saved.version != it?.version },
            selected = saved.selected.toSet(), query = saved.query,
            targets = saved.targetKind?.let { name -> runCatching { ExamTargetKind.valueOf(name) }.getOrNull() }
                ?.takeIf(::canLookUp)?.let { kind -> ExamTargetPickerState(kind, selected = saved.targetIds.toSet(), query = saved.targetQuery) },
            embedded = saved.embedded?.let { form -> EmbeddedExamDraftState(form = form, baseline = saved.embeddedBaseline ?: form,
                beforeIds = saved.beforeIds, uncertain = saved.embeddedUncertain, createdId = saved.createdId) }) }
        if (saved.embedded != null) loadDraftCategories()
        if (_state.value.targets != null) searchTargets(saved.targetQuery, 1)
        if (saved.needsSync && s.exam != null) refresh()
    }
    fun discardRecovery() {
        if (_state.value.busy) return
        viewModelScope.launch {
            val result = recoveryMutex.withLock { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.clearEditor(user.id, storageKey) } }
            ensureActive()
            if (result.isSuccess) pendingRecovery.value = null else storageFailure.value = result.exceptionOrNull()
        }
    }
    fun keepDraft(onDone: () -> Unit) {
        val s = _state.value
        if (s.busy || !s.loaded) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = persistSnapshot(s)
            _state.update { it.copy(busy = false) }
            if (saved) onDone()
        }
    }
    fun discardDraft(onDone: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = recoveryMutex.withLock { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.clearEditor(user.id, storageKey) } }
            ensureActive()
            _state.update { it.copy(busy = false, form = if (result.isSuccess) it.baseline else it.form, embedded = if (result.isSuccess) null else it.embedded, needsSync = if (result.isSuccess) false else it.needsSync, error = result.exceptionOrNull()) }
            if (result.isSuccess) onDone()
        }
    }
    fun reviewCreation() {
        val s = _state.value
        if (!s.needsSync || s.exam != null || s.busy || !user.has("master_exams.create")) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val rows = service.list().items.filter { it.name == s.form.name.trim() }
                val matches = rows.map { service.authorDetail(it.id) }.filter { it.primaryAttending == user.id && it.form().payload(user.id) == s.form.payload(user.id) }
                ensureActive()
                if (matches.size == 1) adopt(matches.single(), R.string.exam_author_saved)
                else _state.update { it.copy(busy = false, message = R.string.author_create_review, plannedIds = emptyList()) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); _state.update { it.copy(busy = false, error = failure) } }
        }
    }
    fun acknowledgeCreationReview() {
        val s = _state.value
        if (s.exam == null && !s.busy && s.needsSync && s.plannedIds != null)
            _state.update { it.copy(needsSync = false, plannedIds = null, pendingMetadata = null, message = null) }
    }


    fun mayManage(exam: MasterExamAuthorDto): Boolean = user.has("master_exams.manage_any") ||
        (user.has("master_exams.manage_own") && (exam.primaryAttending == user.id || exam.coAttendings.any { it.id == user.id }))

    fun editable(s: ExamEditorState = _state.value): Boolean = s.loaded && !s.busy && !s.denied && !s.deleted &&
        !recoveryLoading && pendingRecovery.value == null && !s.needsSync && !s.conflict && s.plannedIds == null && s.embedded?.uncertain != true && s.embedded?.createdId == null &&
        (s.exam?.let { mayManage(it) && it.canEditNow } ?: (initialId == null && user.has("master_exams.create")))

    fun mayDelete(s: ExamEditorState = _state.value): Boolean = s.loaded && !s.busy && !s.denied && !s.deleted &&
        !recoveryLoading && pendingRecovery.value == null && !s.needsSync && !s.conflict && !s.dirty && s.targets == null && s.embedded == null && s.plannedIds == null && s.exam?.let { mayManage(it) && it.canBeDeleted } == true

    fun canTransition(action: String, s: ExamEditorState = _state.value): Boolean {
        val exam = s.exam ?: return false
        if (recoveryLoading || pendingRecovery.value != null || s.busy || !s.loaded || s.denied || s.deleted || s.dirty || s.needsSync || s.conflict ||
            s.plannedIds != null || s.embedded != null || s.targets != null || s.pickerOpen || !mayManage(exam)) return false
        return when (action) {
            "publish" -> exam.storedStatus == "draft" && exam.questionIds.isNotEmpty()
            "cancel" -> exam.storedStatus !in setOf("cancelled", "published_to_bank")
            "publish-to-bank" -> exam.storedStatus != "published_to_bank" &&
                runCatching { !OffsetDateTime.now().isBefore(OffsetDateTime.parse(exam.closesAt)) }.getOrDefault(false) && user.has("master_exams.publish_to_bank")
            else -> false
        }
    }

    fun transition(action: String) {
        val s = _state.value
        val exam = s.exam ?: return
        if (!canTransition(action, s)) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            var posting = false
            try {
                val fresh = service.authorDetail(exam.id)
                ensureActive()
                if (fresh.version != exam.version || fresh.storedStatus != exam.storedStatus ||
                    fresh.questionIds != exam.questionIds || !canTransition(action, s.copy(exam = fresh))) {
                    adopt(fresh, R.string.exam_author_review_retry)
                    return@launch
                }
                // Lifecycle endpoints have no expected_version; the server remains authoritative.
                if (!persistSnapshot(s.copy(needsSync = true))) { _state.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                posting = true
                val saved = service.lifecycle(exam.id, action)
                ensureActive()
                persistSnapshot(s.copy(form = s.baseline, embedded = null, needsSync = false, plannedIds = null))
                adopt(saved, R.string.exam_author_saved)
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val certain = !posting || (failure is ApiException && failure.statusCode in setOf(400, 403, 404, 429))
                _state.update { it.copy(busy = false, error = failure, needsSync = !certain, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404))) }
            }
        }
    }

    fun edit(change: (ExamForm) -> ExamForm) {
        if (editable() && _state.value.targets == null && _state.value.embedded == null) _state.update { it.copy(form = change(it.form), validation = false, message = null) }
    }

    private fun adopt(exam: MasterExamAuthorDto, message: Int? = null) {
        searchJob?.cancel()
        targetJob?.cancel()
        val form = exam.form()
        _state.update { it.copy(exam = exam, form = form, baseline = form, loaded = true, busy = false,
            denied = !mayManage(exam), needsSync = false, conflict = false, latest = null,
            pendingMetadata = null, plannedIds = null, deleting = false, error = null, message = message,
            selected = emptySet(), pickerOpen = false, pickerBusy = false, targets = null) }
        hydrate()
    }

    /** Read recovery never resends a metadata, composition or deletion request. */
    fun refresh() {
        val snapshot = _state.value
        val id = snapshot.exam?.id ?: initialId ?: return
        if (snapshot.busy || snapshot.deleted || snapshot.targets != null) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val fresh = service.authorDetail(id)
                ensureActive()
                val freshForm = fresh.form()
                val metadataConfirmed = snapshot.pendingMetadata != null &&
                    snapshot.pendingMetadata == freshForm.payload(fresh.primaryAttending)
                val compositionConfirmed = snapshot.plannedIds != null && snapshot.plannedIds == fresh.questionIds
                when {
                    snapshot.embedded?.createdId != null -> {
                        val draft = requireNotNull(snapshot.embedded)
                        val createdId = requireNotNull(draft.createdId)
                        adopt(fresh, if (createdId in fresh.questionIds) R.string.exam_draft_saved else R.string.exam_draft_detached)
                        _state.update { it.copy(embedded = if (createdId in fresh.questionIds) null else draft.copy(reconciled = true)) }
                    }
                    snapshot.embedded?.uncertain == true -> {
                        adopt(fresh, R.string.exam_draft_uncertain)
                        _state.update { it.copy(embedded = snapshot.embedded.copy(reconciled = true)) }
                    }
                    metadataConfirmed || compositionConfirmed -> adopt(fresh, R.string.exam_author_saved)
                    snapshot.deleting -> adopt(fresh, R.string.exam_author_review_retry)
                    snapshot.plannedIds != null -> {
                        adopt(fresh, R.string.exam_author_review_retry)
                        _state.update { it.copy(plannedIds = snapshot.plannedIds) }
                    }
                    snapshot.dirty && snapshot.exam != null && fresh.version != snapshot.exam.version -> {
                        _state.update { it.copy(exam = fresh, latest = fresh, loaded = true, busy = false,
                            denied = !mayManage(fresh), conflict = true, needsSync = false, pendingMetadata = null,
                            message = R.string.exam_author_conflict) }
                        hydrate()
                    }
                    snapshot.dirty -> {
                        _state.update { it.copy(exam = fresh, loaded = true, busy = false, denied = !mayManage(fresh),
                            needsSync = false, pendingMetadata = null, message = R.string.exam_author_review_retry) }
                        hydrate()
                    }
                    else -> adopt(fresh)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                if (snapshot.deleting && error is ApiException && error.statusCode == 404) {
                    _state.update { it.copy(busy = false, deleted = true, deleting = false, needsSync = false) }
                } else _state.update { it.copy(busy = false, error = error,
                    denied = it.denied || (error is ApiException && error.statusCode in setOf(403, 404))) }
            }
        }
    }

    fun resolveConflict(keepDraft: Boolean) {
        val s = _state.value
        val latest = s.latest ?: return
        if (s.busy || s.needsSync || !s.conflict) return
        if (!keepDraft) adopt(latest) else {
            _state.update { it.copy(baseline = latest.form(), conflict = false, latest = null, message = null) }
        }
    }

    fun acknowledgePlan() {
        if (!_state.value.busy && !_state.value.needsSync) _state.update { it.copy(plannedIds = null, message = null) }
    }

    fun save() {
        val s = _state.value
        if (!editable(s) || s.targets != null || s.embedded != null || (s.exam != null && !s.dirty)) return
        val payload = s.form.payload(s.exam?.primaryAttending ?: user.id)
        if (payload == null) { _state.update { it.copy(validation = true) }; return }
        _state.update { it.copy(busy = true, validation = false, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (!persistSnapshot(s.copy(needsSync = true, pendingMetadata = payload))) { _state.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                val saved = if (s.exam == null) service.createExam(payload) else service.updateExam(s.exam.id, s.exam.version, payload)
                ensureActive()
                persistSnapshot(s.copy(form = s.baseline, embedded = null, needsSync = false, plannedIds = null))
                adopt(saved, R.string.exam_author_saved)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                // A transport/server failure can have committed. Explicit 4xx validation/denial did not.
                val certain = error is ApiException && error.statusCode in setOf(400, 403, 404)
                _state.update { it.copy(busy = false, error = error, needsSync = !certain,
                    pendingMetadata = if (certain) null else payload, denied = error is ApiException && error.statusCode in setOf(403, 404),
                    message = if (!certain && s.exam == null) R.string.exam_author_create_uncertain else null) }
            }
        }
    }

    fun delete(mode: String) {
        val s = _state.value
        val exam = s.exam ?: return
        if (!mayDelete(s) || mode !in setOf("keep_drafts", "delete_drafts", "publish_drafts")) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val fresh = service.authorDetail(exam.id)
                ensureActive()
                if (!mayManage(fresh) || !fresh.canBeDeleted || fresh.version != exam.version ||
                    fresh.status != exam.status || fresh.questionIds != exam.questionIds) {
                    adopt(fresh, R.string.exam_author_review_retry)
                    return@launch
                }
                // DELETE has no server revision precondition; preflight does not make the read/write gap atomic.
                if (!persistSnapshot(s.copy(needsSync = true, deleting = true))) { _state.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                service.deleteExam(exam.id, mode)
                ensureActive()
                persistSnapshot(s.copy(deleted = true))
                _state.update { it.copy(busy = false, deleted = true) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                _state.update { it.copy(busy = false, error = error, needsSync = true, deleting = true) }
            }
        }
    }

    private fun hydrate() {
        hydrateJob?.cancel()
        val snapshot = _state.value.exam ?: return
        _state.update { it.copy(questionError = null, questions = it.questions.filterKeys(snapshot.questionIds::contains)) }
        hydrateJob = viewModelScope.launch {
            try {
                val bank = snapshot.questionIds.chunked(500).flatMap { questions.batch(it).items }.associateBy { it.id }
                ensureActive()
                if (_state.value.exam?.questionIds == snapshot.questionIds) _state.update { it.copy(questions = bank) }
                val drafts = if (user.has("master_exams.drafts_library")) service.drafts("", "").items.filter { it.id in snapshot.questionIds }.associateBy { it.id } else emptyMap()
                val rows = bank + drafts.mapValues { (id, draft) -> bank[id]?.copy(
                    isDraft = draft.isDraft, draftOwner = draft.draftOwner, updatedAt = draft.updatedAt) ?: draft }
                ensureActive()
                if (_state.value.exam?.questionIds == snapshot.questionIds) _state.update { it.copy(questions = rows) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(questionError = error) } }
        }
    }
    fun retryQuestions() { if (!_state.value.busy) hydrate() }

    fun openEmbeddedDraft() {
        val s = _state.value
        if (!editable(s) || s.exam == null || s.dirty || s.targets != null || s.pickerOpen || s.embedded != null) return
        _state.update { it.copy(embedded = EmbeddedExamDraftState(beforeIds = s.exam.questionIds)) }
        loadDraftCategories()
    }

    fun loadDraftCategories() {
        if (_state.value.embedded == null || _state.value.draftCategoriesBusy || _state.value.busy) return
        _state.update { it.copy(draftCategoriesBusy = true, draftCategoriesError = null) }
        categoryJob = viewModelScope.launch {
            try {
                val found = categories.list()
                ensureActive()
                _state.update { it.copy(draftCategories = found.items, draftCategoriesBusy = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(draftCategoriesBusy = false, draftCategoriesError = error) } }
        }
    }

    fun editEmbeddedDraft(change: (EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) {
        val s = _state.value
        val draft = s.embedded ?: return
        if (!editable(s)) return
        _state.update { it.copy(embedded = draft.copy(form = change(draft.form), validation = false, error = null)) }
    }

    fun closeEmbeddedDraft() {
        val s = _state.value
        val draft = s.embedded ?: return
        if (s.busy || ((draft.uncertain || draft.createdId != null) && !draft.reconciled)) return
        categoryJob?.cancel()
        _state.update { it.copy(embedded = null, draftCategoriesBusy = false) }
    }

    fun createEmbeddedDraft() {
        val s = _state.value
        val exam = s.exam ?: return
        val draft = s.embedded ?: return
        if (!editable(s) || s.dirty || s.targets != null || s.pickerOpen) return
        val payload = draft.form.payload(maxChoices)
        if (payload == null) { _state.update { it.copy(embedded = draft.copy(validation = true)) }; return }
        _state.update { it.copy(busy = true, error = null, message = null, embedded = draft.copy(error = null, validation = false)) }
        viewModelScope.launch {
            var posting = false
            try {
                val fresh = service.authorDetail(exam.id)
                ensureActive()
                if (!mayManage(fresh) || !fresh.canEditNow || fresh.version != exam.version || fresh.questionIds != exam.questionIds) {
                    adopt(fresh, R.string.exam_author_review_retry)
                    return@launch
                }
                // The read/write gap is not atomic: this endpoint accepts no revision or idempotency key.
                if (!persistSnapshot(s.copy(needsSync = true, embedded = draft.copy(uncertain = true)))) { _state.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                posting = true
                val created = service.addDraft(exam.id, payload)
                ensureActive()
                persistSnapshot(s.copy(needsSync = true, embedded = draft.copy(createdId = created.id)))
                _state.update { it.copy(busy = false, needsSync = true,
                    embedded = draft.copy(createdId = created.id), message = R.string.exam_draft_saved) }
                // Confirmed POST and a failed status read are separate outcomes. Never create again.
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                val certain = !posting || (error is ApiException && error.statusCode in setOf(400, 403, 404))
                _state.update { it.copy(busy = false, needsSync = !certain,
                    denied = it.denied || (error is ApiException && error.statusCode in setOf(403, 404)),
                    embedded = draft.copy(error = error, uncertain = !certain),
                    message = if (certain) null else R.string.exam_draft_uncertain) }
            }
        }
    }

    fun canLookUp(kind: ExamTargetKind): Boolean = user.has(
        if (kind == ExamTargetKind.GROUPS) "groups.admin" else "admin.users")

    fun openTargets(kind: ExamTargetKind) {
        val s = _state.value
        if (!editable(s) || s.embedded != null || !canLookUp(kind) || s.pickerOpen || s.targets != null) return
        val ids = parseExamTargetIds(kind.ids(s.form))
        if (ids == null) { _state.update { it.copy(validation = true) }; return }
        _state.update { it.copy(targets = ExamTargetPickerState(kind, selected = ids.toSet())) }
        searchTargets("", 1)
    }

    fun closeTargets() {
        targetJob?.cancel()
        _state.update { it.copy(targets = null) }
    }

    fun searchTargets(query: String, page: Int) {
        val s = _state.value
        val picker = s.targets ?: return
        if (!editable(s) || !canLookUp(picker.kind)) return
        targetJob?.cancel()
        val requested = page.coerceAtLeast(1)
        _state.update { it.copy(targets = picker.copy(busy = true, error = null, query = query, requestedPage = requested)) }
        targetJob = viewModelScope.launch {
            try {
                val rows: List<ExamTargetRow>
                val currentPage: Int
                val pages: Int
                val allNames: Map<Long, String>
                if (picker.kind == ExamTargetKind.GROUPS) {
                    // No server search/pagination contract. Cache only within this open picker.
                    val all = picker.groups ?: groups.adminList().items.map { ExamTargetRow(it.id, it.name) }
                    ensureActive()
                    val filtered = all.filter { it.name.contains(query.trim(), ignoreCase = true) || it.id.toString() == query.trim() }
                    val size = pageSize.coerceAtLeast(1)
                    pages = ((filtered.size + size - 1) / size).coerceAtLeast(1)
                    currentPage = requested.coerceAtMost(pages)
                    rows = filtered.drop((currentPage - 1) * size).take(size)
                    allNames = all.associate { it.id to it.name }
                    _state.update { it.copy(targets = it.targets?.copy(groups = all)) }
                } else {
                    val found = admin.users(page = requested, perPage = pageSize.coerceAtLeast(1), search = query.trim())
                    ensureActive()
                    val primary = s.exam?.primaryAttending ?: user.id
                    rows = found.items.filter { candidate ->
                        candidate.id != primary &&
                            (if (picker.kind == ExamTargetKind.COAUTHORS) candidate.has("master_exams.create")
                            else candidate.role == "member")
                    }
                        .map { ExamTargetRow(it.id, it.displayName + " (@" + it.username + ")") }
                    currentPage = found.page
                    pages = found.totalPages.coerceAtLeast(1)
                    allNames = rows.associate { it.id to it.name }
                }
                ensureActive()
                _state.update { it.copy(targets = it.targets?.copy(busy = false, rows = rows, page = currentPage, totalPages = pages),
                    targetNames = it.targetNames + (picker.kind to (it.targetNames[picker.kind].orEmpty() + allNames))) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                // Retain staged IDs, including assignments absent from this page or lookup.
                _state.update { it.copy(targets = it.targets?.copy(busy = false, error = error)) }
            }
        }
    }

    fun toggleTarget(id: Long) {
        val s = _state.value
        val picker = s.targets ?: return
        if (!editable(s) || !canLookUp(picker.kind) || picker.busy || picker.error != null || picker.rows.none { it.id == id }) return
        _state.update { it.copy(targets = picker.copy(selected =
            if (id in picker.selected) picker.selected - id else picker.selected + id)) }
    }

    fun removeTarget(id: Long) {
        val s = _state.value
        val picker = s.targets ?: return
        if (!editable(s) || !canLookUp(picker.kind) || picker.busy || picker.error != null || id !in picker.selected) return
        _state.update { it.copy(targets = picker.copy(selected = picker.selected - id)) }
    }

    fun applyTargets() {
        val s = _state.value
        val picker = s.targets ?: return
        if (!editable(s) || !canLookUp(picker.kind) || picker.busy || picker.error != null) return
        val ids = picker.selected.sorted().joinToString(", ")
        targetJob?.cancel()
        _state.update { it.copy(form = picker.kind.replace(it.form, ids), targets = null, validation = false, message = null) }
    }

    fun openPicker() {
        val s = _state.value
        if (!editable(s) || s.embedded != null || s.exam == null || s.dirty || s.targets != null) return
        _state.update { it.copy(pickerOpen = true) }
        search(s.query, 1)
    }
    fun closePicker() {
        if (_state.value.busy) return
        searchJob?.cancel()
        _state.update { it.copy(pickerOpen = false, pickerBusy = false) }
    }
    fun search(query: String, page: Int) {
        if (!_state.value.pickerOpen || _state.value.busy || _state.value.needsSync) return
        searchJob?.cancel()
        _state.update { it.copy(pickerBusy = true, pickerError = null, query = query, pickerRequestedPage = page.coerceAtLeast(1)) }
        searchJob = viewModelScope.launch {
            try {
                val found = questions.list(page = page.coerceAtLeast(1), perPage = pageSize.coerceAtLeast(1), search = query.trim())
                ensureActive()
                _state.update { it.copy(pickerBusy = false, pickerPage = found) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(pickerBusy = false, pickerError = error) } }
        }
    }
    fun toggle(id: Long) {
        val s = _state.value
        if (!editable(s) || s.pickerBusy || s.pickerError != null || id in s.exam?.questionIds.orEmpty() || s.pickerPage.items.none { it.id == id }) return
        _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }
    }
    fun addSelected() = compose("add", _state.value.selected.toList())
    fun removeQuestion(id: Long) = compose("remove", listOf(id))
    fun moveQuestion(id: Long, offset: Int) {
        val ids = _state.value.exam?.questionIds?.toMutableList() ?: return
        val index = ids.indexOf(id)
        if (index < 0 || index + offset !in ids.indices) return
        ids.add(index + offset, ids.removeAt(index))
        compose("reorder", ids)
    }
    private fun compose(action: String, ids: List<Long>) {
        val s = _state.value
        val exam = s.exam ?: return
        if (!editable(s) || s.embedded != null || s.targets != null || s.dirty || ids.isEmpty() || ids.any { it <= 0 }) return
        if (action == "remove" && ids.single() !in exam.questionIds) return
        val planned = when (action) {
            "add" -> (exam.questionIds + ids).distinct()
            "remove" -> exam.questionIds - ids.toSet()
            else -> ids
        }
        searchJob?.cancel()
        _state.update { it.copy(busy = true, pickerBusy = false, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (!persistSnapshot(s.copy(needsSync = true, plannedIds = planned))) { _state.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                val saved = service.compose(exam.id, exam.version, action, ids)
                ensureActive()
                persistSnapshot(s.copy(form = s.baseline, embedded = null, needsSync = false, plannedIds = null))
                adopt(saved, R.string.exam_author_saved)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                _state.update { it.copy(busy = false, needsSync = true, error = error, plannedIds = planned) }
            }
        }
    }

    class Factory(private val id: Long?, private val user: UserDto, private val service: MasterExamService,
        private val questions: QuestionService, private val pageSize: Int,
        private val admin: AdminService, private val groups: GroupService,
        private val categories: CategoryService, private val maxChoices: Int, private val store: com.mukhtabir.android.core.SessionDraftStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MasterExamEditorViewModel(id, user, service, questions, pageSize, admin, groups, categories, maxChoices, store) as T
    }
}

@kotlinx.serialization.Serializable
internal data class StoredExamEditor(val form: ExamForm, val baseline: ExamForm, val version: Int?,
    val embedded: EmbeddedExamDraftForm? = null, val embeddedBaseline: EmbeddedExamDraftForm? = null,
    val beforeIds: List<Long> = emptyList(), val embeddedUncertain: Boolean = false, val createdId: Long? = null,
    val needsSync: Boolean = false, val pendingMetadata: MasterExamMetadataWrite? = null,
    val plannedIds: List<Long>? = null, val deleting: Boolean = false,
    val targetKind: String? = null, val targetIds: List<Long> = emptyList(), val targetQuery: String = "",
    val query: String = "", val selected: List<Long> = emptyList())
