@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.fragile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.PagedLearningQuestions
import com.mukhtabir.android.ui.components.PracticeLauncher
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FragileUiState(
    val items: List<QuestionDto> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasUnknownError: Boolean = false,
)

class FragileViewModel(
    private val service: QuestionService,
    private var pageSize: Int,
    userId: Long,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, userId, "fragile_navigation")
    private val _state = MutableStateFlow(FragileUiState(page = saved.page()))
    val state = _state.asStateFlow()
    private var requestedPage = saved.page()
    private var loadJob: Job? = null

    init {
        load(requestedPage)
    }

    fun applyPageSize(value: Int) {
        val normalized = value.coerceAtLeast(1)
        if (pageSize == normalized) return
        pageSize = normalized
        loadJob?.cancel()
        _state.update { it.copy(isLoading = false) }
        load(requestedPage)
    }

    fun load(page: Int) {
        if (_state.value.isLoading) return
        requestedPage = page.coerceAtLeast(1)
        saved.savePage(requestedPage)
        val size = pageSize
        // Set this before launch so repeated taps cannot start a second load.
        _state.update { it.copy(isLoading = true, error = null, hasUnknownError = false) }
        loadJob = viewModelScope.launch {
            runCatching {
                var response = service.fragile(page = requestedPage, perPage = size)
                // Learning can remove rows between visits. Recover a page that
                // no longer exists instead of showing a misleading empty list.
                while (response.page > response.totalPages.coerceAtLeast(1)) {
                    ensureActive()
                    requestedPage = response.totalPages.coerceAtLeast(1)
                    saved.savePage(requestedPage)
                    response = service.fragile(page = requestedPage, perPage = size)
                }
                response
            }.onSuccess { response ->
                ensureActive()
                saved.savePage(response.page)
                _state.update {
                    it.copy(
                        items = response.items,
                        page = response.page,
                        totalPages = response.totalPages,
                        total = response.total,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                ensureActive()
                _state.update {
                    it.copy(isLoading = false, error = error.message,
                        hasUnknownError = error.message == null)
                }
            }
        }
    }

    fun retry() = load(requestedPage)

    fun nextPage() {
        val s = _state.value
        if (s.page < s.totalPages) load(s.page + 1)
    }

    fun previousPage() {
        val s = _state.value
        if (s.page > 1) load(s.page - 1)
    }

    class Factory(
        private val service: QuestionService,
        private val pageSize: Int,
        private val userId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            FragileViewModel(service, pageSize.coerceAtLeast(1), userId, extras.createSavedStateHandle()) as T
    }
}

/**
 * Fragile-correct notebook (Phase 1, item 15).
 *
 * Lists every question the caller last answered correctly but with
 * low confidence. Same shape as [MistakesRoute] — the "Practise
 * these" action hands the current page's ids to the preset holder.
 */
@Composable
fun FragileRoute(
    service: QuestionService,
    imageRepository: ImageRepository,
    canStartSessions: Boolean,
    maxQuestions: Int,
    pageSize: Int,
    userId: Long,
    onStartQuestions: (com.mukhtabir.android.data.remote.TestMode, List<Long>, String) -> Unit,
    onOpenQuestion: (Long) -> Unit,
) {
    val viewModel: FragileViewModel = viewModel(
        key = "fragile-$userId",
        factory = FragileViewModel.Factory(service, pageSize, userId),
    )
    val state by viewModel.state.collectAsLifecycleState()
    LaunchedEffect(pageSize) { viewModel.applyPageSize(pageSize) }
    RefreshOnResume(viewModel::retry)
    var showPractice by androidx.compose.runtime.remember(userId) { androidx.compose.runtime.mutableStateOf(false) }
    if (showPractice && canStartSessions) {
        val ids = androidx.compose.runtime.remember { state.items.map { it.id } }
        PracticeLauncher(stringResource(R.string.session_label_fragile), ids, maxQuestions,
            enabled = !state.isLoading && state.error == null && !state.hasUnknownError,
            onDismiss = { showPractice = false }, onStart = { mode, selected, title ->
                showPractice = false; onStartQuestions(mode, selected, title)
            })
    }

    PagedLearningQuestions(
        title = stringResource(R.string.fragile_title), emptyTitle = stringResource(R.string.fragile_empty),
        emptyMessage = stringResource(R.string.fragile_empty_message), questions = state.items,
        page = state.page, totalPages = state.totalPages, loading = state.isLoading,
        error = state.error, hasUnknownError = state.hasUnknownError, canPractice = canStartSessions,
        imageRepository = imageRepository, onPractice = { showPractice = true }, onRetry = viewModel::retry,
        onPrevious = viewModel::previousPage, onNext = viewModel::nextPage, onOpenQuestion = onOpenQuestion,
    )
}
