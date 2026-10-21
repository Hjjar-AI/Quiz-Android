package com.mukhtabir.android.ui.components

import com.mukhtabir.android.ui.theme.AppTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.Color
import com.mukhtabir.android.R

@Composable
private fun appCardBorder() = BorderStroke(AppLayoutTokens.cardBorderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppControlTokens.cardBorderAlpha))

/** Consistent actions: readable wrapping labels and a minimum 48dp touch target. */
@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    AppFilledButton(onClick, modifier, enabled, MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.onPrimary, content)
}

/** Filled alternative action that remains distinct from the page and card surfaces. */
@Composable
fun AppSecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    AppFilledButton(onClick, modifier, enabled, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onSecondary, content)
}

@Composable
private fun AppFilledButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    containerColor: Color, contentColor: Color, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = AppLayoutTokens.touchTarget), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = AppLayoutTokens.primaryContentPadding,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppControlTokens.disabledContainerAlpha),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha)), content = content)
}

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    AppSecondaryOutlinedButton(onClick, modifier, enabled, MaterialTheme.colorScheme.onSurfaceVariant, content)
}

/** Short primary frame with the same full-height clickable surface as secondary actions. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppCompactButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val foreground = if (enabled) scheme.onPrimary else scheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha)
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = AppLayoutTokens.touchTarget).semantics { role = Role.Button },
        shape = MaterialTheme.shapes.medium, color = Color.Transparent, contentColor = foreground) {
        Box(Modifier.padding(vertical = AppLayoutTokens.secondaryFrameInset), propagateMinConstraints = true) {
            Surface(shape = MaterialTheme.shapes.medium,
                color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = AppControlTokens.disabledContainerAlpha), contentColor = foreground) {
                Row(Modifier.heightIn(min = AppLayoutTokens.secondaryVisualHeight).padding(AppLayoutTokens.secondaryContentPadding),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
                }
            }
        }
    }
}

/** Compact visible frame inside a full clickable/keyboard-accessible Material surface. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AppSecondaryOutlinedButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    contentColor: Color, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val textColor = if (enabled) contentColor else scheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha)
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = AppLayoutTokens.touchTarget).widthIn(min = AppLayoutTokens.touchTarget)
            .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.medium, color = Color.Transparent, contentColor = textColor) {
        Box(Modifier.padding(vertical = AppLayoutTokens.secondaryFrameInset), propagateMinConstraints = true) {
            Surface(shape = MaterialTheme.shapes.medium, color = Color.Transparent, contentColor = textColor,
                border = BorderStroke(AppLayoutTokens.cardBorderWidth, if (enabled) scheme.outlineVariant else scheme.onSurface.copy(alpha = AppControlTokens.disabledBorderAlpha))) {
                Row(Modifier.heightIn(min = AppLayoutTokens.secondaryVisualHeight).padding(AppLayoutTokens.secondaryContentPadding),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
                }
            }
        }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardBorder(), content = content)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppCard(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardBorder(), content = content)
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
        } else if (headingMaxWidth < AppLayoutTokens.headingBreakpoint || fontScale >= 1.3f) {
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
    val motionEnabled = (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f
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
        if (maxWidth < AppLayoutTokens.headingBreakpoint || scale >= 1.3f) {
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
