package com.mukhtabir.android.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppMetricRow
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.theme.appDataColors
import com.mukhtabir.android.util.formatCalendarDate
import com.mukhtabir.android.util.formatDecimal
import kotlin.math.roundToInt

internal data class AnalyticsChartPoint(val date: String, val value: Double, val detail: String)

internal enum class AnalyticsChartKind { LINE, BAR }

/** Decorative geometry and the equivalent keyboard and TalkBack daily values share the same rows. */
@Composable
internal fun AnalyticsChart(points: List<AnalyticsChartPoint>, kind: AnalyticsChartKind) {
    if (points.isEmpty()) return

    val colors = appDataColors()
    val values = points.map { it.value }
    val finite = values.filter { it.isFinite() }
    val dates = points.map { it.date }
    var selectedIndex by remember(dates) { mutableStateOf<Int?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        if (finite.isNotEmpty()) {
            Text(stringResource(R.string.analytics_chart_summary,
                formatDecimal(finite.last()), formatDecimal(finite.min()), formatDecimal(finite.max())),
                style = MaterialTheme.typography.bodySmall, color = colors.label)
            val inspect = Modifier.pointerInput(dates, rtl, kind) {
                detectTapGestures { tap ->
                    if (size.width <= 0) return@detectTapGestures
                    val logicalX = if (rtl) size.width - tap.x else tap.x
                    val width = size.width.toFloat()
                    val index = when (kind) {
                        AnalyticsChartKind.LINE -> {
                            val inset = lineChartInset(width, size.height.toFloat(),
                                AnalyticsChartsGeometry.chartMarkerRadius.toPx(), AnalyticsChartsGeometry.chartStroke.toPx())
                            val plotWidth = (width - 2 * inset).coerceAtLeast(1f)
                            val fraction = ((logicalX - inset) / plotWidth).coerceIn(0f, 1f)
                            (fraction * points.lastIndex).roundToInt()
                        }
                        AnalyticsChartKind.BAR -> (logicalX / width * points.size).toInt()
                    }
                    selectedIndex = index.coerceIn(points.indices)
                }
            }
            when (kind) {
                AnalyticsChartKind.LINE -> AnalyticsLineChart(values, finite, selectedIndex, rtl, colors.primary, colors.track, inspect)
                AnalyticsChartKind.BAR -> AnalyticsBarChart(values, finite, selectedIndex, rtl, colors.secondary, colors.track, inspect)
            }
            if (selectedIndex == null) Text(
                stringResource(R.string.analytics_chart_inspect),
                style = MaterialTheme.typography.bodySmall,
                color = colors.label,
            )
        }
        selectedIndex?.let { index ->
            val point = points[index]
            AppMetricRow(formatCalendarDate(point.date), point.detail,
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite })
        }
        AppSection(stringResource(R.string.analytics_daily_values)) {
            points.forEach { point -> AppMetricRow(formatCalendarDate(point.date), point.detail) }
        }
    }
}

@Composable
private fun AnalyticsLineChart(
    values: List<Double>, finite: List<Double>, selectedIndex: Int?, rtl: Boolean,
    color: Color, grid: Color, modifier: Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(AnalyticsChartsGeometry.chartLineHeight).clearAndSetSemantics { }) {
        val stroke = AnalyticsChartsGeometry.chartStroke.toPx()
        val radius = AnalyticsChartsGeometry.chartMarkerRadius.toPx()
        val inset = lineChartInset(size.width, size.height, radius, stroke)
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
        var selectedPoint: Offset? = null
        values.forEachIndexed { index, value ->
            if (!value.isFinite()) { connected = false; return@forEachIndexed }
            val logicalX = if (values.size == 1) size.width / 2 else inset + width * index / (values.size - 1)
            val point = Offset(if (rtl) size.width - logicalX else logicalX, y(value))
            if (connected) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
            connected = true
            drawCircle(color, radius, point)
            if (index == selectedIndex) selectedPoint = point
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        selectedPoint?.let { drawCircle(color, radius + stroke, it, style = Stroke(stroke)) }
    }
}

@Composable
private fun AnalyticsBarChart(
    values: List<Double>, finite: List<Double>, selectedIndex: Int?, rtl: Boolean,
    color: Color, grid: Color, modifier: Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(AnalyticsChartsGeometry.chartBarHeight).clearAndSetSemantics { }) {
        val stroke = AnalyticsChartsGeometry.chartStroke.toPx()
        val lower = minOf(0.0, finite.minOrNull() ?: 0.0)
        val upper = maxOf(1.0, finite.maxOrNull() ?: 1.0)
        val height = (size.height - stroke).coerceAtLeast(0f)
        fun y(value: Double) = height * (1.0 - (value - lower) / (upper - lower)).toFloat()
        val baseline = y(0.0)
        drawLine(grid, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = stroke)
        val slot = size.width / values.size
        val gap = AnalyticsChartsGeometry.chartGap.toPx().coerceAtMost(slot / 2)
        val barWidth = (slot - gap).coerceAtLeast(0f)
        values.forEachIndexed { index, value ->
            if (!value.isFinite()) return@forEachIndexed
            val x = slot * index + gap / 2
            val left = if (rtl) size.width - x - barWidth else x
            val top = minOf(baseline, y(value))
            val barHeight = kotlin.math.abs(y(value) - baseline)
            if (barHeight > 0) drawRect(color, Offset(left, top), Size(barWidth, barHeight))
            if (index == selectedIndex) {
                val markerY = y(value).coerceIn(stroke / 2, (size.height - stroke / 2).coerceAtLeast(stroke / 2))
                drawLine(color, Offset(left, markerY), Offset(left + barWidth, markerY), stroke * 2, StrokeCap.Round)
            }
        }
    }
}

private fun lineChartInset(width: Float, height: Float, radius: Float, stroke: Float): Float =
    (radius + stroke * 1.5f).coerceAtMost(minOf(width, height) / 2)

private object AnalyticsChartsGeometry {
    val chartBarHeight = 90.dp
    val chartGap = 2.dp
    val chartLineHeight = 120.dp
    val chartMarkerRadius = 3.dp
    val chartStroke = 2.dp
}
