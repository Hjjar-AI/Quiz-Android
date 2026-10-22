package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.SessionResultItemDto
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

/** Completed-session snapshots only. Never use this renderer for active questions. */
@Composable
fun SessionResultReview(
    index: Int,
    result: SessionResultItemDto,
    imageRepository: ImageRepository,
    showHeading: Boolean = true,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
        ) {
            if (showHeading) Text(
                resultReviewTitle(index, result.userAnswer != null, result.isCorrect),
                color = if (result.userAnswer == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else if (result.isCorrect) {
                    AppTheme.feedback.success
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            ClinicalCaseContext(result.case)
            QuestionTextCard(result.question)
            QuestionImage(result.imageUrl, imageRepository)
            result.preAnswer?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.recall_written_answer), style = MaterialTheme.typography.titleSmall)
                Text(it)
            }
            result.choices.forEachIndexed { choiceIndex, choice ->
                val answer = choiceIndex + 1
                AnswerChoice(number=answer,text=choice,selected=answer==result.userAnswer,
                    answerCorrect=if(result.correctAnswer==null) null else when(answer) {
                        result.correctAnswer -> true
                        result.userAnswer -> false
                        else -> null
                    },interactiveText=true,reviewMode=true)
            }
            if(result.userAnswer==null || result.correctAnswer==null) Text(stringResource(R.string.your_answer,
                result.userAnswer?.let(::formatNumber) ?: stringResource(R.string.unanswered)))
            if(result.correctAnswer==null) Text(stringResource(R.string.correct_choice,stringResource(R.string.unknown)))
            AppSection(stringResource(R.string.section_result_details)) {
            if (result.usedFirstAttempt && result.finalAnswer != null && result.finalAnswer != result.userAnswer) {
                Text(stringResource(R.string.review_first_attempt, formatNumber(result.finalAnswer)))
            }
            result.confidenceScore?.let {
                Text(stringResource(R.string.review_confidence, confidenceLabel(it)))
            }
            result.errorReason?.let {
                Text(stringResource(R.string.review_error_reason, reflectionLabel(it)))
            }
            result.categoryName?.let { Text(stringResource(R.string.review_category, it)) }
            result.difficulty?.let { Text(stringResource(R.string.review_difficulty, difficultyLabel(it))) }
            if (result.tagNames.isNotEmpty()) {
                Text(stringResource(R.string.review_tags, result.tagNames.joinToString(", ") { com.mukhtabir.android.util.bidi(it) }))
            }
            }
            result.explanation?.takeIf { it.isNotBlank() }?.let {
                AppSection(stringResource(R.string.explanation), initiallyExpanded = true) {
                    ExplanationCard(it)
                }
            }
        }
    }
}

@Composable
private fun confidenceLabel(value: Int): String = stringResource(
    when (value) {
        1 -> R.string.confidence_guessing
        2 -> R.string.confidence_uncertain
        else -> R.string.confidence_certain
    },
)
