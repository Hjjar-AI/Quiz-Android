package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/** Reports one screen-owned lock. Back interception and the meaning of busy stay with callers. */
@Composable
fun ScreenBusyEffect(busy: Boolean, onBusyChanged: (Boolean) -> Unit) {
    val report by rememberUpdatedState(onBusyChanged)
    DisposableEffect(busy) {
        report(busy)
        onDispose { report(false) }
    }
}
