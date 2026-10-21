package com.mukhtabir.android.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
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
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.SessionResultItemDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import com.mukhtabir.android.util.formatNumber

@Composable
internal fun QuizResultsContent(
    results: SessionResultsDto,
    reviewLoading: Boolean,
    reviewFailed: Boolean,
    reviewError: String?,
    onRefreshReview: () -> Unit,
    imageRepository: ImageRepository,
    onNewSession: () -> Unit,
    maximum: Int,
    canPractice: Boolean,
    categories: Set<Long>,
    onPractice: (TestMode, List<Long>, String) -> Unit,
    onCategory: (Long) -> Unit,
    onDone: () -> Unit,
) {
    var practiceIds by remember { mutableStateOf<List<Long>?>(null) }
    practiceIds?.let { ids -> com.mukhtabir.android.ui.components.PracticeLauncher(stringResource(R.string.results), ids,
        maximum, enabled = canPractice, onDismiss = { practiceIds = null },
        onStart = { mode, selected, label -> practiceIds = null; onPractice(mode, selected, label) }) }
    var reviewFilter by rememberSaveable(results.sourceSessionId, results.historyId) { mutableStateOf(ReviewFilter.ALL) }
    AppPageHeading(stringResource(R.string.results))
    ResultsOverview(results)
    if (results.reviewWithheld) {
        Text(stringResource(R.string.quiz_review_withheld))
        if (reviewFailed) Text(reviewError ?: stringResource(R.string.quiz_review_read_failed),
            color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRefreshReview, enabled = !reviewLoading,
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.quiz_refresh_review)) }
    } else {
        val incorrect = results.results.filter { it.userAnswer != null && !it.isCorrect }.map { it.questionId }.filter { it > 0 }.distinct()
        val unanswered = results.results.filter { it.userAnswer == null }.map { it.questionId }.filter { it > 0 }.distinct()
        QuizSelectionRow {
            if (incorrect.isNotEmpty()) OutlinedButton({ practiceIds = incorrect }, enabled = canPractice) { Text(stringResource(R.string.flow_incorrect)) }
            if (unanswered.isNotEmpty()) OutlinedButton({ practiceIds = unanswered }, enabled = canPractice) { Text(stringResource(R.string.flow_unanswered)) }
        }
        AppSection(stringResource(R.string.flow_category)) {
            results.results.filter { it.categoryId != null }.distinctBy { it.categoryId }.forEach { row ->
                OutlinedButton({ row.categoryId?.let(onCategory) }, enabled = canPractice && row.categoryId?.let { it in categories } == true) {
                    Text(row.categoryName ?: com.mukhtabir.android.util.formatNumber(requireNotNull(row.categoryId)))
                }
            }
        }
        AppSectionHeading(stringResource(R.string.review_details))
        val reviews = results.results.withIndex().filter { (_, result) ->
            reviewFilter.includes(result.userAnswer != null, result.isCorrect)
        }
        ResultReviewFilters(reviewFilter, { reviewFilter = it }, reviews.size, results.results.size)
        var selectedReview by rememberSaveable(results.sourceSessionId, results.historyId) { mutableStateOf<Int?>(null) }
        ResultReviewPanes(reviews, selectedReview, { selectedReview = it }, imageRepository)
        AppSection(stringResource(R.string.quiz_result_breakdowns), composeCollapsedContent = false) {
            ResultBreakdowns(results.results)
        }
    }

    Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.new_session))
    }
    AppTextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.done))
    }
}

@Composable
private fun ResultBreakdowns(results: List<SessionResultItemDto>) {
    val categories = results.groupBy { it.categoryName ?: "—" }
    val difficulties = results.groupBy { it.difficulty ?: "—" }
    if (categories.isNotEmpty()) {
        Text(stringResource(R.string.breakdown_category), style = MaterialTheme.typography.titleMedium)
        categories.forEach { (name, items) ->
            AppProgressMetric(name, items.count { it.isCorrect }, items.size)
        }
    }
    if (difficulties.isNotEmpty()) {
        Text(stringResource(R.string.breakdown_difficulty), style = MaterialTheme.typography.titleMedium)
        difficulties.forEach { (name, items) ->
            AppProgressMetric(name, items.count { it.isCorrect }, items.size)
        }
    }
}

@Composable
private fun reviewRowTitle(index: Int, result: SessionResultItemDto): String =
    if (result.userAnswer == null) stringResource(R.string.quiz_review_unanswered,
        com.mukhtabir.android.util.formatNumber(index + 1))
    else stringResource(if (result.isCorrect) R.string.review_correct else R.string.review_wrong, index + 1)

/** Frozen completion rows only; withheld review never composes either pane. */
@Composable
private fun ResultReviewPanes(reviews: List<IndexedValue<SessionResultItemDto>>, selected: Int?,
    onSelect: (Int) -> Unit, imageRepository: ImageRepository) {
    val active = reviews.firstOrNull { it.index == selected } ?: reviews.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        if (maxWidth >= AppLayoutTokens.detailPaneBreakpoint * scale) {
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
                Column(Modifier.weight(1f - AppLayoutTokens.detailListFraction)) {
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
