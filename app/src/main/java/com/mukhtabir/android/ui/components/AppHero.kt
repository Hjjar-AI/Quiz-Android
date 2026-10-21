package com.mukhtabir.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign

/** Only confirmed progress is passed here; the caller owns freshness and score authority. */
@Composable
fun AppProgressRing(progress: Float, value: String, label: String, modifier: Modifier = Modifier) {
    val amount = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(modifier.size(AppLayoutTokens.progressRingSize * scale).semantics(mergeDescendants = true) {
        progressBarRangeInfo = ProgressBarRangeInfo(amount, 0f..1f)
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val stroke = AppLayoutTokens.progressRingStroke.toPx()
            val inset = stroke / 2
            val diameter = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), diameter,
                style = Stroke(stroke))
            if (amount > 0f) drawArc(accent, -90f, amount * 360f, false,
                androidx.compose.ui.geometry.Offset(inset, inset), diameter,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(Modifier.padding(AppLayoutTokens.cardInsets), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AppHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = AppLayoutTokens.heroElevation) {
        Column(Modifier.padding(AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), content = content)
    }
}

/** Uses available pane width, including text scaling, rather than physical device labels. */
@Composable
fun AppHeroRow(decoration: @Composable () -> Unit, content: @Composable () -> Unit) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < AppLayoutTokens.headingBreakpoint * scale) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) { decoration(); content() }
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            decoration()
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
