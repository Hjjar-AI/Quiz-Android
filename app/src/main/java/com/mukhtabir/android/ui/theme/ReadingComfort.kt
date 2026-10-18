package com.mukhtabir.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mukhtabir.android.core.DisplayPreferences

val LocalReadingComfort = staticCompositionLocalOf { DisplayPreferences() }

/** Additional reading scale; Android's own font scale still applies once at rendering. */
@Composable
fun readingTextStyle(base: TextStyle): TextStyle {
    val preferences = LocalReadingComfort.current
    val scale = preferences.readingSize / 100f
    val size = base.fontSize.takeIf { it.isSp } ?: 16.sp
    val leading = base.lineHeight.takeIf { it.isSp } ?: (size * 1.6f)
    return base.copy(fontSize = size * scale,
        lineHeight = leading * scale * (preferences.readingLeading / 100f))
}

/** Width limits affect content on large displays; narrow displays use available space. */
@Composable
fun readingMaxWidth(): Dp = when (LocalReadingComfort.current.readingWidth) {
    "comfortable" -> 720.dp
    "focused" -> 560.dp
    else -> Dp.Infinity
}

val readingParagraphBaseMargin = 8.dp

/** Shared paragraph margin; density and Android font scaling are applied by the renderer. */
@Composable
fun readingParagraphMargin(compact: Boolean = false): Dp =
    (if (compact) AppTheme.spacing.tight else readingParagraphBaseMargin) * (LocalReadingComfort.current.readingParagraphs / 100f)
