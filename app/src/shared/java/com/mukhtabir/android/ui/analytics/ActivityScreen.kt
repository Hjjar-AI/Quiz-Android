package com.mukhtabir.android.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.data.remote.LearnerAnalytics
import com.mukhtabir.android.data.remote.StreakHistoryDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppActivityDayDetail
import com.mukhtabir.android.ui.components.AppActivityHeatmap
import com.mukhtabir.android.ui.components.AppActivityWeek
import com.mukhtabir.android.ui.components.AppActivityWeekdays
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.activityCalendarCells
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ActivityState(
    val busy: Boolean = false,
    val days: Int = 90,
    val data: StreakHistoryDto? = null,
    val displayedDays: Int? = null,
    val error: Throwable? = null,
)

private class ActivityViewModel(private val service: LearnerAnalytics) : ViewModel() {
    private val _state = MutableStateFlow(ActivityState())
    val state = _state.asStateFlow()
    fun load(days: Int = _state.value.days) {
        if (_state.value.busy || days !in setOf(30, 90, 365)) return
        _state.update { it.copy(busy = true, error = null, days = days) }
        viewModelScope.launch {
            try {
                val data = service.streakHistory(days)
                ensureActive()
                _state.update { it.copy(data = data, displayedDays = days, busy = false) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                ensureActive()
                _state.update { it.copy(error = error, busy = false) }
            }
        }
    }
    class Factory(private val service: LearnerAnalytics) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ActivityViewModel(service) as T
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun ActivityRoute(service: LearnerAnalytics, userId: Long, preferences: AppPreferences) {
    val vm: ActivityViewModel = viewModel(key = "activity-$userId", factory = ActivityViewModel.Factory(service))
    val s by vm.state.collectAsLifecycleState()
    val display by preferences.display.collectAsLifecycleState()
    var showDailyValues by rememberSaveable(userId) { mutableStateOf(false) }
    RefreshOnResume { vm.load() }
    LazyColumn(Modifier.padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.activity_title))
            AppActionRow { listOf(30, 90, 365).forEach { count -> AppFilterChip(s.days == count, { vm.load(count) }, enabled = !s.busy,
                label = { Text(stringResource(R.string.activity_days, formatNumber(count))) }) } }
            AppActionRow {
                AppFilterChip(!display.activityHeatmap, { preferences.setDisplay(display.copy(activityHeatmap=false)) },
                    label={Text(stringResource(R.string.activity_numbered))})
                AppFilterChip(display.activityHeatmap, { preferences.setDisplay(display.copy(activityHeatmap=true)) },
                    label={Text(stringResource(R.string.activity_heatmap))})
            }
            AppOutlinedButton({ vm.load() }, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            if (s.busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else ({ vm.load() })) }
            s.data?.let { data ->
                s.displayedDays?.takeIf { it != s.days }?.let { days ->
                    Text(stringResource(R.string.analytics_showing_period, formatNumber(days)))
                }
                Text(stringResource(R.string.activity_summary, formatNumber(data.currentStreak), formatNumber(data.longestStreak), formatNumber(data.activeDays), formatNumber(data.totalQuestions)))
                Text(stringResource(if(display.activityHeatmap) R.string.activity_heatmap_legend else R.string.activity_legend))
            }
        }
        val days = s.data?.days.orEmpty()
        val cells = activityCalendarCells(days)
        if (s.data != null && days.isEmpty() && !s.busy && s.error == null) item {
            EmptyState(stringResource(R.string.no_items))
        }
        if (display.activityHeatmap && days.isNotEmpty()) {
            item {
                AppFilterChip(showDailyValues, onClick = { showDailyValues = !showDailyValues },
                    label = { Text(stringResource(R.string.analytics_daily_values)) })
                AppActivityHeatmap(cells)
            }
            if (showDailyValues) items(cells.filterNotNull().asReversed()) { day ->
                AppActivityDayDetail(day)
            }
        }
        if (!display.activityHeatmap && days.isNotEmpty()) item {
            AppActivityWeekdays()
        }
        if (!display.activityHeatmap) cells.chunked(7).forEach { week -> item {
            AppActivityWeek(week, showDetails = true)
        } }
    }
}
