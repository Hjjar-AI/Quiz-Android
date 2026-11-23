package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.EditorRecovery
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.collectAsLifecycleState

@Composable
fun <T> editorRecoveryBlocked(recovery: EditorRecovery<T>): Boolean {
    val pending by recovery.pending.collectAsLifecycleState()
    val error by recovery.error.collectAsLifecycleState()
    val ready by recovery.ready.collectAsLifecycleState()
    return !ready || pending != null || error != null
}

/** Mandatory startup decision; visibility, storage and separate action permissions are caller-owned. */
@Composable
fun AuthoringRecoveryPrompt(onRestore: () -> Unit, onDiscard: () -> Unit,
    restoreEnabled: Boolean, discardEnabled: Boolean) {
    AppConfirmationDialog(onDismissRequest = {},
        title = { Text(stringResource(R.string.author_recover_title)) },
        text = { Text(stringResource(R.string.author_recover_body)) },
        confirmButton = { AppTextButton(onRestore, enabled = restoreEnabled) { Text(stringResource(R.string.author_restore)) } },
        dismissButton = { AppDangerTextButton(onDiscard, enabled = discardEnabled) { Text(stringResource(R.string.ux_discard_local_recovery)) } })
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun <T> EditorRecoveryControls(recovery: EditorRecovery<T>, busy: Boolean, restore: (T) -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val pending by recovery.pending.collectAsLifecycleState()
    val error by recovery.error.collectAsLifecycleState()
    val ready by recovery.ready.collectAsLifecycleState()
    if (pending != null) {
        Text(stringResource(R.string.recovery_draft_available))
        AppActionRow() {
            AppButton({ recovery.restore(restore) }, enabled = ready && !busy && error == null) { Text(stringResource(R.string.recovery_restore)) }
            AppDangerOutlinedButton({confirming=true}, enabled = !busy) { Text(stringResource(R.string.ux_discard_local_recovery)) }
        }
    }
    if(confirming) AppConfirmationDialog(onDismissRequest={confirming=false},
        title={Text(stringResource(R.string.ux_discard_local_recovery))},text={Text(stringResource(R.string.recovery_discard_confirm))},
        confirmButton={AppDangerTextButton({confirming=false;recovery.discard()},enabled=!busy) {Text(stringResource(R.string.ux_discard_local_recovery))}},
        dismissButton={AppTextButton({confirming=false}) {Text(stringResource(R.string.cancel))}})
    error?.let { ErrorState(throwable = it, onRetry = if (busy) null else recovery::reload) }
}
