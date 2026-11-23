package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerImages
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay

/** Shared list reading order; account-aware bookmark/rating/moderation actions stay with callers. */
@Composable
fun QuestionListCard(question: QuestionDto, imageRepository: LearnerImages, onOpen: () -> Unit,
    selected: Boolean = false, onSelect: (() -> Unit)? = null, selectionEnabled: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit = {}) {
    val display = question.localizedForDisplay()
    AppCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        AppCardBody {
            onSelect?.let { select ->
                AppCheckboxRow(stringResource(R.string.select_question_number,
                    com.mukhtabir.android.util.formatNumber(question.id)), selected,
                    onCheckedChange = { select() }, enabled = selectionEnabled)
            }
            ClinicalCaseContext(display.case)
            MarkdownText(display.question, Modifier.fillMaxWidth(), interactive = false, maxLines = 3)
            QuestionImage(question.imageUrl, imageRepository, maxHeight = QuestionListCardGeometry.listImageMaxHeight)
            AppMetadataRow(listOfNotNull(question.categoryName, question.difficulty.takeIf(String::isNotBlank)?.let { difficultyLabel(it) },
                stringResource(if (question.verified) R.string.verified else R.string.unverified)))
            QuestionContext(question)
            actions()
        }
    }
}

private object QuestionListCardGeometry {
    val listImageMaxHeight = 120.dp
}
