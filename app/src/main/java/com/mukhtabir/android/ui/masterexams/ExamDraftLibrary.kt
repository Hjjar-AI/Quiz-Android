package com.mukhtabir.android.ui.masterexams

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class DraftLibraryState(
    val rows: List<QuestionDto> = emptyList(), val query: String = "", val usage: String = "", val page: Int = 1,
    val busy: Boolean = false, val denied: Boolean = false, val error: Throwable? = null, val selected: QuestionDto? = null,
    val form: EmbeddedExamDraftForm? = null, val baseline: MasterExamDraftWrite? = null,
    val latest: QuestionDto? = null, val review: Boolean = false, val uncertain: String? = null,
    val categories: List<CategoryDto> = emptyList(), val invalid: Boolean = false, val validationAttempt: Int = 0, val message: Int? = null,
)

internal class ExamDraftLibraryViewModel(private val user: UserDto, private val service: MasterExamService,
    private val categories: CategoryService, private val maxChoices: Int, initialId: Long?, private val store: com.mukhtabir.android.core.SessionDraftStore) : ViewModel() {
    private val mutable = MutableStateFlow(DraftLibraryState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    private fun allowed(row: QuestionDto) = user.has("master_exams.drafts_library") && row.isDraft &&
        (user.has("master_exams.manage_any") || row.draftOwner == user.id)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = "draft-${initialId ?: "new"}"
    private val recoveryMutex = Mutex()
    private val pendingRecovery = MutableStateFlow<StoredLibraryEditor?>(null)
    val recovery = pendingRecovery.asStateFlow()
    private val storageFailure = MutableStateFlow<Throwable?>(null)
    val recoveryError = storageFailure.asStateFlow()
    private var unreadableRecovery = false
    private var loadingRecovery = true
    init {
        viewModelScope.launch {
            try {
                val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.readEditor(user.id, storageKey).getOrThrow() }
                ensureActive()
                pendingRecovery.value = raw?.let { json.decodeFromString(StoredLibraryEditor.serializer(), it) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { unreadableRecovery = true; storageFailure.value = failure }
            loadingRecovery = false
            if (initialId == null) search("", "") else open(initialId)
        }
        viewModelScope.launch { state.collect { value ->
            if (!loadingRecovery && pendingRecovery.value == null && !value.busy && (!unreadableRecovery || (value.selected != null && (value.form?.payload(maxChoices) != value.baseline || value.uncertain != null)))) persist(value)
        } }
    }
    private suspend fun persist(s: DraftLibraryState): Boolean = recoveryMutex.withLock {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val selected = s.selected
            val form = s.form
            if (selected == null || form == null || (form.payload(maxChoices) == s.baseline && s.uncertain == null && !s.review)) store.clearEditor(user.id, storageKey)
            else store.writeEditor(user.id, storageKey, json.encodeToString(StoredLibraryEditor.serializer(),
                StoredLibraryEditor(selected, form, s.baseline, s.uncertain)))
        }
        kotlin.coroutines.coroutineContext.ensureActive()
        storageFailure.value = result.exceptionOrNull()
        if (result.isSuccess) unreadableRecovery = false
        result.isSuccess
    }
    fun restoreDraft() {
        val saved = pendingRecovery.value ?: return
        val s = mutable.value
        if (s.busy || !user.has("master_exams.drafts_library")) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val fresh = service.draft(saved.selected.id)
                ensureActive()
                if (!allowed(fresh)) throw ApiException(403, "")
                val cats = categories.list().items
                ensureActive()
                pendingRecovery.value = null
                mutable.update { it.copy(busy = false, denied = false, selected = fresh, form = saved.form, categories = cats,
                    baseline = fresh.examDraftForm().payload(maxChoices), uncertain = saved.uncertain,
                    latest = fresh, review = saved.selected.version != fresh.version || saved.selected.updatedAt != fresh.updatedAt || saved.baseline != fresh.examDraftForm().payload(maxChoices)) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); mutable.update { it.copy(busy = false, error = failure, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404))) } }
        }
    }
    fun discardRecovery() {
        if (mutable.value.busy) return
        viewModelScope.launch {
            if (persist(DraftLibraryState())) pendingRecovery.value = null
        }
    }

    fun search(query: String, usage: String) {
        val s = mutable.value
        if (!user.has("master_exams.drafts_library") || s.busy || s.selected != null || usage !in setOf("", "orphan", "attached")) return
        loadJob?.cancel()
        mutable.update { it.copy(busy = true, query = query, usage = usage, page = 1, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val rows = service.drafts(query.trim(), usage).items
                ensureActive()
                mutable.update { it.copy(rows = rows.filter(::allowed), busy = false, denied = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); mutable.update { it.copy(busy = false, error = failure, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404))) } }
        }
    }
    fun page(value: Int, pageSize: Int) { if (!mutable.value.busy) mutable.update { it.copy(page = value.coerceIn(1, ((it.rows.size - 1).coerceAtLeast(0) / pageSize.coerceAtLeast(1)) + 1)) } }
    fun open(id: Long) {
        if (!user.has("master_exams.drafts_library") || mutable.value.busy || mutable.value.selected != null) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val row = service.draft(id)
                ensureActive()
                if (!allowed(row)) throw ApiException(403, "")
                val form = row.examDraftForm()
                mutable.update { it.copy(selected = row, form = form, baseline = form.payload(maxChoices), busy = false, denied = false, message = null) }
                try { val cats = categories.list().items; ensureActive(); mutable.update { it.copy(categories = cats) } }
                catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { ensureActive(); mutable.update { it.copy(error = failure) } }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); mutable.update { it.copy(busy = false, error = failure, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404))) } }
        }
    }
    fun edit(change: (EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) {
        val s = mutable.value
        if (s.denied || s.busy || s.review || s.uncertain != null || pendingRecovery.value != null || s.selected?.let(::allowed) != true) return
        mutable.update { it.copy(form = it.form?.let(change), invalid = false, message = null) }
    }
    fun keepDraft(onDone: () -> Unit) {
        val s = mutable.value
        if (s.busy || s.selected == null) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = persist(s)
            mutable.update { it.copy(busy = false) }
            if (saved) onDone()
        }
    }
    fun close() {
        if (mutable.value.busy || mutable.value.uncertain != null) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            if (!persist(DraftLibraryState())) { mutable.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
            mutable.update { it.copy(busy = false, selected = null, form = null, baseline = null, latest = null, review = false, error = null) }
            search(mutable.value.query, mutable.value.usage)
        }
    }
    fun reconcile() {
        val s = mutable.value
        val row = s.selected ?: return
        if (s.busy || !user.has("master_exams.drafts_library")) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val fresh = service.draft(row.id)
                ensureActive()
                if (!allowed(fresh)) throw ApiException(403, "")
                val confirmed = s.uncertain == "save" && fresh.examDraftForm().payload(maxChoices) == s.form?.payload(maxChoices)
                mutable.update { it.copy(busy = false, uncertain = null, latest = fresh, review = !confirmed,
                    denied = false, selected = fresh, baseline = fresh.examDraftForm().payload(maxChoices), message = if (confirmed) R.string.exam_draft_saved else R.string.exam_author_review_retry) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                if (s.uncertain == "delete" && failure is ApiException && failure.statusCode == 404) {
                    mutable.update { it.copy(busy = false, uncertain = null) }; close()
                } else mutable.update { it.copy(busy = false, error = failure, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404))) }
            }
        }
    }
    fun acceptLatest(keep: Boolean) {
        val s = mutable.value
        val row = s.latest ?: return
        if (s.denied || s.busy || s.uncertain != null || !allowed(row)) return
        mutable.update { it.copy(form = if (keep) it.form else row.examDraftForm(), latest = null, review = false) }
    }
    fun save() = mutate(false)
    fun delete() = mutate(true)
    private fun mutate(delete: Boolean) {
        val s = mutable.value
        val row = s.selected ?: return
        if (s.denied || s.busy || s.review || s.uncertain != null || pendingRecovery.value != null || !allowed(row)) return
        val payload = s.form?.payload(maxChoices)
        if (!delete && payload == null) { mutable.update { it.copy(invalid = true, validationAttempt = it.validationAttempt + 1) }; return }
        mutable.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            var posting = false
            try {
                val fresh = service.draft(row.id)
                ensureActive()
                if (!allowed(fresh)) throw ApiException(403, "")
                if (fresh.version != row.version || fresh.updatedAt != row.updatedAt || fresh.examDraftForm().payload(maxChoices) != s.baseline) {
                    mutable.update { it.copy(busy = false, latest = fresh, review = true, selected = fresh, baseline = fresh.examDraftForm().payload(maxChoices)) }
                    return@launch
                }
                // The submitted revision also covers changes after this read.
                if (!persist(s.copy(uncertain = if (delete) "delete" else "save"))) { mutable.update { it.copy(busy = false, error = storageFailure.value) }; return@launch }
                posting = true
                if (delete) {
                    service.deleteDraft(row.id, row.version); ensureActive()
                    mutable.update { it.copy(busy = false) }; close()
                } else {
                    val saved = service.updateDraft(row.id, row.version, requireNotNull(payload)); ensureActive()
                    val form = saved.examDraftForm()
                    persist(DraftLibraryState())
                    mutable.update { it.copy(selected = saved, form = form, baseline = form.payload(maxChoices), busy = false, message = R.string.exam_draft_saved) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val certain = !posting || (failure is ApiException && failure.statusCode in setOf(400, 403, 404, 409, 429))
                mutable.update { it.copy(busy = false, denied = it.denied || (failure is ApiException && failure.statusCode in setOf(403, 404)), error = failure, uncertain = if (certain) null else if (delete) "delete" else "save") }
            }
        }
    }
    class Factory(private val user: UserDto, private val service: MasterExamService, private val categories: CategoryService,
        private val maxChoices: Int, private val initialId: Long?, private val store: com.mukhtabir.android.core.SessionDraftStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExamDraftLibraryViewModel(user, service, categories, maxChoices, initialId, store) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDraftLibraryRoute(user: UserDto, service: MasterExamService, categories: CategoryService, maxChoices: Int,
    pageSize: Int, draftStore: com.mukhtabir.android.core.SessionDraftStore, initialId: Long? = null, onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit) {
    if (!user.has("master_exams.drafts_library")) { Text(stringResource(R.string.error_forbidden)); return }
    val vm: ExamDraftLibraryViewModel = viewModel(key = "exam-drafts-${user.id}-$initialId", factory = ExamDraftLibraryViewModel.Factory(user, service, categories, maxChoices, initialId, draftStore))
    val s by vm.state.collectAsLifecycleState()
    val recovery by vm.recovery.collectAsLifecycleState()
    val storageError by vm.recoveryError.collectAsLifecycleState()
    if (recovery != null) AppConfirmationDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.author_recover_title)) },
        text = { Text(stringResource(R.string.author_recover_body)) },
        confirmButton = { AppTextButton(vm::restoreDraft, enabled = !s.busy) { Text(stringResource(R.string.author_restore)) } },
        dismissButton = { AppDangerTextButton(vm::discardRecovery, enabled = !s.busy) { Text(stringResource(R.string.ux_discard_local_recovery)) } })
    var query by remember { mutableStateOf("") }
    var leave by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    LaunchedEffect(discarding, s.busy, s.selected) { if (discarding && !s.busy && s.selected == null) { discarding = false; if (initialId != null) onBack() } }
    val back: () -> Unit = { if (!s.busy) { if (s.uncertain != null || (s.form != null && s.form?.payload(maxChoices) != s.baseline)) leave = true else if (s.selected != null && initialId == null) vm.close() else onBack() } }
    BackHandler(onBack = back)
    DisposableEffect(s.busy) { onBusyChanged(s.busy); onDispose { onBusyChanged(false) } }
    if (leave) AppConfirmationDialog(onDismissRequest = { leave = false }, title = { Text(stringResource(R.string.unsaved_question_title)) },
        text = { Text(stringResource(if (s.uncertain != null) R.string.author_leave_unknown else R.string.unsaved_question_message)) }, confirmButton = { Column {
            AppTextButton({ leave = false; vm.keepDraft(onBack) }) { Text(stringResource(R.string.author_save_later)) }
            if (s.uncertain == null) AppDangerTextButton({ leave = false; discarding = true; vm.close() }) { Text(stringResource(R.string.ux_discard_question_changes)) }
        } },
        dismissButton = { AppTextButton({ leave = false }) { Text(stringResource(R.string.keep_editing)) } })
    if (deleting) AppConfirmationDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.ux_delete_private_question)) },
        text = { Text(stringResource(R.string.exam_library_delete_warning)) }, confirmButton = { AppDangerTextButton({ deleting = false; vm.delete() }) { Text(stringResource(R.string.ux_delete_private_question)) } },
        dismissButton = { AppTextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } })
    val validation = DraftValidationUi(if (s.invalid) s.form?.validationErrors(maxChoices).orEmpty() else emptyList(), s.validationAttempt)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(validation.attempt) { if (validation.errors.isNotEmpty()) listState.scrollToItem(if (s.review) 2 else 1) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), state = listState, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.exam_library_title))
            AppOutlinedButton(back, enabled = !s.busy) { Text(stringResource(R.string.back)) }
            if (s.busy) AppLoadingIndicator()
            storageError?.let { Text(stringResource(R.string.author_storage_failed)); ErrorState(throwable = it) }
            s.error?.let { ErrorState(throwable = it) }
            s.message?.let { Text(stringResource(it)) }
            if (s.denied) Text(stringResource(R.string.error_forbidden))
            if (s.invalid) DraftValidationSummary(validation)
            if (s.uncertain != null) {
                Text(stringResource(R.string.exam_tools_uncertain))
                AppOutlinedButton(vm::reconcile, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            }
        }
        if (s.selected != null) {
            if (s.review) item {
                Text(stringResource(R.string.question_conflict_message))
                s.latest?.let { ExamDraftFields(it.examDraftForm(), false, maxChoices, s.categories, true, change = { }) }
                AppOutlinedButton({ vm.acceptLatest(false) }, enabled = !s.busy) { Text(stringResource(R.string.use_server_question)) }
                AppOutlinedButton({ vm.acceptLatest(true) }, enabled = !s.busy) { Text(stringResource(R.string.keep_question_draft)) }
            }
            item {
                Text(stringResource(R.string.exam_library_preserve))
                s.form?.let { form -> ExamDraftFields(form, !s.denied && !s.busy && !s.review && s.uncertain == null, maxChoices, s.categories,
                    !s.selected?.case?.stem.isNullOrBlank() && form.caseKey == s.selected?.case?.key, vm::edit, validation = validation) }
                AppButton(vm::save, enabled = !s.denied && !s.busy && !s.review && s.uncertain == null && s.form?.payload(maxChoices) != s.baseline) { Text(stringResource(R.string.save)) }
                AppDangerOutlinedButton({ deleting = true }, enabled = !s.denied && !s.busy && !s.review && s.uncertain == null) { Text(stringResource(R.string.ux_delete_private_question)) }
            }
        } else {
            item {
                AppTextField(query, { query = it }, enabled = !s.busy, label = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth())
                AppOutlinedButton({ vm.search(query, s.usage) }, enabled = !s.busy) { Text(stringResource(R.string.search)) }
                listOf("" to R.string.all, "orphan" to R.string.exam_library_orphan, "attached" to R.string.exam_library_attached).forEach { (value, label) ->
                    AppFilterChip(s.usage == value, { vm.search(query, value) }, enabled = !s.busy, label = { Text(stringResource(label)) })
                }
                if (!s.busy && s.rows.isEmpty()) EmptyState(stringResource(R.string.master_empty))
            }
            val size = pageSize.coerceAtLeast(1)
            items(s.rows.drop((s.page - 1) * size).take(size), key = { it.id }) { row ->
                AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                    MarkdownText(row.question)
                    AppOutlinedButton({ vm.open(row.id) }, enabled = !s.busy) { Text(stringResource(R.string.exam_author_edit)) }
                } }
            }
            item {
                Text("${s.page} / ${((s.rows.size - 1).coerceAtLeast(0) / size) + 1}")
                AppOutlinedButton({ vm.page(s.page - 1, size) }, enabled = !s.busy && s.page > 1) { Text(stringResource(R.string.previous)) }
                AppOutlinedButton({ vm.page(s.page + 1, size) }, enabled = !s.busy && s.page * size < s.rows.size) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

@kotlinx.serialization.Serializable
internal data class StoredLibraryEditor(val selected: QuestionDto, val form: EmbeddedExamDraftForm,
    val baseline: MasterExamDraftWrite?, val uncertain: String? = null)
