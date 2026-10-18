package com.mukhtabir.android.ui.analytics

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.StreakHistoryDto
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
        val days = s.data?.days.orEmpty()
        val cells = com.mukhtabir.android.ui.components.activityCalendarCells(days)
        if (display.activityHeatmap && days.isNotEmpty()) item {
            com.mukhtabir.android.ui.components.AppActivityHeatmap(cells)
        }
        if (!display.activityHeatmap && days.isNotEmpty()) item {
            com.mukhtabir.android.ui.components.AppActivityWeekdays()
        }
        if (!display.activityHeatmap) cells.chunked(7).forEach { week -> item {
            com.mukhtabir.android.ui.components.AppActivityWeek(week, showDetails = true)
        } }
    }
}
