package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLocalDateTime
import com.mukhtabir.android.util.formatNumber
import kotlinx.serialization.json.*

/** Curated labels only: do not surface paths, server diagnostics or arbitrary new fields. */
@Composable
fun OperatorReport(value: JsonObject, keys: List<String>, percentKeys: Set<String> = emptySet()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        keys.forEach { key ->
            val element = value[key]
            val label = reportLabel(key)
            if (element != null && element !is JsonNull && label != null) {
                if (element is JsonPrimitive) {
                    val content = when {
                        key in percentKeys && element.doubleOrNull != null -> java.text.NumberFormat.getPercentInstance().apply {
                            maximumFractionDigits = 2
                        }.format(requireNotNull(element.doubleOrNull) / 100.0)
                        key == "week_start" -> com.mukhtabir.android.util.formatLongDate(element.content)
                        key == "bucket" -> when (element.content) {
                            "0-5" -> stringResource(R.string.analytics_bucket_0_5)
                            "5-15" -> stringResource(R.string.analytics_bucket_5_15)
                            "15-30" -> stringResource(R.string.analytics_bucket_15_30)
                            "30-60" -> stringResource(R.string.analytics_bucket_30_60)
                            "60+" -> stringResource(R.string.analytics_bucket_60_plus)
                            else -> element.content
                        }
                        element.booleanOrNull != null -> stringResource(if (element.booleanOrNull == true) R.string.yes else R.string.no)
                        key.endsWith("_at") -> formatLocalDateTime(element.content)
                        element.longOrNull != null -> formatNumber(requireNotNull(element.longOrNull))
                        element.doubleOrNull != null -> java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(element.doubleOrNull)
                        key == "difficulty" -> difficultyLabel(element.content)
                        else -> element.content
                    }
                    Text(stringResource(label) + ": " + content)
                } else if (key == "distribution" && element is JsonObject) {
                    Text(stringResource(label))
                    element.forEach { (choice, count) -> Text(stringResource(R.string.report_choice_count, choice, (count as? JsonPrimitive)?.content.orEmpty())) }
                } else if (element is JsonArray) {
                    Text(stringResource(label) + ": " + element.mapNotNull { (it as? JsonPrimitive)?.content }.joinToString(", "))
                }
            }
        }
    }
}

@Composable
fun OperatorRows(rows: List<JsonObject>, keys: List<String>, pageSize: Int = 20, percentKeys: Set<String> = emptySet()) {
    var requested by remember { mutableStateOf(1) }
    val size = pageSize.coerceAtLeast(1)
    val pages = ((rows.size - 1).coerceAtLeast(0) / size) + 1
    val page = requested.coerceIn(1, pages)
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        if (rows.isEmpty()) EmptyState(stringResource(R.string.master_empty))
        rows.drop((page - 1) * size).take(size).forEach { row ->
            AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) { OperatorReport(row, if ((row["is_complete"] as? JsonPrimitive)?.booleanOrNull == false)
                keys - setOf("correct_count", "accuracy", "weighted_score") else keys, percentKeys) } }
        }
        if (pages > 1) {
            AppPagination(page, pages,
                onPrevious = { requested = page - 1 }, onNext = { requested = page + 1 },
                statusLabel = stringResource(R.string.report_page, com.mukhtabir.android.util.formatNumber(page), com.mukhtabir.android.util.formatNumber(pages)))
        }
    }
}

fun reportLabel(key: String): Int? = when (key) {
    "total_assigned" -> R.string.report_total_assigned
    "started" -> R.string.report_started
    "finished" -> R.string.report_finished
    "in_progress" -> R.string.report_in_progress
    "not_started" -> R.string.report_not_started
    "avg_accuracy" -> R.string.report_avg_accuracy
    "avg_weighted_score" -> R.string.report_avg_weighted_score
    "full_name" -> R.string.report_full_name
    "username" -> R.string.report_username
    "is_complete" -> R.string.report_is_complete
    "is_makeup" -> R.string.report_is_makeup
    "forced_finish" -> R.string.report_forced_finish
    "correct_count" -> R.string.report_correct_count
    "total_questions" -> R.string.report_total_questions
    "answered_count" -> R.string.report_answered_count
    "accuracy" -> R.string.report_accuracy
    "weighted_score" -> R.string.report_weighted_score
    "started_at" -> R.string.report_started_at
    "finished_at" -> R.string.report_finished_at
    "question_id" -> R.string.report_question_id
    "question_text" -> R.string.report_question_text
    "correct_answer" -> R.string.report_correct_answer
    "correct_rate" -> R.string.report_correct_rate
    "distribution" -> R.string.report_distribution
    "category_name" -> R.string.report_category_name
    "total" -> R.string.report_total
    "answered" -> R.string.report_answered
    "correct" -> R.string.report_correct
    "difficulty" -> R.string.report_difficulty
    "range" -> R.string.report_range
    "count" -> R.string.report_count
    "flagger_username" -> R.string.report_flagger_username
    "reason" -> R.string.report_reason
    "created_at" -> R.string.report_created_at
    "database_type" -> R.string.report_database_type
    "file_size" -> R.string.report_file_size
    "file_modified" -> R.string.report_file_modified
    "total_users" -> R.string.report_total_users
    "total_categories" -> R.string.report_total_categories
    "total_sessions" -> R.string.report_total_sessions
    "verified_count" -> R.string.report_verified_count
    "questions_scanned" -> R.string.report_questions_scanned
    "questions_with_issues" -> R.string.report_questions_with_issues
    "duplicate_question" -> R.string.report_duplicate_question
    "duplicate_choices" -> R.string.report_duplicate_choices
    "missing_explanation" -> R.string.report_missing_explanation
    "missing_category" -> R.string.report_missing_category
    "missing_source_document" -> R.string.report_missing_source_document
    "invalid_translations" -> R.string.report_invalid_translations
    "translation_choice_count_mismatch" -> R.string.report_translation_choice_count_mismatch
    "has_open_quality_flag" -> R.string.report_has_open_quality_flag
    "scanned" -> R.string.report_scanned
    "updated" -> R.string.report_updated
    "flags_created" -> R.string.report_flags_created
    "name" -> R.string.report_name
    "size" -> R.string.report_size
    "modified" -> R.string.report_modified
    "positions" -> R.string.report_positions
    "locales" -> R.string.report_locales
    "related_question_ids" -> R.string.report_related_question_ids
    "question_count" -> R.string.report_total_questions
    "total_answered" -> R.string.report_answered_count
    "total_correct" -> R.string.report_correct_count
    "observed_accuracy" -> R.string.analytics_observed_accuracy
    "author" -> R.string.analytics_author
    "flagged_questions" -> R.string.analytics_flagged_questions
    "flag_rate" -> R.string.analytics_flag_rate
    "group_name" -> R.string.analytics_group_name
    "member_count" -> R.string.analytics_members
    "active_users" -> R.string.active_users
    "session_count" -> R.string.sessions
    "week_start" -> R.string.analytics_week_start
    "active_prior_week" -> R.string.analytics_prior_active
    "active_this_week" -> R.string.analytics_this_active
    "retained" -> R.string.analytics_retained
    "retention_rate" -> R.string.analytics_retention_rate
    "bucket" -> R.string.analytics_duration_bucket
    "percentage" -> R.string.analytics_percentage
    else -> null
}
