package com.mukhtabir.android.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionChoiceCard
import com.mukhtabir.android.ui.components.QuestionContext
import com.mukhtabir.android.ui.components.QuestionTextCard

@Composable
internal fun QuestionDraftPreview(text: String, choices: List<String>, correctAnswer: Int?, explanation: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        QuestionTextCard(text)
        choices.forEachIndexed { index, choice ->
            QuestionChoiceCard {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.choice_number, com.mukhtabir.android.util.formatNumber(index + 1)))
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

@Composable
internal fun QuestionExtendedPreview(question: QuestionDto) {
    QuestionContext(question)
    question.case?.let { case ->
        Text(stringResource(R.string.editor_case_key)+": "+case.key.orEmpty())
        if(!case.stem.isNullOrBlank()) MarkdownText(case.stem.orEmpty())
    }
    question.caseOrder?.let { Text(stringResource(R.string.editor_case_order)+": "+it) }
    question.knowledgeObject?.let { Text(stringResource(R.string.editor_knowledge_id,it)) }
    val baseChoices=question.choices.map { QuestionChoiceDraft(text=it) }
    translationDrafts(question.translations,baseChoices).forEach { (locale,translation) ->
        Text(locale,style=MaterialTheme.typography.titleSmall)
        QuestionDraftPreview(translation.question,translatedChoices(translation,baseChoices)+translation.extraChoices,
            null,translation.explanation)
    }
}
