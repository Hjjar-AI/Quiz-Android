package com.mukhtabir.android.ui.questions

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class QuestionImageState(
    val question: QuestionDto? = null,
    val selected: SelectedQuestionImage? = null,
    val busy: Boolean = false,
    val denied: Boolean = false,
    val error: Throwable? = null,
    val storageError: Throwable? = null,
    val needsReview: Boolean = false,
    val changed: Boolean = false,
    val success: Boolean = false,
    val imageRevision: Int = 0,
)

internal class QuestionImageViewModel(
    private val context: Context,
    private val id: Long,
    private val questions: QuestionService,
    private val userId: Long,
    private val canEditAny: Boolean,
    private val canUploadOwn: Boolean,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
) : ViewModel() {
    private val _state=MutableStateFlow(QuestionImageState())
    val state=_state.asStateFlow()
    private fun permitted(q: QuestionDto) = canEditAny || (canUploadOwn && q.ownedBy==userId)
    private val recoveryMutex = Mutex()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = "image-$id"
    private val pendingRecovery = MutableStateFlow<StoredImageEditor?>(null)
    val recovery = pendingRecovery.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.readEditor(userId, storageKey).getOrThrow() }
                coroutineContext.ensureActive()
                pendingRecovery.value = raw?.let { json.decodeFromString(StoredImageEditor.serializer(), it) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(error = failure, storageError = failure) } }
            refresh()
        }
    }
    private suspend fun persist(selected: SelectedQuestionImage?, needsReview: Boolean): Boolean = recoveryMutex.withLock {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (selected == null) store.clearEditor(userId, storageKey)
            else store.writeEditor(userId, storageKey, json.encodeToString(StoredImageEditor.serializer(), StoredImageEditor(
                _state.value.question?.imageUrl, android.util.Base64.encodeToString(selected.bytes, android.util.Base64.NO_WRAP), needsReview)))
        }
        coroutineContext.ensureActive()
        _state.update { it.copy(storageError = result.exceptionOrNull(), error = if (result.isFailure) result.exceptionOrNull() else it.error) }
        result.isSuccess
    }
    fun restoreDraft() {
        val saved = pendingRecovery.value ?: return
        val s = _state.value
        if (s.busy || s.denied || s.question == null) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.mukhtabir.android.data.remote.decodeQuestionImageData(saved.data) }
                val image = decodeQuestionImage(context, bytes)
                coroutineContext.ensureActive()
                pendingRecovery.value = null
                _state.update { it.copy(selected = image, busy = false, needsReview = saved.needsReview || s.question.imageUrl != saved.baseImageUrl) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(busy = false, error = failure) } }
        }
    }
    fun discardRecovery() {
        if (_state.value.busy) return
        viewModelScope.launch { if (persist(null, false)) pendingRecovery.value = null }
    }


    fun refresh() {
        if(_state.value.busy) return
        _state.update { it.copy(busy=true,error=null,success=false) }
        viewModelScope.launch {
            try {
                val q=questions.get(id)
                coroutineContext.ensureActive()
                _state.update { it.copy(question=q,denied=!permitted(q),busy=false,needsReview=false,imageRevision=it.imageRevision+1) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(busy=false,error=failure,needsReview=true) } }
        }
    }
    fun select(uri: Uri, onRead: () -> Unit = {}) {
        val s=_state.value
        if(s.busy || s.denied || s.question==null || pendingRecovery.value != null) { onRead(); return }
        _state.update { it.copy(busy=true,error=null,success=false) }
        viewModelScope.launch {
            try {
                val image=readQuestionImage(context,uri)
                coroutineContext.ensureActive()
                val stored = persist(image, s.needsReview)
                _state.update { it.copy(selected=image,busy=false, error = if (stored) null else it.error) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) { coroutineContext.ensureActive(); _state.update { it.copy(busy=false,error=if(failure is java.io.IOException || failure is SecurityException)
                    ApiException(null,context.getString(R.string.question_image_unreadable),failure) else failure) } }
            finally { onRead() }
        }
    }
    fun selectionError(error: Throwable) {
        if(!_state.value.busy) _state.update { it.copy(error=error,success=false) }
    }
    fun keepDraft(onDone: () -> Unit) {
        val s = _state.value
        if (s.busy || s.selected == null) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = persist(s.selected, s.needsReview)
            _state.update { it.copy(busy = false) }
            if (saved) onDone()
        }
    }
    fun clearSelection() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = persist(null, false)
            _state.update { it.copy(busy = false, selected = if (saved) null else it.selected, success = false) }
        }
    }
    fun consumeChange() = _state.update { it.copy(changed=false) }
    fun upload() = mutate(remove=false)
    fun remove() = mutate(remove=true)
    private fun mutate(remove: Boolean) {
        val s=_state.value
        val q=s.question ?: return
        if(s.busy || s.denied || s.needsReview || !permitted(q) || (!remove && s.selected==null) || (remove && q.imageUrl.isNullOrBlank())) return
        _state.update { it.copy(busy=true,error=null,success=false) }
        viewModelScope.launch {
            try {
                // Best-effort preflight; Django does not support image version conditions.
                val current=questions.get(id)
                coroutineContext.ensureActive()
                if(!permitted(current) || current.imageUrl!=q.imageUrl) {
                    _state.update { it.copy(question=current,denied=!permitted(current),busy=false,changed=true,imageRevision=it.imageRevision+1,
                        error=ApiException(null,context.getString(R.string.question_image_changed))) }
                    return@launch
                }
                if (!persist(s.selected, true)) { _state.update { it.copy(busy = false) }; return@launch }
                val result=if(remove) questions.removeImage(id) else {
                    val image=requireNotNull(s.selected)
                    questions.uploadImage(id,image.bytes,image.mime,image.filename)
                }
                coroutineContext.ensureActive()
                if (!remove) persist(null, false)
                else persist(s.selected, false)
                _state.update { it.copy(question=current.copy(imageUrl=result.imageUrl),selected=if(remove) it.selected else null,
                    busy=false,needsReview=false,changed=true,success=true,imageRevision=it.imageRevision+1) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                // A missing response can still mean the server saved the image. Require a read before another write.
                persist(s.selected, true)
                _state.update { it.copy(busy=false,error=failure,needsReview=true,changed=true) }
            }
        }
    }
    class Factory(private val context: Context,private val id: Long,private val questions: QuestionService,
        private val userId: Long,private val canEditAny: Boolean,private val canUploadOwn: Boolean, private val store: com.mukhtabir.android.core.SessionDraftStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T =
            QuestionImageViewModel(context.applicationContext,id,questions,userId,canEditAny,canUploadOwn,store) as T
    }
}

@kotlinx.serialization.Serializable
internal data class StoredImageEditor(val baseImageUrl: String?, val data: String, val needsReview: Boolean = false)
