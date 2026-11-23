package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.util.formatNumber

@Composable
fun difficultyLabel(value: String?): String = stringResource(when (value) {
    "easy" -> R.string.difficulty_easy
    "medium" -> R.string.difficulty_medium
    "hard" -> R.string.difficulty_hard
    else -> R.string.unknown
})

@Composable
fun reflectionLabel(value: String): String = stringResource(when (value) {
    "unknown" -> R.string.reflection_unknown
    "misread" -> R.string.reflection_misread
    "confused" -> R.string.reflection_confused
    "guessed" -> R.string.reflection_guessed
    else -> R.string.unknown
})

@Composable
fun confidenceLabel(value: Int): String = stringResource(when (value) {
    1 -> R.string.confidence_guessing
    2 -> R.string.confidence_uncertain
    else -> R.string.confidence_certain
})

@Composable
fun resultReviewTitle(index: Int, answered: Boolean, correct: Boolean): String =
    stringResource(when {
        !answered -> R.string.quiz_review_unanswered
        correct -> R.string.quiz_review_correct_title
        else -> R.string.quiz_review_wrong_title
    }, formatNumber(index))
