package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.theme.AppShapes
import com.mukhtabir.android.util.ltrInputStyle

/** Shared geometry; multiline editors retain Enter while single-line forms default to Next. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val options = if (singleLine && keyboardOptions.imeAction == ImeAction.Default)
        keyboardOptions.copy(imeAction = ImeAction.Next) else keyboardOptions
    OutlinedTextField(value, onValueChange,
        modifier = modifier.heightIn(min = AppLayoutTokens.touchTarget), enabled = enabled,
        readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, supportingText = supportingFieldText(isError, supportingText),
        isError = isError, visualTransformation = visualTransformation, keyboardOptions = options,
        keyboardActions = keyboardActions ?: KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = { focus.clearFocus(); keyboard?.hide() }),
        singleLine = singleLine, maxLines = maxLines, minLines = minLines,
        shape = AppShapes.control)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AppTextField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val options = if (singleLine && keyboardOptions.imeAction == ImeAction.Default)
        keyboardOptions.copy(imeAction = ImeAction.Next) else keyboardOptions
    OutlinedTextField(value, onValueChange,
        modifier = modifier.heightIn(min = AppLayoutTokens.touchTarget), enabled = enabled,
        readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, supportingText = supportingFieldText(isError, supportingText),
        isError = isError, visualTransformation = visualTransformation, keyboardOptions = options,
        keyboardActions = keyboardActions ?: KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = { focus.clearFocus(); keyboard?.hide() }),
        singleLine = singleLine, maxLines = maxLines, minLines = minLines,
        shape = AppShapes.control)
}

/** Visibility is memory-only and reset when the form leaves composition. */
@Composable
fun AppPasswordField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, label: (@Composable () -> Unit)? = null,
    isError: Boolean = false, supportingText: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = ltrInputStyle(),
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(enabled, value.isEmpty()) {
        if (!enabled || value.isEmpty()) visible = false
    }
    AppTextField(value, onValueChange, modifier, enabled = enabled, label = label,
        textStyle = textStyle, isError = isError, supportingText = supportingText,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Password, autoCorrect = false),
        keyboardActions = keyboardActions, singleLine = singleLine,
        trailingIcon = {
            AppIconButton({ visible = !visible }, enabled = enabled) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password))
            }
        })
}

@Composable
private fun supportingFieldText(isError: Boolean, content: (@Composable () -> Unit)?): (@Composable () -> Unit)? {
    if (content == null || !isError) return content
    return {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
