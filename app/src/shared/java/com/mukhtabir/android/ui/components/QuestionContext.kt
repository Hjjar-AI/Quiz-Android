package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.QuestionDto

@Composable
fun QuestionContext(question: QuestionDto) {
    if (listOf(question.source, question.sourceDocument, question.knowledgeObjectTitle, question.knowledgeObjectObjective).all { it.isNullOrBlank() } && question.sourcePage == null) return
    AppSection(stringResource(R.string.section_reference)) {
        question.source?.takeIf(String::isNotBlank)?.let {
            Text(stringResource(R.string.question_reference, it), style = MaterialTheme.typography.bodySmall)
        }
        question.sourceDocument?.takeIf(String::isNotBlank)?.let {
            Text(stringResource(R.string.question_document, it), style = MaterialTheme.typography.bodySmall)
        }
        question.sourcePage?.let {
            Text(stringResource(R.string.question_page, com.mukhtabir.android.util.formatNumber(it)), style = MaterialTheme.typography.bodySmall)
        }
        question.knowledgeObjectTitle?.takeIf(String::isNotBlank)?.let {
            Text(stringResource(R.string.question_knowledge, it), style = MaterialTheme.typography.titleSmall)
        }
        question.knowledgeObjectObjective?.takeIf(String::isNotBlank)?.let {
            Text(stringResource(R.string.question_objective), style = MaterialTheme.typography.titleSmall)
            MarkdownText(it, Modifier.fillMaxWidth())
        }
    }
}
