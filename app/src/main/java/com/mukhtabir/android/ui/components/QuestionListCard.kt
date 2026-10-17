package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay

/** Shared list reading order; account-aware bookmark/rating/moderation actions stay with callers. */
@Composable
fun QuestionListCard(question: QuestionDto, imageRepository: ImageRepository, onOpen: () -> Unit,
    selected: Boolean = false, onSelect: (() -> Unit)? = null, selectionEnabled: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit = {}) {
    val display = question.localizedForDisplay()
    AppCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        AppCardBody {
            onSelect?.let { select ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(selected, { select() }, enabled = selectionEnabled)
                    Text(stringResource(R.string.select_question_number, question.id))
                }
            }
            ClinicalCaseContext(display.case)
            MarkdownText(display.question, Modifier.fillMaxWidth())
            QuestionImage(question.imageUrl, imageRepository)
            AppMetadataRow(listOfNotNull(question.categoryName, question.difficulty.takeIf(String::isNotBlank)?.let { difficultyLabel(it) },
                stringResource(if (question.verified) R.string.verified else R.string.unverified)))
            QuestionContext(question)
            actions()
        }
    }
}

@Composable
fun difficultyLabel(value: String): String = when (value) {
    "easy" -> stringResource(R.string.difficulty_easy)
    "medium" -> stringResource(R.string.difficulty_medium)
    "hard" -> stringResource(R.string.difficulty_hard)
    else -> value
}
