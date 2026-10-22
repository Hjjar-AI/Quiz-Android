package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import com.mukhtabir.android.ui.theme.AppShapes
import com.mukhtabir.android.ui.theme.AppSurfaces
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

/** Caller supplies reveal-safe feedback; this renderer never derives correctness. */
@Composable
fun AnswerChoice(number: Int, text: String, modifier: Modifier = Modifier,
    selected: Boolean = false, enabled: Boolean = true, onClick: (() -> Unit)? = null,
    answerCorrect: Boolean? = null, neutralSelection: Boolean = false,
    markdown: Boolean = true, accessibilityLabel: String? = null, interactiveText: Boolean = false,
    reviewMode: Boolean = false) {
    val visual = ChoiceVisualState.from(selected, answerCorrect, neutralSelection, reviewMode)
    QuestionChoiceCard(modifier.semantics {
        this.selected = selected
        accessibilityLabel?.let { contentDescription = it }
    },
        visual, enabled, onClick) {
        Text("\u2066${formatNumber(number)}.\u2069",
            Modifier.width(AppPresentationGeometry.choiceNumberWidth * LocalDensity.current.fontScale),
            style = MaterialTheme.typography.bodyLarge)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                if (markdown) MarkdownText(text, interactive = interactiveText)
                else Text(text, style = com.mukhtabir.android.ui.theme.readingTextStyle(MaterialTheme.typography.bodyLarge))
            }
            val answerLabel = when (visual) {
                ChoiceVisualState.SELECTED_CORRECT -> listOf(
                    stringResource(R.string.quiz_your_choice),
                    stringResource(R.string.answer_status_correct),
                ).joinToString(" · ")
                ChoiceVisualState.CORRECT -> stringResource(R.string.answer_status_correct)
                ChoiceVisualState.SELECTED_INCORRECT -> stringResource(R.string.quiz_your_wrong_choice)
                ChoiceVisualState.REVIEWED_SELECTED -> stringResource(R.string.quiz_your_choice)
                else -> null
            }
            answerLabel?.let { label ->
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        if (selected) liveRegion = LiveRegionMode.Polite
                    },
                    horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (answerCorrect != null) Icon(if (answerCorrect) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null, modifier = Modifier.size(AppIconSize.inline))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Shared compact status; state, wording and allowed actions belong to the screen. */
@Composable
fun AppSaveStatus(message: String, modifier: Modifier = Modifier, failed: Boolean = false,
    action: (@Composable () -> Unit)? = null) {
    AppStatus(message, modifier, kind = if (failed) AppStatusKind.ERROR else AppStatusKind.INFO,
        style = MaterialTheme.typography.bodySmall, action = action)
}

@Composable
fun AppRecoveryPanel(message: String? = null, throwable: Throwable? = null,
    modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null, retryEnabled: Boolean = true,
    actions: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        message?.let { AppStatus(it, kind = AppStatusKind.WARNING) }
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
        { androidx.compose.foundation.layout.Box(Modifier.semantics(mergeDescendants = true) { heading() }) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) { content() }
        } }
    }
    val body: (@Composable () -> Unit)? = text?.let { content ->
        { Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
        } }
    }
    AlertDialog(onDismissRequest, confirmButton, modifier = modifier, dismissButton = dismissButton,
        title = heading, text = body, icon = icon, shape = AppShapes.dialog)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = AppShapes.chip,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha),
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

/** Read-only metadata with no click handler or selected/toggle semantics. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppMetadataChips(values: List<String>, modifier: Modifier = Modifier) {
    AppActionRow(modifier) {
        values.filter(String::isNotBlank).forEach { value ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                Text(com.mukhtabir.android.util.bidi(value), Modifier.padding(AppLayoutTokens.denseCardInsets),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Label/value rows grow vertically at narrow widths or enlarged text. */
@Composable
fun AppMetricRow(label: String, value: String, modifier: Modifier = Modifier) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (AppAdaptiveTokens.stack(maxWidth, AppLayoutTokens.sessionHeaderBreakpoint, scale, AppAdaptiveTokens.extraLargeText)) {
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
        AppProgressBar(progress = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f,
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

/** One width policy prevents each screen inventing incompatible breakpoints. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTileGrid(count: Int, modifier: Modifier = Modifier,
    content: @Composable (Int, Modifier) -> Unit) {
    if (count <= 0) return
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gap = AppLayoutTokens.controlGap
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (AppPresentationGeometry.tileMinWidth * scale + gap)).toInt()
            .coerceIn(1, minOf(count, AppPresentationGeometry.tileMaxColumns))
        val tileWidth = ((maxWidth - gap * (columns - 1)) / columns - AppPresentationGeometry.tileWidthAllowance).coerceAtLeast(0.dp)
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
    container: Color = AppSurfaces.choice,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(AppLayoutTokens.denseCardInsets),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            icon?.let { Icon(it, contentDescription = null, tint = accent) }
            Text(value, style = valueStyle, color = accent, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (onClick != null) Icon(Icons.Filled.ChevronRight, contentDescription = null,
                modifier = Modifier.size(AppLayoutTokens.secondaryIconSize).directionalIcon(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val border = BorderStroke(AppLayoutTokens.cardBorderWidth, AppTheme.questionSurfaces.border)
    if (onClick == null) Surface(modifier, shape = AppShapes.container, color = container,
        border = border, content = body)
    else Surface(onClick, modifier.heightIn(min = AppLayoutTokens.touchTarget),
        shape = AppShapes.container, color = container, border = border, content = body)
}

private object AppPresentationGeometry {
    val choiceNumberWidth = 24.dp
    const val tileMaxColumns = 3
    val tileMinWidth = 112.dp
    val tileWidthAllowance = .5.dp
}
