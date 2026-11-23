package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerImages
import com.mukhtabir.android.data.remote.SessionResultItemDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.util.formatNumber

@Composable
internal fun QuizResultsContent(
    results: SessionResultsDto,
    reviewLoading: Boolean,
    reviewFailed: Boolean,
    reviewError: String?,
    onRefreshReview: () -> Unit,
    imageRepository: LearnerImages,
    onNewSession: () -> Unit,
    maximum: Int,
    canPractice: Boolean,
    categories: Set<Long>,
    onPractice: (TestMode, List<Long>, String) -> Unit,
    onCategory: (Long) -> Unit,
    onDone: () -> Unit,
) {
    var practiceIds by remember { mutableStateOf<List<Long>?>(null) }
    practiceIds?.let { ids -> PracticeLauncher(stringResource(R.string.results), ids,
        maximum, enabled = canPractice, onDismiss = { practiceIds = null },
        onStart = { mode, selected, label -> practiceIds = null; onPractice(mode, selected, label) }) }
    var reviewFilter by rememberSaveable(results.sourceSessionId, results.historyId) { mutableStateOf(ReviewFilter.ALL) }
    AppPageHeading(stringResource(R.string.results))
    ResultsOverview(results)
    if (results.reviewWithheld) {
        Text(stringResource(R.string.quiz_review_withheld))
        if (reviewFailed) AppStatus(reviewError ?: stringResource(R.string.quiz_review_read_failed))
        AppOutlinedButton(onClick = onRefreshReview, enabled = !reviewLoading,
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.quiz_refresh_review)) }
    } else {
        val incorrect = remember(results) { results.results.filter { it.userAnswer != null && !it.isCorrect }.map { it.questionId }.filter { it > 0 }.distinct() }
        val unanswered = remember(results) { results.results.filter { it.userAnswer == null }.map { it.questionId }.filter { it > 0 }.distinct() }
        QuizSelectionRow {
            if (incorrect.isNotEmpty()) AppOutlinedButton({ practiceIds = incorrect }, enabled = canPractice) { Text(stringResource(R.string.flow_incorrect)) }
            if (unanswered.isNotEmpty()) AppOutlinedButton({ practiceIds = unanswered }, enabled = canPractice) { Text(stringResource(R.string.flow_unanswered)) }
        }
        AppSection(stringResource(R.string.flow_category)) {
            remember(results) { results.results.filter { it.categoryId != null }.distinctBy { it.categoryId } }.forEach { row ->
                AppOutlinedButton({ row.categoryId?.let(onCategory) }, enabled = canPractice && row.categoryId?.let { it in categories } == true) {
                    Text(row.categoryName ?: com.mukhtabir.android.util.formatNumber(requireNotNull(row.categoryId)))
                }
            }
        }
        AppSectionHeading(stringResource(R.string.review_details))
        val reviews = remember(results, reviewFilter) { results.results.withIndex().filter { (_, result) ->
            reviewFilter.includes(result.userAnswer != null, result.isCorrect)
        } }
        ResultReviewFilters(reviewFilter, { reviewFilter = it }, reviews.size, results.results.size)
        var selectedReview by rememberSaveable(results.sourceSessionId, results.historyId) { mutableStateOf<Int?>(null) }
        ResultReviewPanes(reviews, selectedReview, { selectedReview = it }, imageRepository)
        AppSection(stringResource(R.string.quiz_result_breakdowns), composeCollapsedContent = false) {
            ResultBreakdowns(results.results)
        }
    }

    AppButton(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.new_session))
    }
    AppTextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.done))
    }
}

@Composable
private fun ResultBreakdowns(results: List<SessionResultItemDto>) {
    val categories = remember(results) { results.groupBy { it.categoryName ?: "—" }
        .map { (name, items) -> Triple(name, items.count { it.isCorrect }, items.size) } }
    val difficulties = remember(results) { results.groupBy { it.difficulty }
        .map { (name, items) -> Triple(name, items.count { it.isCorrect }, items.size) } }
    if (categories.isNotEmpty()) {
        AppSectionHeading(stringResource(R.string.breakdown_category))
        categories.forEach { (name, correct, total) ->
            AppProgressMetric(name, correct, total)
        }
    }
    if (difficulties.isNotEmpty()) {
        AppSectionHeading(stringResource(R.string.breakdown_difficulty))
        difficulties.forEach { (name, correct, total) ->
            AppProgressMetric(difficultyLabel(name), correct, total)
        }
    }
}

@Composable
private fun reviewRowTitle(index: Int, result: SessionResultItemDto): String =
    resultReviewTitle(index + 1, result.userAnswer != null, result.isCorrect)

/** Frozen completion rows only; withheld review never composes either pane. */
@Composable
private fun ResultReviewPanes(reviews: List<IndexedValue<SessionResultItemDto>>, selected: Int?,
    onSelect: (Int) -> Unit, imageRepository: LearnerImages) {
    val active = remember(reviews, selected) { reviews.firstOrNull { it.index == selected } ?: reviews.firstOrNull() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        if (maxWidth >= AppLayoutTokens.detailPaneBreakpoint * scale) {
            val readerScroll = rememberScrollState()
            LaunchedEffect(active?.index) { readerScroll.scrollTo(0) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                Column(Modifier.weight(AppLayoutTokens.detailListFraction)
                    .heightIn(max = AppLayoutTokens.reviewListMaxHeight * scale).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(stringResource(R.string.ui_review_pick), style = MaterialTheme.typography.bodySmall)
                    reviews.forEach { (index, result) ->
                        AppCard(onClick = { onSelect(index) },
                            modifier = Modifier.fillMaxWidth().semantics { this.selected = active?.index == index }) {
                            AppCardBody {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                                    Icon(when {
                                        result.userAnswer == null -> Icons.Outlined.HelpOutline
                                        result.isCorrect -> Icons.Filled.Check
                                        else -> Icons.Filled.Close
                                    }, contentDescription = null)
                                    Text(reviewRowTitle(index, result), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    if (active?.index == index) Icon(Icons.Filled.ChevronRight, contentDescription = null,
                                        modifier = Modifier.directionalIcon())
                                }
                                Text(stringResource(R.string.your_answer,
                                    result.userAnswer?.let(::formatNumber) ?: stringResource(R.string.unanswered)),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f - AppLayoutTokens.detailListFraction)
                    .heightIn(max = AppLayoutTokens.reviewListMaxHeight * scale).verticalScroll(readerScroll)) {
                    active?.let { (index, result) -> key(index) {
                        SessionResultReview(index + 1, result, imageRepository)
                    } }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                reviews.forEach { (index, result) -> key(index) {
                    AppSection(reviewRowTitle(index, result), initiallyExpanded = index == active?.index,
                        subtitle = stringResource(R.string.your_answer,
                            result.userAnswer?.let(::formatNumber) ?: stringResource(R.string.unanswered)),
                        composeCollapsedContent = false) {
                        SessionResultReview(index + 1, result, imageRepository, showHeading = false)
                    }
                } }
            }
        }
    }
}
