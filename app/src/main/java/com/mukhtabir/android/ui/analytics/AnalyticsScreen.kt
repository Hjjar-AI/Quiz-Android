@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.analytics

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.AnalyticsSummaryDto
import com.mukhtabir.android.data.remote.CategoryMasteryDto
import com.mukhtabir.android.data.remote.ExamDurationReportDto
import com.mukhtabir.android.data.remote.ActiveUsersPointDto
import com.mukhtabir.android.data.remote.StreakHistoryDto
import kotlinx.serialization.json.JsonObject
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.util.formatNumber
import com.mukhtabir.android.util.formatDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val summary: AnalyticsSummaryDto? = null,
    val mastery: CategoryMasteryDto? = null,
    val streak: StreakHistoryDto? = null,
    val adminReports: Map<String, List<JsonObject>> = emptyMap(),
    val duration: ExamDurationReportDto? = null,
    val activeUsers: List<ActiveUsersPointDto>? = null,
    val activeUsersDays: Int? = null,
    val days: Int = 30,
    val summaryDays: Int? = null,
    val loading: Set<String> = emptySet(),
    val errors: Map<String, Throwable> = emptyMap(),
) {
    val isLoading: Boolean get() = loading.isNotEmpty()
}

class AnalyticsViewModel(
    private val service: AnalyticsService,
    private val canViewAll: Boolean,
    private val canViewActiveUsers: Boolean,
    savedStateHandle: SavedStateHandle,
    userId: Long,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, userId, "analytics-period")
    private val _state = MutableStateFlow(AnalyticsUiState(days = saved.filter()?.toIntOrNull()?.takeIf { it in setOf(7, 30, 90) } ?: 30))
    val state = _state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    private val adminKeys = listOf("difficulty_calibration", "author_flag_rate", "cohort_comparison", "retention", "exam_duration")

    init { load() }

    fun selectDays(days: Int) {
        if (days !in setOf(7, 30, 90) || days == _state.value.days) return
        saved.saveFilter(days.toString())
        (listOf("summary") + if (canViewActiveUsers) listOf("active_users") else emptyList()).forEach { key ->
            jobs.remove(key)?.cancel()
            _state.update { it.copy(days = days, loading = it.loading - key, errors = it.errors - key) }
            retry(key)
        }
    }

    fun load() {
        (listOf("summary", "mastery", "streak") + (if (canViewAll) adminKeys else emptyList()) +
            (if (canViewActiveUsers) listOf("active_users") else emptyList())).forEach(::retry)
    }

    fun retry(key: String) {
        if (key !in listOf("summary", "mastery", "streak") && !(canViewAll && key in adminKeys) &&
            !(canViewActiveUsers && key == "active_users")) return
        if (key in _state.value.loading) return
        val days = _state.value.days
        _state.update { it.copy(loading = it.loading + key, errors = it.errors - key) }
        jobs[key] = viewModelScope.launch {
            try {
                when (key) {
                    "summary" -> {
                        val result = service.summary(days); ensureActive()
                        _state.update { it.copy(summary = result, summaryDays = days) }
                    }
                    "active_users" -> {
                        val result = service.activeUsers(days); ensureActive()
                        _state.update { it.copy(activeUsers = result, activeUsersDays = days) }
                    }
                    "exam_duration" -> {
                        val result = service.examDuration(); ensureActive()
                        _state.update { it.copy(duration = result, adminReports = it.adminReports + (key to result.buckets)) }
                    }
                    "mastery" -> {
                        val result = service.categoryMastery(); ensureActive()
                        _state.update { it.copy(mastery = result) }
                    }
                    "streak" -> {
                        val result = service.streakHistory(); ensureActive()
                        _state.update { it.copy(streak = result) }
                    }
                    else -> {
                        val rows = when (key) {
                            "difficulty_calibration" -> service.difficultyCalibration().rows
                            "author_flag_rate" -> service.authorFlagRate().rows
                            "cohort_comparison" -> service.cohortComparison().rows
                            "retention" -> service.retention().rows
                            else -> error("Unsupported analytics report")
                        }
                        ensureActive()
                        _state.update { it.copy(adminReports = it.adminReports + (key to rows)) }
                    }
                }
                _state.update { it.copy(loading = it.loading - key) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val denied = failure is com.mukhtabir.android.data.remote.ApiException && failure.statusCode in setOf(401, 403, 404)
                _state.update { it.copy(loading = it.loading - key, errors = it.errors + (key to failure),
                    summary = if (denied && key == "summary") null else it.summary,
                    mastery = if (denied && key == "mastery") null else it.mastery,
                    streak = if (denied && key == "streak") null else it.streak,
                    activeUsers = if (denied && key == "active_users") null else it.activeUsers,
                    duration = if (denied && key == "exam_duration") null else it.duration,
                    adminReports = if (denied) it.adminReports - key else it.adminReports) }
            }
        }
    }

    class Factory(private val service: AnalyticsService, private val canViewAll: Boolean, private val canViewActiveUsers: Boolean, private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AnalyticsViewModel(service, canViewAll, canViewActiveUsers, extras.createSavedStateHandle(), userId) as T
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsRoute(
    service: AnalyticsService,
    canViewAll: Boolean,
    canViewActiveUsers: Boolean,
    userId: Long,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
) {
    val viewModel: AnalyticsViewModel = viewModel(key = "analytics-$userId-$canViewAll-$canViewActiveUsers", factory = AnalyticsViewModel.Factory(service, canViewAll, canViewActiveUsers, userId))
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppPageHeading(stringResource(R.string.analytics))
                Button(onClick = viewModel::load, enabled = !state.isLoading) {
                    Text(stringResource(R.string.refresh))
                }
            }
        }
        if (state.isLoading) item { AppLoadingIndicator() }
        item {
            AppActionRow() {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(selected = state.days == days, onClick = { viewModel.selectDays(days) },
                        label = { Text(stringResource(R.string.activity_days, formatNumber(days))) })
                }
            }
            state.summaryDays?.let { days -> Text(stringResource(R.string.analytics_showing_period, formatNumber(days))) }
        }
        state.errors.forEach { (key, error) -> item(key = "analytics-error-$key") {
            Text(when (key) {
                "summary" -> stringResource(R.string.analytics)
                "mastery" -> stringResource(R.string.category_mastery)
                "streak" -> stringResource(R.string.study_streak)
                "active_users" -> stringResource(R.string.active_users)
                else -> analyticsReportTitle(key)
            }, style = MaterialTheme.typography.titleSmall)
            ErrorState(throwable = error, onRetry = if (key in state.loading) null else ({ viewModel.retry(key) }))
        } }

        val summary = state.summary
        if (summary != null) {
            if (summary.userPerformance.isNotEmpty()) {
                item {
                    AnalyticsCard(stringResource(R.string.performance_trend)) {
                        LineChart(summary.userPerformance.map { it.accuracy })
                        AppSection(stringResource(R.string.analytics_daily_values)) {
                            summary.userPerformance.forEach { point ->
                                com.mukhtabir.android.ui.components.AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(point.date), stringResource(R.string.analytics_daily_performance, point.accuracy, formatNumber(point.questionsAnswered)))
                            }
                        }
                    }
                }
            }

            item {
                val confidence = summary.confidenceStats
                AnalyticsCard(stringResource(R.string.confidence)) {
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.total_attempted), formatNumber(confidence.totalAttempted))
                    com.mukhtabir.android.ui.components.AppProgressMetric(stringResource(R.string.confident_correct), confidence.correctConfident, confidence.totalAttempted)
                    com.mukhtabir.android.ui.components.AppProgressMetric(stringResource(R.string.fragile_answers), confidence.correctFragile, confidence.totalAttempted)
                    com.mukhtabir.android.ui.components.AppProgressMetric(stringResource(R.string.open_mistakes), confidence.wrongOpen, confidence.totalAttempted)
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.fragile_ratio), "${"%.1f".format(confidence.fragileRatio)}%")
                }
            }

            if (summary.weakCategories.isNotEmpty()) {
                item {
                    AnalyticsCard(stringResource(R.string.weak_categories_title)) {
                        summary.weakCategories.forEach { row ->
                            val name = row["category_name"]?.toString()?.trim('"') ?: "—"
                            val accuracy = row["accuracy"]?.toString()?.toDoubleOrNull() ?: 0.0
                            com.mukhtabir.android.ui.components.AppMetricRow(name, "${"%.1f".format(accuracy)}%")
                        }
                    }
                }
            }
        }

        val activeUsers = if (canViewActiveUsers) state.activeUsers else if (canViewAll) summary?.activeUsers else null
        activeUsers?.let { points -> item {
            AnalyticsCard(stringResource(R.string.active_users)) {
                val period = if (canViewActiveUsers) state.activeUsersDays else state.summaryDays
                period?.let { Text(stringResource(R.string.analytics_showing_period, formatNumber(it))) }
                if (points.isEmpty() && !state.isLoading && state.errors.isEmpty()) EmptyState(stringResource(R.string.no_items))
                else {
                    LineChart(points.map { it.activeUsers.toDouble() })
                    AppSection(stringResource(R.string.analytics_daily_values)) {
                        points.forEach { point ->
                            com.mukhtabir.android.ui.components.AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(point.date), stringResource(R.string.active_users_metric, point.activeUsers, point.newUsers, point.totalSessions))
                        }
                    }
                }
            }
        } }

        state.mastery?.let { mastery ->
            item {
                AnalyticsCard(stringResource(R.string.category_mastery)) {
                    if (mastery.categories.isEmpty() && !state.isLoading && state.errors.isEmpty()) EmptyState(stringResource(R.string.no_items))
                    mastery.categories.forEach { category ->
                        Text(category.categoryName, style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = (category.masteryScore / 100.0).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        com.mukhtabir.android.ui.components.AppMetricRow(
                            stringResource(R.string.mastery_details, category.attempts, category.distinctQuestions),
                            "${"%.1f".format(category.masteryScore)}%",
                        )
                    }
                }
            }
        }

        state.streak?.let { streak ->
            item {
                AnalyticsCard(stringResource(R.string.study_streak)) {
                    val labels=listOf(stringResource(R.string.current_streak),stringResource(R.string.longest_streak),
                        stringResource(R.string.active_days))
                    val values=listOf(streak.currentStreak,streak.longestStreak,streak.activeDays)
                    com.mukhtabir.android.ui.components.AppTileGrid(values.size) { index,tileModifier ->
                        com.mukhtabir.android.ui.components.AppMetricTile(formatNumber(values[index]),labels[index],tileModifier)
                    }
                    BarChart(streak.days.map { it.questions.toDouble() })
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.sessions), formatNumber(streak.totalSessions))
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.questions), formatNumber(streak.totalQuestions))
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.unique_questions), formatNumber(streak.uniqueQuestions))
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.unique_concepts), formatNumber(streak.uniqueConcepts))
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.due_reviews_completed), formatNumber(streak.dueReviews))
                    com.mukhtabir.android.ui.components.AppMetricRow(stringResource(R.string.mastery_change), formatDecimal(streak.masteryGain))
                    AppSection(stringResource(R.string.analytics_daily_values)) {
                        streak.days.forEach { day ->
                            com.mukhtabir.android.ui.components.AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(day.date), stringResource(R.string.analytics_daily_activity, formatNumber(day.questions), formatNumber(day.sessions)))
                        }
                    }
                }
            }
        }

        if (summary != null && summary.categoryCoverage.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.category_coverage)) {
                    summary.categoryCoverage.forEach { category ->
                        com.mukhtabir.android.ui.components.AppProgressMetric(category.categoryName, category.verified, category.total)
                    }
                }
            }
        }
        if (summary != null && summary.tagCoverage.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.tag_coverage)) {
                    summary.tagCoverage.forEach { tag ->
                        com.mukhtabir.android.ui.components.AppProgressMetric(tag.tagName, tag.verifiedCount, tag.count)
                    }
                }
            }
        }
        if (summary != null && summary.difficultyStats.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.difficulty_breakdown)) {
                    summary.difficultyStats.forEach { difficulty ->
                        com.mukhtabir.android.ui.components.AppMetricRow(
                            analyticsDifficultyLabel(difficulty.difficulty),
                            stringResource(
                                R.string.difficulty_metric,
                                difficulty.count,
                                difficulty.averageCorrectRate * 100,
                            ),
                        )
                    }
                }
            }
        }
        if (!state.isLoading && state.errors.isEmpty() && state.summary == null) {
            item { EmptyState(stringResource(R.string.no_items)) }
        }
        if (state.adminReports.isNotEmpty()) {
            item { AppPageHeading(stringResource(R.string.administrative_reports)) }
            state.adminReports.forEach { (name, rows) ->
                item(key = "analytics-report-$name") {
                    AnalyticsCard(analyticsReportTitle(name)) {
                        AnalyticsReport(name, rows, state.duration)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppSection(title, initiallyExpanded = true, content = content)
}

@Composable
private fun LineChart(values: List<Double>) {
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
private fun BarChart(values: List<Double>) {
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
