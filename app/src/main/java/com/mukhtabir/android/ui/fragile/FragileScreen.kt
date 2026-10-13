@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.fragile

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

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
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    pageSize: Int,
    userId: Long,
    onPractiseIds: (List<Long>) -> Unit,
    onOpenQuestion: (Long) -> Unit,
) {
    val viewModel: FragileViewModel = viewModel(
        key = "fragile-$userId",
        factory = FragileViewModel.Factory(service, pageSize, userId),
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(pageSize) { viewModel.applyPageSize(pageSize) }
    RefreshOnResume(viewModel::retry)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppPageHeading(stringResource(R.string.fragile_title), action = {
                AppActionRow(fillWidth = false) {
                    Button(
                onClick = { onPractiseIds(state.items.map { it.id }) },
                enabled = canStartSessions && state.items.isNotEmpty() && !state.isLoading &&
                    state.error == null && !state.hasUnknownError,
            ) {
                Text(stringResource(R.string.practise_these_n, state.items.size))
            }
                }
            })

        if (state.isLoading) AppLoadingIndicator()
        if (state.error != null || state.hasUnknownError) {
            ErrorState(
                message = state.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                onRetry = if (state.isLoading) null else viewModel::retry,
            )
        }
        when {
            state.items.isEmpty() && !state.isLoading &&
                state.error == null && !state.hasUnknownError -> {
                EmptyState(
                    title = stringResource(R.string.fragile_empty),
                    message = stringResource(R.string.fragile_empty_message),
                )
            }
            state.items.isNotEmpty() -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
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
                            val meta = buildList {
                                question.categoryName?.let(::add)
                                question.difficulty.takeIf { it.isNotBlank() }?.let(::add)
                            }.joinToString(" · ")
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
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
