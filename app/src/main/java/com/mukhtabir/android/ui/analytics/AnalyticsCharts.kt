package com.mukhtabir.android.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.util.formatDecimal

/** Detailed date/value rows remain beside the chart in AnalyticsScreen. */
@Composable
private fun ChartSummary(values: List<Double>) {
    Text(stringResource(R.string.analytics_chart_summary,
        formatDecimal(values.last()), formatDecimal(values.minOrNull() ?: 0.0),
        formatDecimal(values.maxOrNull() ?: 0.0)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun AnalyticsLineChart(values: List<Double>) {
    if (values.isEmpty()) return
    // An invalid point leaves a gap rather than inventing a zero measurement.
    val finite = values.filter { it.isFinite() }
    if (finite.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column {
        ChartSummary(finite)
        Canvas(Modifier.fillMaxWidth().height(AppLayoutTokens.chartLineHeight).clearAndSetSemantics { }) {
            val stroke = AppLayoutTokens.chartStroke.toPx()
            val radius = AppLayoutTokens.chartMarkerRadius.toPx()
            val inset = radius.coerceAtMost(minOf(size.width, size.height) / 2)
            val width = (size.width - inset * 2).coerceAtLeast(0f)
            val height = (size.height - inset * 2).coerceAtLeast(0f)
            val lower = minOf(0.0, finite.minOrNull() ?: 0.0)
            val upper = maxOf(1.0, finite.maxOrNull() ?: 1.0)
            fun y(value: Double) = inset + height * (1.0 - (value - lower) / (upper - lower)).toFloat()
            for (step in 0..4) {
                val row = inset + height * step / 4
                drawLine(grid, Offset(inset, row), Offset(size.width - inset, row), strokeWidth = stroke / 2)
            }
            val path = Path()
            var connected = false
            values.forEachIndexed { index, value ->
                if (!value.isFinite()) { connected = false; return@forEachIndexed }
                val logicalX = if (values.size == 1) size.width / 2 else inset + width * index / (values.size - 1)
                val point = Offset(if (rtl) size.width - logicalX else logicalX, y(value))
                if (connected) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                connected = true
                drawCircle(color, radius, point)
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
internal fun AnalyticsBarChart(values: List<Double>) {
    if (values.isEmpty()) return
    val finite = values.filter { it.isFinite() }
    if (finite.isEmpty()) return
    val color = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column {
        ChartSummary(finite)
        Canvas(Modifier.fillMaxWidth().height(AppLayoutTokens.chartBarHeight).clearAndSetSemantics { }) {
            val stroke = AppLayoutTokens.chartStroke.toPx()
            val lower = minOf(0.0, finite.minOrNull() ?: 0.0)
            val upper = maxOf(1.0, finite.maxOrNull() ?: 1.0)
            val height = (size.height - stroke).coerceAtLeast(0f)
            fun y(value: Double) = height * (1.0 - (value - lower) / (upper - lower)).toFloat()
            val baseline = y(0.0)
            drawLine(grid, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = stroke)
            val slot = size.width / values.size
            val gap = AppLayoutTokens.chartGap.toPx().coerceAtMost(slot / 2)
            val barWidth = (slot - gap).coerceAtLeast(0f)
            values.forEachIndexed { index, value ->
                if (!value.isFinite()) return@forEachIndexed
                val x = slot * index + gap / 2
                val top = minOf(baseline, y(value))
                drawRect(color,
                    topLeft = Offset(if (rtl) size.width - x - barWidth else x, top),
                    size = Size(barWidth, kotlin.math.abs(y(value) - baseline)))
            }
        }
    }
}
