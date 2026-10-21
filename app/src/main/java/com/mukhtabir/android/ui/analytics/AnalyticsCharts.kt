@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
internal fun AnalyticsLineChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val path = Path()
        values.forEachIndexed { index, value ->
            val logicalX = if (values.size == 1) size.width / 2 else size.width * index / (values.size - 1)
            val x = if (rtl) size.width - logicalX else logicalX
            val y = size.height - (value / max * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}

@Composable
internal fun AnalyticsBarChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.tertiary
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val gap = 2f
        val barWidth = (size.width / values.size - gap).coerceAtLeast(1f)
        values.forEachIndexed { index, value ->
            val height = (value / max * size.height).toFloat()
            drawRect(
                color,
                topLeft = Offset(if (rtl) size.width - barWidth - index * (barWidth + gap) else index * (barWidth + gap), size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
            )
        }
    }
}
