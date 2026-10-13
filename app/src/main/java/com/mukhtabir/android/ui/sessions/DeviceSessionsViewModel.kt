package com.mukhtabir.android.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.DeviceSessionDto
import com.mukhtabir.android.data.remote.DeviceSessionsService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class DeviceSessionsState(val items: List<DeviceSessionDto> = emptyList(), val loaded: Boolean = false,
    val busy: Boolean = false, val password: String = "", val pending: String? = null,
    val reviewReady: Boolean = false, val error: Throwable? = null, val message: Int? = null,
    val signedOut: Boolean = false)
internal class DeviceSessionsViewModel(private val service: DeviceSessionsService) : ViewModel() {
    private val _state = MutableStateFlow(DeviceSessionsState())
    val state = _state.asStateFlow()
    init { refresh() }
    fun password(value: String) { if (!_state.value.busy) _state.update { it.copy(password=value) } }
    fun clearPassword() { if (!_state.value.busy) _state.update { it.copy(password="") } }
    fun acknowledgeReview() {
        if (!_state.value.busy && _state.value.reviewReady) _state.update { it.copy(pending=null,reviewReady=false,password="") }
    }
    fun refresh() {
        if (_state.value.busy || _state.value.signedOut) return
        _state.update { it.copy(busy=true,error=null,reviewReady=false) }
        viewModelScope.launch {
            try {
                val items=service.list(); coroutineContext.ensureActive()
                _state.update { it.copy(items=items,loaded=true,reviewReady=it.pending!=null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(error=error) } }
            finally { _state.update { it.copy(busy=false) } }
        }
    }
    fun revoke(id: String) {
        val s=_state.value
        if (s.busy || s.signedOut || s.pending!=null || s.password.isBlank() || s.password.codePointCount(0,s.password.length)>128 || s.items.none { it.id==id }) return
        _state.update { it.copy(busy=true,error=null,message=null) }
        viewModelScope.launch {
            var started=false
            try {
                val items=service.list(); coroutineContext.ensureActive()
                _state.update { it.copy(items=items,loaded=true) }
                if (items.none { it.id==id }) { _state.update { it.copy(message=R.string.device_session_absent,password="") };return@launch }
                started=true
                val result=service.revoke(id,s.password); coroutineContext.ensureActive()
                check(result.revoked)
                _state.update { it.copy(items=it.items.filterNot { row -> row.id==id },password="",message=R.string.device_session_revoked,signedOut=result.current) }
                if (!result.current) {
                    try { val refreshed=service.list();coroutineContext.ensureActive();_state.update { it.copy(items=refreshed) } }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (error: Exception) { _state.update { it.copy(error=error) } }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                coroutineContext.ensureActive()
                val certain=error is ApiException && error.statusCode in setOf(400,401,403,404,409,429,501)
                _state.update { it.copy(error=error,password="",pending=id.takeIf { started && !certain },reviewReady=false) }
            } finally { _state.update { it.copy(busy=false) } }
        }
    }
    class Factory(private val service: DeviceSessionsService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T=DeviceSessionsViewModel(service) as T
    }
}
