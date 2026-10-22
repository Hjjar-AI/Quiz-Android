package com.mukhtabir.android.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField

private enum class MarkdownAction(@StringRes val label: Int) {
    BOLD(R.string.markdown_bold), ITALIC(R.string.markdown_italic), CODE(R.string.markdown_code),
    LINK(R.string.markdown_link), BULLETS(R.string.markdown_bullets), NUMBERED(R.string.markdown_numbered),
    HEADING(R.string.markdown_heading), QUOTE(R.string.markdown_quote),
    UNDERLINE(R.string.markdown_underline), STRIKE(R.string.markdown_strike),
    SUPERSCRIPT(R.string.markdown_superscript), SUBSCRIPT(R.string.markdown_subscript),
    HIGHLIGHT(R.string.markdown_highlight), TABLE(R.string.markdown_table),
}

/** Cursor state is memory-only; the existing editor owns text, validation and recovery. */
@Composable
fun MarkdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    minLines: Int = 1,
    modifier: Modifier = Modifier,
    error: String? = null,
    fieldFocusRequester: FocusRequester? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    var toolsVisible by remember { mutableStateOf(false) }
    val localFocus = remember { FocusRequester() }
    val focus = fieldFocusRequester ?: localFocus
    val placeholder = stringResource(R.string.markdown_text_placeholder)
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(
            field.selection.start.coerceIn(0, value.length), field.selection.end.coerceIn(0, value.length)))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        AppTextButton(onClick = { toolsVisible = !toolsVisible }, enabled = enabled,
            modifier = Modifier.heightIn(min = AppLayoutTokens.touchTarget)) {
            Text(stringResource(if (toolsVisible) R.string.markdown_hide_tools else R.string.markdown_show_tools, label))
        }
        if (toolsVisible) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            MarkdownAction.values().forEach { action ->
                AppTextButton(onClick = {
                    if (enabled) {
                        field = insertMarkdown(field, action, placeholder)
                        onValueChange(field.text)
                        focus.requestFocus()
                    }
                }, enabled = enabled, modifier = Modifier.heightIn(min = AppLayoutTokens.touchTarget)) { Text(stringResource(action.label)) }
            }
        }
        AppTextField(value = field, onValueChange = {
            field = it
            onValueChange(it.text)
        }, enabled = enabled, label = { Text(label) }, minLines = minLines,
            isError = error != null, supportingText = error?.let { message -> { Text(message) } },
            modifier = Modifier.fillMaxWidth().focusRequester(focus))
    }
}

private fun insertMarkdown(field: TextFieldValue, action: MarkdownAction, placeholder: String): TextFieldValue {
    val text = field.text
    val start = field.selection.min.coerceIn(0, text.length)
    val end = field.selection.max.coerceIn(start, text.length)
    if (action in setOf(MarkdownAction.BULLETS, MarkdownAction.NUMBERED, MarkdownAction.HEADING, MarkdownAction.QUOTE)) {
        val lineStart = if (start == 0) 0 else text.lastIndexOf('\n', start - 1) + 1
        val lastSelected = if (end > start) end - 1 else end
        val lineEnd = text.indexOf('\n', lastSelected).let { if (it < 0) text.length else it }
        val block = text.substring(lineStart, lineEnd).ifEmpty { placeholder }
        val replacement = block.split('\n').mapIndexed { index, line ->
            val prefix = when (action) {
                MarkdownAction.BULLETS -> "- "
                MarkdownAction.NUMBERED -> "${index + 1}. "
                MarkdownAction.HEADING -> "## "
                else -> "> "
            }
            prefix + line
        }.joinToString("\n")
        return TextFieldValue(text.replaceRange(lineStart, lineEnd, replacement), TextRange(lineStart, lineStart + replacement.length))
    }
    val selected = text.substring(start, end).ifEmpty { placeholder }
    if (action == MarkdownAction.TABLE) {
        val replacement = "\n\n| $placeholder | $placeholder |\n| --- | --- |\n| $placeholder | $placeholder |\n\n"
        return TextFieldValue(text.replaceRange(start, end, replacement), TextRange(start + 4, start + 4 + placeholder.length))
    }
    val wrappers = when (action) {
        MarkdownAction.UNDERLINE -> "<u>" to "</u>"
        MarkdownAction.SUPERSCRIPT -> "^(" to ")^"
        MarkdownAction.SUBSCRIPT -> "~(" to ")~"
        else -> null
    }
    if (wrappers != null) {
        val (opening, closing) = wrappers
        return TextFieldValue(text.replaceRange(start, end, opening + selected + closing),
            TextRange(start + opening.length, start + opening.length + selected.length))
    }
    val marker = when (action) {
        MarkdownAction.BOLD -> "**"
        MarkdownAction.ITALIC -> "*"
        MarkdownAction.CODE -> "`"
        MarkdownAction.STRIKE -> "~~"
        MarkdownAction.HIGHLIGHT -> "=="
        else -> ""
    }
    if (action == MarkdownAction.LINK) {
        val replacement = "[$selected](https://)"
        val urlStart = start + selected.length + 3
        return TextFieldValue(text.replaceRange(start, end, replacement), TextRange(urlStart, urlStart + 8))
    }
    return TextFieldValue(text.replaceRange(start, end, marker + selected + marker),
        TextRange(start + marker.length, start + marker.length + selected.length))
}
