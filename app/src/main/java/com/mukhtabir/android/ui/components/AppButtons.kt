package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.ui.theme.AppShapes

/** Visual minimums only: every size retains a 48dp target and grows for wrapping text. */
enum class AppButtonSize(val visualHeight: Dp) { SMALL(32.dp), MEDIUM(36.dp), LARGE(48.dp) }
enum class AppButtonStyle { FILLED, SECONDARY, OUTLINED, TEXT }
enum class AppButtonTone { DEFAULT, DANGER }

/** Explicit sizes share geometry across styles; legacy entry points retain their defaults. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppButton(onClick: () -> Unit, size: AppButtonSize, modifier: Modifier = Modifier,
    enabled: Boolean = true, style: AppButtonStyle = AppButtonStyle.FILLED,
    tone: AppButtonTone = AppButtonTone.DEFAULT, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val filled = style == AppButtonStyle.FILLED || style == AppButtonStyle.SECONDARY
    val accent = when {
        tone == AppButtonTone.DANGER -> scheme.error
        style == AppButtonStyle.SECONDARY -> scheme.secondary
        filled -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
    val container = when {
        !filled -> Color.Transparent
        !enabled -> scheme.onSurface.copy(alpha = AppControlTokens.disabledContainerAlpha)
        else -> accent
    }
    val foreground = when {
        !enabled -> scheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha)
        !filled -> accent
        tone == AppButtonTone.DANGER -> scheme.onError
        style == AppButtonStyle.SECONDARY -> scheme.onSecondary
        else -> scheme.onPrimary
    }
    val border = if (style == AppButtonStyle.OUTLINED) BorderStroke(AppLayoutTokens.cardBorderWidth,
        when {
            !enabled -> scheme.onSurface.copy(alpha = AppControlTokens.disabledBorderAlpha)
            tone == AppButtonTone.DANGER -> scheme.error
            else -> scheme.outlineVariant
        }) else null
    val inset = (AppLayoutTokens.touchTarget - size.visualHeight) / 2
    val padding = if (size == AppButtonSize.LARGE) AppLayoutTokens.primaryContentPadding
        else AppLayoutTokens.secondaryContentPadding
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = AppLayoutTokens.touchTarget)
            .widthIn(min = AppLayoutTokens.touchTarget).semantics { role = Role.Button },
        shape = AppShapes.control, color = Color.Transparent, contentColor = foreground) {
        Box(Modifier.padding(vertical = inset), propagateMinConstraints = true) {
            Surface(shape = AppShapes.control, color = container, contentColor = foreground, border = border) {
                Row(Modifier.heightIn(min = size.visualHeight).padding(padding),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    ProvideTextStyle(if (size == AppButtonSize.SMALL) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.labelLarge) { content() }
                }
            }
        }
    }
}

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.LARGE, modifier, enabled, content = content)

@Composable
fun AppSecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.LARGE, modifier, enabled, AppButtonStyle.SECONDARY, content = content)

@Composable
fun AppCompactButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.MEDIUM, modifier, enabled, content = content)

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.MEDIUM, modifier, enabled, AppButtonStyle.OUTLINED, content = content)

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.MEDIUM, modifier, enabled, AppButtonStyle.TEXT, content = content)

/** Destructive labels stay explicit; color is additional feedback. */
@Composable
fun AppDangerTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.MEDIUM, modifier, enabled, AppButtonStyle.TEXT, AppButtonTone.DANGER, content)

@Composable
fun AppDangerOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) =
    AppButton(onClick, AppButtonSize.MEDIUM, modifier, enabled, AppButtonStyle.OUTLINED, AppButtonTone.DANGER, content)
