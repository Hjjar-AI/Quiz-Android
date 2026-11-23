package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun Modifier.directionalIcon(): Modifier =
    scale(scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f, scaleY = 1f)
