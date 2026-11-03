package com.mukhtabir.android.ui.questions

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.QuestionContext
import com.mukhtabir.android.ui.components.QuestionDraftPreview

@Composable
internal fun QuestionExtendedPreview(question: QuestionDto) {
    QuestionContext(question)
    question.case?.let { case ->
        Text(stringResource(R.string.editor_case_key)+": "+case.key.orEmpty())
    }
    question.caseOrder?.let { Text(stringResource(R.string.editor_case_order)+": "+it) }
    question.knowledgeObject?.let { Text(stringResource(R.string.editor_knowledge_id,it)) }
    val baseChoices=question.choices.map { QuestionChoiceDraft(text=it) }
    translationDrafts(question.translations,baseChoices).forEach { (locale,translation) ->
        Text(locale,style=MaterialTheme.typography.titleSmall)
        QuestionDraftPreview(translation.question,translatedChoices(translation,baseChoices)+translation.extraChoices,
            null,translation.explanation,question.localizedForDisplay(locale).case)
    }
}
