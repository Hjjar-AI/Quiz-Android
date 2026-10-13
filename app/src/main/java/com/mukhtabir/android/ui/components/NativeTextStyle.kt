package com.mukhtabir.android.ui.components

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.core.widget.TextViewCompat

/** Reset every update so switching language/style never retains a previous multiplier. */
internal fun TextView.applyReadingStyle(style: TextStyle, density: Density) {
    includeFontPadding = false
    val size = style.fontSize.takeIf { it.isSp }?.value ?: 16f
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    setLineSpacing(0f, 1f)
    val height = if (style.lineHeight.isSp) with(density) { style.lineHeight.toPx() }
        else textSize * 1.6f
    // Compact labels never compress below the active font's natural metrics.
    val metrics = paint.fontMetricsInt
    val naturalHeight = metrics.descent - metrics.ascent
    TextViewCompat.setLineHeight(this, height.toInt().coerceAtLeast(naturalHeight).coerceAtLeast(1))
    textDirection = View.TEXT_DIRECTION_FIRST_STRONG
    textAlignment = View.TEXT_ALIGNMENT_TEXT_START
}

internal fun TextView.alignReadingText() {
    val content = text.toString().replace(Regex("^[A-Ha-h][.)]\\s+"), "")
    val direction = content.asSequence().firstNotNullOfOrNull { character ->
        when (Character.getDirectionality(character)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> View.TEXT_DIRECTION_FIRST_STRONG_LTR
            Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> View.TEXT_DIRECTION_FIRST_STRONG_RTL
            else -> null
        }
    }
    // Resolve each paragraph from its own content; the first paragraph supplies
    // the fallback for neutral lines rather than forcing one direction on all text.
    textDirection = direction ?: View.TEXT_DIRECTION_FIRST_STRONG
}
