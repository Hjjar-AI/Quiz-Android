package com.mukhtabir.android.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class AdminAccountState(
    val user: UserDto? = null,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
    val validation: Int? = null,
    val adminPassword: String = "",
    val newPassword: String = "",
    val repeatPassword: String = "",
    val needsReview: Boolean = false,
    val success: Int? = null,
    val generatedPassword: String? = null,
    val generatedUncertain: Boolean = false,
    val deletionPending: Boolean = false,
    val deleted: Boolean = false,
)

internal class AdminAccountViewModel(private val id: Long,private val actingUserId: Long,
    private val service: AdminService,private val canManage: Boolean): ViewModel() {
    private val _state=MutableStateFlow(AdminAccountState())
    val state=_state.asStateFlow()
    init { refresh() }
    fun refresh() {
        val s=_state.value
        if(!canManage || s.busy || s.loading || s.deleted) return
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                val user=service.user(id)
                coroutineContext.ensureActive()
                _state.update { it.copy(user=user,loading=false,needsReview=false,deletionPending=false) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                if(s.deletionPending && failure is ApiException && failure.statusCode==404) {
                    _state.update { it.copy(loading=false,user=null,deleted=true,deletionPending=false,
                        needsReview=false,actionError=null,adminPassword="",newPassword="",repeatPassword="",
                        success=R.string.account_deleted) }
                } else _state.update { it.copy(loading=false,error=failure) }
            }
        }
    }
    fun field(name: String, value: String) {
        val s=_state.value
        if(!canManage || s.busy || s.loading || s.deleted || s.generatedPassword!=null || s.generatedUncertain) return
        _state.update { when(name) {
            "admin" -> it.copy(adminPassword=value,validation=null,success=null)
            "new" -> it.copy(newPassword=value,validation=null,success=null)
            "repeat" -> it.copy(repeatPassword=value,validation=null,success=null)
            else -> it
        } }
    }
    fun clearSecrets() {
        if(!_state.value.busy) _state.update { it.copy(adminPassword="",newPassword="",repeatPassword="",generatedPassword=null) }
    }
    override fun onCleared() {
        _state.update { it.copy(adminPassword="",newPassword="",repeatPassword="",generatedPassword=null) }
        super.onCleared()
    }
    fun acknowledgeGeneratedOutcome() {
        val s=_state.value
        if(!canManage || s.busy || s.loading || (s.generatedUncertain && s.needsReview)) return
        _state.update { it.copy(generatedPassword=null,generatedUncertain=false) }
    }
    fun toggle() = mutate("toggle")
    fun resetPassword() = mutate("reset")
    fun generatePassword() = mutate("generate")
    fun deleteUser() = mutate("delete")
    private fun mutate(action: String) {
        val s=_state.value
        val user=s.user ?: return
        val reset=action=="reset"
        val generate=action=="generate"
        val delete=action=="delete"
        if(!canManage || s.busy || s.loading || s.needsReview || s.deleted || s.generatedPassword!=null ||
            s.generatedUncertain || id==actingUserId || (action=="toggle" && user.isStub)) return
        val validation=when {
            s.adminPassword.isBlank() || s.adminPassword.length>128 -> R.string.account_admin_password_required
            reset && (s.newPassword.isBlank() || s.newPassword.length>128) -> R.string.account_password_required
            reset && s.newPassword!=s.repeatPassword -> R.string.account_password_mismatch
            else -> null
        }
        if(validation!=null) { _state.update { it.copy(validation=validation) };return }
        _state.update { it.copy(busy=true,actionError=null,validation=null,success=null) }
        viewModelScope.launch {
            var writeStarted=false
            try {
                val current=service.user(id)
                coroutineContext.ensureActive()
                // These endpoints have no revision condition; changed identity/status needs confirmation again.
                if(current.username!=user.username || current.role!=user.role || current.isStub!=user.isStub ||
                    (action=="toggle" && (current.isStub || current.isActive!=user.isActive)) ||
                    (delete && current.isActive!=user.isActive)) {
                    _state.update { it.copy(user=current,busy=false,validation=R.string.account_changed_review) }
                    return@launch
                }
                writeStarted=true
                if(delete) _state.update { it.copy(deletionPending=true) }
                val generated=when(action) {
                    "reset" -> { service.resetPassword(id,s.adminPassword,s.newPassword); null }
                    "generate" -> service.generatePassword(id,s.adminPassword).tempPassword.also { require(it.isNotBlank()) }
                    "delete" -> { service.deleteUser(id,s.adminPassword); null }
                    else -> { service.setUserActive(id,s.adminPassword,!user.isActive); null }
                }
                coroutineContext.ensureActive()
                _state.update { it.copy(busy=false,adminPassword="",newPassword=if(reset || delete) "" else it.newPassword,
                    repeatPassword=if(reset || delete) "" else it.repeatPassword,needsReview=!delete,
                    generatedPassword=generated,deleted=delete,deletionPending=false,
                    user=if(delete) null else current,
                    success=when { delete -> R.string.account_deleted; reset || generate -> R.string.account_password_saved
                        else -> R.string.account_status_saved }) }
                // A failed follow-up read must not turn a confirmed write into a failed write.
                refresh()
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                val rejected=failure is ApiException && failure.statusCode in setOf(400,401,403,404,409,422,429)
                _state.update { it.copy(busy=false,actionError=failure,adminPassword="",needsReview=true,
                    generatedUncertain=generate && writeStarted && !rejected,
                    deletionPending=delete && writeStarted && !rejected) }
            }
        }
    }
    class Factory(private val id: Long,private val actingId: Long,private val service: AdminService,
        private val canManage: Boolean): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T = AdminAccountViewModel(id,actingId,service,canManage) as T
    }
}
