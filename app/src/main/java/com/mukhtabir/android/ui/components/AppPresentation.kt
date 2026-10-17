package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

/** Caller supplies reveal-safe feedback; this renderer never derives correctness. */
@Composable
fun AnswerChoice(number: Int, text: String, modifier: Modifier = Modifier,
    selected: Boolean = false, enabled: Boolean = true, onClick: (() -> Unit)? = null,
    answerCorrect: Boolean? = null, neutralSelection: Boolean = false,
    markdown: Boolean = true, accessibilityLabel: String? = null, interactiveText: Boolean = false) {
    QuestionChoiceCard(modifier.semantics {
        this.selected = selected
        accessibilityLabel?.let { contentDescription = it }
    },
        selected, enabled, onClick, answerCorrect, neutralSelection) {
        Text("\u2066${formatNumber(number)}.\u2069",
            Modifier.width(AppLayoutTokens.choiceNumberWidth * LocalDensity.current.fontScale),
            style = MaterialTheme.typography.bodyLarge)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                if (markdown) MarkdownText(text, interactive = interactiveText)
                else Text(text, style = com.mukhtabir.android.ui.theme.readingTextStyle(MaterialTheme.typography.bodyLarge))
            }
            if (answerCorrect == true) Text(stringResource(R.string.correct_answer),
                style = MaterialTheme.typography.labelMedium,
                modifier = if (selected) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)
            if (selected && answerCorrect != null) Text(stringResource(
                if (answerCorrect) R.string.quiz_your_choice else R.string.quiz_your_wrong_choice),
                style = MaterialTheme.typography.labelMedium,
                modifier = if (!answerCorrect) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)
        }
    }
}

/** Shared compact status; state, wording and allowed actions belong to the screen. */
@Composable
fun AppSaveStatus(message: String, modifier: Modifier = Modifier, failed: Boolean = false,
    action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun AppRecoveryPanel(message: String? = null, throwable: Throwable? = null,
    modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null, retryEnabled: Boolean = true,
    actions: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium) }
        throwable?.let { ErrorState(throwable = it) }
        if (onRetry != null) AppOutlinedButton(onRetry, enabled = retryEnabled) { Text(stringResource(R.string.retry)) }
        actions?.invoke()
    }
}

/** Material owns dialog action order; callers retain guarded/destructive action controls. */
@Composable
fun AppConfirmationDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier, dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null) {
    val heading: (@Composable () -> Unit)? = title?.let { content ->
        { ProvideTextStyle(MaterialTheme.typography.titleMedium) { content() } }
    }
    val body: (@Composable () -> Unit)? = text?.let { content ->
        { Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
        } }
    }
    AlertDialog(onDismissRequest, confirmButton, modifier = modifier, dismissButton = dismissButton,
        title = heading, text = body, icon = icon, shape = MaterialTheme.shapes.medium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary)) {
    val leading: (@Composable () -> Unit)? = leadingIcon?.let { content ->
        { Box(Modifier.size(AppLayoutTokens.secondaryIconSize)) { content() } }
    }
    val trailing: (@Composable () -> Unit)? = trailingIcon?.let { content ->
        { Box(Modifier.size(AppLayoutTokens.secondaryIconSize)) { content() } }
    }
    // Material supplies the minimum interactive target around this shorter visual frame.
    FilterChip(selected, onClick, label, modifier.heightIn(min = AppLayoutTokens.secondaryVisualHeight),
        enabled = enabled,
        leadingIcon = leading, trailingIcon = trailing,
        shape = shape, colors = colors)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AppMetadataRow(values: List<String>, modifier: Modifier = Modifier) {
    if (values.none { it.isNotBlank() }) return
    AppActionRow(modifier) {
        values.filter { it.isNotBlank() }.forEach {
            Text(com.mukhtabir.android.util.bidi(it), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Label/value rows grow vertically at narrow widths or enlarged text. */
@Composable
fun AppMetricRow(label: String, value: String, modifier: Modifier = Modifier) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < AppLayoutTokens.sessionHeaderBreakpoint || scale >= 1.5f) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(com.mukhtabir.android.util.bidi(value), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(com.mukhtabir.android.util.bidi(value), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Present confirmed counts only; the caller owns loading/error and report authority. */
@Composable
fun AppProgressMetric(label: String, value: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        AppMetricRow(label, "${formatNumber(value)} / ${formatNumber(total)}")
        LinearProgressIndicator(progress = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun AppSectionHeading(title: String, modifier: Modifier = Modifier, subtitle: String? = null,
    count: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            count?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        action?.invoke()
    }
}

@Composable
fun AppPendingSessionCard(title: String, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier.fillMaxWidth()) {
        AppCardBody {
            AppSectionHeading(title)
            content()
        }
    }
}

/** One width policy prevents each screen inventing incompatible breakpoints. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTileGrid(count: Int, modifier: Modifier = Modifier,
    content: @Composable (Int, Modifier) -> Unit) {
    if (count <= 0) return
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gap = AppLayoutTokens.controlGap
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (AppLayoutTokens.tileMinWidth * scale + gap)).toInt()
            .coerceIn(1, minOf(count, AppLayoutTokens.tileMaxColumns))
        val tileWidth = ((maxWidth - gap * (columns - 1)) / columns - AppLayoutTokens.tileWidthAllowance).coerceAtLeast(0.dp)
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(count) { content(it, Modifier.width(tileWidth)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMetricTile(value: String, label: String, modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null, accent: Color = MaterialTheme.colorScheme.primary,
    container: Color = AppTheme.questionSurfaces.choice,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge) {
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(AppLayoutTokens.denseCardInsets),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            Text(value, style = valueStyle, color = accent, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
    val border = BorderStroke(AppLayoutTokens.cardBorderWidth, AppTheme.questionSurfaces.border)
    if (onClick == null) Surface(modifier, shape = MaterialTheme.shapes.medium, color = container,
        border = border, content = body)
    else Surface(onClick, modifier.heightIn(min = AppLayoutTokens.touchTarget),
        shape = MaterialTheme.shapes.medium, color = container, border = border, content = body)
}
