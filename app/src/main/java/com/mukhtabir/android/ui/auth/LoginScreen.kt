package com.mukhtabir.android.ui.auth

import com.mukhtabir.android.ui.components.AppLaunchLayout
import com.mukhtabir.android.ui.components.AppLaunchHeader
import com.mukhtabir.android.ui.components.OfflineLaunchActions
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import androidx.compose.ui.semantics.heading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppSaveStatus
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.ui.components.AppTextButton

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.LocalContentColor
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.VisualTransformation
import com.mukhtabir.android.ui.components.AppIconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.mukhtabir.android.R
import com.mukhtabir.android.core.userMessage

@Composable
fun LoginScreen(
    state: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onHelp: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onChangeServer: (() -> Unit)? = null,
    onOffline: (() -> Unit)? = null,
    onDemo: (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading, state.password.isEmpty()) {
        if (state.isLoading || state.password.isEmpty()) passwordVisible = false
    }
    AppLaunchLayout {
        AppLaunchHeader()
        AppCard(Modifier.fillMaxWidth()) {
            AppCardBody(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.username)) },
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = com.mukhtabir.android.util.ltrInputStyle(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password)) },
                    enabled = !state.isLoading,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AppIconButton({ passwordVisible = !passwordVisible }, enabled = !state.isLoading) {
                            Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onLogin() }),
                    textStyle = com.mukhtabir.android.util.ltrInputStyle(),
                )
                state.loginError?.let {
                    AppSaveStatus(it.userMessage(), failed = true,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.username.isNotBlank() &&
                        state.password.isNotBlank() &&
                        !state.isLoading,
                ) {
                    if (state.isLoading) {
                        AppLoadingIndicator(compact = true, color = LocalContentColor.current)
                    } else {
                        Text(stringResource(R.string.sign_in))
                    }
                }
            }
        }
        OfflineLaunchActions(onDownloaded = onOffline, onDemo = onDemo, enabled = !state.isLoading)
        AppSection(stringResource(R.string.startup_more_options), enabled = !state.isLoading) {
            AppTextButton(onHelp, enabled=!state.isLoading) { Text(stringResource(R.string.help_title)) }
            AppTextButton(onPrivacy, enabled=!state.isLoading) { Text(stringResource(R.string.privacy_title)) }
            onChangeServer?.let { change -> AppTextButton(change,enabled=!state.isLoading) {
                Text(stringResource(R.string.server_change))
            } }
        }
    }
}
