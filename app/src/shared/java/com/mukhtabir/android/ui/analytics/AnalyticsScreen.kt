@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerAnalytics
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetricRow
import com.mukhtabir.android.ui.components.AppMetricTile
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppProgressBar
import com.mukhtabir.android.ui.components.AppProgressMetric
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppTileGrid
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.bidi
import com.mukhtabir.android.util.formatDecimal
import com.mukhtabir.android.util.formatNumber

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsRoute(
    service: LearnerAnalytics,
    canViewAll: Boolean,
    canViewActiveUsers: Boolean,
    userId: Long,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
) {
    val viewModel: AnalyticsViewModel = viewModel(key = "analytics-$userId-$canViewAll-$canViewActiveUsers", factory = AnalyticsViewModel.Factory(service, canViewAll, canViewActiveUsers, userId))
    val state by viewModel.state.collectAsLifecycleState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        item {
            AppPageHeading(stringResource(R.string.analytics), action = {
                AppButton(onClick = viewModel::load, enabled = !state.isLoading) { Text(stringResource(R.string.refresh)) }
            })
        }
        if (state.isLoading) item { AppLoadingIndicator() }
        item {
            AppActionRow() {
                listOf(7, 30, 90).forEach { days ->
                    AppFilterChip(selected = state.days == days, onClick = { viewModel.selectDays(days) },
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
                        AnalyticsLineChart(summary.userPerformance.map { it.accuracy })
                        AppSection(stringResource(R.string.analytics_daily_values)) {
                            summary.userPerformance.forEach { point ->
                                AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(point.date), stringResource(R.string.analytics_daily_performance, point.accuracy, formatNumber(point.questionsAnswered)))
                            }
                        }
                    }
                }
            }

            item {
                val confidence = summary.confidenceStats
                AnalyticsCard(stringResource(R.string.confidence)) {
                    AppMetricRow(stringResource(R.string.total_attempted), formatNumber(confidence.totalAttempted))
                    AppProgressMetric(stringResource(R.string.confident_correct), confidence.correctConfident, confidence.totalAttempted)
                    AppProgressMetric(stringResource(R.string.fragile_answers), confidence.correctFragile, confidence.totalAttempted)
                    AppProgressMetric(stringResource(R.string.open_mistakes), confidence.wrongOpen, confidence.totalAttempted)
                    AppMetricRow(stringResource(R.string.fragile_ratio), bidi("${formatDecimal(confidence.fragileRatio)}%"))
                }
            }

            if (summary.weakCategories.isNotEmpty()) {
                item {
                    AnalyticsCard(stringResource(R.string.weak_categories_title)) {
                        summary.weakCategories.forEach { row ->
                            val name = row["category_name"]?.toString()?.trim('"') ?: "—"
                            val accuracy = row["accuracy"]?.toString()?.toDoubleOrNull() ?: 0.0
                            AppMetricRow(name, bidi("${formatDecimal(accuracy)}%"))
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
                    AnalyticsLineChart(points.map { it.activeUsers.toDouble() })
                    AppSection(stringResource(R.string.analytics_daily_values)) {
                        points.forEach { point ->
                            AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(point.date), stringResource(R.string.active_users_metric, com.mukhtabir.android.util.formatNumber(point.activeUsers), com.mukhtabir.android.util.formatNumber(point.newUsers), com.mukhtabir.android.util.formatNumber(point.totalSessions)))
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
                        AppProgressBar(
                            progress = (category.masteryScore / 100.0).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AppMetricRow(
                            stringResource(R.string.mastery_details, com.mukhtabir.android.util.formatNumber(category.attempts), com.mukhtabir.android.util.formatNumber(category.distinctQuestions)),
                            bidi("${formatDecimal(category.masteryScore)}%"),
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
                    AppTileGrid(values.size) { index,tileModifier ->
                        AppMetricTile(formatNumber(values[index]),labels[index],tileModifier)
                    }
                    AnalyticsBarChart(streak.days.map { it.questions.toDouble() })
                    AppMetricRow(stringResource(R.string.sessions), formatNumber(streak.totalSessions))
                    AppMetricRow(stringResource(R.string.questions), formatNumber(streak.totalQuestions))
                    AppMetricRow(stringResource(R.string.unique_questions), formatNumber(streak.uniqueQuestions))
                    AppMetricRow(stringResource(R.string.unique_concepts), formatNumber(streak.uniqueConcepts))
                    AppMetricRow(stringResource(R.string.due_reviews_completed), formatNumber(streak.dueReviews))
                    AppMetricRow(stringResource(R.string.mastery_change), formatDecimal(streak.masteryGain))
                    AppSection(stringResource(R.string.analytics_daily_values)) {
                        streak.days.forEach { day ->
                            AppMetricRow(com.mukhtabir.android.util.formatCalendarDate(day.date), stringResource(R.string.analytics_daily_activity, formatNumber(day.questions), formatNumber(day.sessions)))
                        }
                    }
                }
            }
        }

        if (summary != null && summary.categoryCoverage.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.category_coverage)) {
                    summary.categoryCoverage.forEach { category ->
                        AppProgressMetric(category.categoryName, category.verified, category.total)
                    }
                }
            }
        }
        if (summary != null && summary.tagCoverage.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.tag_coverage)) {
                    summary.tagCoverage.forEach { tag ->
                        AppProgressMetric(tag.tagName, tag.verifiedCount, tag.count)
                    }
                }
            }
        }
        if (summary != null && summary.difficultyStats.isNotEmpty()) {
            item {
                AnalyticsCard(stringResource(R.string.difficulty_breakdown)) {
                    summary.difficultyStats.forEach { difficulty ->
                        AppMetricRow(
                            analyticsDifficultyLabel(difficulty.difficulty),
                            stringResource(
                                R.string.difficulty_metric, com.mukhtabir.android.util.formatNumber(difficulty.count),
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
