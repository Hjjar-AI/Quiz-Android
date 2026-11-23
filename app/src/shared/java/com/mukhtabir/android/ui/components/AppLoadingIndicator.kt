package com.mukhtabir.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import kotlin.math.PI
import kotlin.math.cos

/** Quiet, continuous three-dot pulse; no motion when system animations are disabled. */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val label = stringResource(R.string.loading)
    val motionEnabled = rememberMotionEnabled()
    val phase = if (motionEnabled) {
        rememberInfiniteTransition(label = "Loading dots").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = AppMotion.loadingCycleMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "Loading pulse",
        )
    } else null

    Canvas(modifier.size(if (compact) 32.dp else 48.dp, if (compact) 20.dp else 32.dp).semantics {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }) {
        // Read animated state during drawing, so the surrounding screen does not recompose per frame.
        val progress = phase?.value ?: 0f
        val unit = minOf((if (compact) 1.dp else 1.5.dp).toPx(), size.width / 32f, size.height / 20f)
        val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        repeat(3) { index ->
            val pulse = if (motionEnabled) {
                ((1.0 - cos(2.0 * PI * (progress - index / 3f))) / 2.0).toFloat()
            } else 0.5f
            drawCircle(
                color = color.copy(alpha = color.alpha * (0.55f + 0.45f * pulse)),
                radius = unit * (2.5f + 0.6f * pulse),
                center = Offset(center.x + direction * (index - 1) * 10f * unit, center.y - 2f * unit * pulse),
            )
        }
    }
}
