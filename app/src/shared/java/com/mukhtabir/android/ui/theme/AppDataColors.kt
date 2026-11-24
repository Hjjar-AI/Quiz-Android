package com.mukhtabir.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** Theme-derived roles for display of confirmed study data on card surfaces. */
internal data class AppDataColors(
    val primary: Color,
    val secondary: Color,
    val track: Color,
    val label: Color,
)

@Composable
internal fun appDataColors(): AppDataColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        val surfaces = listOf(scheme.surface)
        AppDataColors(
            primary = contrastInk(scheme.primary, surfaces, 3.0),
            secondary = contrastInk(scheme.tertiary, surfaces, 3.0),
            track = scheme.outlineVariant,
            label = scheme.onSurfaceVariant,
        )
    }
}
