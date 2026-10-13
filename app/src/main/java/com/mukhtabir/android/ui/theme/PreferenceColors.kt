package com.mukhtabir.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.ColorScheme
import com.mukhtabir.android.core.ThemePreference

internal fun themedColors(base: ColorScheme, theme: ThemePreference): ColorScheme {
    val scheme = when (theme) {
    ThemePreference.IRIS -> base.copy(primary = Color(0xFF503194), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFCFC0ED), onBackground = Color(0xFF231438), surface = Color(0xFFE7DCF5), onSurface = Color(0xFF231438), surfaceVariant = Color(0xFFDBC9EE), onSurfaceVariant = Color(0xFF44305D), outline = Color(0xFF594070), error = Color(0xFF96232D))
    ThemePreference.BLOSSOM -> base.copy(primary = Color(0xFF972460), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFF0BDCD), onBackground = Color(0xFF351322), surface = Color(0xFFF8DBE5), onSurface = Color(0xFF351322), surfaceVariant = Color(0xFFEFCBD8), onSurfaceVariant = Color(0xFF5C2B41), outline = Color(0xFF733C54), error = Color(0xFFA21430))
    ThemePreference.LAGOON -> base.copy(primary = Color(0xFF005D58), onPrimary = Color(0xFFFFFFFF), background = Color(0xFF98D4CE), onBackground = Color(0xFF092F2B), surface = Color(0xFFD1ECE5), onSurface = Color(0xFF092F2B), surfaceVariant = Color(0xFFB8E0D6), onSurfaceVariant = Color(0xFF214F46), outline = Color(0xFF305A51), error = Color(0xFF9E252F))
    ThemePreference.SLATE -> base.copy(primary = Color(0xFF354E6D), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFB2C5DB), onBackground = Color(0xFF18283B), surface = Color(0xFFD9E3EE), onSurface = Color(0xFF18283B), surfaceVariant = Color(0xFFC6D4E4), onSurfaceVariant = Color(0xFF35495F), outline = Color(0xFF3F5266), error = Color(0xFF952731))
    ThemePreference.INK -> base.copy(primary = Color(0xFF292C27), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFC4C4BC), onBackground = Color(0xFF181C17), surface = Color(0xFFE1E2D9), onSurface = Color(0xFF181C17), surfaceVariant = Color(0xFFD3D5CA), onSurfaceVariant = Color(0xFF363D31), outline = Color(0xFF485142), error = Color(0xFF962121))
    ThemePreference.AMBER -> base.copy(primary = Color(0xFF6B4300), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFE8C77D), onBackground = Color(0xFF30200A), surface = Color(0xFFF5E4B6), onSurface = Color(0xFF30200A), surfaceVariant = Color(0xFFEED497), onSurfaceVariant = Color(0xFF51390F), outline = Color(0xFF66491D), error = Color(0xFF9D204D))
    ThemePreference.RUBY -> base.copy(primary = Color(0xFF750A38), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFD5A0B2), onBackground = Color(0xFF330E21), surface = Color(0xFFEFCBD8), onSurface = Color(0xFF330E21), surfaceVariant = Color(0xFFE2B5C8), onSurfaceVariant = Color(0xFF56263E), outline = Color(0xFF64324C), error = Color(0xFF7A193C))
    ThemePreference.MIDNIGHT -> base.copy(primary = Color(0xFFa6a1fd), onPrimary = Color(0xFF171536), background = Color(0xFF0a0f1e), onBackground = Color(0xFFe3e8f7), surface = Color(0xFF131a2d), onSurface = Color(0xFFe3e8f7), surfaceVariant = Color(0xFF1c2540), onSurfaceVariant = Color(0xFFbcc5de), outline = Color(0xFF9ba6c4), error = Color(0xFFef7e7c))
    ThemePreference.ONYX -> base.copy(primary = Color(0xFF68D8CB), onPrimary = Color(0xFF072921), background = Color(0xFF031412), onBackground = Color(0xFFD7F0EA), surface = Color(0xFF0B2825), onSurface = Color(0xFFD7F0EA), surfaceVariant = Color(0xFF143E39), onSurfaceVariant = Color(0xFFACCDC5), outline = Color(0xFF8BB0A8), error = Color(0xFFFFA0AB))
    ThemePreference.CONTRAST -> base.copy(primary = Color(0xFF003891), onPrimary = Color(0xFFFFFFFF), background = Color(0xFFC4D7F7), onBackground = Color(0xFF080F1A), surface = Color(0xFFE5EDFC), onSurface = Color(0xFF080F1A), surfaceVariant = Color(0xFFD5E2F8), onSurfaceVariant = Color(0xFF17263B), outline = Color(0xFF263C58), error = Color(0xFF860019))
    else -> suggestedColors(theme) ?: base
}

    return if (theme in setOf(ThemePreference.IRIS, ThemePreference.BLOSSOM, ThemePreference.LAGOON,
            ThemePreference.SLATE, ThemePreference.INK, ThemePreference.AMBER, ThemePreference.RUBY,
            ThemePreference.MIDNIGHT, ThemePreference.ONYX, ThemePreference.CONTRAST)) {
        val dark = scheme.surface.luminance() < .2f
        val containerMix = if (dark) .72f else .88f
        val accent = blendOpaque(scheme.primary, scheme.onSurface, .2f)
        val tertiary = blendOpaque(scheme.primary, scheme.onSurface, .55f)
        scheme.copy(
            primaryContainer = blendOpaque(scheme.primary, scheme.surface, containerMix), onPrimaryContainer = scheme.onSurface,
            secondary = accent, onSecondary = scheme.onPrimary,
            secondaryContainer = blendOpaque(accent, scheme.surface, containerMix), onSecondaryContainer = scheme.onSurface,
            tertiary = tertiary, onTertiary = scheme.onPrimary,
            tertiaryContainer = blendOpaque(tertiary, scheme.surface, containerMix), onTertiaryContainer = scheme.onSurface,
            errorContainer = blendOpaque(scheme.error, scheme.surface, containerMix), onErrorContainer = scheme.onSurface,
            outlineVariant = blendOpaque(scheme.outline, scheme.surface, .7f), surfaceTint = scheme.primary,
            inverseSurface = scheme.onSurface, inverseOnSurface = scheme.surface, inversePrimary = scheme.surface,
        )
    } else scheme
}

private fun blendOpaque(start: Color, end: Color, amount: Float): Color = Color(
    red = start.red + (end.red - start.red) * amount,
    green = start.green + (end.green - start.green) * amount,
    blue = start.blue + (end.blue - start.blue) * amount,
    alpha = 1f,
)
