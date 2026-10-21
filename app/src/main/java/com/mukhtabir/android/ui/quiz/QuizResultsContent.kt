package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.ReviewFilter
import com.mukhtabir.android.ui.components.ResultReviewFilters

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.SessionResultItemDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.SessionResultReview

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
            if (incorrect.isNotEmpty()) Button({ practiceIds = incorrect }, enabled = canPractice) { Text(stringResource(R.string.flow_incorrect)) }
            if (unanswered.isNotEmpty()) OutlinedButton({ practiceIds = unanswered }, enabled = canPractice) { Text(stringResource(R.string.flow_unanswered)) }
        }
        AppSection(stringResource(R.string.flow_category)) {
            results.results.filter { it.categoryId != null }.distinctBy { it.categoryId }.forEach { row ->
                OutlinedButton({ row.categoryId?.let(onCategory) }, enabled = canPractice && row.categoryId?.let { it in categories } == true) {
                    Text(row.categoryName ?: com.mukhtabir.android.util.formatNumber(requireNotNull(row.categoryId)))
                }
            }
        }
        com.mukhtabir.android.ui.components.AppSectionHeading(stringResource(R.string.review_details))
        val reviews = results.results.withIndex().filter { (_, result) ->
            reviewFilter.includes(result.userAnswer != null, result.isCorrect)
        }
        ResultReviewFilters(reviewFilter, { reviewFilter = it }, reviews.size, results.results.size)
        reviews.forEach { (index, result) ->
            AppSection(title = if (result.userAnswer == null) stringResource(R.string.quiz_review_unanswered,
                com.mukhtabir.android.util.formatNumber(index + 1)) else stringResource(
                    if (result.isCorrect) R.string.review_correct else R.string.review_wrong, index + 1),
                subtitle = stringResource(R.string.your_answer,
                    result.userAnswer?.let { com.mukhtabir.android.util.formatNumber(it) } ?: stringResource(R.string.unanswered)),
                composeCollapsedContent = false) {
                SessionResultReview(index + 1, result, imageRepository, showHeading = false)
            }
        }
        AppSection(stringResource(R.string.quiz_result_breakdowns), composeCollapsedContent = false) {
            ResultBreakdowns(results.results)
        }
    }

    Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.new_session))
    }
    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
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
            Text(stringResource(R.string.breakdown_value, name, items.count { it.isCorrect }, items.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (difficulties.isNotEmpty()) {
        Text(stringResource(R.string.breakdown_difficulty), style = MaterialTheme.typography.titleMedium)
        difficulties.forEach { (name, items) ->
            Text(stringResource(R.string.breakdown_value, name, items.count { it.isCorrect }, items.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
