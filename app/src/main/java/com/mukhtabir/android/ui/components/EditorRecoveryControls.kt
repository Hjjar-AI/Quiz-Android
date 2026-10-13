package com.mukhtabir.android.ui.components

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.core.EditorRecovery

@Composable
internal fun <T> editorRecoveryBlocked(recovery: EditorRecovery<T>): Boolean {
    val pending by recovery.pending.collectAsState()
    val error by recovery.error.collectAsState()
    val ready by recovery.ready.collectAsState()
    return !ready || pending != null || error != null
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun <T> EditorRecoveryControls(recovery: EditorRecovery<T>, busy: Boolean, restore: (T) -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val pending by recovery.pending.collectAsState()
    val error by recovery.error.collectAsState()
    val ready by recovery.ready.collectAsState()
    if (pending != null) {
        Text(stringResource(R.string.recovery_draft_available))
        AppActionRow() {
            AppButton({ recovery.restore(restore) }, enabled = ready && !busy && error == null) { Text(stringResource(R.string.recovery_restore)) }
            AppDangerOutlinedButton({confirming=true}, enabled = !busy) { Text(stringResource(R.string.discard)) }
        }
    }
    if(confirming) AlertDialog(onDismissRequest={confirming=false},
        title={Text(stringResource(R.string.discard))},text={Text(stringResource(R.string.recovery_discard_confirm))},
        confirmButton={AppDangerTextButton({confirming=false;recovery.discard()},enabled=!busy) {Text(stringResource(R.string.discard))}},
        dismissButton={TextButton({confirming=false}) {Text(stringResource(R.string.cancel))}})
    error?.let { ErrorState(throwable = it, onRetry = if (busy) null else recovery::reload) }
}
