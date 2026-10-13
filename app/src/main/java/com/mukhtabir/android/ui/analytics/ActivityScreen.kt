package com.mukhtabir.android.ui.analytics

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.StreakHistoryDto
import com.mukhtabir.android.data.remote.StreakDayDto
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ActivityState(val busy: Boolean = false, val days: Int = 90, val data: StreakHistoryDto? = null, val error: Throwable? = null)
private class ActivityViewModel(private val service: AnalyticsService) : ViewModel() {
    private val _state = MutableStateFlow(ActivityState())
    val state = _state.asStateFlow()
    fun load(days: Int = _state.value.days) {
        if (_state.value.busy || days !in setOf(30, 90, 365)) return
        _state.update { it.copy(busy = true, error = null, days = days) }
        viewModelScope.launch {
            try { val data = service.streakHistory(days); ensureActive(); _state.update { it.copy(data = data, busy = false) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(error = error, busy = false) } }
        }
    }
    class Factory(private val service: AnalyticsService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ActivityViewModel(service) as T
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun ActivityRoute(service: AnalyticsService, userId: Long, preferences: AppPreferences) {
    val vm: ActivityViewModel = viewModel(key = "activity-$userId", factory = ActivityViewModel.Factory(service))
    val s by vm.state.collectAsState()
    val display by preferences.display.collectAsState()
    RefreshOnResume { vm.load() }
    LazyColumn(Modifier.padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.activity_title))
            com.mukhtabir.android.ui.components.AppActionRow { listOf(30, 90, 365).forEach { count -> FilterChip(s.days == count, { vm.load(count) }, enabled = !s.busy,
                label = { Text(stringResource(R.string.activity_days, formatNumber(count))) }) } }
            com.mukhtabir.android.ui.components.AppActionRow {
                FilterChip(!display.activityHeatmap, { preferences.setDisplay(display.copy(activityHeatmap=false)) },
                    label={Text(stringResource(R.string.activity_numbered))})
                FilterChip(display.activityHeatmap, { preferences.setDisplay(display.copy(activityHeatmap=true)) },
                    label={Text(stringResource(R.string.activity_heatmap))})
            }
            OutlinedButton({ vm.load() }, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            if (s.busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else ({ vm.load() })) }
            s.data?.let { data ->
                Text(stringResource(R.string.activity_summary, formatNumber(data.currentStreak), formatNumber(data.longestStreak), formatNumber(data.activeDays), formatNumber(data.totalQuestions)))
                Text(stringResource(if(display.activityHeatmap) R.string.activity_heatmap_legend else R.string.activity_legend))
            }
        }
        val days = s.data?.days.orEmpty().sortedBy { it.date }
        val firstWeekday = days.firstOrNull()?.date?.let { runCatching { LocalDate.parse(it).dayOfWeek.value - 1 }.getOrDefault(0) } ?: 0
        val cells: List<StreakDayDto?> = List(firstWeekday) { null } + days
        if (display.activityHeatmap && days.isNotEmpty()) item { ActivityHeatmap(cells) }
        if (!display.activityHeatmap && days.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                DayOfWeek.values().forEach { Text(it.getDisplayName(TextStyle.NARROW, Locale.getDefault()), modifier = Modifier.weight(1f)) }
            }
        }
        // Calendar weeks align to Monday; the exact date/count list provides the accessible alternative.
        if (!display.activityHeatmap) cells.chunked(7).forEach { partial -> item {
            val week = partial + List<StreakDayDto?>(7 - partial.size) { null }
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.tightGap)) {
                week.forEach { day ->
                    if (day == null) {
                        Column(Modifier.weight(1f)) { Text(" ") }
                    } else {
                        val cellColor = activityCellColor(day)
                        Column(Modifier.weight(1f).background(cellColor).padding(4.dp)) {
                            Text(formatNumber(day.questions), color = if (cellColor.luminance() > .179f) Color.Black else Color.White)
                        }
                    }
                }
            }
            week.filterNotNull().forEach { day -> Text(stringResource(R.string.activity_day, com.mukhtabir.android.util.formatCalendarDate(day.date), formatNumber(day.sessions), formatNumber(day.questions))) }
        } }
    }
}

@Composable
private fun activityCellColor(day: StreakDayDto): Color {
    val intensity = if (day.questions == 0 && day.sessions == 0) .08f
        else if (day.questions < 10) .3f else if (day.questions < 30) .6f else 1f
    return MaterialTheme.colorScheme.primary.copy(alpha=intensity).compositeOver(MaterialTheme.colorScheme.surface)
}

/** Seven weekday rows and scrollable week columns, without visible numeric counts. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun ActivityHeatmap(cells: List<StreakDayDto?>) {
    val cellSize = AppLayoutTokens.heatmapCellSize * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gap = AppLayoutTokens.tightGap
    Column(verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gap)) {
            Column(Modifier.clearAndSetSemantics { },verticalArrangement=Arrangement.spacedBy(gap)) {
                DayOfWeek.values().forEach { day ->
                    Box(Modifier.size(cellSize),contentAlignment=Alignment.Center) {
                        Text(day.getDisplayName(TextStyle.NARROW,Locale.getDefault()),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(gap)) {
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
        com.mukhtabir.android.ui.components.AppActionRow {
            Text(stringResource(R.string.activity_less),style=MaterialTheme.typography.bodySmall)
            listOf(.08f,.3f,.6f,1f).forEach { intensity ->
                Box(Modifier.size(cellSize).background(
                    MaterialTheme.colorScheme.primary.copy(alpha=intensity).compositeOver(MaterialTheme.colorScheme.surface),
                    MaterialTheme.shapes.extraSmall).clearAndSetSemantics { })
            }
            Text(stringResource(R.string.activity_more),style=MaterialTheme.typography.bodySmall)
        }
    }
}
