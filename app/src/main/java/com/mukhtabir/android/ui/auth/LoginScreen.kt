package com.mukhtabir.android.ui.auth

import com.mukhtabir.android.ui.components.AppTextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.LocalContentColor
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(com.mukhtabir.android.ui.components.AppLayoutTokens.authInsets),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )
            onDemo?.let { open -> com.mukhtabir.android.ui.demo.DemoInvitation(onOpen = open, enabled = !state.isLoading) }
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))
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
            Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password)) },
                enabled = !state.isLoading,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onLogin() }),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            state.loginError?.let {
                Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))
                Text(
                    text = it.userMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))
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
            AppTextButton(onHelp, enabled=!state.isLoading) { Text(stringResource(R.string.help_title)) }
            AppTextButton(onPrivacy, enabled=!state.isLoading) { Text(stringResource(R.string.privacy_title)) }
            onOffline?.let {open -> AppTextButton(open,enabled=!state.isLoading) {Text(stringResource(R.string.offline_open_cached))} }
            onChangeServer?.let { change -> AppTextButton(change,enabled=!state.isLoading) {
                Text(stringResource(R.string.server_change))
            } }
        }
    }
}
