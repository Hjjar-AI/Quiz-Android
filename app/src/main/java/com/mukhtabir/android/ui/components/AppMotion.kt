package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale

object AppMotion {
    const val revealMillis = 180
    const val loadingCycleMillis = 1400
}

@Composable
fun rememberMotionEnabled(): Boolean =
    (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f
