package com.mukhtabir.android.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
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
