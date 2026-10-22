package com.mukhtabir.android.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.theme.AppShapes
import com.mukhtabir.android.ui.theme.AppSurfaces
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
private fun appCardBorder() = BorderStroke(AppLayoutTokens.cardBorderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppControlTokens.cardBorderAlpha))

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = AppShapes.container,
        colors = CardDefaults.cardColors(containerColor = AppSurfaces.container),
        border = appCardBorder(), content = content)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppCard(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = AppShapes.container,
        colors = CardDefaults.cardColors(containerColor = AppSurfaces.container),
        border = BorderStroke(AppLayoutTokens.cardBorderWidth, MaterialTheme.colorScheme.outline), content = content)
}

@Composable
private fun PageHeadingText(title: String, subtitle: String?, modifier: Modifier = Modifier, titleAnimationDurationMillis: Int = 0) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        if (titleAnimationDurationMillis > 0) {
            AppWritingText(title, MaterialTheme.typography.headlineSmall, titleAnimationDurationMillis)
        } else {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AppPageHeading(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null, modifier: Modifier = Modifier, keepActionInline: Boolean = false, titleAnimationDurationMillis: Int = 0) {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val headingMaxWidth = maxWidth
        if (keepActionInline && action != null) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), verticalAlignment = Alignment.CenterVertically) {
                    PageHeadingText(title, null, Modifier.weight(1f), titleAnimationDurationMillis)
                    action()
                }
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else if (AppAdaptiveTokens.stack(headingMaxWidth, AppLayoutTokens.headingBreakpoint, fontScale)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                PageHeadingText(title, subtitle, titleAnimationDurationMillis = titleAnimationDurationMillis)
                action?.invoke()
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), verticalAlignment = Alignment.CenterVertically) {
                PageHeadingText(title, subtitle, Modifier.weight(1f), titleAnimationDurationMillis)
                if (action != null) androidx.compose.foundation.layout.Box(Modifier.widthIn(max = headingMaxWidth / 2)) { action() }
            }
        }
    }
}

/** Collapsing presentation never clears the caller's fields or selection. */
@Composable
fun AppSection(title: String, modifier: Modifier = Modifier, initiallyExpanded: Boolean = false,
    subtitle: String? = null, revealError: Boolean = false, revealRequest: Int = 0, expandedOverride: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null, composeCollapsedContent: Boolean = true, enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    var savedExpanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val expanded = expandedOverride ?: savedExpanded
    androidx.compose.runtime.LaunchedEffect(revealError, revealRequest) { if (revealError) { savedExpanded = true; onExpandedChange?.invoke(true) } }
    val focusManager = LocalFocusManager.current
    val state = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
    val motionEnabled = rememberMotionEnabled()
    AppCard(modifier.fillMaxWidth().then(if (motionEnabled) Modifier.animateContentSize() else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = AppLayoutTokens.touchTarget).clickable(enabled = enabled, role = Role.Button) { if (expanded) focusManager.clearFocus(); savedExpanded = !expanded; onExpandedChange?.invoke(!expanded) }
            .semantics { stateDescription = state }.padding(AppLayoutTokens.cardInsets),
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), verticalAlignment = Alignment.CenterVertically) {
            AppSectionHeading(title,subtitle=subtitle,modifier=Modifier.weight(1f),action={
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            })
        }
        // Forms retain their composition/state by default. Read-only lazy sections can opt
        // out; their caller owns cached data and in-flight reads. Hidden content is
        // excluded from accessibility in either mode.
        if (expanded || composeCollapsedContent) Layout(
            modifier = Modifier.fillMaxWidth().then(if (expanded) Modifier else Modifier.clearAndSetSemantics { }),
            content = {
                Column(Modifier.fillMaxWidth().padding(start = AppLayoutTokens.cardPadding, end = AppLayoutTokens.cardPadding, bottom = AppLayoutTokens.cardVerticalPadding),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section), content = content)
            },
        ) { measurables, constraints ->
            val child = measurables.single().measure(constraints.copy(minHeight = 0))
            layout(child.width, if (expanded) child.height else 0) {
                if (expanded) child.placeRelative(0, 0)
            }
        }
    }
}

@Composable
fun AppCardBody(modifier: Modifier = Modifier, dense: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(if (dense) AppLayoutTokens.denseCardInsets else AppLayoutTokens.cardInsets),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), content = content)
}

@Composable
fun AppToggleRow(modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(AppLayoutTokens.controlGap),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = AppLayoutTokens.touchTarget), horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment, content = content)
}

@Composable
fun AppListRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = AppLayoutTokens.touchTarget)
        .padding(horizontal = AppLayoutTokens.cardPadding, vertical = AppLayoutTokens.denseCardVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
        verticalAlignment = Alignment.CenterVertically, content = content)
}

/** One semantic and touch target for a label and its passive Material control. */
@Composable
fun AppSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    AppToggleRow(modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch,
        onValueChange = onCheckedChange)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun AppCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    AppToggleRow(modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox,
        onValueChange = onCheckedChange)) {
        androidx.compose.material3.Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Keep the field usable when the action label grows with language or text scaling. */
@Composable
fun AppSearchRow(field: @Composable (Modifier) -> Unit, action: @Composable () -> Unit) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (AppAdaptiveTokens.stack(maxWidth, AppLayoutTokens.headingBreakpoint, scale)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                field(Modifier.fillMaxWidth())
                action()
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
                verticalAlignment = Alignment.CenterVertically) {
                field(Modifier.weight(1f))
                action()
            }
        }
    }
}

/** One labelled single-choice target; callers provide a selectableGroup around related rows. */
@Composable
fun AppRadioRow(label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null) {
    AppToggleRow(modifier.selectable(selected = selected, enabled = enabled,
        role = Role.RadioButton, onClick = onClick).padding(AppLayoutTokens.pagePadding)) {
        androidx.compose.material3.RadioButton(selected, onClick = null, enabled = enabled)
        leadingIcon?.invoke()
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}
