package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/** Uses the existing lifecycle-runtime-ktx dependency; producer jobs retain their own ownership. */
@Composable
fun <T> StateFlow<T>.collectAsLifecycleState(): State<T> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val foreground = remember(this, lifecycle) { flowWithLifecycle(lifecycle, Lifecycle.State.STARTED) }
    return foreground.collectAsState(initial = value)
}
