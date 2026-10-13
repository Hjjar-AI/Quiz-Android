@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.bookmarks

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppTextButton

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext

import com.mukhtabir.android.ui.components.RefreshOnResume

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarksUiState(
    val items: List<QuestionDto> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val mutationError: Throwable? = null,
    val removingId: Long? = null,
    val recoveryRequired: Boolean = false,
    val removed: Boolean? = null,
)

class BookmarksViewModel(
    private val service: BookmarkService,
    private var pageSize: Int,
    userId: Long,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, userId, "bookmarks_navigation")
    private val _state = MutableStateFlow(BookmarksUiState(page = saved.page()))
    val state = _state.asStateFlow()
    private var requestedPage = saved.page()
    private var loadJob: Job? = null

    init { load() }

    fun applyPageSize(value: Int) {
        val normalized = value.coerceAtLeast(1)
        if (pageSize == normalized) return
        pageSize = normalized
        // The mutation's follow-up read uses the new size once it completes.
        if (_state.value.removingId != null) return
        loadJob?.cancel()
        _state.update { it.copy(isLoading = false) }
        load(requestedPage)
    }

    fun load(page: Int = requestedPage) {
        if (_state.value.isLoading || _state.value.removingId != null) return
        requestedPage = page.coerceAtLeast(1)
        saved.savePage(requestedPage)
        val size = pageSize
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            runCatching {
                var response = service.page(requestedPage, size)
                while (response.page > response.totalPages.coerceAtLeast(1)) {
                    ensureActive()
                    requestedPage = response.totalPages.coerceAtLeast(1)
                    saved.savePage(requestedPage)
                    response = service.page(requestedPage, size)
                }
                response
            }.onSuccess { response ->
                ensureActive()
                saved.savePage(response.page)
                _state.update {
                    it.copy(items = response.items, page = response.page,
                        totalPages = response.totalPages, total = response.total,
                        isLoading = false, recoveryRequired = false, mutationError = null)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                ensureActive()
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    fun retry() = load(requestedPage)

    fun remove(questionId: Long) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.removingId != null || snapshot.recoveryRequired ||
            snapshot.error != null || snapshot.items.none { it.id == questionId }
        ) return
        _state.update { it.copy(removingId = questionId, mutationError = null, removed = null) }
        viewModelScope.launch {
            runCatching { service.set(questionId, false) }
                .onSuccess { added ->
                    ensureActive()
                    _state.update {
                        it.copy(
                            items = if (added) it.items else it.items.filterNot { row -> row.id == questionId },
                            total = if (added) it.total else (it.total - 1).coerceAtLeast(0),
                            removingId = null,
                            recoveryRequired = true,
                            removed = !added,
                        )
                    }
                    // Refresh the explicit removal; repeated requests cannot add the bookmark.
                    load(if (!added && snapshot.items.size == 1 && snapshot.page > 1) {
                        snapshot.page - 1
                    } else snapshot.page)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    ensureActive()
                    _state.update { it.copy(removingId = null, mutationError = error,
                        recoveryRequired = true) }
                }
        }
    }

    fun previousPage() = _state.value.let { if (it.page > 1) load(it.page - 1) else Unit }
    fun nextPage() = _state.value.let { if (it.page < it.totalPages) load(it.page + 1) else Unit }

    class Factory(
        private val service: BookmarkService,
        private val pageSize: Int,
        private val userId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            BookmarksViewModel(service, pageSize.coerceAtLeast(1), userId, extras.createSavedStateHandle()) as T
    }
}

/**
 * Bookmarks screen.
 *
 * The per-screen Back button was removed in Phase 0. The system back
 * gesture is the primary exit; when reached from the bottom bar the
 * Bookmarks tab simply stays active.
 */
@Composable
fun BookmarksRoute(
    service: BookmarkService,
    imageRepository: ImageRepository,
    canStartSessions: Boolean,
    pageSize: Int,
    userId: Long,
    onOpenQuestion: (Long) -> Unit,
    onStartQuestions: (TestMode, List<Long>, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    onDownloadOffline: ((List<Long>) -> Unit)? = null,
) {
    val viewModel: BookmarksViewModel = viewModel(
        key = "bookmarks-$userId",
        factory = BookmarksViewModel.Factory(service, pageSize, userId),
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(pageSize) { viewModel.applyPageSize(pageSize) }
    RefreshOnResume(viewModel::retry)
    val busy = state.isLoading || state.removingId != null
    val canAct = !busy && !state.recoveryRequired && state.error == null
    val sessionLabel = stringResource(R.string.bookmarks)
    var showPractice by rememberSaveable(userId) { mutableStateOf(false) }

    com.mukhtabir.android.ui.components.PullToRefresh(state.isLoading,viewModel::retry,enabled=state.removingId==null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppPageHeading(stringResource(R.string.bookmarks))
        if (canStartSessions && state.items.isNotEmpty()) Button({ showPractice = true }) {
            Text(stringResource(R.string.library_practice, state.items.size))
        }
        if (busy) AppLoadingIndicator()
        val actionError = state.mutationError
        val loadError = state.error
        if (actionError != null || loadError != null) {
            ErrorState(
                throwable = loadError ?: actionError,
                onRetry = if (busy) null else viewModel::retry,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (state.recoveryRequired && !busy) {
            Text(stringResource(R.string.bookmark_recovery_required),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.removed?.let { removed ->
            Text(stringResource(if (removed) R.string.bookmark_removed else R.string.bookmark_remove_changed),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        when {
            state.items.isEmpty() && canAct -> {
                EmptyState(title = stringResource(R.string.no_items))
            }
            state.items.isNotEmpty() -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            ) {
                items(state.items, key = { it.id }) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenQuestion(question.id) },
                    ) {
                        com.mukhtabir.android.ui.components.AppCardBody() {
                            ClinicalCaseContext(question.localizedForDisplay().case)
                            MarkdownText(
                                question.localizedForDisplay().question,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            QuestionImage(question.imageUrl, imageRepository)
                            question.categoryName?.let { Text(it) }
                            OutlinedButton(onClick = { viewModel.remove(question.id) }, enabled = canAct) {
                                Text(stringResource(R.string.remove_bookmark))
                            }
                        }
                    }
                }
                if (state.totalPages > 1) {
                    item {
                        AppPagination(state.page, state.totalPages, onPrevious = viewModel::previousPage, onNext = viewModel::nextPage, previousEnabled = state.page > 1 && !busy, nextEnabled = state.page < state.totalPages && !busy, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    }
    if (showPractice && canStartSessions) com.mukhtabir.android.ui.components.AppConfirmationDialog(
        onDismissRequest = { showPractice = false },
        title = { Text(stringResource(R.string.library_practice_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                Text(stringResource(R.string.selection_page_fallback))
                Button({ showPractice = false; onStartQuestions(TestMode.STUDY, state.items.map { it.id }, sessionLabel) },
                    enabled = canAct && state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_study)) }
                OutlinedButton({ showPractice = false; onStartQuestions(TestMode.EXAM, state.items.map { it.id }, sessionLabel) },
                    enabled = canAct && state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_exam)) }
                onDownloadOffline?.let { download ->
                    OutlinedButton({ showPractice = false; download(state.items.map { it.id }) },
                        enabled = canAct && state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.offline_download)) }
                }
            }
        },
        confirmButton = { AppTextButton({ showPractice = false }) { Text(stringResource(R.string.close)) } },
    )
}
