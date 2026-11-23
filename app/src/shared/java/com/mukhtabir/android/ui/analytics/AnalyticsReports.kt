package com.mukhtabir.android.ui.analytics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.ExamDurationReportDto
import com.mukhtabir.android.ui.components.OperatorRows
import com.mukhtabir.android.ui.components.difficultyLabel
import com.mukhtabir.android.util.formatNumber
import java.text.NumberFormat
import kotlinx.serialization.json.JsonObject

@Composable
internal fun analyticsReportTitle(key: String): String = stringResource(when (key) {
    "difficulty_calibration" -> R.string.analytics_calibration_title
    "author_flag_rate" -> R.string.analytics_author_flags_title
    "cohort_comparison" -> R.string.analytics_cohorts_title
    "retention" -> R.string.analytics_retention_title
    "exam_duration" -> R.string.analytics_duration_title
    else -> R.string.administrative_reports
})

@Composable
internal fun analyticsDifficultyLabel(key: String): String = difficultyLabel(key)

/** Curated backend fields, accessible text values and paging over every returned row. */
@Composable
internal fun AnalyticsReport(key: String, rows: List<JsonObject>, duration: ExamDurationReportDto?) {
    val fields = when (key) {
        "difficulty_calibration" -> listOf("difficulty", "question_count", "total_answered", "total_correct", "observed_accuracy")
        "author_flag_rate" -> listOf("author", "total_questions", "flagged_questions", "flag_rate")
        "cohort_comparison" -> listOf("group_name", "member_count", "active_users", "session_count", "total_questions", "avg_accuracy")
        "retention" -> listOf("week_start", "active_prior_week", "active_this_week", "retained", "retention_rate")
        "exam_duration" -> listOf("bucket", "count", "percentage")
        else -> emptyList()
    }
    when (key) {
        "exam_duration" -> {
            Text(stringResource(R.string.analytics_duration_window))
            duration?.let {
                Text(stringResource(R.string.analytics_duration_total, formatNumber(it.totalSessions)))
                val seconds = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it.averageSeconds)
                Text(stringResource(R.string.analytics_duration_average, seconds))
            }
        }
        "cohort_comparison" -> Text(stringResource(R.string.analytics_cohort_window))
        "retention" -> Text(stringResource(R.string.analytics_retention_window))
        "author_flag_rate" -> Text(stringResource(R.string.analytics_author_top))
    }
    OperatorRows(rows, fields, percentKeys = setOf("observed_accuracy", "flag_rate", "avg_accuracy", "retention_rate", "percentage"))
}
