package com.mukhtabir.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/** Native derived surfaces can need stronger ink than the shared palette seeds. */
internal fun contrastInk(seed: Color, surfaces: List<Color>, minimum: Double): Color {
    val backgrounds = surfaces.map { it.toArgb() }
    fun passes(argb: Int) = backgrounds.all { ColorUtils.calculateContrast(argb, it) >= minimum }
    val original = seed.copy(alpha = 1f).toArgb()
    if (passes(original)) return seed.copy(alpha = 1f)
    val target = listOf(android.graphics.Color.BLACK, android.graphics.Color.WHITE)
        .maxByOrNull { ink -> backgrounds.minOf { ColorUtils.calculateContrast(ink, it) } }!!
    for (step in 1..100) {
        val candidate = ColorUtils.blendARGB(original, target, step / 100f)
        if (passes(candidate)) return Color(candidate)
    }
    return Color(target)
}
