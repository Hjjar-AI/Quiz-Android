package com.mukhtabir.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.StreakDayDto
import com.mukhtabir.android.util.formatCalendarDate
import com.mukhtabir.android.util.formatNumber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Shared classification includes sessions without answered questions. */
object ActivityCalendarTokens {
    val levels = listOf(.08f, .3f, .6f, 1f)
    fun level(day: StreakDayDto): Float = when {
        day.questions == 0 && day.sessions == 0 -> levels[0]
        day.questions < 10 -> levels[1]
        day.questions < 30 -> levels[2]
        else -> levels[3]
    }
}

fun activityCalendarCells(days: List<StreakDayDto>): List<StreakDayDto?> {
    val ordered = days.sortedBy { it.date }
    val leading = ordered.firstOrNull()?.date?.let {
        runCatching { LocalDate.parse(it).dayOfWeek.value - 1 }.getOrDefault(0)
    } ?: 0
    return List<StreakDayDto?>(leading) { null } + ordered
}

@Composable
private fun activityLevelColor(level: Float): Color = MaterialTheme.colorScheme.primary.copy(alpha = level)
    .compositeOver(MaterialTheme.colorScheme.surface)

@Composable
private fun activityCellColor(day: StreakDayDto): Color = activityLevelColor(ActivityCalendarTokens.level(day))

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
    Row(Modifier.fillMaxWidth().then(if (showDetails) Modifier.clearAndSetSemantics { } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        week.forEach { day ->
            if (day == null) Box(Modifier.weight(1f).clearAndSetSemantics { })
            else {
                val label = stringResource(R.string.activity_day, formatCalendarDate(day.date),
                    formatNumber(day.sessions), formatNumber(day.questions))
                val color = activityCellColor(day)
                Box(Modifier.weight(1f).background(color, MaterialTheme.shapes.extraSmall)
                    .semantics(mergeDescendants = true) { contentDescription = label }
                    .padding(AppLayoutTokens.denseCardInsets), contentAlignment = Alignment.Center) {
                    Text(formatNumber(day.questions), style = MaterialTheme.typography.labelSmall,
                        color = if (color.luminance() > .179f) Color.Black else Color.White,
                        modifier = Modifier.clearAndSetSemantics { })
                }
            }
        }
    }
    if (showDetails) week.filterNotNull().forEach { day ->
        Text(stringResource(R.string.activity_day, formatCalendarDate(day.date),
            formatNumber(day.sessions), formatNumber(day.questions)))
    }
}

/** Seven weekday rows and scrollable week columns, without visible numeric counts. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun AppActivityHeatmap(cells: List<StreakDayDto?>) {
    val cellSize = AppActivityCalendarGeometry.heatmapCellSize * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gap = AppLayoutTokens.tightGap
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
                                val label=stringResource(R.string.activity_day,
                                    com.mukhtabir.android.util.formatCalendarDate(day.date),
                                    formatNumber(day.sessions),formatNumber(day.questions))
                                Box(Modifier.size(cellSize).background(activityCellColor(day),MaterialTheme.shapes.extraSmall)
                                    .semantics { contentDescription=label })
                            }
                        }
                    }
                }
            }
        }
        AppActionRow {
            Text(stringResource(R.string.activity_less),style=MaterialTheme.typography.bodySmall)
            ActivityCalendarTokens.levels.forEach { intensity ->
                Box(Modifier.size(cellSize).background(
                    activityLevelColor(intensity),
                    MaterialTheme.shapes.extraSmall).clearAndSetSemantics { })
            }
            Text(stringResource(R.string.activity_more),style=MaterialTheme.typography.bodySmall)
        }
    }
}

private object AppActivityCalendarGeometry {
    val heatmapCellSize = 24.dp
}
