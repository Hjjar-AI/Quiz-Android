@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.QuestionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class QuestionDetailViewModel(
    private val questionId: Long,
    private val questionService: QuestionService,
    private val bookmarkService: BookmarkService,
    private val userId: Long,
    private var canDuplicate: Boolean,
    private var canDeleteOwn: Boolean,
    private var canDeleteAny: Boolean,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionDetailUiState())
    val state = _state.asStateFlow()

    fun permissions(duplicate: Boolean, deleteOwn: Boolean, deleteAny: Boolean) {canDuplicate=duplicate;canDeleteOwn=deleteOwn;canDeleteAny=deleteAny}
    private var pendingBookmark: Boolean? = null
    private var duplicateOperation: String? = null
    val recovery = com.mukhtabir.android.core.EditorRecovery(viewModelScope, store, userId,
        "question_detail-$questionId", DetailRecovery.serializer())
    init { load() }
    private fun snapshot() = if(pendingBookmark != null || duplicateOperation != null) DetailRecovery(pendingBookmark, duplicateOperation) else null
    fun restore(value: DetailRecovery) {
        pendingBookmark = value.bookmark; duplicateOperation = value.operation
        _state.update { it.copy(duplicateUncertain = value.operation != null) }
    }

    fun load() {
        if (_state.value.isLoading || _state.value.mutationBusy || _state.value.bookmarkBusy) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { questionService.get(questionId) }
                .onSuccess { question ->
                    ensureActive()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {store.recordLastQuestion(userId,question.id)}
                    ensureActive()
                    _state.value = QuestionDetailUiState(question = question, isLoading = false, duplicateUncertain = duplicateOperation != null)
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _state.update { it.copy(isLoading = false, error = failure.message) }
                }
        }
    }

    fun toggleBookmark() {
        if (recovery.blocked || duplicateOperation != null || _state.value.bookmarkBusy || _state.value.mutationBusy || _state.value.isLoading || _state.value.question == null) return
        _state.update { it.copy(bookmarkBusy = true, actionError = null, bookmarkChange = null) }
        viewModelScope.launch {
            try {
                val actual = bookmarkService.state(questionId)
                val desired = pendingBookmark ?: !actual
                pendingBookmark = desired
                if(!recovery.persist(snapshot())) return@launch
                // An explicit-state write is safe, but restored intent is first checked against live state.
                val added = if(actual == desired) actual else bookmarkService.set(questionId, desired)
                pendingBookmark = null
                recovery.persist(snapshot())
                _state.update { it.copy(bookmarkChange = if (added) BookmarkChange.ADDED else BookmarkChange.REMOVED) }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _state.update { it.copy(actionError = failure.message) }
            } finally {
                _state.update { it.copy(bookmarkBusy = false) }
            }
        }
    }

    fun duplicate() = mutate(delete = false)
    fun retryDuplicate() { if (duplicateOperation != null) mutate(delete = false, retry = true) }
    fun delete(version: Int) = mutate(delete = true, deleteVersion=version)

    private fun mutate(delete: Boolean, retry: Boolean = false, deleteVersion: Int? = null) {
        val s = _state.value
        val question = s.question
        if (recovery.blocked || pendingBookmark != null || s.mutationBusy || s.bookmarkBusy || s.isLoading || s.deleted || s.duplicatedId != null) return
        if (delete) {
            if (question == null || (!canDeleteAny && !(canDeleteOwn && question.ownedBy == userId))) return
        } else if (!canDuplicate || (question == null && (duplicateOperation == null || retry))) return
        val reconcilingDuplicate = duplicateOperation != null
        _state.update { it.copy(mutationBusy = true, actionError = null, deleteConflict = false) }
        viewModelScope.launch {
            try {
                if (delete) {
                    questionService.delete(questionId,requireNotNull(deleteVersion))
                    _state.update { it.copy(deleted = true) }
                } else {
                    val pending = duplicateOperation
                    val copy = if (pending != null && !retry) {
                        // Resolve a lost response with a read; never blindly create again.
                        questionService.duplicateReceipt(questionId, pending)
                    } else {
                        val operation = pending ?: java.util.UUID.randomUUID().toString()
                        duplicateOperation = operation
                        if(!recovery.persist(snapshot())) return@launch
                        questionService.duplicate(questionId, operation)
                    }
                    duplicateOperation = null
                    recovery.persist(snapshot())
                    _state.update { it.copy(duplicatedId = copy.id, duplicateUncertain = false) }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                if (!delete && !reconcilingDuplicate && failure is ApiException && failure.statusCode in setOf(400, 403, 404, 409, 429)) duplicateOperation = null
                recovery.persist(snapshot())
                _state.update { it.copy(actionError = failure.message, duplicateUncertain = duplicateOperation != null,
                    deleteConflict = delete && failure is ApiException && failure.statusCode == 409) }
            } finally {
                _state.update { it.copy(mutationBusy = false) }
            }
        }
    }

    fun consumeMutation() = _state.update { it.copy(duplicatedId = null, deleted = false) }

    class Factory(
        private val questionId: Long,
        private val questionService: QuestionService,
        private val bookmarkService: BookmarkService,
        private val userId: Long, private val canDuplicate: Boolean,
        private val canDeleteOwn: Boolean, private val canDeleteAny: Boolean,
        private val store: com.mukhtabir.android.core.SessionDraftStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuestionDetailViewModel(questionId, questionService, bookmarkService, userId, canDuplicate, canDeleteOwn, canDeleteAny, store) as T
    }
}
