package com.mukhtabir.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R

/** Reveal the already-shaped Arabic word so combining marks and joined letters stay intact. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun StartupBrand(modifier: Modifier = Modifier, onFinished: () -> Unit = {}) {
    val motionEnabled = (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f
    val duration = integerResource(R.integer.startup_reveal_duration)
    val ink = MaterialTheme.colorScheme.primary
    var revealed by remember { mutableStateOf(false) }
    val finishedCallback by rememberUpdatedState(onFinished)
    var finished by remember { mutableStateOf(false) }
    val complete: () -> Unit = { if(!finished) { finished=true; finishedCallback() } }
    LaunchedEffect(motionEnabled) { revealed = true; if(!motionEnabled) complete() }
    val progress = animateFloatAsState(
        targetValue = if (revealed || !motionEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = if (motionEnabled) duration else 0, easing = LinearEasing),
        label = "Startup name reveal",
        finishedListener = { if(it >= 1f) complete() },
    )
    val title = stringResource(R.string.launcher_name)
    val titleStyle = MaterialTheme.typography.headlineLarge.copy(
        fontFamily = FontFamily(Font(R.font.amiri)), fontWeight = FontWeight.Normal,
        fontSize = 104.sp, lineHeight = 177.sp, letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl, textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        // Keep the enlarged, shaped word on one line on narrow/large-text screens.
        val titleWidth = textMeasurer.measure(title, style = titleStyle, softWrap = false).size.width
        val availableWidth = with(density) { maxWidth.toPx() }
        val fit = (availableWidth / titleWidth.coerceAtLeast(1)).coerceIn(0f, 1f)
        Text(
            text = title,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.drawWithContent {
                val fraction = if (motionEnabled) progress.value else 1f
                val edge = size.width * (1f - fraction)
                clipRect(left = edge) { this@drawWithContent.drawContent() }
                if (motionEnabled && fraction < 1f) {
                    val fade = ((1f - fraction) / 0.12f).coerceIn(0f, 1f)
                    val pen = Offset(edge, size.height * 0.62f)
                    drawCircle(ink.copy(alpha = 0.16f * fade), radius = 7.dp.toPx(), center = pen)
                    drawCircle(ink.copy(alpha = fade), radius = 3.5.dp.toPx(), center = pen)
                }
            },
            color = ink,
            style = titleStyle.copy(
                fontSize = titleStyle.fontSize * fit,
                lineHeight = titleStyle.lineHeight * fit,
            ),
        )
    }
}
