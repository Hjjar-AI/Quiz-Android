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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.HistoryService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.SessionResultReview
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume

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
