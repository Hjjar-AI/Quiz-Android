package com.mukhtabir.android.ui.auth

import com.mukhtabir.android.ui.theme.AppTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.LocalContentColor
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.mukhtabir.android.R
import com.mukhtabir.android.data.auth.AuthRepository
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.core.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Change-password screen.
 *
 * Two entry paths:
 *
 *   • [forced] = true — shown by the App shell when
 *     `user.mustChangePassword` is set. No back navigation is exposed;
 *     completion re-renders the main graph on [onDone]; an uncertain
 *     change also offers explicit local sign-out.
 *
 *   • [forced] = false — reachable from the More menu. The caller is
 *     responsible for popping the back stack on [onDone].
 *
 * Passwords stay memory-only. Only the uncertain-outcome marker survives
 * recreation; restoration never submits a password change again.
 */
@Composable
fun ChangePasswordScreen(
    repository: AuthRepository,
    onDone: () -> Unit,
    onSignOut: () -> Unit,
    forced: Boolean = false,
    onBusyChanged: (Boolean) -> Unit = {},
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<Throwable?>(null) }
    var uncertain by rememberSaveable { mutableStateOf(false) }
    var gateStillRequired by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = isLoading || uncertain)
    DisposableEffect(isLoading, uncertain) {
        onBusyChanged(isLoading || uncertain)
        onDispose { onBusyChanged(false) }
    }

    // Minimum length matches the server's MinimumLengthValidator (6).
    val minLength = 6
    val canSubmit = !isLoading && !uncertain &&
        currentPassword.isNotBlank() &&
        newPassword.length >= minLength &&
        confirmPassword == newPassword

    fun submit() {
        if (isLoading || uncertain || currentPassword.isBlank() || newPassword.length < minLength ||
            confirmPassword != newPassword) return
        val current = currentPassword
        val replacement = newPassword
        failure = null
        isLoading = true
        uncertain = true
        scope.launch {
            runCatching {
                repository.changePassword(current, replacement)
            }.onSuccess {
                coroutineContext.ensureActive()
                isLoading = false
                uncertain = false
                currentPassword = ""; newPassword = ""; confirmPassword = ""
                onDone()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                coroutineContext.ensureActive()
                isLoading = false
                failure = error
                if (error is ApiException && error.statusCode in setOf(400, 401, 403, 404, 409, 429)) uncertain = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(com.mukhtabir.android.ui.components.AppLayoutTokens.authInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        if (forced) {
            Text(
                text = stringResource(R.string.must_change_password_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.must_change_password_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.change_password_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; failure = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.current_password)) },
            enabled = !isLoading && !uncertain,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; failure = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.new_password)) },
            enabled = !isLoading && !uncertain,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = {
                if (newPassword.isNotEmpty() && newPassword.length < minLength) {
                    Text(stringResource(R.string.password_too_short, minLength))
                }
            },
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; failure = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.confirm_password)) },
            enabled = !isLoading && !uncertain,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            supportingText = {
                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                    Text(
                        text = stringResource(R.string.password_mismatch),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )

        failure?.let {
            Text(
                text = it.userMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (uncertain) {
            Text(stringResource(R.string.password_change_uncertain))
            if (forced) OutlinedButton({
                if (!isLoading) {
                    isLoading = true
                    failure = null
                    scope.launch {
                        try {
                            val user = repository.currentUser()
                            coroutineContext.ensureActive()
                            // A profile read can establish whether the forced-change gate remains.
                            if (!user.mustChangePassword) {
                                uncertain = false
                                currentPassword = ""; newPassword = ""; confirmPassword = ""
                                onDone()
                            } else gateStillRequired = true
                        } catch (error: CancellationException) { throw error }
                        catch (error: Exception) { coroutineContext.ensureActive(); failure = error }
                        finally { isLoading = false }
                    }
                }
            }, enabled = !isLoading) { Text(stringResource(R.string.password_change_check)) }
            if (gateStillRequired) Text(stringResource(R.string.password_change_still_required))
            OutlinedButton(onSignOut, enabled = !isLoading) { Text(stringResource(R.string.sign_out)) }
        }

        Spacer(modifier = Modifier.height(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap))

        Button(
            onClick = ::submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit,
        ) {
            if (isLoading) {
                AppLoadingIndicator(compact = true, color = LocalContentColor.current)
            } else {
                Text(stringResource(R.string.change_password_submit))
            }
        }
    }
}
