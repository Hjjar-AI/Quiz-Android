package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppProgressBarSize(val thickness: Dp) { THIN(2.dp), STANDARD(4.dp), THICK(8.dp) }

/** Material supplies range semantics. Callers can associate a domain label with the range. */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    size: AppProgressBarSize = AppProgressBarSize.STANDARD,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    LinearProgressIndicator(progress = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f,
        color = color, trackColor = trackColor,
        modifier = modifier.height(size.thickness).then(
            if (label == null) Modifier else Modifier.semantics { contentDescription = label }))
}
