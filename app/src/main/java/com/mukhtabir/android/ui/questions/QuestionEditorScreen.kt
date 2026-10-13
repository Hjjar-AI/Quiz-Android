package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.ExplanationCard

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.QuestionWriteRequest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto
import com.mukhtabir.android.data.remote.KnowledgeAssociationDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import com.mukhtabir.android.ui.components.ErrorState

enum class QuestionEditorValidationError {
    INVALID_FORM, TOO_MANY_CHOICES, TEXT_TOO_LONG, CHOICE_TOO_LONG, DUPLICATE_CHOICES,
    INVALID_ASSOCIATION, INVALID_SOURCE_PAGE, SOURCE_TOO_LONG, INVALID_LOCALE, TRANSLATION_REQUIRED, TRANSLATION_CHOICES,
}

// Identity, rather than a positional answer number, follows a choice as it moves.
@kotlinx.serialization.Serializable
data class QuestionChoiceDraft(val id: String = UUID.randomUUID().toString(), val text: String = "")

@kotlinx.serialization.Serializable
data class QuestionEditorState(
    val revisedDate: String = "",
    val needsReview: Boolean = false,
    val operationId: String? = null,
    @kotlinx.serialization.Transient val receiptMissing: Boolean = false,
    val reviewReady: Boolean = false,
    @kotlinx.serialization.Transient val reviewRows: List<QuestionDto> = emptyList(),
    val text: String = "",
    val choices: List<QuestionChoiceDraft> = List(2) { QuestionChoiceDraft() },
    val correctChoiceId: String? = null,
    val explanation: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val tagValidation: Int? = null,
    @kotlinx.serialization.Transient val tagSuggestions: List<String> = emptyList(),
    val tagsLoading: Boolean = false,
    @kotlinx.serialization.Transient val tagsError: Throwable? = null,
    val source: String = "",
    val sourceDocument: String = "",
    val sourcePage: String = "",
    val caseKey: String = "",
    val caseStem: String = "",
    val caseStemLocked: Boolean = false,
    val caseOrder: String = "",
    val knowledgeObject: Long? = null,
    val knowledgeTitle: String = "",
    val knowledgeObjective: String = "",
    val lookupKind: String? = null,
    val lookupQuery: String = "",
    val lookupBusy: Boolean = false,
    @kotlinx.serialization.Transient val lookupError: Throwable? = null,
    @kotlinx.serialization.Transient val caseResults: List<ClinicalCaseContextDto> = emptyList(),
    @kotlinx.serialization.Transient val knowledgeResults: List<KnowledgeAssociationDto> = emptyList(),
    val lookupTotal: Int = 0,
    @kotlinx.serialization.Transient val knowledgeCreation: KnowledgeCreationState? = null,
    val translations: Map<String, QuestionTranslationDraft> = emptyMap(),
    val difficulty: String = "medium",
    val category: Long? = null,
    @kotlinx.serialization.Transient val categories: List<CategoryDto> = emptyList(),
    val version: Int? = null,
    val maxChoices: Int = 8,
    val accessDenied: Boolean = false,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    @kotlinx.serialization.Transient val error: Throwable? = null,
    @kotlinx.serialization.Transient val validationError: QuestionEditorValidationError? = null,
    @kotlinx.serialization.Transient val validationAttempt: Int = 0,
    val savedId: Long? = null,
    val dirty: Boolean = false,
    val conflict: Boolean = false,
    @kotlinx.serialization.Transient val latest: QuestionDto? = null,
)

class QuestionEditorViewModel(
    private val id: Long?,
    private val questions: QuestionService,
    private val categories: CategoryService,
    private val userId: Long,
    private val canCreate: Boolean,
    private val canEditOwn: Boolean,
    private val canEditAny: Boolean,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionEditorState())
    val state = _state.asStateFlow()
    private var baseline: QuestionWriteRequest? = null
    private fun draft(s: QuestionEditorState) = QuestionWriteRequest(
        question = s.text, choices = s.choices.map { it.text },
        correctAnswer = s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1,
        explanation = s.explanation, tags = s.tags, difficulty = s.difficulty, category = s.category,
        knowledgeObject=s.knowledgeObject, caseKey=s.caseKey.trim().takeIf(String::isNotBlank),
        caseStem=s.caseStem.trim().takeIf(String::isNotBlank), caseOrder=s.caseOrder.trim().toIntOrNull(),
        source=s.source, sourceDocument=s.sourceDocument, sourcePage=s.sourcePage.trim().toIntOrNull(),
        translations=translationPayload(s.translations,s.choices), lastRevisedAt=s.revisedDate.takeIf(String::isNotBlank),
    )
    private var baselinePage = ""
    private var baselineOrder = ""
    private var lookupJob: Job? = null
    private val recoveryMutex = Mutex()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = "question-${id ?: "new"}"
    private var unreadableRecovery = false
    private val pendingRecovery = MutableStateFlow<StoredQuestionEditor?>(null)
    internal val recovery = pendingRecovery.asStateFlow()
    private val storageFailure = MutableStateFlow<Throwable?>(null)
    val recoveryError = storageFailure.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.readEditor(userId, storageKey).getOrThrow() }
                coroutineContext.ensureActive()
                pendingRecovery.value = raw?.let { json.decodeFromString(StoredQuestionEditor.serializer(), it) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { unreadableRecovery = true; storageFailure.value = failure }
            load()
        }
        viewModelScope.launch {
            state.collect { value ->
                if (value.loaded && pendingRecovery.value == null && !value.loading && !value.accessDenied && (!unreadableRecovery || value.dirty || value.needsReview)) persist(value)
            }
        }
    }
    private suspend fun persist(s: QuestionEditorState, posting: Boolean = false) = recoveryMutex.withLock {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!s.dirty && !posting && !s.needsReview) store.clearEditor(userId, storageKey)
            else store.writeEditor(userId, storageKey, json.encodeToString(StoredQuestionEditor.serializer(),
                StoredQuestionEditor(s.copy(lookupKind = null), baseline, baselinePage, baselineOrder, posting)))
        }
        coroutineContext.ensureActive()
        storageFailure.value = result.exceptionOrNull()
        if (result.isSuccess) unreadableRecovery = false
    }
    fun restoreDraft() {
        val saved = pendingRecovery.value ?: return
        val current = _state.value
        if (!current.loaded || current.loading || current.accessDenied) return
        pendingRecovery.value = null
        val stored = saved.state
        _state.value = stored.copy(loaded = true, loading = false, categories = current.categories, maxChoices = current.maxChoices,
            accessDenied = current.accessDenied, dirty = true, savedId = null, error = null, lookupKind = null, knowledgeCreation = null,
            needsReview = stored.needsReview || saved.posting, reviewReady = false, reviewRows = emptyList(),
            conflict = id != null && current.version != stored.version, latest = null,
            caseStemLocked = current.caseKey == stored.caseKey && current.caseStemLocked)
        baseline = saved.baseline
        baselinePage = saved.baselinePage
        baselineOrder = saved.baselineOrder
        if (_state.value.conflict && !_state.value.needsReview) fetchLatest()
    }
    fun discardRecovery() {
        if (_state.value.loading) return
        viewModelScope.launch {
            val result = recoveryMutex.withLock { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.clearEditor(userId, storageKey) } }
            coroutineContext.ensureActive()
            if (result.isSuccess) pendingRecovery.value = null else storageFailure.value = result.exceptionOrNull()
        }
    }
    fun keepDraft(onDone: () -> Unit) {
        val s = _state.value
        if (s.loading || !s.loaded) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            persist(s)
            _state.update { it.copy(loading = false) }
            if (storageFailure.value == null) onDone()
        }
    }
    fun discardDraft(onDone: () -> Unit) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = recoveryMutex.withLock { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.clearEditor(userId, storageKey) } }
            coroutineContext.ensureActive()
            _state.update { it.copy(loading = false, dirty = if (result.isSuccess) false else it.dirty, needsReview = if (result.isSuccess) false else it.needsReview, error = result.exceptionOrNull()) }
            if (result.isSuccess) onDone()
        }
    }
    fun reviewWrite() {
        val s = _state.value
        if (!s.needsReview || s.loading || s.accessDenied) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                if (id != null) {
                    val fresh = questions.get(id)
                    coroutineContext.ensureActive()
                    if (fresh.answersHidden || !(canEditAny || (canEditOwn && fresh.ownedBy == userId))) throw ApiException(403, "")
                    _state.update { it.copy(loading = false, latest = fresh, conflict = true, reviewReady = true) }
                } else if (s.operationId != null) {
                    val confirmed = questions.createReceipt(s.operationId)
                    coroutineContext.ensureActive()
                    persist(s.copy(dirty = false, needsReview = false))
                    _state.update { it.copy(loading = false, savedId = confirmed.id, dirty = false, needsReview = false) }
                } else {
                    val rows = questions.list(search = s.text.trim().take(120)).items.filter { it.ownedBy == userId &&
                        it.question.trim() == s.text.trim() && it.choices == s.choices.map { choice -> choice.text.trim() } &&
                        it.correctAnswer == s.choices.indexOfFirst { choice -> choice.id == s.correctChoiceId } + 1 }
                    coroutineContext.ensureActive()
                    _state.update { it.copy(loading = false, reviewRows = rows, reviewReady = true) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(loading = false, error = failure, receiptMissing = id == null && s.operationId != null && failure is ApiException && failure.statusCode == 404) } }
        }
    }
    fun retryCreateReceipt() {
        val s = _state.value
        if (id != null || !s.receiptMissing || s.operationId == null || s.loading || !canCreate) return
        _state.update { it.copy(needsReview = false, receiptMissing = false) }
        save()
    }
    fun acknowledgeWrite() {
        if (_state.value.needsReview && _state.value.reviewReady && !_state.value.loading && id == null)
            _state.update { it.copy(needsReview = false, reviewReady = false, reviewRows = emptyList()) }
    }
    fun useExisting(id: Long) {
        if (_state.value.loading || !_state.value.reviewReady || _state.value.reviewRows.none { it.id == id && it.ownedBy == userId }) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            persist(_state.value.copy(dirty = false, needsReview = false))
            _state.update { it.copy(loading = false, savedId = id, dirty = false, needsReview = false) }
        }
    }


    fun load() {
        if (_state.value.loaded || _state.value.accessDenied || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val (cats, question) = coroutineScope {
                    val cats = async { categories.list() }
                    val question = if (id != null) async { questions.get(id) } else null
                    cats.await() to question?.await()
                }
                coroutineContext.ensureActive()
                val recent=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {store.authorNavigation(userId).getOrDefault(com.mukhtabir.android.core.AuthorNavigation())}
                val permitted = if (id == null) canCreate else
                    canEditAny || (canEditOwn && question?.ownedBy == userId)
                val choices = question?.choices?.map { QuestionChoiceDraft(text = it) }
                    ?: List(2) { QuestionChoiceDraft() }
                _state.update { previous ->
                    QuestionEditorState(
                        text = question?.question.orEmpty(), choices = choices,
                        correctChoiceId = if (question != null)
                            choices.getOrNull((question.correctAnswer ?: 0) - 1)?.id else null,
                        explanation = question?.explanation.orEmpty(),
                        tags = question?.tags.orEmpty(),
                        difficulty = question?.difficulty ?: "medium", category = question?.category,
                        categories = cats.items.sortedBy {recent.categories.indexOf(it.id).takeIf {index->index>=0} ?: Int.MAX_VALUE}, version = question?.version,
                        maxChoices = previous.maxChoices, accessDenied = !permitted || question?.answersHidden==true,
                        revisedDate = question?.lastRevisedAt.orEmpty(),
                    loaded = true, loading = false,
                        caseKey=question?.case?.key.orEmpty(),caseStem=question?.case?.stem.orEmpty(),
                        caseStemLocked=!question?.case?.stem.isNullOrBlank(),caseOrder=question?.caseOrder?.toString().orEmpty(),
                        knowledgeObject=question?.knowledgeObject,knowledgeTitle=question?.knowledgeObjectTitle.orEmpty(),
                        knowledgeObjective=question?.knowledgeObjectObjective.orEmpty(),
                        source=question?.source.orEmpty(), sourceDocument=question?.sourceDocument.orEmpty(),
                        sourcePage=question?.sourcePage?.toString().orEmpty(),
                        translations=translationDrafts(question?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),choices),
                    )
                }
                baseline = draft(_state.value)
                baselinePage = _state.value.sourcePage
                baselineOrder = _state.value.caseOrder
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) { _state.update { it.copy(loading=false,error=failure) } }
        }
    }

    private fun edit(transform: (QuestionEditorState) -> QuestionEditorState) = _state.update {
        if (!it.loaded || it.loading || it.accessDenied || it.savedId != null || it.conflict || it.needsReview || pendingRecovery.value != null || it.knowledgeCreation!=null) it
        else transform(it).let { edited ->
            edited.copy(dirty = draft(edited) != baseline || edited.sourcePage != baselinePage || edited.caseOrder != baselineOrder || edited.tagInput.isNotBlank(), validationError = null, error = null)
        }
    }

    fun field(name: String, value: String) = edit {
        when (name) {
            "revisedDate" -> it.copy(revisedDate = value)
            "text" -> it.copy(text = value)
            "explanation" -> it.copy(explanation = value)
            "tagInput" -> it.copy(tagInput=value,tagValidation=null)
            "source" -> it.copy(source=value)
            "sourceDocument" -> it.copy(sourceDocument=value)
            "sourcePage" -> it.copy(sourcePage=value)
            "caseKey" -> if(value==it.caseKey) it else it.copy(caseKey=value,caseStem="",caseStemLocked=false,caseOrder="")
            "caseStem" -> if(it.caseStemLocked) it else it.copy(caseStem=value)
            "caseOrder" -> it.copy(caseOrder=value)
            "difficulty" -> if (value in listOf("easy", "medium", "hard")) it.copy(difficulty = value) else it
            else -> it
        }
    }
    fun addTag(value: String = _state.value.tagInput) = edit { state ->
        val name=value.trim()
        when {
            name.isEmpty() -> state.copy(tagValidation=R.string.editor_tag_required)
            name.codePointCount(0,name.length)>50 -> state.copy(tagValidation=R.string.editor_tag_too_long)
            else -> state.copy(tags=(state.tags+name).distinct(),tagInput="",tagValidation=null)
        }
    }
    fun removeTag(name: String) = edit { it.copy(tags=it.tags-name,tagValidation=null) }
    fun clearTags() = edit { it.copy(tags=emptyList(),tagValidation=null) }
    fun refreshTags() {
        val s=_state.value
        if(!s.loaded || s.loading || s.accessDenied || s.conflict || s.savedId!=null || s.tagsLoading) return
        _state.update { it.copy(tagsLoading=true,tagsError=null) }
        viewModelScope.launch {
            try {
                val result=questions.tags()
                val recent=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {store.authorNavigation(userId).getOrDefault(com.mukhtabir.android.core.AuthorNavigation())}
                coroutineContext.ensureActive()
                _state.update { it.copy(tagsLoading=false,tagSuggestions=result.items.map { tag -> tag.name }.distinct().sortedBy {recent.tags.indexOf(it).takeIf {index->index>=0} ?: Int.MAX_VALUE}) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(tagsLoading=false,tagsError=failure) }
            }
        }
    }
    fun detachCase() = edit { it.copy(caseKey="",caseStem="",caseStemLocked=false,caseOrder="") }
    fun clearKnowledge() = edit { it.copy(knowledgeObject=null,knowledgeTitle="",knowledgeObjective="") }
    fun selectCase(value: ClinicalCaseContextDto) {
        if(_state.value.lookupKind!="case" || _state.value.lookupBusy) return
        edit { it.copy(caseKey=value.key.orEmpty(),caseStem=value.stem.orEmpty(),
            caseStemLocked=!value.stem.isNullOrBlank(),caseOrder=if(it.caseKey==value.key) it.caseOrder else "") }
        closeLookup()
    }
    fun selectKnowledge(value: KnowledgeAssociationDto) {
        if(_state.value.lookupKind!="knowledge" || _state.value.lookupBusy) return
        edit { it.copy(knowledgeObject=value.id,knowledgeTitle=value.title,knowledgeObjective=value.learningObjective) }
        closeLookup()
    }
    fun openLookup(kind: String) {
        val s=_state.value
        if(kind !in listOf("case","knowledge") || !s.loaded || s.loading || s.accessDenied || s.conflict || s.savedId!=null || s.lookupKind!=null || s.knowledgeCreation!=null) return
        _state.update { it.copy(lookupKind=kind,lookupQuery="",caseResults=emptyList(),knowledgeResults=emptyList(),lookupTotal=0) }
        searchLookup("")
    }
    fun closeLookup() {
        lookupJob?.cancel()
        _state.update { it.copy(lookupKind=null,lookupBusy=false,lookupError=null) }
    }
    fun searchLookup(query: String) {
        val kind=_state.value.lookupKind ?: return
        if(_state.value.loading || _state.value.accessDenied || _state.value.conflict) return
        lookupJob?.cancel()
        _state.update { it.copy(lookupBusy=true,lookupError=null,lookupQuery=query,caseResults=emptyList(),knowledgeResults=emptyList(),lookupTotal=0) }
        lookupJob=viewModelScope.launch {
            try {
                if(kind=="case") {
                    val result=questions.cases(query.trim())
                    coroutineContext.ensureActive()
                    _state.update { it.copy(lookupBusy=false,caseResults=result.items,lookupTotal=result.total) }
                } else {
                    val result=questions.knowledgeAssociations(query.trim())
                    coroutineContext.ensureActive()
                    _state.update { it.copy(lookupBusy=false,knowledgeResults=result.items,lookupTotal=result.count) }
                }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(lookupBusy=false,lookupError=failure) } }
        }
    }
    val canCreateKnowledge: Boolean get() = canCreate || canEditAny
    fun openKnowledgeCreator() {
        val s=_state.value
        if(!canCreateKnowledge || !s.loaded || s.loading || s.accessDenied || s.conflict || s.savedId!=null || s.lookupKind!=null || s.knowledgeCreation!=null) return
        _state.update { it.copy(knowledgeCreation=KnowledgeCreationState()) }
    }
    fun closeKnowledgeCreator() {
        if(!_state.value.loading) _state.update { it.copy(knowledgeCreation=null) }
    }
    fun knowledgeCreationField(field: String, value: String) {
        val s=_state.value
        val draft=s.knowledgeCreation ?: return
        if(!canCreateKnowledge || s.loading || s.accessDenied || s.conflict) return
        val changed=when(field) {
            "title" -> draft.copy(title=value)
            "objective" -> draft.copy(objective=value)
            "answer" -> draft.copy(answer=value)
            "facts" -> draft.copy(facts=value)
            else -> return
        }
        // Keep review required after uncertain POST even when the user edits its draft.
        _state.update { it.copy(knowledgeCreation=changed.copy(validation=null,reviewed=false,matches=emptyList(),matchCount=0)) }
    }
    fun createKnowledge() {
        val s=_state.value
        val draft=s.knowledgeCreation ?: return
        if(!canCreateKnowledge || !s.loaded || s.loading || s.accessDenied || s.conflict || s.savedId!=null || draft.needsReview) return
        val title=draft.title.trim()
        val objective=draft.objective.trim()
        val answer=draft.answer.trim()
        val validation=when {
            title.isBlank() || objective.isBlank() -> R.string.knowledge_create_required
            title.codePointCount(0,title.length)>200 || objective.codePointCount(0,objective.length)>3000 ||
                answer.codePointCount(0,answer.length)>3000 -> R.string.knowledge_create_length
            else -> null
        }
        if(validation!=null) {
            _state.update { it.copy(knowledgeCreation=draft.copy(validation=validation)) };return
        }
        _state.update { it.copy(loading=true,knowledgeCreation=draft.copy(error=null,validation=null,matches=emptyList(),reviewed=false)) }
        viewModelScope.launch {
            try {
                val created=questions.createKnowledge(title,objective,answer,draft.facts.lineSequence().map(String::trim)
                    .filter(String::isNotBlank).toList(),s.category)
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,knowledgeCreation=null) }
                edit { it.copy(knowledgeObject=created.id,knowledgeTitle=created.title,knowledgeObjective=created.learningObjective) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,knowledgeCreation=draft.copy(error=failure,needsReview=true,reviewed=false)) }
            }
        }
    }
    fun reviewKnowledgeCreation() {
        val s=_state.value
        val draft=s.knowledgeCreation ?: return
        if(!canCreateKnowledge || s.loading || s.accessDenied || s.conflict || s.savedId!=null) return
        _state.update { it.copy(loading=true,knowledgeCreation=draft.copy(error=null,validation=null)) }
        viewModelScope.launch {
            try {
                val result=questions.knowledgeAssociations(draft.title.trim())
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,knowledgeCreation=draft.copy(needsReview=false,reviewed=true,
                    matches=result.items,matchCount=result.count,error=null)) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,knowledgeCreation=draft.copy(error=failure,needsReview=true,reviewed=false)) }
            }
        }
    }
    fun attachReviewedKnowledge(id: Long) {
        val s=_state.value
        val draft=s.knowledgeCreation ?: return
        if(!canCreateKnowledge || s.loading || s.accessDenied || s.conflict || s.savedId!=null || !draft.reviewed || draft.needsReview) return
        val item=draft.matches.firstOrNull { it.id==id } ?: return
        _state.update { it.copy(knowledgeCreation=null) }
        edit { it.copy(knowledgeObject=item.id,knowledgeTitle=item.title,knowledgeObjective=item.learningObjective) }
    }
    fun addTranslation(value: String) {
        val locale=normalizedTranslationLocale(value)
        if(locale.isBlank()) {
            _state.update { if(it.loaded && !it.loading && !it.accessDenied && !it.conflict && it.savedId==null)
                it.copy(validationError=QuestionEditorValidationError.INVALID_LOCALE, validationAttempt=it.validationAttempt+1) else it }
            return
        }
        edit { state -> if(locale in state.translations) state else state.copy(translations=state.translations + (locale to QuestionTranslationDraft())) }
    }
    fun translationField(locale: String, field: String, value: String) = edit { state ->
        val translation=state.translations[locale] ?: return@edit state
        state.copy(translations=state.translations + (locale to when(field) {
            "question" -> translation.copy(question=value)
            "explanation" -> translation.copy(explanation=value)
            else -> translation
        }))
    }
    fun translationChoice(locale: String, choiceId: String, value: String) = edit { state ->
        val translation=state.translations[locale] ?: return@edit state
        if(state.choices.none { it.id==choiceId }) state else
            state.copy(translations=state.translations + (locale to translation.copy(choices=translation.choices + (choiceId to value))))
    }
    fun removeTranslation(locale: String) = edit { it.copy(translations=it.translations-locale) }
    fun alignTranslation(locale: String) = edit { state ->
        val translation=state.translations[locale] ?: return@edit state
        state.copy(translations=state.translations + (locale to translation.copy(extraChoices=emptyList())))
    }
    fun category(id: Long?) = edit { it.copy(category = id) }
    fun applyMaxChoices(value: Int) = _state.update { it.copy(maxChoices = value.coerceAtLeast(2)) }
    fun choice(id: String, text: String) = edit {
        it.copy(choices = it.choices.map { choice -> if (choice.id == id) choice.copy(text = text) else choice })
    }
    fun selectCorrect(id: String) = edit {
        if (it.choices.any { choice -> choice.id == id }) it.copy(correctChoiceId = id) else it
    }
    fun addChoice() = edit {
        if (it.choices.size < it.maxChoices) it.copy(choices = it.choices + QuestionChoiceDraft()) else it
    }
    fun removeChoice(id: String) = edit {
        if (it.choices.size <= 2) it else it.copy(
            choices = it.choices.filterNot { choice -> choice.id == id },
            correctChoiceId = it.correctChoiceId.takeUnless { selected -> selected == id },
            translations=it.translations.mapValues { (_,translation) -> translation.copy(choices=translation.choices-id) },
        )
    }
    fun moveChoice(id: String, offset: Int) = edit { state ->
        val from = state.choices.indexOfFirst { it.id == id }
        val to = from + offset
        if (from < 0 || to !in state.choices.indices) state else {
            val choices = state.choices.toMutableList()
            choices.add(to, choices.removeAt(from))
            state.copy(choices = choices)
        }
    }

    fun fetchLatest() {
        if (id == null || !_state.value.conflict || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val latest = questions.get(id)
                coroutineContext.ensureActive()
                val permitted = !latest.answersHidden && (canEditAny || (canEditOwn && latest.ownedBy == userId))
                _state.update { it.copy(loading = false, latest = latest, accessDenied = !permitted) }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _state.update { it.copy(loading = false, error = failure) }
            }
        }
    }

    fun resolveConflict(keepDraft: Boolean) {
        val s = _state.value
        val latest = s.latest ?: return
        if (s.loading || s.accessDenied || !s.conflict) return
        val choices = latest.choices.map { QuestionChoiceDraft(text = it) }
        val server = s.copy(text = latest.question, choices = choices, revisedDate = latest.lastRevisedAt.orEmpty(), needsReview = false, reviewReady = false,
            correctChoiceId = choices.getOrNull((latest.correctAnswer ?: 0) - 1)?.id,
            explanation = latest.explanation.orEmpty(), tags = latest.tags,
            tagInput="",tagValidation=null,
            difficulty = latest.difficulty, category = latest.category, version = latest.version,
            caseKey=latest.case?.key.orEmpty(),caseStem=latest.case?.stem.orEmpty(),caseStemLocked=!latest.case?.stem.isNullOrBlank(),
            caseOrder=latest.caseOrder?.toString().orEmpty(),knowledgeObject=latest.knowledgeObject,
            knowledgeTitle=latest.knowledgeObjectTitle.orEmpty(),knowledgeObjective=latest.knowledgeObjectObjective.orEmpty(),
            source=latest.source.orEmpty(),sourceDocument=latest.sourceDocument.orEmpty(),sourcePage=latest.sourcePage?.toString().orEmpty(),
            translations=translationDrafts(latest.translations,choices),
            conflict = false, latest = null, dirty = false, error = null, validationError = null)
        baseline = draft(server)
        baselinePage = server.sourcePage
        baselineOrder = server.caseOrder
        _state.value = if (keepDraft) s.copy(version = latest.version, conflict = false,
            latest = null, needsReview = false, reviewReady = false, dirty = draft(s) != baseline || s.sourcePage != baselinePage || s.caseOrder != baselineOrder || s.tagInput.isNotBlank(), error = null, validationError = null) else server
    }

    fun save() {
        val s = _state.value
        if (!s.loaded || s.loading || s.accessDenied || s.savedId != null || s.conflict || s.needsReview || pendingRecovery.value != null || s.lookupKind!=null || s.knowledgeCreation!=null) return
        if(s.tagInput.isNotBlank()) {
            _state.update { it.copy(tagValidation=R.string.editor_tag_pending) };return
        }
        val choices = s.choices.map { it.text.trim() }
        val correct = s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1
        val activeTranslations=s.translations.values.filter { t ->
            t.question.isNotBlank() || t.explanation.isNotBlank() || translatedChoices(t,s.choices).any(String::isNotBlank) || t.extraChoices.isNotEmpty()
        }
        val error = when {
            choices.size > s.maxChoices -> QuestionEditorValidationError.TOO_MANY_CHOICES
            s.text.isBlank() || choices.size < 2 || choices.any(String::isBlank) || correct == 0 ->
                QuestionEditorValidationError.INVALID_FORM
            s.text.length > 3000 || s.explanation.length > 3000 -> QuestionEditorValidationError.TEXT_TOO_LONG
            choices.any { it.length > 300 } -> QuestionEditorValidationError.CHOICE_TOO_LONG
            choices.map { it.lowercase(Locale.ROOT) }.distinct().size != choices.size ->
                QuestionEditorValidationError.DUPLICATE_CHOICES
            s.revisedDate.isNotBlank() && runCatching { java.time.LocalDate.parse(s.revisedDate) }.isFailure -> QuestionEditorValidationError.INVALID_FORM
            s.difficulty !in listOf("easy", "medium", "hard") -> QuestionEditorValidationError.INVALID_FORM
            s.sourcePage.isNotBlank() && (s.sourcePage.trim().toIntOrNull()?.let { it>0 } != true) -> QuestionEditorValidationError.INVALID_SOURCE_PAGE
            s.caseKey.trim().length>64 || s.caseStem.length>3000 ||
                (s.caseKey.isBlank() && (s.caseStem.isNotBlank() || s.caseOrder.isNotBlank())) ||
                (s.caseOrder.isNotBlank() && s.caseOrder.trim().toIntOrNull()?.let { it>0 }!=true) -> QuestionEditorValidationError.INVALID_ASSOCIATION
            s.source.length>200 || s.sourceDocument.length>500 -> QuestionEditorValidationError.SOURCE_TOO_LONG
            activeTranslations.any { it.question.isBlank() } -> QuestionEditorValidationError.TRANSLATION_REQUIRED
            activeTranslations.any { t -> t.extraChoices.isNotEmpty() || translatedChoices(t,s.choices).let { texts -> texts.any(String::isNotBlank) && texts.any(String::isBlank) } } -> QuestionEditorValidationError.TRANSLATION_CHOICES
            activeTranslations.any { it.question.length>3000 || it.explanation.length>3000 } -> QuestionEditorValidationError.TEXT_TOO_LONG
            activeTranslations.any { translatedChoices(it,s.choices).any { choice -> choice.length>300 } } -> QuestionEditorValidationError.CHOICE_TOO_LONG
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = null, validationError = error, validationAttempt = it.validationAttempt + 1) }
            return
        }
        // Set this before launching to reject repeated taps synchronously.
        _state.update { it.copy(loading = true, error = null, validationError = null) }
        viewModelScope.launch {
            val postingState = if (id == null) s.copy(operationId = s.operationId ?: UUID.randomUUID().toString()) else s
            _state.update { it.copy(operationId = postingState.operationId) }
            persist(postingState, posting = true)
            if (storageFailure.value != null) { _state.update { it.copy(loading = false, error = storageFailure.value) }; return@launch }
            val request = draft(s).copy(question=s.text.trim(),choices=choices,correctAnswer=correct,
                writeTags=id==null || s.tags!=baseline?.tags,
                lastRevisedAt=s.revisedDate.takeIf { it.isNotBlank() && (id == null || it != baseline?.lastRevisedAt) },
                explanation=s.explanation.takeIf(String::isNotBlank),expectedVersion=s.version,
                source=s.source.trim(),sourceDocument=s.sourceDocument.trim())
            runCatching { if (id == null) questions.create(request, postingState.operationId) else questions.update(id, request) }
                .onSuccess { question -> coroutineContext.ensureActive(); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {store.recordAuthorChoices(userId,question.category,question.tags);store.recordLastQuestion(userId,question.id)}; persist(s.copy(dirty = false, needsReview = false)); _state.update { it.copy(loading = false, savedId = question.id, dirty = false) } }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    coroutineContext.ensureActive()
                    val conflict = id != null && failure is ApiException && failure.statusCode == 409
                    if (failure is ApiException && failure.statusCode in setOf(400, 403, 404, 409, 429)) persist(s)
                    _state.update { it.copy(loading = false, error = if (conflict) null else failure,
                        accessDenied = it.accessDenied || (failure is ApiException && failure.statusCode in setOf(403, 404)),
                        conflict = conflict, latest = null, needsReview = !(failure is ApiException && failure.statusCode in setOf(400, 403, 404, 409, 429)) || (id == null && failure.statusCode == 409), reviewReady = false) }
                    if (conflict) fetchLatest()
                }
        }
    }

    class Factory(
        private val id: Long?, private val q: QuestionService, private val c: CategoryService,
        private val userId: Long, private val canCreate: Boolean,
        private val canEditOwn: Boolean, private val canEditAny: Boolean, private val store: com.mukhtabir.android.core.SessionDraftStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuestionEditorViewModel(id, q, c, userId, canCreate, canEditOwn, canEditAny, store) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuestionEditorRoute(
    questionId: Long?, questionService: QuestionService, categoryService: CategoryService,
    maxChoices: Int, userId: Long, canCreate: Boolean, canEditOwn: Boolean,
    canEditAny: Boolean, draftStore: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onSaved: (Long) -> Unit,
) {
    val vm: QuestionEditorViewModel = viewModel(
        key = "editor-$questionId-$userId",
        factory = QuestionEditorViewModel.Factory(questionId, questionService, categoryService,
            userId, canCreate, canEditOwn, canEditAny, draftStore),
    )
    val s by vm.state.collectAsState()
    val recovery by vm.recovery.collectAsState()
    val storageError by vm.recoveryError.collectAsState()
    if (recovery != null && s.loaded) AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.author_recover_title)) },
        text = { Text(stringResource(R.string.author_recover_body)) },
        confirmButton = { TextButton(vm::restoreDraft, enabled = !s.loading && !s.accessDenied) { Text(stringResource(R.string.author_restore)) } },
        dismissButton = { AppDangerTextButton(vm::discardRecovery, enabled = !s.loading) { Text(stringResource(R.string.discard)) } })
    LaunchedEffect(maxChoices) { vm.applyMaxChoices(maxChoices) }
    LaunchedEffect(s.loaded) { if(s.loaded) vm.refreshTags() }
    LaunchedEffect(s.savedId) { s.savedId?.let(onSaved) }
    var showLeave by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val leave = { if (s.dirty) showLeave = true else onBack() }
    BackHandler(enabled = s.savedId == null) { if (!s.loading) leave() }
    if (showLeave) AlertDialog(
        onDismissRequest = { showLeave = false },
        title = { Text(stringResource(R.string.unsaved_question_title)) },
        text = { Text(stringResource(if (s.needsReview) R.string.author_leave_unknown else R.string.unsaved_question_message)) },
        confirmButton = { Column {
            TextButton({ showLeave = false; vm.keepDraft(onBack) }, enabled = !s.loading) { Text(stringResource(R.string.author_save_later)) }
            AppDangerTextButton({ showLeave = false; vm.discardDraft(onBack) }, enabled = !s.loading) { Text(stringResource(R.string.discard)) }
        } },
        dismissButton = { TextButton({ showLeave = false }) { Text(stringResource(R.string.keep_editing)) } },
    )
    if (!s.loading && s.accessDenied) {
        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets)) {
            Text(stringResource(R.string.error_forbidden), color = MaterialTheme.colorScheme.error)
            TextButton(leave) { Text(stringResource(R.string.cancel)) }
        }
        return
    }
    val enabled = s.loaded && !s.loading && s.savedId == null && !s.conflict && !s.needsReview && recovery == null
    QuestionAssociationPicker(s,vm)
    QuestionKnowledgeCreator(s,vm)
    var localeInput by rememberSaveable { mutableStateOf("") }
    var removeTranslation by rememberSaveable { mutableStateOf<String?>(null) }
    var alignTranslation by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingLocale=removeTranslation ?: alignTranslation
    if(pendingLocale!=null) AlertDialog(
        onDismissRequest={if(!s.loading) { removeTranslation=null;alignTranslation=null }},
        title={Text(stringResource(R.string.editor_translation_action,pendingLocale))},
        text={Text(stringResource(if(removeTranslation!=null) R.string.editor_translation_remove_confirm else R.string.editor_translation_align_confirm))},
        confirmButton={TextButton({
            if(removeTranslation!=null) vm.removeTranslation(pendingLocale) else vm.alignTranslation(pendingLocale)
            removeTranslation=null;alignTranslation=null
        },enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({removeTranslation=null;alignTranslation=null},enabled=!s.loading){Text(stringResource(R.string.cancel))}},
    )
    var categoryMenu by remember { mutableStateOf(false) }
    val validationMessage = s.validationError?.let {
        when (it) {
                QuestionEditorValidationError.INVALID_ASSOCIATION -> stringResource(R.string.editor_association_invalid)
                QuestionEditorValidationError.INVALID_FORM -> stringResource(R.string.invalid_question_form)
                QuestionEditorValidationError.TOO_MANY_CHOICES -> stringResource(R.string.too_many_choices, s.maxChoices)
                QuestionEditorValidationError.TEXT_TOO_LONG -> stringResource(R.string.question_text_too_long)
                QuestionEditorValidationError.CHOICE_TOO_LONG -> stringResource(R.string.choice_text_too_long)
                QuestionEditorValidationError.DUPLICATE_CHOICES -> stringResource(R.string.duplicate_choices)
                QuestionEditorValidationError.INVALID_SOURCE_PAGE -> stringResource(R.string.editor_source_page_invalid)
                QuestionEditorValidationError.SOURCE_TOO_LONG -> stringResource(R.string.editor_source_length)
                QuestionEditorValidationError.INVALID_LOCALE -> stringResource(R.string.editor_translation_locale_invalid)
                QuestionEditorValidationError.TRANSLATION_REQUIRED -> stringResource(R.string.editor_translation_question_required)
                QuestionEditorValidationError.TRANSLATION_CHOICES -> stringResource(R.string.editor_translation_choices_required)
            }
    }
    val fieldError: (Boolean) -> String? = { invalid -> validationMessage.takeIf { invalid } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(if (questionId == null) R.string.create_question else R.string.edit_question))
        TextButton(leave, enabled = !s.loading) { Text(stringResource(R.string.cancel)) }
        if (s.loading) AppLoadingIndicator()
        if (s.conflict) {
            Text(stringResource(R.string.question_conflict_message), color = MaterialTheme.colorScheme.error)
            s.latest?.let { latest ->
                Text(stringResource(R.string.server_question_version, latest.version), style = MaterialTheme.typography.titleMedium)
                QuestionDraftPreview(latest.question, latest.choices, latest.correctAnswer, latest.explanation.orEmpty())
                Text(stringResource(R.string.question_metadata_comparison,
                    latest.tags.joinToString(" · "), latest.difficulty,
                    s.categories.firstOrNull { it.id == latest.category }?.name ?: latest.category?.toString().orEmpty()))
                QuestionExtendedPreview(latest)
                Text(stringResource(R.string.your_question_draft), style = MaterialTheme.typography.titleMedium)
                QuestionDraftPreview(s.text, s.choices.map { it.text }, s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1, s.explanation)
                Text(stringResource(R.string.question_metadata_comparison, s.tags.joinToString(" · "), s.difficulty,
                    s.categories.firstOrNull { it.id == s.category }?.name ?: s.category?.toString().orEmpty()))
                QuestionExtendedPreview(QuestionDto(id=0,question=s.text,choices=s.choices.map { it.text },
                    source=s.source,sourceDocument=s.sourceDocument,sourcePage=s.sourcePage.toIntOrNull(),
                    knowledgeObject=s.knowledgeObject,knowledgeObjectTitle=s.knowledgeTitle,
                    knowledgeObjectObjective=s.knowledgeObjective,
                    case=ClinicalCaseContextDto(key=s.caseKey,stem=s.caseStem),caseOrder=s.caseOrder.toIntOrNull(),
                    translations=translationPayload(s.translations,s.choices)))
                Text(stringResource(R.string.question_conflict_resolution))
                OutlinedButton({ vm.resolveConflict(false) }, enabled = !s.loading) { Text(stringResource(R.string.use_server_question)) }
                OutlinedButton({ vm.resolveConflict(true) }, enabled = !s.loading) { Text(stringResource(R.string.keep_question_draft)) }
            } ?: OutlinedButton(vm::fetchLatest, enabled = !s.loading) { Text(stringResource(R.string.retry)) }
        }
        storageError?.let { Text(stringResource(R.string.author_storage_failed)); ErrorState(throwable = it) }
        if (s.needsReview) {
            Text(stringResource(R.string.author_write_unknown))
            OutlinedButton(vm::reviewWrite, enabled = !s.loading) { Text(stringResource(R.string.refresh)) }
            if (s.receiptMissing) OutlinedButton(vm::retryCreateReceipt, enabled = !s.loading) { Text(stringResource(R.string.retry_same_request)) }
            s.reviewRows.forEach { row ->
                QuestionDraftPreview(row.question, row.choices, row.correctAnswer ?: 0, row.explanation.orEmpty())
                OutlinedButton({ vm.useExisting(row.id) }, enabled = !s.loading) { Text(stringResource(R.string.author_use_existing)) }
            }
            if (s.reviewReady && questionId == null) OutlinedButton(vm::acknowledgeWrite, enabled = !s.loading) { Text(stringResource(R.string.tools_reviewed)) }
        }
        RevisedDateControl(s.revisedDate, enabled = s.loaded && !s.loading && !s.accessDenied && !s.conflict && !s.needsReview) { vm.field("revisedDate", it) }
        FilterChip(selected = preview, onClick = { preview = !preview }, enabled = s.loaded && !s.loading,
            label = { Text(stringResource(R.string.question_markdown_preview)) })
        if (preview) QuestionDraftPreview(s.text, s.choices.map { it.text },
            s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1, s.explanation)
        validationMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        s.error?.let {
            ErrorState(throwable=it,onRetry=if(!s.loaded && !s.loading) vm::load else null)
        }
        AppSection(stringResource(R.string.section_question_content), initiallyExpanded = true, revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_FORM, QuestionEditorValidationError.TOO_MANY_CHOICES, QuestionEditorValidationError.TEXT_TOO_LONG, QuestionEditorValidationError.CHOICE_TOO_LONG, QuestionEditorValidationError.DUPLICATE_CHOICES)) {
            MarkdownField(s.text, { vm.field("text", it) }, enabled = enabled,
                label = stringResource(R.string.question_text), error = fieldError(s.text.isBlank() || s.text.length > 3000), minLines = 3, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.maximum_choices, s.maxChoices))
            s.choices.forEachIndexed { index, choice ->
                key(choice.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MarkdownField(choice.text, { vm.choice(choice.id, it) }, enabled = enabled,
                            label = stringResource(R.string.choice_number, index + 1), error = fieldError(choice.text.isBlank() || choice.text.length > 300 || s.choices.count { it.text.trim().equals(choice.text.trim(), ignoreCase = true) } > 1), modifier = Modifier.fillMaxWidth())
                        FilterChip(selected = s.correctChoiceId == choice.id, onClick = { vm.selectCorrect(choice.id) },
                            enabled = enabled, modifier = Modifier.semantics { if (validationMessage != null && s.correctChoiceId == null) error(validationMessage) }, label = { Text(stringResource(R.string.correct_answer)) })
                        AppActionRow() {
                            TextButton({ vm.moveChoice(choice.id, -1) }, enabled = enabled && index > 0) { Text(stringResource(R.string.move_choice_up)) }
                            TextButton({ vm.moveChoice(choice.id, 1) }, enabled = enabled && index < s.choices.lastIndex) { Text(stringResource(R.string.move_choice_down)) }
                            AppDangerTextButton({ vm.removeChoice(choice.id) }, enabled = enabled && s.choices.size > 2) { Text(stringResource(R.string.remove_choice)) }
                        }
                    }
                }
            }
            OutlinedButton(vm::addChoice, enabled = enabled && s.choices.size < s.maxChoices) { Text(stringResource(R.string.add_choice)) }
        }
        AppSection(stringResource(R.string.explanation), initiallyExpanded = true, revealRequest = s.validationAttempt, revealError = validationMessage != null && s.explanation.length > 3000) {
            MarkdownField(s.explanation, { vm.field("explanation", it) }, enabled = enabled,
                label = stringResource(R.string.explanation), error = fieldError(s.explanation.length > 3000), minLines = 2, modifier = Modifier.fillMaxWidth())
        }
        AppSection(stringResource(R.string.section_reference), revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_SOURCE_PAGE, QuestionEditorValidationError.SOURCE_TOO_LONG)) {
            OutlinedTextField(s.source,{vm.field("source",it)},enabled=enabled,
                label={Text(stringResource(R.string.question_source))},isError=fieldError(s.source.length > 200)!=null, supportingText=fieldError(s.source.length > 200)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
            OutlinedTextField(s.sourceDocument,{vm.field("sourceDocument",it)},enabled=enabled,
                label={Text(stringResource(R.string.editor_source_document))},isError=fieldError(s.sourceDocument.length > 500)!=null, supportingText=fieldError(s.sourceDocument.length > 500)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
            OutlinedTextField(s.sourcePage,{vm.field("sourcePage",it)},enabled=enabled,
                label={Text(stringResource(R.string.editor_source_page))},isError=fieldError(s.validationError == QuestionEditorValidationError.INVALID_SOURCE_PAGE)!=null, supportingText=fieldError(s.validationError == QuestionEditorValidationError.INVALID_SOURCE_PAGE)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
        }
        AppSection(stringResource(R.string.section_associations), revealRequest = s.validationAttempt, revealError = s.validationError == QuestionEditorValidationError.INVALID_ASSOCIATION) {
            Text(stringResource(R.string.editor_image_after_save))
            QuestionAssociationFields(s,vm,enabled)
        }
        AppSection(stringResource(R.string.editor_translations), revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_LOCALE, QuestionEditorValidationError.TRANSLATION_REQUIRED, QuestionEditorValidationError.TRANSLATION_CHOICES) || (validationMessage != null && s.translations.values.any { it.question.length > 3000 || it.explanation.length > 3000 || it.choices.values.any { text -> text.length > 300 } })) {
            Text(stringResource(R.string.editor_translation_hint))
            OutlinedTextField(localeInput,{localeInput=it},enabled=enabled,
                label={Text(stringResource(R.string.editor_translation_locale))},isError=s.validationError==QuestionEditorValidationError.INVALID_LOCALE, supportingText=fieldError(s.validationError==QuestionEditorValidationError.INVALID_LOCALE)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedButton({vm.addTranslation(localeInput)},enabled=enabled && localeInput.isNotBlank()) { Text(stringResource(R.string.editor_translation_add)) }
            s.translations.forEach { (locale,translation) ->
                key(locale) {
                    Text(locale,style=MaterialTheme.typography.titleMedium)
                    MarkdownField(translation.question,{vm.translationField(locale,"question",it)},enabled=enabled,
                        label=stringResource(R.string.question_text),error=fieldError((s.validationError==QuestionEditorValidationError.TRANSLATION_REQUIRED && translation.question.isBlank()) || translation.question.length>3000),minLines=2,modifier=Modifier.fillMaxWidth())
                    s.choices.forEachIndexed { index,choice ->
                        MarkdownField(translation.choices[choice.id].orEmpty(),{vm.translationChoice(locale,choice.id,it)},enabled=enabled,
                            label=stringResource(R.string.choice_number,index+1),error=fieldError((s.validationError==QuestionEditorValidationError.TRANSLATION_CHOICES && translation.choices[choice.id].isNullOrBlank()) || translation.choices[choice.id].orEmpty().length>300),modifier=Modifier.fillMaxWidth())
                    }
                    if(translation.extraChoices.isNotEmpty()) {
                        Text(stringResource(R.string.editor_translation_extra),color=MaterialTheme.colorScheme.error)
                        translation.extraChoices.forEach { MarkdownText(it) }
                        OutlinedButton({alignTranslation=locale},enabled=enabled) { Text(stringResource(R.string.editor_translation_align)) }
                    }
                    MarkdownField(translation.explanation,{vm.translationField(locale,"explanation",it)},enabled=enabled,
                        label=stringResource(R.string.explanation),error=fieldError(translation.explanation.length>3000),minLines=2,modifier=Modifier.fillMaxWidth())
                    if(preview) QuestionDraftPreview(translation.question,translatedChoices(translation,s.choices),
                        s.choices.indexOfFirst { it.id==s.correctChoiceId }+1,translation.explanation)
                    AppDangerOutlinedButton({removeTranslation=locale},enabled=enabled) { Text(stringResource(R.string.editor_translation_remove)) }
                }
            }
        }
        AppSection(stringResource(R.string.section_organization), revealRequest = s.validationAttempt, revealError = s.validationError==QuestionEditorValidationError.INVALID_FORM && s.difficulty !in listOf("easy", "medium", "hard")) {
            QuestionTagFields(s,vm,enabled)
            Text(stringResource(R.string.difficulty))
            AppActionRow() {
                listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                    FilterChip(selected = s.difficulty == value, onClick = { vm.field("difficulty", value) },
                        enabled = enabled, label = { Text(stringResource(label)) })
                }
            }
            ExposedDropdownMenuBox(categoryMenu, { if (enabled) categoryMenu = !categoryMenu }) {
                OutlinedTextField(s.categories.firstOrNull { it.id == s.category }?.name.orEmpty(), {},
                    readOnly = true, enabled = enabled, label = { Text(stringResource(R.string.nav_categories)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(categoryMenu && enabled, { categoryMenu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.none)) }, { vm.category(null); categoryMenu = false })
                    s.categories.forEach { cat ->
                        DropdownMenuItem({ Text(cat.name) }, { vm.category(cat.id); categoryMenu = false })
                    }
                }
            }
        }
        Button(vm::save, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun QuestionDraftPreview(text: String, choices: List<String>, correctAnswer: Int?, explanation: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
        com.mukhtabir.android.ui.components.QuestionTextCard(text)
        choices.forEachIndexed { index, choice ->
            com.mukhtabir.android.ui.components.QuestionChoiceCard {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.choice_number, index + 1))
                    MarkdownText(choice, Modifier.fillMaxWidth())
                    if (correctAnswer == index + 1) Text(stringResource(R.string.correct_answer), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (explanation.isNotBlank()) {
            Text(stringResource(R.string.explanation), style = MaterialTheme.typography.titleSmall)
            ExplanationCard(explanation)
        }
    }
}


@Composable
private fun QuestionExtendedPreview(question: QuestionDto) {
    com.mukhtabir.android.ui.components.QuestionContext(question)
    question.case?.let { case ->
        Text(stringResource(R.string.editor_case_key)+": "+case.key.orEmpty())
        if(!case.stem.isNullOrBlank()) MarkdownText(case.stem.orEmpty())
    }
    question.caseOrder?.let { Text(stringResource(R.string.editor_case_order)+": "+it) }
    question.knowledgeObject?.let { Text(stringResource(R.string.editor_knowledge_id,it)) }
    val baseChoices=question.choices.map { QuestionChoiceDraft(text=it) }
    translationDrafts(question.translations,baseChoices).forEach { (locale,translation) ->
        Text(locale,style=MaterialTheme.typography.titleSmall)
        QuestionDraftPreview(translation.question,translatedChoices(translation,baseChoices)+translation.extraChoices,
            null,translation.explanation)
    }
}

@kotlinx.serialization.Serializable
internal data class StoredQuestionEditor(val state: QuestionEditorState, val baseline: QuestionWriteRequest?,
    val baselinePage: String, val baselineOrder: String, val posting: Boolean = false)

@Composable
private fun RevisedDateControl(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    OutlinedButton({
        val date = runCatching { java.time.LocalDate.parse(value) }.getOrElse { java.time.LocalDate.now() }
        android.app.DatePickerDialog(context, { _, year, month, day -> onChange(java.time.LocalDate.of(year, month + 1, day).toString()) },
            date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, enabled = enabled) { Text(stringResource(R.string.author_revised_date) + ": " + value.ifBlank { stringResource(R.string.author_default_date) }) }
}
