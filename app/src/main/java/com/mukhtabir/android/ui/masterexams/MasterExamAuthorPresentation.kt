@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.MarkdownText

@Composable
internal fun AuthorQuestionPreview(question: QuestionDto) {
    val localized = question.localizedForDisplay()
    ClinicalCaseContext(localized.case)
    MarkdownText(localized.question, Modifier.fillMaxWidth())
}

internal fun examActionLabel(action: String): Int = when (action) {
    "publish" -> R.string.exam_publish
    "cancel" -> R.string.exam_cancel
    else -> R.string.exam_publish_bank
}
internal fun examActionWarning(action: String): Int = when (action) {
    "publish" -> R.string.exam_publish_warning
    "cancel" -> R.string.exam_cancel_warning
    else -> R.string.exam_bank_warning
}
