package com.mukhtabir.android.ui.notices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.MasterExamService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class NoticesState(val items: List<MasterExamDto> = emptyList(), val loaded: Boolean = false,
    val busy: Boolean = false, val error: Throwable? = null)
internal class NoticesViewModel(private val service: MasterExamService) : ViewModel() {
    private val _state = MutableStateFlow(NoticesState())
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val notices = service.notices()
                coroutineContext.ensureActive()
                _state.update { it.copy(items = notices.items, loaded = true) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(error = error) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    class Factory(private val service: MasterExamService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = NoticesViewModel(service) as T
    }
}
