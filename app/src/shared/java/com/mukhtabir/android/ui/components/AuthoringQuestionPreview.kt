package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto
import com.mukhtabir.android.util.formatNumber

/** Authoring/reconciliation material only. Active sessions keep their own reveal-safe readers. */
@Composable
fun QuestionDraftPreview(text: String, choices: List<String>, correctAnswer: Int?, explanation: String,
    caseContext: ClinicalCaseContextDto? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        ClinicalCaseContext(caseContext)
        QuestionTextCard(text)
        choices.forEachIndexed { index, choice ->
            QuestionChoiceCard {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.choice_number, formatNumber(index + 1)))
                    MarkdownText(choice, Modifier.fillMaxWidth())
                    if (correctAnswer == index + 1) Text(stringResource(R.string.correct_answer), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (explanation.isNotBlank()) {
            Text(stringResource(R.string.explanation), style = MaterialTheme.typography.titleSmall)
            ExplanationCard(explanation)
        }
    }
}
