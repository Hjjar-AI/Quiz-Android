package com.mukhtabir.android.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Reuses authenticated, profile-bound encrypted storage; never replays a write. */
class EditorRecovery<T>(private val scope: CoroutineScope, private val store: LearnerStorage,
    private val userId: Long, private val key: String, private val serializer: KSerializer<T>) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private var latest: T? = null
    private val _pending = MutableStateFlow<T?>(null)
    val pending = _pending.asStateFlow()
    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    val blocked: Boolean get() = !_ready.value || _pending.value != null || _error.value != null
    init { reload() }
    fun reload() {
        if (_ready.value && _error.value == null) return
        if (_ready.value && _pending.value == null) { scope.launch { persist(latest) }; return }
        scope.launch {
            mutex.withLock {
                try {
                    val raw = withContext(Dispatchers.IO) { store.readEditor(userId, key).getOrThrow() }
                    _pending.value = raw?.let { json.decodeFromString(serializer, it) }
                    _error.value = null
                    _ready.value = true
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { _error.value = failure }
            }
        }
    }
    @OptIn(FlowPreview::class)
    fun <S> observe(state: StateFlow<S>, canPersist: (S) -> Boolean = { true }, snapshot: (S) -> T?) {
        scope.launch {
            state.debounce(300).collect {
                mutex.withLock {
                    if (_ready.value && _pending.value == null && canPersist(state.value)) {
                        latest = snapshot(state.value)
                        if (!blocked) writeLocked(latest)
                    }
                }
            }
        }
    }
    suspend fun persist(value: T?): Boolean = mutex.withLock { writeLocked(value) }
    private suspend fun writeLocked(value: T?): Boolean {
        latest = value
        try {
            withContext(Dispatchers.IO) {
                if (value == null) store.clearEditor(userId, key).getOrThrow()
                else store.writeEditor(userId, key, json.encodeToString(serializer, value)).getOrThrow()
            }
            _error.value = null
            return true
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { _error.value = failure; return false }
    }
    fun restore(accept: (T) -> Unit) {
        val value = _pending.value ?: return
        accept(value)
        _pending.value = null
    }
    fun discard() { scope.launch { if (persist(null)) _pending.value = null } }
}
