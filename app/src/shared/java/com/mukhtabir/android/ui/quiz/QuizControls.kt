package com.mukhtabir.android.ui.quiz

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.components.AnswerChoice
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.theme.AppTheme

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun QuizSelectionRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AppActionRow(modifier.fillMaxWidth()) { content() }
}

@Composable
fun SelectableButton(
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
    val selectionModifier = modifier.heightIn(min = AppLayoutTokens.touchTarget).semantics {
        this.selected = selected
        role = androidx.compose.ui.semantics.Role.RadioButton
        accessibilityLabel?.let { contentDescription = it }
    }
    if (choiceNumber != null) {
        AnswerChoice(
            number = choiceNumber, text = text, modifier = modifier,
            selected = selected, enabled = enabled, onClick = onClick, answerCorrect = answerCorrect,
            neutralSelection = neutralSelection, markdown = markdown, accessibilityLabel = accessibilityLabel)
    } else if (selected) {
        AppButton(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    } else {
        AppOutlinedButton(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    }
    answerCorrect?.takeIf { choiceNumber == null }?.let { correct ->
        Text(stringResource(if (correct) R.string.correct_answer else R.string.incorrect_answer),
            color = if (correct) com.mukhtabir.android.ui.theme.AppTheme.feedback.success else MaterialTheme.colorScheme.error)
    }
}
