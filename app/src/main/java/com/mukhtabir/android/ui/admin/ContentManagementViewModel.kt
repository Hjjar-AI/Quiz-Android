package com.mukhtabir.android.ui.admin

import androidx.lifecycle.*
import com.mukhtabir.android.R
import com.mukhtabir.android.core.*
import kotlinx.serialization.Serializable
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

enum class ContentKind { KNOWLEDGE, CASE }
fun mayManageContent(user: UserDto, kind: ContentKind): Boolean = when (kind) {
    ContentKind.KNOWLEDGE -> listOf("questions.create", "questions.edit_own", "questions.edit_any").any(user::has)
    ContentKind.CASE -> user.has("questions.edit_case_stem_any") || user.has("questions.edit_case_stem_own")
}
@Serializable
internal data class KnowledgeForm(
    val title: String = "", val objective: String = "", val answer: String = "", val facts: String = "",
    val misconceptions: String = "", val category: Long? = null, val tags: List<String> = emptyList(),
    val tagInput: String = "", val status: String = "active", val document: String = "", val page: String = "",
    val revised: String = "", val translations: JsonObject = JsonObject(emptyMap()),
) {
    fun body(): JsonObject = buildJsonObject {
        put("title", title.trim()); put("learning_objective", objective.trim()); put("canonical_answer", answer.trim())
        put("key_facts", JsonArray(facts.lines().map(String::trim).filter(String::isNotBlank).map(::JsonPrimitive)))
        put("misconceptions", JsonArray(misconceptions.lines().map(String::trim).filter(String::isNotBlank).map(::JsonPrimitive)))
        put("category", category?.let(::JsonPrimitive) ?: JsonNull); put("tag_names", JsonArray(tags.map(::JsonPrimitive)))
        put("status", status); put("source_document", document.trim().takeIf(String::isNotBlank)?.let(::JsonPrimitive) ?: JsonNull)
        put("source_page", page.trim().toIntOrNull()?.let(::JsonPrimitive) ?: JsonNull)
        if (revised.isNotBlank()) put("last_revised_at", revised.trim())
        put("translations", translations)
    }
}
internal fun KnowledgeDetailDto.form() = KnowledgeForm(title, learningObjective, canonicalAnswer, keyFacts.joinToString("\n"),
    misconceptions.joinToString("\n"), category, tags, status = status, document = sourceDocument.orEmpty(), page = sourcePage?.toString().orEmpty(),
    revised = lastRevisedAt.orEmpty(), translations = translations)
@Serializable
internal data class ContentDraft(val form: KnowledgeForm, val caseTitle: String, val knowledge: KnowledgeDetailDto?,
    val case: CaseAdminDto?, val posting: Boolean, val creating: Boolean, val operation: String?)
internal data class ContentManagementState(
    val knowledge: List<KnowledgeDetailDto> = emptyList(), val cases: List<CaseAdminDto> = emptyList(), val total: Int = 0,
    val page: Int = 1, val totalPages: Int = 1, val requestedPage: Int = 1, val query: String = "", val appliedQuery: String = "", val appliedStatus: String = "", val appliedCategory: Long? = null,
    val requestedQuery: String = "", val requestedStatus: String = "", val requestedCategory: Long? = null, val status: String = "", val category: Long? = null,
    val categories: List<CategoryDto> = emptyList(), val loading: Boolean = false, val busy: Boolean = false,
    val error: Throwable? = null, val actionError: Throwable? = null, val lookupError: Throwable? = null, val saved: Boolean = false,
    val editing: Boolean = false, val selectedKnowledge: KnowledgeDetailDto? = null, val selectedCase: CaseAdminDto? = null,
    val form: KnowledgeForm = KnowledgeForm(), val caseTitle: String = "", val conflict: Boolean = false,
    val operation: String? = null, val posting: Boolean = false,
    val uncertain: Boolean = false, val reviewed: Boolean = false, val uncertainCreate: Boolean = false,
    val receiptMissing: Boolean = false, val noticeMissing: Boolean = false, val validation: Int? = null, val linked: QuestionPageDto? = null, val linkedError: Throwable? = null, val linkedRequestedPage: Int = 1,
) {
    val dirty: Boolean get() = if (!editing) false else if (selectedCase != null) caseTitle != selectedCase.title.orEmpty() || form.translations != selectedCase.translations
        else form != (selectedKnowledge?.form() ?: KnowledgeForm())
}
internal class ContentManagementViewModel(private val kind: ContentKind, private val service: ContentManagementService,
    private val categories: CategoryService, private var user: UserDto, val size: Int, store: SessionDraftStore) : ViewModel() {
    private val _state = MutableStateFlow(ContentManagementState()); val state = _state.asStateFlow()
    private var baselineKnowledge: KnowledgeDetailDto? = null
    private var baselineCase: CaseAdminDto? = null
    val recovery=EditorRecovery(viewModelScope,store,user.id,"content_${kind.name.lowercase()}-new",ContentDraft.serializer())
    private fun snapshot() = _state.value.let {s->ContentDraft(s.form,s.caseTitle,baselineKnowledge,baselineCase?.copy(questions=emptyList()),s.posting || s.uncertain,s.uncertainCreate || (kind==ContentKind.KNOWLEDGE && s.selectedKnowledge==null),s.operation).takeIf {s.editing && (s.dirty || s.uncertain || s.posting)} }
    init {recovery.observe(state) {snapshot()}; viewModelScope.launch { recovery.ready.first {it}; if(recovery.pending.value==null) refresh(); loadCategories() }}
    fun restore(d: ContentDraft) {
        baselineKnowledge=d.knowledge;baselineCase=d.case
        _state.update {it.copy(editing=true,selectedKnowledge=d.knowledge,selectedCase=d.case,form=d.form,caseTitle=d.caseTitle,
            uncertain=d.posting,uncertainCreate=d.posting && d.creating,operation=d.operation,conflict=!d.creating && !d.posting,reviewed=false)}
        viewModelScope.launch { kotlinx.coroutines.yield(); if(d.knowledge!=null || d.case!=null) refreshDetail() }
    }
    fun permissions(current: UserDto) { if(current.id==user.id && current.uuid==user.uuid) user=current }
    private fun allowed() = !recovery.blocked && mayManageContent(user, kind) && !_state.value.loading && !_state.value.busy
    fun mayWriteKnowledge(row: KnowledgeDetailDto?) = user.has("questions.edit_any") ||
        if (row == null) user.has("questions.create") else user.has("questions.edit_own") && row.createdBy == user.id
    fun mayWriteCase(row: CaseAdminDto?) = user.has("questions.edit_case_stem_any") ||
        (user.has("questions.edit_case_stem_own") && row?.questions?.any { it.authoredBy == user.id } == true)
    private fun mayWrite() = if (kind == ContentKind.KNOWLEDGE) mayWriteKnowledge(_state.value.selectedKnowledge) else mayWriteCase(_state.value.selectedCase)
    fun query(value: String) { if (allowed()) _state.update { it.copy(query = value) } }
    fun filter(status: String, category: Long?) { if (allowed()) _state.update { it.copy(status = status, category = category) } }
    fun page(page: Int) {
        if (allowed() && !_state.value.editing && !_state.value.uncertain)
            refresh(targetPage=page.coerceIn(1,pages()))
    }
    fun pages() = _state.value.totalPages.coerceAtLeast(1)
    fun search() = refresh(useDraftQuery = true)
    fun retryList() = refresh(retry = true)
    fun refresh(useDraftQuery: Boolean = false, retry: Boolean = false, targetPage: Int? = null) {
        if (!allowed()) return
        val s = _state.value
        val requestedPage = if(retry) s.requestedPage else if(useDraftQuery) 1 else targetPage ?: s.page
        val query = if(retry) s.requestedQuery else if(useDraftQuery) s.query.trim() else s.appliedQuery
        val status = if(retry) s.requestedStatus else if(useDraftQuery) s.status else s.appliedStatus
        val category = if(retry) s.requestedCategory else if(useDraftQuery) s.category else s.appliedCategory
        _state.update { it.copy(loading = true, error = null, requestedQuery=query, requestedStatus=status, requestedCategory=category,requestedPage=requestedPage) }
        viewModelScope.launch {
            try {
                if (kind == ContentKind.KNOWLEDGE) {
                    var result = service.knowledge(query, status, category, requestedPage, size)
                    if(result.page > result.totalPages) result=service.knowledge(query,status,category,result.totalPages.coerceAtLeast(1),size)
                    coroutineContext.ensureActive()
                    _state.update { it.copy(knowledge = result.items, total = result.count, appliedQuery = query, appliedStatus=status, appliedCategory=category, page = result.page, totalPages=result.totalPages, loading = false,
                        reviewed = it.uncertainCreate) }
                } else {
                    var result = service.cases(query, requestedPage, size)
                    if(result.page > result.totalPages) result=service.cases(query,result.totalPages.coerceAtLeast(1),size)
                    coroutineContext.ensureActive()
                    _state.update { it.copy(cases = result.items, total = result.total, appliedQuery = query, page = result.page, totalPages=result.totalPages, loading = false) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(loading = false, error = failure) } }
        }
    }
    fun loadCategories() {
        if (kind != ContentKind.KNOWLEDGE || !mayManageContent(user, kind)) return
        viewModelScope.launch {
            try { val result = categories.list().items; coroutineContext.ensureActive(); _state.update { it.copy(categories = result, lookupError = null) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(lookupError = failure) } }
        }
    }
    fun create() {
        if (!allowed() || _state.value.uncertain || !mayWriteKnowledge(null) || kind != ContentKind.KNOWLEDGE) return
        baselineKnowledge = null; baselineCase = null
        _state.update { it.copy(editing = true, selectedKnowledge = null, selectedCase = null, form = KnowledgeForm(), noticeMissing=false,
            linked = null, actionError = null, saved = false, validation = null, conflict = false) }
    }
    fun open(id: Long?, key: String?) {
        if (!allowed() || _state.value.uncertain) return
        _state.update { it.copy(loading = true, actionError = null, validation=null, noticeMissing=false, linked = null, linkedError = null, saved = false) }
        viewModelScope.launch {
            try {
                if (kind == ContentKind.KNOWLEDGE) {
                    val result = service.knowledgeDetail(requireNotNull(id)); coroutineContext.ensureActive()
                    baselineKnowledge = result
                    _state.update { it.copy(editing = true, selectedKnowledge = result, selectedCase = null, form = result.form(), loading = false, conflict = false) }
                } else {
                    val result = service.caseDetail(requireNotNull(key)); coroutineContext.ensureActive()
                    baselineCase = result
                    _state.update { it.copy(editing = true, selectedCase = result, selectedKnowledge = null, caseTitle = result.title.orEmpty(), form = KnowledgeForm(translations = result.translations), loading = false, conflict = false) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(loading = false, actionError = failure) } }
        }
    }
    fun refreshDetail() {
        val s=_state.value
        if(!allowed() || !s.editing || s.uncertain) return
        if(s.selectedKnowledge==null && s.selectedCase==null) return
        _state.update { it.copy(loading=true, actionError=null) }
        viewModelScope.launch {
            try {
                if(kind==ContentKind.KNOWLEDGE) {
                    val fresh=service.knowledgeDetail(requireNotNull(s.selectedKnowledge).id);coroutineContext.ensureActive()
                    val changed=fresh.version!=baselineKnowledge?.version || fresh.updatedAt!=baselineKnowledge?.updatedAt
                    if(!s.dirty) baselineKnowledge=fresh
                    _state.update { it.copy(selectedKnowledge=fresh, form=if(s.dirty) it.form else fresh.form(), conflict=s.dirty && changed,reviewed=true) }
                } else {
                    val fresh=service.caseDetail(requireNotNull(s.selectedCase).key);coroutineContext.ensureActive()
                    val changed=fresh.version!=baselineCase?.version || fresh.updatedAt!=baselineCase?.updatedAt || fresh.title!=baselineCase?.title || fresh.stem!=baselineCase?.stem || fresh.translations!=baselineCase?.translations
                    if(!s.dirty) baselineCase=fresh
                    _state.update { it.copy(selectedCase=fresh, caseTitle=if(s.dirty) it.caseTitle else fresh.title.orEmpty(), form=if(s.dirty) it.form else KnowledgeForm(translations=fresh.translations), conflict=s.dirty && changed,reviewed=true) }
                }
            } catch(cancel: CancellationException) { throw cancel }
            catch(failure: Exception) { _state.update { it.copy(actionError=failure) } }
            finally { _state.update { it.copy(loading=false) } }
        }
    }
    fun close() { if (allowed() && !_state.value.uncertain) _state.update { it.copy(editing = false, selectedKnowledge = null, selectedCase = null, linked = null) } }
    fun field(name: String, value: String) {
        if (!allowed() || !mayWrite() || _state.value.uncertain) return
        _state.update { s -> s.copy(caseTitle = if (name == "caseTitle") value else s.caseTitle, form = when (name) {
            "title" -> s.form.copy(title = value); "objective" -> s.form.copy(objective = value); "answer" -> s.form.copy(answer = value)
            "facts" -> s.form.copy(facts = value); "misconceptions" -> s.form.copy(misconceptions = value)
            "document" -> s.form.copy(document = value); "page" -> s.form.copy(page = value); "revised" -> s.form.copy(revised = value)
            "status" -> s.form.copy(status = value); "tagInput" -> s.form.copy(tagInput = value); else -> s.form
        }, saved = false, validation = null) }
    }
    fun category(id: Long?) { if (allowed() && mayWrite() && !_state.value.uncertain && (id == null || _state.value.categories.any { it.id == id }))
        _state.update { it.copy(form = it.form.copy(category = id), saved = false) } }
    fun tag(add: Boolean, name: String) {
        if (!allowed() || !mayWrite() || _state.value.uncertain) return
        val cleaned = name.trim()
        if (add && (cleaned.isBlank() || cleaned.codePointCount(0, cleaned.length) > 50)) { _state.update { it.copy(validation = R.string.editor_tag_too_long) }; return }
        _state.update { it.copy(form = it.form.copy(tags = if (add) (it.form.tags + cleaned).distinct() else it.form.tags - name, tagInput = ""), saved = false) }
    }
    fun translation(locale: String, field: String, value: String) {
        if (!allowed() || !mayWrite() || _state.value.uncertain) return
        _state.update { s ->
            val fields = (s.form.translations[locale] as? JsonObject).orEmpty().toMutableMap(); fields[field] = JsonPrimitive(value)
            s.copy(form = s.form.copy(translations = JsonObject(s.form.translations + (locale to JsonObject(fields)))), saved = false)
        }
    }
    fun resolve(keep: Boolean) {
        if (!allowed() || _state.value.uncertain || !_state.value.conflict || !_state.value.reviewed) return
        baselineKnowledge = _state.value.selectedKnowledge; baselineCase = _state.value.selectedCase
        _state.update { it.copy(conflict = false, form = if (keep) it.form else it.selectedKnowledge?.form() ?: it.selectedCase?.let { row -> KnowledgeForm(translations = row.translations) } ?: it.form,
            caseTitle = if (keep) it.caseTitle else it.selectedCase?.title.orEmpty()) }
    }
    fun linked(page: Int = 1) {
        val row = _state.value.selectedKnowledge ?: return
        if (!allowed() || page < 1) return
        _state.update { it.copy(loading = true, linkedError = null, linkedRequestedPage = page) }
        viewModelScope.launch {
            try {
                var result = service.linkedKnowledge(row.id, page, size)
                if(result.page > result.totalPages) result=service.linkedKnowledge(row.id, result.totalPages.coerceAtLeast(1), size)
                coroutineContext.ensureActive(); _state.update { it.copy(linked = result, loading = false) }
            }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(loading = false, linkedError = failure) } }
        }
    }
    private suspend fun preflight(deleting: Boolean): Boolean {
        val s = _state.value
        if (kind == ContentKind.KNOWLEDGE && s.selectedKnowledge != null) {
            val fresh = service.knowledgeDetail(s.selectedKnowledge.id); coroutineContext.ensureActive()
            _state.update { it.copy(selectedKnowledge = fresh) }
            if (!mayWriteKnowledge(fresh) || fresh.version != baselineKnowledge?.version || fresh.updatedAt != baselineKnowledge?.updatedAt || (deleting && fresh.questionCount != baselineKnowledge?.questionCount)) {
                _state.update { it.copy(conflict = true,reviewed=true) }; return false
            }
        } else if (s.selectedCase != null) {
            val fresh = service.caseDetail(s.selectedCase.key); coroutineContext.ensureActive()
            _state.update { it.copy(selectedCase = fresh) }
            if (!mayWriteCase(fresh) || fresh.version != baselineCase?.version || fresh.title != baselineCase?.title || fresh.stem != baselineCase?.stem || fresh.translations != baselineCase?.translations || fresh.updatedAt != baselineCase?.updatedAt ||
                fresh.questions.map { it.id }.toSet() != baselineCase?.questions?.map { it.id }?.toSet()) {
                _state.update { it.copy(conflict = true,reviewed=true) }; return false
            }
        }
        return true
    }
    fun save() = mutate(false)
    fun delete() = mutate(true)
    private fun mutate(delete: Boolean) {
        val s = _state.value
        if (!allowed() || !s.editing || s.uncertain || s.conflict || !mayWrite()) return
        if (delete && kind == ContentKind.CASE && !user.has("questions.edit_case_stem_any")) return
        if (delete && s.selectedKnowledge == null && s.selectedCase == null) return
        if (!delete && kind == ContentKind.KNOWLEDGE) {
            val f = s.form
            if (f.title.isBlank() || f.title.codePointCount(0, f.title.length) > 200 || f.objective.isBlank() ||
                f.objective.codePointCount(0, f.objective.length) > 3000 || f.answer.codePointCount(0, f.answer.length) > 3000 ||
                f.tagInput.isNotBlank() || (f.page.isNotBlank() && (f.page.trim().toIntOrNull() ?: 0) <= 0) ||
                (f.revised.isNotBlank() && runCatching { java.time.LocalDate.parse(f.revised.trim()) }.isFailure)) {
                _state.update { it.copy(validation = R.string.content_invalid) }; return
            }
        }
        _state.update { it.copy(busy = true, actionError = null, validation = null, saved = false) }
        viewModelScope.launch {
            var dispatched = false
            try {
                if (!preflight(delete)) { _state.update { it.copy(busy = false) }; return@launch }
                val operation=if(!delete && kind==ContentKind.KNOWLEDGE && s.selectedKnowledge==null) s.operation ?: java.util.UUID.randomUUID().toString() else s.operation
                _state.update {it.copy(operation=operation,posting=true)}
                if(!recovery.persist(requireNotNull(snapshot()))) return@launch
                dispatched = true
                if (kind == ContentKind.KNOWLEDGE) {
                    if (delete) service.deleteKnowledge(requireNotNull(s.selectedKnowledge).id,requireNotNull(baselineKnowledge).version)
                    else {
                        val body = s.form.body(); val baseline = baselineKnowledge?.form()?.body()
                        val patch = if (baseline == null) body else JsonObject(body.filter { (key, value) -> baseline[key] != value })
                        val versionedPatch = if (s.selectedKnowledge == null) JsonObject(patch + ("operation_id" to JsonPrimitive(requireNotNull(operation)))) else JsonObject(patch + ("expected_version" to JsonPrimitive(requireNotNull(baselineKnowledge).version)))
                        val result = service.saveKnowledge(s.selectedKnowledge?.id, versionedPatch); coroutineContext.ensureActive()
                        baselineKnowledge = result
                        _state.update { it.copy(selectedKnowledge = result, form = result.form()) }
                    }
                } else {
                    if (delete) service.deleteCase(requireNotNull(s.selectedCase).key, requireNotNull(baselineCase).version)
                    else {
                        val result = service.saveCase(requireNotNull(s.selectedCase).key, s.caseTitle.trim(), requireNotNull(baselineCase).version, s.form.translations); coroutineContext.ensureActive()
                        // PUT returns metadata only; retain fresh detail questions for actual ownership checks.
                        val full = result.copy(questions = _state.value.selectedCase?.questions.orEmpty())
                        baselineCase = full
                        _state.update { it.copy(selectedCase = full, caseTitle = full.title.orEmpty(), form = KnowledgeForm(translations = full.translations)) }
                    }
                }
                coroutineContext.ensureActive()
                _state.update { it.copy(busy = false, posting=false, operation=null, saved = true, editing = if (delete) false else it.editing,
                    selectedKnowledge = if (delete) null else it.selectedKnowledge, selectedCase = if (delete) null else it.selectedCase) }
                recovery.persist(null)
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val certain = failure is ApiException && failure.statusCode in setOf(400, 401, 403, 404, 409, 422, 429)
                _state.update { it.copy(busy = false, posting=false, actionError = failure, uncertain = dispatched && !certain,
                    uncertainCreate = dispatched && !certain && s.selectedKnowledge == null && kind == ContentKind.KNOWLEDGE, conflict=it.conflict || (failure is ApiException && failure.statusCode==409), reviewed = false) }
            } finally { _state.update { it.copy(busy=false, posting=false) } }
        }
    }
    fun reconcile() {
        val s = _state.value
        if (!allowed() || !s.uncertain) return
        if(s.uncertainCreate && s.operation!=null) {
            _state.update {it.copy(loading=true,actionError=null)}
            viewModelScope.launch {
                try { val row=service.knowledgeReceipt(s.operation);coroutineContext.ensureActive();baselineKnowledge=row
                    _state.update {it.copy(selectedKnowledge=row,form=row.form(),uncertain=false,uncertainCreate=false,operation=null,saved=true)}
                    recovery.persist(null)
                } catch(cancel: CancellationException) {throw cancel}
                catch(failure: Exception) {_state.update {it.copy(actionError=failure,receiptMissing=failure is ApiException && failure.statusCode==404,noticeMissing=failure is ContentTargetGoneException,reviewed=failure is ContentTargetGoneException)}}
                finally {_state.update {it.copy(loading=false)}}
            };return
        }
        if (s.uncertainCreate) { _state.update { it.copy(query = it.form.title, status = "", category = null) }; refresh(useDraftQuery = true); return }
        _state.update { it.copy(loading = true, actionError = null, reviewed = false) }
        viewModelScope.launch {
            try {
                if (kind == ContentKind.KNOWLEDGE) {
                    val row = service.knowledgeDetail(requireNotNull(s.selectedKnowledge).id); coroutineContext.ensureActive()
                    baselineKnowledge = row; _state.update { it.copy(selectedKnowledge = row, conflict = false, reviewed = true) }
                } else {
                    val row = service.caseDetail(requireNotNull(s.selectedCase).key); coroutineContext.ensureActive()
                    baselineCase = row; _state.update { it.copy(selectedCase = row, conflict = false, reviewed = true) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                if (failure is ApiException && failure.statusCode == 404) _state.update { it.copy(reviewed = true, noticeMissing = true) }
                else _state.update { it.copy(actionError = failure) }
            } finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun retryCreate() {
        val s=_state.value
        if(!allowed() || !s.uncertainCreate || !s.receiptMissing || s.operation==null) return
        _state.update {it.copy(uncertain=false,uncertainCreate=false,receiptMissing=false)}
        save()
    }
    fun acknowledge() { if (allowed() && _state.value.reviewed) _state.update { it.copy(uncertain = false, uncertainCreate = false, operation=null,receiptMissing=false,noticeMissing=false,reviewed=false,editing = if(it.uncertainCreate) true else !it.noticeMissing && it.editing) } }
    class Factory(private val kind: ContentKind, private val service: ContentManagementService, private val categories: CategoryService,
        private val user: UserDto, private val size: Int, private val store: SessionDraftStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ContentManagementViewModel(kind, service, categories, user, size.coerceIn(1, 100), store) as T
    }
}
