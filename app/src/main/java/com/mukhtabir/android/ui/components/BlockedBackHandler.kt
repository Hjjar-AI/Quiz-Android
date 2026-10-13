package com.mukhtabir.android.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R

@Composable
fun BlockedBackHandler(enabled: Boolean) {
    val context = LocalContext.current
    val message = stringResource(R.string.navigation_pending_operation)
    val toast = remember(context, message) { Toast.makeText(context, message, Toast.LENGTH_SHORT) }
    DisposableEffect(toast) { onDispose { toast.cancel() } }
    BackHandler(enabled) { toast.cancel(); toast.show() }
}
