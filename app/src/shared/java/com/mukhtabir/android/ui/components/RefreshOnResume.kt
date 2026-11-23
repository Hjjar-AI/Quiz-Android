package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Refresh retained navigation destinations after another screen changes their data. */
@Composable
fun RefreshOnResume(onRefresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val refresh by rememberUpdatedState(onRefresh)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        // Lifecycle also brings a newly registered observer up to its current
        // state. Screen loaders guard an already running initial request.
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
