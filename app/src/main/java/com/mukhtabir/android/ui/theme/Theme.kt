package com.mukhtabir.android.ui.theme

import androidx.core.os.ConfigurationCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalNativeTypeface = staticCompositionLocalOf<android.graphics.Typeface> { android.graphics.Typeface.DEFAULT }

val LocalAppBackground = staticCompositionLocalOf<androidx.compose.ui.graphics.Brush?> { null }

// Native equivalents of the web Stone and Dark palettes.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6F4A32), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDACC), onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF815500), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDEA3), onSecondaryContainer = Color(0xFF291800),
    tertiary = Color(0xFF176B87), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0EFF9), onTertiaryContainer = Color(0xFF00202C),
    background = Color(0xFFD8CEBC), onBackground = Color(0xFF2D241B),
    surface = Color(0xFFEEE5D5), onSurface = Color(0xFF2D241B),
    surfaceVariant = Color(0xFFE3D7C1), onSurfaceVariant = Color(0xFF4C3D30),
    outline = Color(0xFF62503E), outlineVariant = Color(0xFFBBC6CF),
    error = Color(0xFFAB2117), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF312E29), inverseOnSurface = Color(0xFFF7F0E7), inversePrimary = Color(0xFFFFB595),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6A15B), onPrimary = Color(0xFF26180C),
    primaryContainer = Color(0xFF653B16), onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFE6CF45), onSecondary = Color(0xFF251A08),
    secondaryContainer = Color(0xFF514617), onSecondaryContainer = Color(0xFFFFEB98),
    tertiary = Color(0xFF67B2D5), onTertiary = Color(0xFF10202A),
    tertiaryContainer = Color(0xFF194D63), onTertiaryContainer = Color(0xFFCEEFFF),
    background = Color(0xFF0D1218), onBackground = Color(0xFFE2DDD5),
    surface = Color(0xFF18212A), onSurface = Color(0xFFE2DDD5),
    surfaceVariant = Color(0xFF222D38), onSurfaceVariant = Color(0xFFC9C1B6),
    outline = Color(0xFFA9A196), outlineVariant = Color(0xFF4A535D),
    error = Color(0xFFF48998), onError = Color(0xFF2A1512),
    errorContainer = Color(0xFF64313A), onErrorContainer = Color(0xFFFFD9DF),
    inverseSurface = Color(0xFFE2DDD5), inverseOnSurface = Color(0xFF18212A), inversePrimary = Color(0xFF832500),
)

/** Theme previews use the same palette resolver as the installed app theme. */
internal fun previewThemeColors(preference: com.mukhtabir.android.core.ThemePreference, systemDark: Boolean): androidx.compose.material3.ColorScheme {
    val dark = if(preference == com.mukhtabir.android.core.ThemePreference.SYSTEM) systemDark
        else preference in com.mukhtabir.android.core.AppPreferences.DARK_THEMES
    return themedColors(if(dark) DarkColors else LightColors,preference)
}

data class FeedbackColors(val success: Color, val warning: Color, val info: Color)
data class QuestionSurfaces(val question: Color, val choice: Color, val border: Color)
private val LocalFeedback = staticCompositionLocalOf {
    FeedbackColors(Color(0xFF064126), Color(0xFF533800), Color(0xFF123C56))
}
data class AppSpacing(
    val section: androidx.compose.ui.unit.Dp = 4.dp,
    val card: androidx.compose.ui.unit.Dp = 6.dp,
    val denseCard: androidx.compose.ui.unit.Dp = 4.dp,
    val content: androidx.compose.ui.unit.Dp = 3.dp,
    val tight: androidx.compose.ui.unit.Dp = 3.dp,
    val libraryCard: androidx.compose.ui.unit.Dp = 4.dp,
    val cardVertical: androidx.compose.ui.unit.Dp = 3.dp,
    val denseCardVertical: androidx.compose.ui.unit.Dp = 2.dp,
)
private val LocalSpacing = staticCompositionLocalOf { AppSpacing() }

object AppTheme {
    val spacing: AppSpacing @Composable get() = LocalSpacing.current
    val feedback: FeedbackColors @Composable get() = LocalFeedback.current
    val questionSurfaces: QuestionSurfaces @Composable get() {
        val colors = MaterialTheme.colorScheme
        // Related opaque shades of the current background, in either mode.
        // Avoid default Material container colors leaking across named themes.
        return QuestionSurfaces(
            question = lerp(colors.background, colors.onBackground, .11f),
            choice = lerp(colors.background, colors.onBackground, .055f),
            border = lerp(colors.background, colors.outline, .45f),
        )
    }
}

private fun TextStyle.readable(arabic: Boolean, minimumRatio: Float = 1.6f): TextStyle = copy(
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeight = if (arabic) maxOf(lineHeight.value, fontSize.value * minimumRatio).sp else lineHeight,
)

private fun TextStyle.readableHeading(arabic: Boolean): TextStyle = copy(
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeight = if (arabic) maxOf(lineHeight.value, fontSize.value * 1.3f).sp else lineHeight,
)

@Composable
fun MukhtabirTheme(darkTheme: Boolean = isSystemInDarkTheme(), preference: com.mukhtabir.android.core.ThemePreference = com.mukhtabir.android.core.ThemePreference.SYSTEM,
    display: com.mukhtabir.android.core.DisplayPreferences = com.mukhtabir.android.core.DisplayPreferences(), content: @Composable () -> Unit) {
    val arabic = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language == "ar"
    val resolvedFont = if (display.font == "default") "amiri" else display.font
    val fontRes = when (resolvedFont) {
        "noto-arabic" -> com.mukhtabir.android.R.font.noto_arabic
        "inter" -> com.mukhtabir.android.R.font.inter
        "outfit" -> com.mukhtabir.android.R.font.outfit
        "tajawal" -> com.mukhtabir.android.R.font.tajawal
        "cairo" -> com.mukhtabir.android.R.font.cairo
        "ibm-plex-arabic" -> com.mukhtabir.android.R.font.ibm_plex_arabic
        "amiri" -> com.mukhtabir.android.R.font.amiri
        else -> 0
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val nativeTypeface = androidx.compose.runtime.remember(fontRes, context) { if (fontRes == 0) android.graphics.Typeface.DEFAULT else androidx.core.content.res.ResourcesCompat.getFont(context, fontRes) ?: android.graphics.Typeface.DEFAULT }
    val family = when (resolvedFont) {
        "noto-arabic" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.noto_arabic))
        "inter" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.inter))
        "outfit" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.outfit))
        "tajawal" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.tajawal))
        "cairo" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.cairo))
        "ibm-plex-arabic" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.ibm_plex_arabic))
        "amiri" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.mukhtabir.android.R.font.amiri))
        else -> androidx.compose.ui.text.font.FontFamily.Default
    }
    val original = Typography()
    val base = Typography(
        displayLarge = original.displayLarge.copy(fontFamily = family), displayMedium = original.displayMedium.copy(fontFamily = family), displaySmall = original.displaySmall.copy(fontFamily = family),
        headlineLarge = original.headlineLarge.copy(fontFamily = family), headlineMedium = original.headlineMedium.copy(fontFamily = family), headlineSmall = original.headlineSmall.copy(fontFamily = family, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleLarge = original.titleLarge.copy(fontFamily = family, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp), titleMedium = original.titleMedium.copy(fontFamily = family, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp), titleSmall = original.titleSmall.copy(fontFamily = family, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        bodyLarge = original.bodyLarge.copy(fontFamily = family, fontSize = 16.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp), bodyMedium = original.bodyMedium.copy(fontFamily = family, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp), bodySmall = original.bodySmall.copy(fontFamily = family, fontSize = 12.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
        labelLarge = original.labelLarge.copy(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp), labelMedium = original.labelMedium.copy(fontFamily = family, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp), labelSmall = original.labelSmall.copy(fontFamily = family, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp))
    val labelMinimumRatio = androidx.compose.runtime.remember(nativeTypeface) {
        val paint = android.graphics.Paint().apply { typeface = nativeTypeface; textSize = 100f }
        val metrics = paint.fontMetrics
        maxOf(1.45f, (metrics.descent - metrics.ascent) / paint.textSize)
    }
    val typography = Typography(
        displayLarge = base.displayLarge.readableHeading(arabic), displayMedium = base.displayMedium.readableHeading(arabic), displaySmall = base.displaySmall.readableHeading(arabic),
        headlineLarge = base.headlineLarge.readableHeading(arabic), headlineMedium = base.headlineMedium.readableHeading(arabic), headlineSmall = base.headlineSmall.readableHeading(arabic),
        titleLarge = base.titleLarge.readableHeading(arabic), titleMedium = base.titleMedium.readableHeading(arabic), titleSmall = base.titleSmall.readableHeading(arabic),
        bodyLarge = base.bodyLarge.readable(arabic), bodyMedium = base.bodyMedium.readable(arabic), bodySmall = base.bodySmall.readable(arabic),
        labelLarge = base.labelLarge.readable(arabic, labelMinimumRatio), labelMedium = base.labelMedium.readable(arabic, labelMinimumRatio), labelSmall = base.labelSmall.readable(arabic, labelMinimumRatio),
    )
    val shapes = themeShapes(preference)
    val background = if (preference == com.mukhtabir.android.core.ThemePreference.WEATHER)
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1A2A6C), Color(0xFF384D9F))) else null
    val feedback = if (preference == com.mukhtabir.android.core.ThemePreference.WEATHER)
        FeedbackColors(Color(0xFFB6F2CD), Color(0xFFFFE0A3), Color(0xFFC7EDFF))
    else if (darkTheme) FeedbackColors(Color(0xFF6BB98D), Color(0xFFE6CF45), Color(0xFF67B2D5)) else LocalFeedback.current
    val spacing = if (display.compact) AppSpacing(section = 3.dp, card = 4.dp, denseCard = 3.dp, content = 2.dp, tight = 2.dp, libraryCard = 3.dp, cardVertical = 2.dp, denseCardVertical = 1.dp) else AppSpacing()
    CompositionLocalProvider(LocalSpacing provides spacing, LocalAppBackground provides background, LocalFeedback provides feedback, LocalNativeTypeface provides nativeTypeface, LocalReadingComfort provides display) {
        MaterialTheme(colorScheme = themedColors(if (darkTheme) DarkColors else LightColors, preference), typography = typography,
            shapes = shapes,
            content = content)
    }
}
