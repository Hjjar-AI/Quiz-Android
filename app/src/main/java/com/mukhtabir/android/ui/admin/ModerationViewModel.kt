package com.mukhtabir.android.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.ModerationFlagDto
import com.mukhtabir.android.data.remote.ModerationService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

enum class ModerationMode { FLAGS, VERIFICATION, TAGS }

internal data class ModerationState(
    val flags: List<ModerationFlagDto> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val ratings: Map<Long, QuestionRatingRowDto> = emptyMap(),
    val ratingsLoading: Boolean = false,
    val ratingsError: Throwable? = null,
    val selected: Set<Long> = emptySet(),
    val page: Int = 1,
    val requestedPage: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
    val needsReview: Boolean = false,
    val query: String = "",
    val appliedQuery: String = "",
    val requestedQuery: String = "",
    val requestedUnverifiedOnly: Boolean = true,
    val unverifiedOnly: Boolean = true,
    val appliedUnverifiedOnly: Boolean = true,
    val notes: String = "",
    val addTags: String = "",
    val removeTags: String = "",
    val tagError: ModerationTagError? = null,
    val processed: Int? = null,
    val submitted: Int = 0,
    val invalidated: Boolean = false,
)

internal class ModerationViewModel(
    private val mode: ModerationMode,
    private val service: ModerationService,
    private val size: Int,
    private val canManage: Boolean,
) : ViewModel() {
    private val _state=MutableStateFlow(ModerationState(unverifiedOnly=mode!=ModerationMode.TAGS,
        appliedUnverifiedOnly=mode!=ModerationMode.TAGS,requestedUnverifiedOnly=mode!=ModerationMode.TAGS))
    val state=_state.asStateFlow()
    private var ratingsJob: Job? = null
    private var ratingsGeneration = 0L
    init { refresh() }
    private fun questionIds(s: ModerationState): List<Long> =
        (if (mode == ModerationMode.FLAGS) s.flags.map { it.questionId } else s.questions.map { it.id }).distinct()
    fun refreshRatings() {
        val s = _state.value
        if (!canManage || !s.loaded || s.loading || s.busy) return
        val ids = questionIds(s)
        ratingsJob?.cancel()
        val generation = ++ratingsGeneration
        _state.update { it.copy(ratingsLoading = ids.isNotEmpty(), ratingsError = null) }
        if (ids.isEmpty()) { _state.update { it.copy(ratings = emptyMap()) }; return }
        ratingsJob = viewModelScope.launch {
            try {
                val ratings = service.ratings(ids); coroutineContext.ensureActive()
                if (generation == ratingsGeneration && ids == questionIds(_state.value))
                    _state.update { it.copy(ratings = ratings, ratingsLoading = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                if (generation == ratingsGeneration) _state.update { it.copy(ratingsLoading = false, ratingsError = failure) }
            }
        }
    }
    private fun ids(s: ModerationState): Set<Long> = if(mode==ModerationMode.FLAGS)
        s.flags.map { it.id }.toSet() else s.questions.map { it.id }.toSet()
    private fun editable(s: ModerationState) = canManage && !s.loading && !s.busy

    fun query(value: String) { if(editable(_state.value)) _state.update { it.copy(query=value) } }
    fun unverified(value: Boolean) { if(editable(_state.value)) _state.update { it.copy(unverifiedOnly=value) } }
    fun notes(value: String) { if(editable(_state.value)) _state.update { it.copy(notes=value) } }
    fun tagField(add: Boolean, value: String) {
        if(editable(_state.value)) _state.update {
            if(add) it.copy(addTags=value,tagError=null) else it.copy(removeTags=value,tagError=null)
        }
    }
    fun toggle(id: Long) {
        val s=_state.value
        if(!editable(s) || s.needsReview || id !in ids(s)) return
        _state.update { it.copy(selected=if(id in it.selected) it.selected-id else it.selected+id) }
    }
    fun selectPage() {
        val s=_state.value
        if(editable(s) && !s.needsReview) _state.update { it.copy(selected=ids(it)) }
    }
    fun clearSelection() { if(editable(_state.value)) _state.update { it.copy(selected=emptySet()) } }
    fun refresh() = load(_state.value.page)
    fun retry() = load(_state.value.requestedPage,_state.value.requestedQuery,_state.value.requestedUnverifiedOnly)
    fun page(value: Int) {
        val s=_state.value
        if(value in 1..s.totalPages) load(value)
    }
    fun applySearch() {
        val s=_state.value
        load(1,s.query.trim(),s.unverifiedOnly)
    }
    private suspend fun readPage(s: ModerationState, page: Int, query: String, unverified: Boolean): ModerationState =
        if(mode==ModerationMode.FLAGS) {
            val result=service.flags(page,size)
            s.copy(flags=result.items,page=result.page,total=result.total,totalPages=result.totalPages.coerceAtLeast(1))
        } else {
            val result=service.questions(page,size,query,unverified)
            s.copy(questions=result.items,page=result.page,total=result.total,totalPages=result.totalPages.coerceAtLeast(1))
        }
    private fun load(page: Int, query: String=_state.value.appliedQuery, unverified: Boolean=_state.value.appliedUnverifiedOnly) {
        val s=_state.value
        if(!editable(s)) return
        ratingsJob?.cancel(); ratingsGeneration++
        _state.update { it.copy(loading=true,ratingsLoading=false,error=null,requestedPage=page,requestedQuery=query,requestedUnverifiedOnly=unverified) }
        viewModelScope.launch {
            try {
                var result=readPage(s,page,query,unverified)
                if(page>result.totalPages) result=readPage(s,result.totalPages,query,unverified)
                coroutineContext.ensureActive()
                val samePage=s.page==result.page && query==s.appliedQuery && unverified==s.appliedUnverifiedOnly
                _state.update { it.copy(flags=result.flags,questions=result.questions,page=result.page,requestedPage=result.page,
                    total=result.total,totalPages=result.totalPages,loaded=true,loading=false,needsReview=false,
                    selected=if(samePage) it.selected.intersect(ids(result)) else emptySet(),
                    appliedQuery=query,appliedUnverifiedOnly=unverified,
                    ratings=if(samePage) it.ratings.filterKeys { id -> id in questionIds(result) } else emptyMap(), ratingsError=null) }
                refreshRatings()
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,error=failure) }
            }
        }
    }
    fun consumeInvalidation() = _state.update { it.copy(invalidated=false) }
    fun resolve() { if(mode==ModerationMode.FLAGS) mutate(verified=null) }
    fun verify(value: Boolean) { if(mode==ModerationMode.VERIFICATION) mutate(verified=value) }
    fun updateTags() { if(mode==ModerationMode.TAGS) mutate(verified=null) }
    private fun mutate(verified: Boolean?) {
        val s=_state.value
        val selected=s.selected.intersect(ids(s)).sorted()
        if(!editable(s) || !s.loaded || s.needsReview || selected.isEmpty() || selected.size>500 ||
            (mode==ModerationMode.FLAGS && verified!=null) || (mode==ModerationMode.VERIFICATION && verified==null) ||
            (mode==ModerationMode.TAGS && verified!=null)) return
        val add=moderationTagNames(s.addTags)
        val remove=moderationTagNames(s.removeTags)
        if(mode==ModerationMode.TAGS) moderationTagError(add,remove)?.let { failure ->
            _state.update { it.copy(tagError=failure) }
            return
        }
        _state.update { it.copy(busy=true,actionError=null,tagError=null,processed=null,submitted=selected.size) }
        viewModelScope.launch {
            var processed=0
            try {
                if(mode==ModerationMode.FLAGS) {
                    // No batch endpoint exists. Stop at the first failure, retaining the remaining selection.
                    for(id in selected) {
                        service.resolveFlag(id)
                        coroutineContext.ensureActive()
                        processed++
                        _state.update { it.copy(flags=it.flags.filterNot { flag -> flag.id==id },selected=it.selected-id,
                            total=(it.total-1).coerceAtLeast(0),processed=processed) }
                    }
                } else {
                    processed=if(mode==ModerationMode.TAGS) service.tags(selected,add,remove).count
                        else service.verify(selected,requireNotNull(verified),s.notes.trim()).count
                    coroutineContext.ensureActive()
                    _state.update { it.copy(selected=emptySet()) }
                }
                _state.update { it.copy(busy=false,processed=processed,needsReview=true,
                    invalidated=mode!=ModerationMode.FLAGS) }
                // Read recovery is separate from the confirmed write result; never replay a POST here.
                refresh()
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(busy=false,processed=if(mode==ModerationMode.FLAGS) processed else null,
                    actionError=failure,needsReview=true,invalidated=mode!=ModerationMode.FLAGS) }
            }
        }
    }
    class Factory(private val mode: ModerationMode,private val service: ModerationService,private val size: Int,
        private val canManage: Boolean): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T =
            ModerationViewModel(mode,service,size.coerceIn(1,500),canManage) as T
    }
}
