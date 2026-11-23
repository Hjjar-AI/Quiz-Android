@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.util.formatNumber

/** Keys, mutations and validation belong to the editor; slots retain its focus/reveal wrappers. */
@Composable
fun AuthoringChoiceEditor(
    number: Int, text: String, correct: Boolean, enabled: Boolean,
    onTextChange: (String) -> Unit, onSelectCorrect: () -> Unit,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRemove: () -> Unit,
    canMoveUp: Boolean, canMoveDown: Boolean, canRemove: Boolean,
    error: String? = null, correctModifier: Modifier = Modifier,
    textField: (@Composable () -> Unit)? = null,
    correctControl: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        if (textField != null) textField()
        else AuthoringChoiceTextField(number, text, onTextChange, enabled, error)
        if (correctControl != null) correctControl()
        else CorrectChoiceControl(correct, onSelectCorrect, enabled, correctModifier)
        AppActionRow {
            AppTextButton(onMoveUp, enabled = enabled && canMoveUp) { Text(stringResource(R.string.move_choice_up)) }
            AppTextButton(onMoveDown, enabled = enabled && canMoveDown) { Text(stringResource(R.string.move_choice_down)) }
            AppDangerTextButton(onRemove, enabled = enabled && canRemove) { Text(stringResource(R.string.remove_choice)) }
        }
    }
}

@Composable
fun AuthoringChoiceTextField(number: Int, text: String, onTextChange: (String) -> Unit,
    enabled: Boolean, error: String? = null, focus: FocusRequester? = null) {
    MarkdownField(text, onTextChange, stringResource(R.string.choice_number, formatNumber(number)),
        enabled, modifier = Modifier.fillMaxWidth(), error = error, fieldFocusRequester = focus)
}

@Composable
fun CorrectChoiceControl(selected: Boolean, onSelect: () -> Unit, enabled: Boolean,
    modifier: Modifier = Modifier) {
    AppFilterChip(selected, onSelect, enabled = enabled, modifier = modifier,
        label = { Text(stringResource(R.string.correct_answer)) })
}

/** Exact names, including commas, remain intact. Controllers retain all input and suggestions. */
@Composable
fun ExactTagEditor(tags: List<String>, input: String, onInputChange: (String) -> Unit,
    onAdd: (String) -> Unit, onRemove: (String) -> Unit, enabled: Boolean,
    validation: String? = null, suggestions: List<String> = emptyList(),
    suggestionsTitle: String? = null, suggestionsEnabled: Boolean = enabled,
    onClear: (() -> Unit)? = null, status: @Composable () -> Unit = {}) {
    var clearing by rememberSaveable { mutableStateOf(false) }
    if (clearing && onClear != null) AppConfirmationDialog(onDismissRequest = { clearing = false },
        title = { Text(stringResource(R.string.editor_tags_clear)) },
        text = { Text(stringResource(R.string.editor_tags_clear_confirm)) },
        confirmButton = { AppTextButton({ onClear(); clearing = false }, enabled = enabled) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { AppTextButton({ clearing = false }) { Text(stringResource(R.string.cancel)) } })
    Text(stringResource(R.string.editor_tags_exact_hint))
    tags.forEach { name -> key(name) {
        AppSearchRow(field = { modifier -> Text(name, modifier) }, action = {
            AppTextButton({ onRemove(name) }, enabled = enabled) { Text(stringResource(R.string.editor_tag_remove, name)) }
        })
    } }
    if (tags.isEmpty()) Text(stringResource(R.string.none))
    if (onClear != null) AppOutlinedButton({ clearing = true }, enabled = enabled && tags.isNotEmpty()) {
        Text(stringResource(R.string.editor_tags_clear))
    }
    AppTextField(input, onInputChange, enabled = enabled, singleLine = true,
        label = { Text(stringResource(R.string.editor_tag_name)) }, modifier = Modifier.fillMaxWidth())
    validation?.let { AppStatus(it) }
    AppOutlinedButton({ onAdd(input) }, enabled = enabled) { Text(stringResource(R.string.editor_tag_add)) }
    status()
    if (suggestions.isNotEmpty()) {
        suggestionsTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        AppActionRow {
            suggestions.forEach { name -> key(name) {
                AppOutlinedButton({ onAdd(name) }, enabled = suggestionsEnabled) { Text(name) }
            } }
        }
    }
}
