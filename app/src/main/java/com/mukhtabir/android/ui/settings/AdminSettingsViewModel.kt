package com.mukhtabir.android.ui.settings

import androidx.lifecycle.*
import com.mukhtabir.android.core.*
import com.mukhtabir.android.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsDraft(val expiry: String, val renewal: String, val duration: String,
    val baseline: RuntimeSettingsDto, val pending: Boolean = false)
internal data class AdminSettingsState(val draft: SettingsDraft? = null, val latest: RuntimeSettingsDto? = null,
    val busy: Boolean = false, val error: Throwable? = null, val saved: Boolean = false,
    val conflict: Boolean = false, val reviewed: Boolean = false, val invalid: Boolean = false)
internal class AdminSettingsViewModel(private val service: AdminService, private val allowed: Boolean,
    store: SessionDraftStore, userId: Long): ViewModel() {
    private val _state=MutableStateFlow(AdminSettingsState()); val state=_state.asStateFlow()
    val recovery=EditorRecovery(viewModelScope,store,userId,"settings-new",SettingsDraft.serializer())
    private fun dirty(d: SettingsDraft) = d.expiry!=d.baseline.defaultExpiryDays || d.renewal!=d.baseline.defaultRenewalDays || d.duration!=d.baseline.examDurationMinutes
    init { recovery.observe(state) {s-> s.draft?.takeIf {dirty(it) || it.pending}}; refresh() }
    fun restore(d: SettingsDraft) { _state.update {it.copy(draft=d,conflict=!d.pending,saved=false,reviewed=false)} }
    private fun editable()=allowed && !_state.value.busy && !recovery.blocked && !_state.value.conflict && _state.value.draft?.pending!=true
    fun field(expiry: String?=null,renewal: String?=null,duration: String?=null) {
        if(!editable()) return
        _state.update {s->s.copy(draft=s.draft?.copy(expiry=expiry ?: s.draft.expiry,renewal=renewal ?: s.draft.renewal,duration=duration ?: s.draft.duration),saved=false,invalid=false)}
    }
    fun refresh() {
        if(!allowed || _state.value.busy) return
        _state.update {it.copy(busy=true,error=null,reviewed=false)}
        viewModelScope.launch {
            try {
                val current=service.settings(); ensureActive()
                _state.update {s->
                    val d=s.draft
                    val confirmed=d?.pending==true && d.expiry==current.defaultExpiryDays && d.renewal==current.defaultRenewalDays && d.duration==current.examDurationMinutes
                    s.copy(latest=current,draft=if(d==null || confirmed) SettingsDraft(current.defaultExpiryDays,current.defaultRenewalDays,current.examDurationMinutes,current) else d,
                        saved=s.saved || confirmed,conflict=d!=null && !confirmed && d.baseline.version!=current.version,reviewed=true)
                }
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {_state.update {it.copy(error=failure)}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun resolve(keep: Boolean) {
        val s=_state.value;val latest=s.latest ?: return
        if(!allowed || s.busy || !s.reviewed || recovery.blocked) return
        _state.update {it.copy(draft=if(keep) it.draft?.copy(baseline=latest,pending=false) else SettingsDraft(latest.defaultExpiryDays,latest.defaultRenewalDays,latest.examDurationMinutes,latest),conflict=false,reviewed=false)}
    }
    fun save() {
        val s=_state.value;val d=s.draft ?: return
        if(!editable()) return
        val expiry=d.expiry.trim().toIntOrNull();val renewal=d.renewal.trim().toIntOrNull();val duration=d.duration.trim().toIntOrNull()
        if(expiry==null || renewal==null || duration==null || expiry<0 || renewal<0 || duration<1) {_state.update {it.copy(invalid=true)};return}
        _state.update {it.copy(busy=true,error=null,saved=false,draft=d.copy(pending=true))}
        viewModelScope.launch {
            var sent=false
            try {
                if(!recovery.persist(d.copy(pending=true))) return@launch
                sent=true
                val result=service.updateSettings(RuntimeSettingsUpdateRequest(d.baseline.version,expiry,renewal,duration));ensureActive()
                val base=RuntimeSettingsDto(result.version,expiry.toString(),renewal.toString(),duration.toString())
                _state.update {it.copy(draft=SettingsDraft(base.defaultExpiryDays,base.defaultRenewalDays,base.examDurationMinutes,base),latest=base,saved=true)}
                recovery.persist(null)
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {
                _state.update {it.copy(error=failure,draft=d.copy(pending=sent && !failure.isRejectedWrite()),conflict=failure is ApiException && failure.statusCode==409)}
            } finally {_state.update {it.copy(busy=false)}}
        }
    }
    class Factory(private val service: AdminService,private val allowed: Boolean,private val store: SessionDraftStore,private val user: Long): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = AdminSettingsViewModel(service,allowed,store,user) as T
    }
}
