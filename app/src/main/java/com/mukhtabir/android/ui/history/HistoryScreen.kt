package com.mukhtabir.android.ui.history

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.HistoryItemDto
import com.mukhtabir.android.data.remote.HistoryDetailDto
import com.mukhtabir.android.data.remote.HistoryService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.SessionResultReview
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<HistoryItemDto> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val expandedId: Long? = null,
    val detail: HistoryDetailDto? = null,
    val detailLoading: Boolean = false,
    val isLoading: Boolean = false,
    val userFilter: String = "",
    val appliedUserFilter: String = "",
    val invalidUserFilter: Boolean = false,
    val error: Throwable? = null,
    val detailError: Throwable? = null,
)

enum class HistoryScope { OWN, ALL }

class HistoryViewModel(
    private val service: HistoryService,
    private val scope: HistoryScope,
    private val signedInUserId: Long,
    private var pageSize: Int,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, signedInUserId, "history_${scope.name}_navigation")
    private val _state = MutableStateFlow(HistoryUiState(
        page = saved.page(),
        expandedId = saved.expandedId(),
        userFilter = if (scope == HistoryScope.ALL) saved.draftFilter().orEmpty() else "",
        appliedUserFilter = if (scope == HistoryScope.ALL) saved.filter().orEmpty() else "",
    ))
    val state = _state.asStateFlow()
    private var requestedPage = saved.page()
    private var loadJob: Job? = null
    private var detailJob: Job? = null

    init { load() }

    fun applyPageSize(value: Int) {
        val normalized = value.coerceAtLeast(1)
        if (pageSize == normalized) return
        pageSize = normalized
        loadJob?.cancel()
        _state.update { it.copy(isLoading = false) }
        load(requestedPage)
    }

    fun load(page: Int = requestedPage) {
        if (_state.value.isLoading) return
        // Pagination/retry use the applied filter, never an unsubmitted draft.
        val requestedUserId = when (scope) {
            HistoryScope.OWN -> signedInUserId
            HistoryScope.ALL -> {
                val filter = _state.value.appliedUserFilter
                if (filter.isBlank()) null else filter.toLongOrNull()?.takeIf { it > 0 } ?: run {
                    _state.update { it.copy(invalidUserFilter = true) }
                    return
                }
            }
        }
        requestedPage = page.coerceAtLeast(1)
        saved.savePage(requestedPage)
        val size = pageSize
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            runCatching {
                var response = service.list(page = requestedPage, perPage = size, userId = requestedUserId)
                while (response.page > response.totalPages.coerceAtLeast(1)) {
                    ensureActive()
                    requestedPage = response.totalPages.coerceAtLeast(1)
                    saved.savePage(requestedPage)
                    response = service.list(page = requestedPage, perPage = size, userId = requestedUserId)
                }
                response
            }.onSuccess { response ->
                ensureActive()
                val expanded = _state.value.expandedId?.takeIf { id -> response.items.any { it.id == id } }
                if (expanded == null) detailJob?.cancel()
                saved.savePage(response.page)
                saved.saveExpandedId(expanded)
                _state.update {
                    it.copy(items = response.items, page = response.page,
                        totalPages = response.totalPages, total = response.total,
                        isLoading = false, expandedId = expanded,
                        detail = it.detail.takeIf { expanded != null },
                        detailError = it.detailError.takeIf { expanded != null },
                        detailLoading = expanded != null && it.detailLoading)
                }
                if (expanded != null && _state.value.detail == null && !_state.value.detailLoading &&
                    _state.value.detailError == null
                ) loadDetail(expanded)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                ensureActive()
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    fun retry() = load(requestedPage)

    fun setUserFilter(value: String) {
        if (scope != HistoryScope.ALL) return
        _state.update { it.copy(userFilter = value, invalidUserFilter = false) }
        saved.saveDraftFilter(value)
    }

    fun applyUserFilter() {
        if (scope != HistoryScope.ALL || _state.value.isLoading) return
        val raw = _state.value.userFilter.trim()
        val id = raw.toLongOrNull()?.takeIf { it > 0 }
        if (raw.isNotEmpty() && id == null) {
            _state.update { it.copy(invalidUserFilter = true) }
            return
        }
        val filter = id?.toString().orEmpty()
        closeDetails()
        // Rows for another filter must not appear under the new filter label.
        _state.update { it.copy(appliedUserFilter = filter, invalidUserFilter = false,
            items = emptyList(), page = 1, total = 0, totalPages = 1) }
        saved.saveFilter(filter)
        load(1)
    }

    fun clearUserFilter() {
        if (scope != HistoryScope.ALL || _state.value.isLoading) return
        setUserFilter("")
        applyUserFilter()
    }

    fun previousPage() = _state.value.let { if (it.page > 1) load(it.page - 1) else Unit }
    fun nextPage() = _state.value.let { if (it.page < it.totalPages) load(it.page + 1) else Unit }

    fun toggleDetails(id: Long) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.items.none { it.id == id }) return
        if (snapshot.expandedId == id) {
            closeDetails()
        } else {
            closeDetails()
            _state.update { it.copy(expandedId = id) }
            saved.saveExpandedId(id)
            loadDetail(id)
        }
    }

    fun retryDetails() {
        val snapshot = _state.value
        val id = snapshot.expandedId ?: return
        if (snapshot.isLoading || snapshot.detailLoading || snapshot.items.none { it.id == id }) return
        loadDetail(id)
    }

    private fun closeDetails() {
        detailJob?.cancel()
        _state.update { it.copy(expandedId = null, detail = null,
            detailLoading = false, detailError = null) }
        saved.saveExpandedId(null)
    }

    private fun loadDetail(id: Long) {
        detailJob?.cancel()
        _state.update { it.copy(detailLoading = true, detailError = null) }
        detailJob = viewModelScope.launch {
            runCatching { service.detail(id).also { check(it.id == id) } }
                .onSuccess { detail ->
                    ensureActive()
                    if (_state.value.expandedId != id) return@onSuccess
                    _state.update { it.copy(detail = detail, detailLoading = false) }
                }.onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    ensureActive()
                    if (_state.value.expandedId != id) return@onFailure
                    _state.update { it.copy(detailLoading = false, detailError = failure) }
                }
        }
    }

    class Factory(
        private val service: HistoryService,
        private val scope: HistoryScope,
        private val signedInUserId: Long,
        private val pageSize: Int,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            HistoryViewModel(service, scope, signedInUserId, pageSize.coerceAtLeast(1),
                extras.createSavedStateHandle()) as T
    }
}

/**
 * History screen.
 *
 * Per-screen Back button removed in Phase 0.
 */
@Composable
fun HistoryRoute(
    signedInUserId: Long,
    scope: HistoryScope = HistoryScope.OWN,
    pageSize: Int,
    service: HistoryService,
    imageRepository: ImageRepository,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
) {
    val viewModel: HistoryViewModel = viewModel(
        key = "history-${scope.name.lowercase()}-$signedInUserId",
        factory = HistoryViewModel.Factory(service, scope, signedInUserId, pageSize),
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(pageSize) { viewModel.applyPageSize(pageSize) }
    RefreshOnResume(viewModel::retry)
    val showAllUsers = scope == HistoryScope.ALL

    com.mukhtabir.android.ui.components.PullToRefresh(state.isLoading,viewModel::retry,enabled=!state.detailLoading) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppPageHeading(stringResource(if (showAllUsers) R.string.all_history else R.string.history))

        if (showAllUsers) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
            ) {
                OutlinedTextField(
                    value = state.userFilter,
                    onValueChange = viewModel::setUserFilter,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.filter_user_id)) },
                    isError = state.invalidUserFilter,
                    supportingText = {
                        if (state.invalidUserFilter) {
                            Text(stringResource(R.string.invalid_user_id))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                Button(
                    onClick = viewModel::applyUserFilter,
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(R.string.search))
                }
            }
            com.mukhtabir.android.ui.components.ActiveFilterSummary(
                if (state.appliedUserFilter.isBlank()) emptyList() else listOf(com.mukhtabir.android.ui.components.ActiveFilter(
                    "user", stringResource(R.string.history_filter_user, state.appliedUserFilter), viewModel::clearUserFilter)),
                onClear = viewModel::clearUserFilter, enabled = !state.isLoading)

        }

        if (showAllUsers) {
            Text(if (state.appliedUserFilter.isBlank()) stringResource(R.string.history_filter_all)
                else stringResource(R.string.history_filter_user, state.appliedUserFilter))
        }
        if (state.isLoading) AppLoadingIndicator()
        state.error?.let {
            ErrorState(throwable = it,
                onRetry = if (state.isLoading) null else viewModel::retry,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        when {
            state.items.isEmpty() && !state.isLoading && state.error == null -> {
                EmptyState(title = stringResource(R.string.no_items))
            }
            state.items.isNotEmpty() -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
            ) {
                items(state.items, key = { it.id }) { item ->
                    val expanded = state.expandedId == item.id
                    val detailState = stringResource(if (expanded) R.string.history_expanded else R.string.history_collapsed)
                    val detailAction = stringResource(if (expanded) R.string.history_collapse else R.string.history_expand, item.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { stateDescription = detailState }
                            .clickable(enabled = !state.isLoading, onClickLabel = detailAction) {
                                viewModel.toggleDetails(item.id)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),
                            verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
                        ) {
                            if (showAllUsers) {
                                val userName = item.fullName?.takeIf(String::isNotBlank)
                                    ?: item.username?.takeIf(String::isNotBlank)
                                    ?: stringResource(R.string.unknown)
                                Text(
                                    stringResource(
                                        R.string.history_user,
                                        userName,
                                        item.user?.toString() ?: stringResource(R.string.unknown),
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.completed_session,
                                    item.mode,
                                    item.accuracy,
                                    item.correctCount,
                                    item.totalQuestions,
                                ),
                            )
                            item.masterMetadata.weightedScore?.let { score ->
                                Text(stringResource(R.string.report_weighted_score) + ": " + com.mukhtabir.android.util.formatDecimal(score) + "%")
                            }
                            com.mukhtabir.android.ui.components.AppMetadataRow(listOf(item.completedAt))
                            if (state.expandedId == item.id) {
                                com.mukhtabir.android.ui.components.AppMetadataRow(listOfNotNull(
                                    item.tag?.let { stringResource(R.string.history_tag,it) },
                                    stringResource(R.string.history_answered,item.answeredCount,item.totalQuestions),
                                    stringResource(R.string.history_duration,formatDuration(item.timeSpent)),
                                    stringResource(R.string.history_record_id,item.id)))
                                if (state.detailLoading) AppLoadingIndicator()
                                state.detailError?.let {
                                    ErrorState(throwable = it,
                                        onRetry = if (state.detailLoading || state.isLoading) null else viewModel::retryDetails,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                }
                                state.detail?.let { detail ->
                                    com.mukhtabir.android.ui.components.LearningResultsSummary(detail.correctCount, detail.totalQuestions,
                                        detail.answeredCount, detail.accuracy)
                                    val rows = detail.results
                                    var reviewFilter by androidx.compose.runtime.saveable.rememberSaveable(item.id) {
                                        androidx.compose.runtime.mutableStateOf(com.mukhtabir.android.ui.components.ReviewFilter.ALL)
                                    }
                                    val visible = rows.withIndex().filter { (_, result) -> reviewFilter.includes(result.userAnswer != null, result.isCorrect) }
                                    com.mukhtabir.android.ui.components.ResultReviewFilters(reviewFilter, { reviewFilter = it }, visible.size, rows.size)
                                    visible.forEach { (index, result) -> SessionResultReview(index + 1, result, imageRepository) }
                                }
                            } else {
                                Text(
                                    stringResource(R.string.tap_for_details),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (state.totalPages > 1) {
                    item {
                        AppPagination(state.page, state.totalPages, onPrevious = viewModel::previousPage, onNext = viewModel::nextPage, previousEnabled = state.page > 1 && !state.isLoading, nextEnabled = state.page < state.totalPages && !state.isLoading, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val remaining = safe % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remaining)
    else "%02d:%02d".format(minutes, remaining)
}
