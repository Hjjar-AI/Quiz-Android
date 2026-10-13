package com.mukhtabir.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.mukhtabir.android.core.ThemePreference

// Adapted from Android/suggested themes.txt. Semantic colors are opaque so text
// stays readable in dialogs, nested cards and scrolling question content.
internal fun suggestedColors(theme: ThemePreference): ColorScheme? = when (theme) {
    ThemePreference.FOOD_DELIVERY -> lightColorScheme(
        primary = Color(0xFF892D19), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF0CAB4), onPrimaryContainer = Color(0xFF4D1A0D),
        secondary = Color(0xFF806000), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFF0CC), onSecondaryContainer = Color(0xFF3A2A00),
        tertiary = Color(0xFF08763C), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD8F5E3), onTertiaryContainer = Color(0xFF073C20),
        background = Color(0xFFE4AB8B), onBackground = Color(0xFF351C12),
        surface = Color(0xFFF3D3BE), onSurface = Color(0xFF351C12),
        surfaceVariant = Color(0xFFE8BFA3), onSurfaceVariant = Color(0xFF5B3423),
        outline = Color(0xFF72452E), outlineVariant = Color(0xFFC58D6D),
        surfaceTint = Color(0xFF892D19),
        error = Color(0xFF8D2419), onError = Color.White,
        errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        inverseSurface = Color(0xFF322F35), inverseOnSurface = Color(0xFFF5EFF7), inversePrimary = Color(0xFFD0BCFF),
    )
    ThemePreference.FITNESS_TRACKER -> darkColorScheme(
        primary = Color(0xFFC6F542), onPrimary = Color(0xFF0B0F14),
        primaryContainer = Color(0xFF293719), onPrimaryContainer = Color(0xFFE4F8BA),
        secondary = Color(0xFF3DDBF0), onSecondary = Color(0xFF002F35),
        secondaryContainer = Color(0xFF143A42), onSecondaryContainer = Color(0xFFBCF2FA),
        tertiary = Color(0xFFFF8C42), onTertiary = Color(0xFF381800),
        tertiaryContainer = Color(0xFF503021), onTertiaryContainer = Color(0xFFFFDCC8),
        background = Color(0xFF0B0F14), onBackground = Color(0xFFEEF4FB),
        surface = Color(0xFF141A22), onSurface = Color(0xFFEEF4FB),
        surfaceVariant = Color(0xFF1A222C), onSurfaceVariant = Color(0xFFB0BDCD),
        outline = Color(0xFF8293A8), outlineVariant = Color(0xFF344252),
        surfaceTint = Color(0xFFC6F542),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFE6E1E5), inverseOnSurface = Color(0xFF211F26), inversePrimary = Color(0xFF6750A4),
    )
    ThemePreference.WEATHER -> darkColorScheme(
        primary = Color(0xFFDCE4FF), onPrimary = Color(0xFF1A2A6C),
        primaryContainer = Color(0xFF465AAB), onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFC7EDFF), onSecondary = Color(0xFF073145),
        secondaryContainer = Color(0xFF285678), onSecondaryContainer = Color(0xFFE0F4FF),
        tertiary = Color(0xFFFFE0A3), onTertiary = Color(0xFF3F2B00),
        tertiaryContainer = Color(0xFF61512E), onTertiaryContainer = Color(0xFFFFF0CF),
        background = Color(0xFF1A2A6C), onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF2F4084), onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF3D5099), onSurfaceVariant = Color(0xFFE0E6FF),
        outline = Color(0xFFBCC9F4), outlineVariant = Color(0xFF7183BE),
        surfaceTint = Color(0xFFDCE4FF),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFE6E1E5), inverseOnSurface = Color(0xFF211F26), inversePrimary = Color(0xFF6750A4),
    )
    ThemePreference.MATERIAL_YOU_LIGHT -> lightColorScheme(
        primary = Color(0xFF285E2F), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC2DDB4), onPrimaryContainer = Color(0xFF17331B),
        secondary = Color(0xFF54632D), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDDE2B8), onSecondaryContainer = Color(0xFF293211),
        tertiary = Color(0xFF7B4E20), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEAD3AF), onTertiaryContainer = Color(0xFF39230C),
        background = Color(0xFFBDDCAA), onBackground = Color(0xFF19331C),
        surface = Color(0xFFDFEED2), onSurface = Color(0xFF19331C),
        surfaceVariant = Color(0xFFCBDFC0), onSurfaceVariant = Color(0xFF344B30),
        outline = Color(0xFF486244), outlineVariant = Color(0xFFA0B990),
        surfaceTint = Color(0xFF285E2F),
        error = Color(0xFF8E202A), onError = Color.White,
        errorContainer = Color(0xFFEBC8C4), onErrorContainer = Color(0xFF3A0A10),
        inverseSurface = Color(0xFF243B25), inverseOnSurface = Color(0xFFE8F2DE), inversePrimary = Color(0xFFAED09A),
    )
    ThemePreference.MATERIAL_YOU_DARK -> darkColorScheme(
        primary = Color(0xFFEDA4D4), onPrimary = Color(0xFF42113B),
        primaryContainer = Color(0xFF713764), onPrimaryContainer = Color(0xFFF9D6EE),
        secondary = Color(0xFFBFC6ED), onSecondary = Color(0xFF282B51),
        secondaryContainer = Color(0xFF42466E), onSecondaryContainer = Color(0xFFE2E6FF),
        tertiary = Color(0xFFF2C28E), onTertiary = Color(0xFF442711),
        tertiaryContainer = Color(0xFF66432A), onTertiaryContainer = Color(0xFFFFDFC0),
        background = Color(0xFF301632), onBackground = Color(0xFFF8DEF1),
        surface = Color(0xFF452547), onSurface = Color(0xFFF8DEF1),
        surfaceVariant = Color(0xFF57365C), onSurfaceVariant = Color(0xFFE0BDDA),
        outline = Color(0xFFBF97B8), outlineVariant = Color(0xFF79557A),
        surfaceTint = Color(0xFFEDA4D4),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF7B2E36), onErrorContainer = Color(0xFFFFE1DB),
        inverseSurface = Color(0xFFF8DEF1), inverseOnSurface = Color(0xFF301632), inversePrimary = Color(0xFF713764),
    )
    else -> null
}
