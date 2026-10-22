@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ActiveFilter
import com.mukhtabir.android.ui.components.ActiveFilterSummary
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppBottomSheet
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppFilterOptions
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.PracticeLauncher
import com.mukhtabir.android.ui.components.PullToRefresh
import com.mukhtabir.android.ui.components.QuestionListCard
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
    maxQuestions: Int,
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
    val state by viewModel.state.collectAsLifecycleState()
    LaunchedEffect(pageSize) { viewModel.applyPageSize(pageSize) }
    RefreshOnResume(viewModel::retry)
    val busy = state.isLoading || state.removingId != null
    val canAct = !busy && !state.recoveryRequired && state.error == null
    val sessionLabel = stringResource(R.string.bookmarks)
    var showPractice by rememberSaveable(userId) { mutableStateOf(false) }
    var showFilters by rememberSaveable(userId) { mutableStateOf(false) }
    var query by androidx.compose.runtime.remember(userId, state.page, pageSize) { mutableStateOf("") }
    var category by androidx.compose.runtime.remember(userId, state.page, pageSize) { mutableStateOf<String?>(null) }
    var book by androidx.compose.runtime.remember(userId, state.page, pageSize) { mutableStateOf<String?>(null) }
    var tag by androidx.compose.runtime.remember(userId, state.page, pageSize) { mutableStateOf<String?>(null) }
    val filteredItems = state.items.filter { question ->
        (category == null || question.categoryName == category) && (book == null || question.sourceDocument == book) &&
            (tag?.let { it in question.tags } ?: true) && question.localizedForDisplay().question.contains(query, ignoreCase = true)
    }


    val filterControls: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
        AppTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.search_questions)) }, enabled = !busy)
        Text(stringResource(R.string.ui_page_filter_hint), style = MaterialTheme.typography.bodySmall)
        ActiveFilterSummary(buildList {
            if (query.isNotBlank()) add(ActiveFilter("query", query, { query = "" }))
            category?.let { value -> add(ActiveFilter("category", stringResource(R.string.quiz_filter_category, value), { category = null })) }
            book?.let { value -> add(ActiveFilter("book", stringResource(R.string.quiz_filter_book, value), { book = null })) }
            tag?.let { value -> add(ActiveFilter("tag", stringResource(R.string.quiz_filter_tag, value), { tag = null })) }
        }, onClear = { query = ""; category = null; book = null; tag = null }, enabled = !busy)
        AppOutlinedButton({ showFilters = true }, enabled = !busy) {
            Text(stringResource(R.string.library_active_filters, com.mukhtabir.android.util.formatNumber(listOf(category, book, tag).count { it != null })))
        }
        }
    }

    PullToRefresh(state.isLoading,viewModel::retry,enabled=state.removingId==null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppPageHeading(stringResource(R.string.bookmarks))
        if (canStartSessions && filteredItems.isNotEmpty()) AppButton({ showPractice = true }) {
            Text(stringResource(R.string.library_practice, com.mukhtabir.android.util.formatNumber(filteredItems.size)))
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
                item { filterControls() }
                if (filteredItems.isEmpty() && !busy && state.error == null) item { EmptyState(stringResource(R.string.no_questions_found)) }
                items(filteredItems, key = { it.id }) { question ->
                    QuestionListCard(question, imageRepository,
                        onOpen = { onOpenQuestion(question.id) }) {
                        AppOutlinedButton(onClick = { viewModel.remove(question.id) }, enabled = canAct) {
                            Text(stringResource(R.string.remove_bookmark))
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
    if (showFilters) AppBottomSheet(
        stringResource(R.string.ui_page_filters), onDismiss = { showFilters = false }, actions = {
            AppActionRow {
                AppButton({ showFilters = false }) { Text(stringResource(R.string.done)) }
                AppTextButton({ category = null; book = null; tag = null }, enabled = !busy) {
                    Text(stringResource(R.string.ui_clear_all))
                }
            }
        }) {
        Text(stringResource(R.string.ui_page_filter_hint), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.ux_filter_single_tag), style = MaterialTheme.typography.bodySmall)
        val any = "" to stringResource(R.string.difficulty_any)
        AppFilterOptions(stringResource(R.string.source_categories),
            listOf(any) + state.items.mapNotNull { it.categoryName }.distinct().map { it to it }, category.orEmpty(),
            onSelect = { category = it.takeIf(String::isNotBlank) }, enabled = !busy)
        AppFilterOptions(stringResource(R.string.select_source_book),
            listOf(any) + state.items.mapNotNull { it.sourceDocument?.takeIf(String::isNotBlank) }.distinct().map { it to it }, book.orEmpty(),
            onSelect = { book = it.takeIf(String::isNotBlank) }, enabled = !busy)
        AppFilterOptions(stringResource(R.string.source_tags),
            listOf(any) + state.items.flatMap { it.tags }.distinct().map { it to it }, tag.orEmpty(),
            onSelect = { tag = it.takeIf(String::isNotBlank) }, enabled = !busy)
    }
    if (showPractice && canStartSessions) {
        val ids = androidx.compose.runtime.remember { filteredItems.map { it.id } }
        PracticeLauncher(sessionLabel, ids, maxQuestions, enabled = canAct,
            onDismiss = { showPractice = false },
            onStart = { mode, selected, title -> showPractice = false; onStartQuestions(mode, selected, title) },
            onDownload = onDownloadOffline?.let { download -> ({ selected -> showPractice = false; download(selected) }) })
    }
}
