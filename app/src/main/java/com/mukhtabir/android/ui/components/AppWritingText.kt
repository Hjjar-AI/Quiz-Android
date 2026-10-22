package com.mukhtabir.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp

/** Reveal fully shaped text without changing its layout or accessibility label per frame. */
@Composable
internal fun AppWritingText(text: String, style: TextStyle, durationMillis: Int) {
    val progress = remember(text) { Animatable(0f) }
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val motionEnabled = rememberMotionEnabled()
    val penColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(text, durationMillis, motionEnabled) {
        if (motionEnabled) {
            progress.animateTo(1f, tween(durationMillis = durationMillis, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    Text(text, style = style, onTextLayout = { layout = it },
        modifier = Modifier.semantics { heading() }.drawWithContent {
            val measured = layout
            val reveal = if (motionEnabled) progress.value else 1f
            if (measured == null || reveal >= 1f) {
                drawContent()
            } else {
                // Walk visual lines, retaining joined Arabic glyphs, diacritics and mixed names.
                val totalWidth = (0 until measured.lineCount).sumOf {
                    (measured.getLineRight(it) - measured.getLineLeft(it)).coerceAtLeast(0f).toDouble()
                }.toFloat()
                var remaining = totalWidth * reveal
                var pen: Offset? = null
                for (line in 0 until measured.lineCount) {
                    val left = measured.getLineLeft(line)
                    val right = measured.getLineRight(line)
                    val width = (right - left).coerceAtLeast(0f)
                    val visible = remaining.coerceIn(0f, width)
                    val rtl = measured.getParagraphDirection(measured.getLineStart(line)) == ResolvedTextDirection.Rtl
                    val edge = if (rtl) right - visible else left + visible
                    if (visible > 0f) {
                        clipRect(left = if (rtl) edge else left, right = if (rtl) right else edge,
                            top = measured.getLineTop(line), bottom = measured.getLineBottom(line)) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    if (pen == null && remaining >= 0f && remaining < width) {
                        val lineTop = measured.getLineTop(line)
                        val lineHeight = measured.getLineBottom(line) - lineTop
                        pen = Offset(edge.coerceIn(0f, size.width), lineTop + lineHeight * 0.62f)
                    }
                    remaining -= width
                }
                pen?.let { drawCircle(penColor, radius = 2.dp.toPx(), center = it) }
            }
        })
}
