package com.mukhtabir.android.ui.knowledge

import com.mukhtabir.android.util.QuestionContentLimits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.core.*
import kotlinx.serialization.Serializable
import com.mukhtabir.android.data.remote.ClinicalCaseDetailDto
import com.mukhtabir.android.data.remote.QuestionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

@Serializable
internal data class StemDraft(val data: ClinicalCaseDetailDto?, val draft: String, val baseline: String, val pending: Boolean, val version: Int = 1)
internal data class CaseStemState(
    val data: ClinicalCaseDetailDto? = null,
    val draft: String = "",
    val baseline: String = "",
    val baselineVersion: Int = 1,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val conflict: Boolean = false,
    val needsReview: Boolean = false,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
    val validation: Int? = null,
    val savedCount: Int? = null,
    val changed: Boolean = false,
) {
    val dirty: Boolean get() = draft.trim()!=baseline
}

internal fun ClinicalCaseDetailDto.mayEditStem(userId: Long,any: Boolean,own: Boolean): Boolean =
    any || (own && questions.any { it.authoredBy==userId })

internal class CaseStemViewModel(private val key: String,private val userId: Long,
    private val service: QuestionService,private var any: Boolean,private var own: Boolean, store: SessionDraftStore): ViewModel() {
    private val _state=MutableStateFlow(CaseStemState())
    val state=_state.asStateFlow()
    fun permissions(any: Boolean, own: Boolean) {this.any=any;this.own=own}
    fun mayEdit(data: ClinicalCaseDetailDto?): Boolean = data?.mayEditStem(userId,any,own)==true
    private val recoveryKey="case_stem-"+java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") {"%02x".format(it)}
    val recovery=EditorRecovery(viewModelScope,store,userId,recoveryKey,StemDraft.serializer())
    private fun snapshot()=_state.value.let {s->StemDraft(s.data?.copy(questions=emptyList()),s.draft,s.baseline,s.needsReview || s.busy,s.baselineVersion).takeIf {s.dirty || s.needsReview || s.busy}}
    init {recovery.observe(state) {snapshot()};refresh()}
    fun restore(d: StemDraft) {_state.update {it.copy(data=d.data,draft=d.draft,baseline=d.baseline,baselineVersion=d.version,loaded=true,needsReview=d.pending,conflict=true)};viewModelScope.launch {kotlinx.coroutines.yield();refresh()}}
    fun refresh() {
        val s=_state.value
        if((!any && !own) || s.busy || s.loading) return
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                val current=service.caseDetail(key)
                coroutineContext.ensureActive()
                val stem=current.stem.orEmpty().trim()
                _state.update { it.copy(data=current,loaded=true,loading=false,needsReview=false,
                    baseline=if(it.loaded) it.baseline else stem,baselineVersion=if(it.loaded) it.baselineVersion else current.version,draft=if(it.loaded) it.draft else stem,
                    conflict=it.loaded && (stem!=it.baseline || current.version!=it.baselineVersion)) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,error=failure) }
            }
        }
    }
    fun field(value: String) {
        val s=_state.value
        if(recovery.blocked || !s.loaded || s.loading || s.busy || !mayEdit(s.data)) return
        _state.update { it.copy(draft=value,validation=null,savedCount=null) }
    }
    fun resolve(keepDraft: Boolean) {
        val s=_state.value
        val data=s.data ?: return
        if(s.busy || s.loading || s.needsReview || !s.conflict || !mayEdit(data)) return
        val stem=data.stem.orEmpty().trim()
        _state.update { it.copy(baseline=stem,baselineVersion=data.version,draft=if(keepDraft) it.draft else stem,conflict=false,
            actionError=null,validation=null,savedCount=null) }
    }
    fun consumeChange() { _state.update { it.copy(changed=false) } }
    fun save() {
        val s=_state.value
        if(recovery.blocked || !s.loaded || s.busy || s.loading || s.needsReview || s.conflict || !s.dirty || !mayEdit(s.data)) return
        val stem=s.draft.trim()
        if(stem.codePointCount(0,stem.length)>QuestionContentLimits.CASE_STEM) {
            _state.update { it.copy(validation=R.string.case_stem_length) };return
        }
        _state.update { it.copy(busy=true,actionError=null,validation=null,savedCount=null) }
        viewModelScope.launch {
            var attempted=false
            try {
                val current=service.caseDetail(key)
                coroutineContext.ensureActive()
                if(!mayEdit(current)) {
                    _state.update { it.copy(data=current,busy=false,validation=R.string.error_forbidden) }
                    return@launch
                }
                if(current.stem.orEmpty().trim()!=s.baseline || current.version!=s.baselineVersion) {
                    _state.update { it.copy(data=current,busy=false,conflict=true) }
                    return@launch
                }
                if(!recovery.persist(requireNotNull(snapshot()))) return@launch
                attempted=true
                val result=service.updateCaseStem(key,stem.takeIf(String::isNotEmpty),s.baselineVersion)
                coroutineContext.ensureActive()
                val applied=result.caseStem.orEmpty().trim()
                _state.update { it.copy(data=current.copy(stem=result.caseStem,version=result.version),draft=applied,baseline=applied,baselineVersion=result.version,
                    busy=false,conflict=false,needsReview=true,savedCount=result.updated,changed=true) }
                // Follow-up read errors stay separate from a confirmed save.
                refresh()
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(busy=false,actionError=failure,needsReview=true,changed=it.changed || attempted) }
            } finally { _state.update {it.copy(busy=false)} }
        }
    }
    class Factory(private val key: String,private val userId: Long,private val service: QuestionService,
        private val any: Boolean,private val own: Boolean,private val store: SessionDraftStore): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T = CaseStemViewModel(key,userId,service,any,own,store) as T
    }
}
