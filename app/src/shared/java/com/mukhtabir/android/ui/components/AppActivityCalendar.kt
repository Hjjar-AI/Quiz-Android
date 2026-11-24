package com.mukhtabir.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.StreakDayDto
import com.mukhtabir.android.ui.theme.appDataColors
import com.mukhtabir.android.ui.theme.contrastInk
import com.mukhtabir.android.util.formatCalendarDate
import com.mukhtabir.android.util.formatNumber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Shared classification includes sessions without answered questions. */
object ActivityCalendarTokens {
    val levels = listOf(.08f, .3f, .6f, 1f)
    fun level(day: StreakDayDto): Float = levels[levelIndex(day)]
    internal fun levelIndex(day: StreakDayDto): Int = when {
        day.questions == 0 && day.sessions == 0 -> 0
        day.questions < 10 -> 1
        day.questions < 30 -> 2
        else -> 3
    }
}

fun activityCalendarCells(days: List<StreakDayDto>): List<StreakDayDto?> {
    val ordered = days.sortedBy { it.date }
    val leading = ordered.firstOrNull()?.date?.let {
        runCatching { LocalDate.parse(it).dayOfWeek.value - 1 }.getOrDefault(0)
    } ?: 0
    return List<StreakDayDto?>(leading) { null } + ordered
}

private data class ActivityCellStyle(val fill: Color, val ink: Color)

@Composable
private fun activityCellStyles(): List<ActivityCellStyle> {
    val accent = appDataColors().primary
    val scheme = MaterialTheme.colorScheme
    return remember(accent, scheme) {
        ActivityCalendarTokens.levels.map { intensity ->
            val fill = accent.copy(alpha = intensity).compositeOver(scheme.surface)
            ActivityCellStyle(fill, contrastInk(scheme.onSurface, listOf(fill), 4.5))
        }
    }
}

@Composable
private fun activityDayLabel(day: StreakDayDto): String {
    val date = formatCalendarDate(day.date)
    return if (day.sessions == 0 && day.questions == 0) {
        stringResource(R.string.activity_no_recorded_day, date)
    } else {
        stringResource(R.string.activity_day, date, formatNumber(day.sessions), formatNumber(day.questions))
    }
}

@Composable
internal fun AppActivityDayDetail(day: StreakDayDto) {
    Text(activityDayLabel(day), style = MaterialTheme.typography.bodySmall, color = appDataColors().label)
}

@Composable
private fun ActivityCell(
    level: Int,
    style: ActivityCellStyle,
    modifier: Modifier,
    label: String? = null,
) {
    Box(
        modifier.background(style.fill, MaterialTheme.shapes.extraSmall)
            .then(if (label == null) Modifier.clearAndSetSemantics { } else Modifier.semantics { contentDescription = label }),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(AppActivityCalendarGeometry.markerSize)) {
            val stroke = AppActivityCalendarGeometry.markerStroke.toPx()
            if (level == 0) {
                drawLine(style.ink, Offset(size.width / 4, center.y), Offset(size.width * 3 / 4, center.y),
                    stroke, StrokeCap.Round)
            } else {
                repeat(level) { index ->
                    val x = size.width * (index + 1) / (level + 1)
                    drawLine(style.ink, Offset(x, size.height / 4), Offset(x, size.height * 3 / 4),
                        stroke, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun AppActivityWeekdays() {
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        DayOfWeek.values().forEach {
            Text(it.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Compact cells and the detailed list use the same dates, levels and accessible wording. */
@Composable
fun AppActivityWeek(partial: List<StreakDayDto?>, showDetails: Boolean = false) {
    val week = partial + List<StreakDayDto?>((7 - partial.size).coerceAtLeast(0)) { null }
    val styles = activityCellStyles()
    Row(Modifier.fillMaxWidth().then(if (showDetails) Modifier.clearAndSetSemantics { } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        week.forEach { day ->
            if (day == null) Box(Modifier.weight(1f).clearAndSetSemantics { })
            else {
                val label = activityDayLabel(day)
                val style = styles[ActivityCalendarTokens.levelIndex(day)]
                Box(Modifier.weight(1f).background(style.fill, MaterialTheme.shapes.extraSmall)
                    .semantics(mergeDescendants = true) { contentDescription = label }
                    .padding(AppLayoutTokens.denseCardInsets), contentAlignment = Alignment.Center) {
                    Text(formatNumber(day.questions), style = MaterialTheme.typography.labelSmall,
                        color = style.ink,
                        modifier = Modifier.clearAndSetSemantics { })
                }
            }
        }
    }
    if (showDetails) week.filterNotNull().forEach { day -> AppActivityDayDetail(day) }
}

/** Seven weekday rows and scrollable week columns, without visible numeric counts. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun AppActivityHeatmap(cells: List<StreakDayDto?>) {
    val cellSize = AppActivityCalendarGeometry.heatmapCellSize * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gap = AppLayoutTokens.tightGap
    val styles = activityCellStyles()
    val labelColor = appDataColors().label
    val scroll = rememberScrollState()
    // Dates run oldest to newest; scroll offsets are logical-start relative in RTL too.
    LaunchedEffect(cells.lastOrNull()?.date, scroll.maxValue) {
        if (scroll.maxValue != Int.MAX_VALUE) scroll.scrollTo(scroll.maxValue)
    }
    Column(verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gap)) {
            Column(Modifier.clearAndSetSemantics { },verticalArrangement=Arrangement.spacedBy(gap)) {
                DayOfWeek.values().forEach { day ->
                    Box(Modifier.size(cellSize),contentAlignment=Alignment.Center) {
                        Text(day.getDisplayName(TextStyle.NARROW,Locale.getDefault()),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.weight(1f).horizontalScroll(scroll),horizontalArrangement=Arrangement.spacedBy(gap)) {
                cells.chunked(7).forEach { partial ->
                    Column(verticalArrangement=Arrangement.spacedBy(gap)) {
                        (partial + List<StreakDayDto?>(7-partial.size) { null }).forEach { day ->
                            if(day==null) Box(Modifier.size(cellSize).clearAndSetSemantics { })
                            else {
                                val level = ActivityCalendarTokens.levelIndex(day)
                                ActivityCell(level, styles[level], Modifier.size(cellSize), label = activityDayLabel(day))
                            }
                        }
                    }
                }
            }
        }
        AppActionRow {
            Text(stringResource(R.string.activity_none), style = MaterialTheme.typography.bodySmall, color = labelColor)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                styles.forEachIndexed { level, style -> ActivityCell(level, style, Modifier.size(cellSize)) }
            }
            Text(stringResource(R.string.activity_more), style = MaterialTheme.typography.bodySmall, color = labelColor)
        }
    }
}

private object AppActivityCalendarGeometry {
    val heatmapCellSize = 24.dp
    val markerSize = 12.dp
    val markerStroke = 1.5.dp
}
