@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.util.bidi
import com.mukhtabir.android.util.formatDecimal
import com.mukhtabir.android.util.formatNumber
import com.mukhtabir.android.util.formatSessionDuration

/** Presentation only: callers retain navigation, deadline, ownership and write guards. */
@Composable
fun LearningSessionHeader(index: Int, total: Int, answered: Int, seconds: Int? = null,
    overtime: Boolean = false, critical: Boolean = false, tools: @Composable () -> Unit = {}) {
    Surface(color = if (critical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium) {
        BoxWithConstraints {
            val stacked = maxWidth < AppLayoutTokens.sessionHeaderBreakpoint || LocalDensity.current.fontScale >= 1.3f
            Column(Modifier.fillMaxWidth().padding(horizontal = AppLayoutTokens.pagePadding, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    if (index >= 0) Text(stringResource(R.string.session_position, formatNumber(index + 1), formatNumber(total)),
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                    else Spacer(Modifier.weight(1f))
                    if (seconds != null && !stacked) Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(if (overtime) R.string.session_clock_additional else R.string.session_clock_remaining),
                            style = MaterialTheme.typography.labelSmall)
                        StableLearningClock(seconds)
                    }
                    tools()
                }
                if (seconds != null && stacked) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (overtime) R.string.session_clock_additional else R.string.session_clock_remaining),
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    StableLearningClock(seconds)
                }
                if (critical) Text(stringResource(R.string.session_time_critical), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.session_answered_progress, formatNumber(answered.coerceIn(0,total.coerceAtLeast(0))), formatNumber(total)),
                    style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress = if (total > 0) (answered.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 2.dp, max = 2.dp))
            }
        }
    }
}

@Composable
private fun StableLearningClock(seconds: Int) {
    val style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
    val hours = (seconds.coerceAtLeast(0) / 3600).toString().length.coerceAtLeast(2)
    Box(contentAlignment = Alignment.CenterEnd) {
        (0..9).forEach { value ->
            val digit = formatNumber(value)
            Text("\u2066${digit.repeat(hours)}:${digit.repeat(2)}:${digit.repeat(2)}\u2069",
                Modifier.alpha(0f).clearAndSetSemantics { }, style = style, maxLines = 1, softWrap = false)
        }
        Text(formatSessionDuration(seconds.toLong()), style = style, maxLines = 1, softWrap = false, textAlign = TextAlign.End)
    }
}

@Composable
fun LearningSessionNavigation(onPrevious: () -> Unit, onNext: () -> Unit, previousEnabled: Boolean,
    nextEnabled: Boolean, nextLabel: String, onQuestions: (() -> Unit)? = null) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppLayoutTokens.pagePadding), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            AppIconButton(onPrevious, enabled = previousEnabled) {
                Icon(if (rtl) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft, stringResource(R.string.previous))
            }
            onQuestions?.let { action -> AppIconButton(action) { Icon(Icons.Filled.List, stringResource(R.string.session_question_navigation)) } }
            AppCompactButton(onNext, Modifier.weight(1f), enabled = nextEnabled) { Text(nextLabel) }
        }
    }
}

enum class ReviewFilter {
    ALL, INCORRECT, UNANSWERED;
    fun includes(answered: Boolean, correct: Boolean): Boolean = when (this) {
        ALL -> true
        INCORRECT -> answered && !correct
        UNANSWERED -> !answered
    }
}

@Composable
fun ResultReviewFilters(selected: ReviewFilter, onSelect: (ReviewFilter) -> Unit, shown: Int, total: Int) {
    AppActionRow {
        ReviewFilter.values().forEach { filter ->
            AppFilterChip(selected == filter, { onSelect(filter) }, label = { Text(stringResource(when (filter) {
                ReviewFilter.ALL -> R.string.quiz_review_all
                ReviewFilter.INCORRECT -> R.string.quiz_review_incorrect
                ReviewFilter.UNANSWERED -> R.string.unanswered
            })) })
        }
    }
    Text(stringResource(R.string.quiz_review_count, formatNumber(shown), formatNumber(total)),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (shown == 0) EmptyState(stringResource(R.string.quiz_review_empty))
}

@Composable
fun LearningResultsSummary(correct: Int, total: Int, answered: Int, accuracy: Double? = null,
    details: @Composable ColumnScope.() -> Unit = {}) {
    AppCard(Modifier.fillMaxWidth()) {
        AppCardBody {
            val values = listOf(bidi("${formatNumber(correct)} / ${formatNumber(total)}"),
                bidi("${formatDecimal(accuracy ?: if (total > 0) correct * 100.0 / total else 0.0)}%"),
                formatNumber(answered), formatNumber((total - answered).coerceAtLeast(0)))
            val labels = listOf(stringResource(R.string.quiz_metric_score), stringResource(R.string.quiz_metric_accuracy),
                stringResource(R.string.quiz_metric_answered), stringResource(R.string.unanswered))
            AppTileGrid(values.size) { index, tileModifier -> AppMetricTile(values[index], labels[index], tileModifier) }
            details()
        }
    }
}

data class ActiveFilter(val key: String, val label: String, val onRemove: () -> Unit)

@Composable
fun ActiveFilterSummary(filters: List<ActiveFilter>, onClear: () -> Unit, enabled: Boolean = true) {
    if (filters.isEmpty()) return
    AppActionRow {
        filters.forEach { filter -> key(filter.key) {
            val remove = stringResource(R.string.quiz_remove_filter, filter.label)
            AppFilterChip(selected = true, onClick = filter.onRemove, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = remove },
                label = { Text(filter.label) }, trailingIcon = { Icon(Icons.Filled.Close, null) })
        } }
        AppTextButton(onClear, enabled = enabled) { Text(stringResource(R.string.ui_clear_all)) }
    }
}

enum class LearningSaveState { DEVICE, WAITING, SERVER, REVIEW, SYNCING }

@Composable
fun LearningSaveStatus(state: LearningSaveState, detail: String? = null, actions: @Composable (() -> Unit)? = null) {
    val label = stringResource(when (state) {
        LearningSaveState.DEVICE -> R.string.ui_saved_device
        LearningSaveState.WAITING -> R.string.ui_waiting_sync
        LearningSaveState.SERVER -> R.string.ui_saved_server
        LearningSaveState.REVIEW -> R.string.ui_needs_review
        LearningSaveState.SYNCING -> R.string.quiz_syncing
    })
    AppSaveStatus(label + (detail?.takeIf { it.isNotBlank() && it != label }?.let { " · $it" } ?: ""),
        failed = state == LearningSaveState.REVIEW, action = actions)
}

/** No answers/keys: navigation position, completion and local revisit flags only. */
@Composable
fun LearningQuestionMap(current: Int, total: Int, answered: Set<Int>, flagged: Set<Int>, enabled: Boolean,
    onChoose: (Int) -> Unit) {
    Column(Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text(stringResource(R.string.flow_final_counts, formatNumber(answered.size),
            formatNumber((total - answered.size).coerceAtLeast(0)), formatNumber(flagged.size)))
        Text(stringResource(R.string.flow_map_hint), style = MaterialTheme.typography.bodySmall)
        LearningQuestionChips(current, total, answered, flagged, enabled, onChoose)
    }
}

/** Reused by ordinary and master maps without imposing flag controls or write behavior. */
@Composable
fun LearningQuestionChips(current: Int, total: Int, answered: Set<Int>, flagged: Set<Int>, enabled: Boolean,
    onChoose: (Int) -> Unit) {
    AppActionRow {
        (0 until total).forEach { index ->
            val questionLabel = stringResource(R.string.select_question_number, index + 1)
            val isAnswered = index in answered
            val isFlagged = index in flagged
            val status = stringResource(if (isAnswered) R.string.session_question_answered else R.string.session_question_unanswered) +
                if (isFlagged) " · " + stringResource(R.string.flow_flagged) else ""
            AppFilterChip(selected = index == current, onClick = { onChoose(index) }, enabled = enabled,
                modifier = Modifier.heightIn(min = AppLayoutTokens.touchTarget).semantics {
                    contentDescription = questionLabel
                    stateDescription = status
                },
                label = { Text(formatNumber(index + 1) + (if (isAnswered) " ✓" else "") + (if (isFlagged) " ⚑" else "")) })
        }
    }
}
