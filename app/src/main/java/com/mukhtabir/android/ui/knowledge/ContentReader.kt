package com.mukhtabir.android.ui.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class ContentReadState<T>(val data: T? = null,val loading: Boolean = false,val error: Throwable? = null)

/** Read-only destinations retain the last successful snapshot on refresh failure. */
internal class ContentReader<T>(private val read: suspend () -> T): ViewModel() {
    private val _state=MutableStateFlow(ContentReadState<T>())
    val state=_state.asStateFlow()
    init { refresh() }
    fun refresh() {
        if(_state.value.loading) return
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                val result=read()
                coroutineContext.ensureActive()
                _state.update { it.copy(data=result,loading=false) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,error=failure) }
            }
        }
    }
    class Factory<T>(private val read: suspend () -> T): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V: ViewModel> create(modelClass: Class<V>): V = ContentReader(read) as V
    }
}
