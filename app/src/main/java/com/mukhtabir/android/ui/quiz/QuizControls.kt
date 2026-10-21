package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.MarkdownText

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun QuizSelectionRow(content: @Composable () -> Unit) {
    AppActionRow(Modifier.fillMaxWidth()) { content() }
}

@Composable
internal fun SelectableButton(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    text: String,
    markdown: Boolean = false,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    choiceNumber: Int? = null,
    answerCorrect: Boolean? = null,
    neutralSelection: Boolean = false,
) {
    val selectionModifier = modifier.heightIn(min = com.mukhtabir.android.ui.components.AppLayoutTokens.touchTarget).semantics {
        this.selected = selected
        accessibilityLabel?.let { contentDescription = it }
    }
    if (choiceNumber != null) {
        com.mukhtabir.android.ui.components.AnswerChoice(
            number = choiceNumber, text = text, modifier = modifier,
            selected = selected, enabled = enabled, onClick = onClick, answerCorrect = answerCorrect,
            neutralSelection = neutralSelection, markdown = markdown, accessibilityLabel = accessibilityLabel)
    } else if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    }
    answerCorrect?.takeIf { choiceNumber == null }?.let { correct ->
        Text(stringResource(if (correct) R.string.correct_answer else R.string.incorrect_answer),
            color = if (correct) com.mukhtabir.android.ui.theme.AppTheme.feedback.success else MaterialTheme.colorScheme.error)
    }
}
