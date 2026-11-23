package com.mukhtabir.android.ui.theme

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.mukhtabir.android.core.ThemePreference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class NativeContrastTest {
    private fun contrast(ink: Color, surface: Color) = ColorUtils.calculateContrast(ink.toArgb(), surface.toArgb())
    @Test fun `passing seed stays unchanged and opaque`() {
        val result = contrastInk(Color.Black.copy(alpha = .3f), listOf(Color.White), 4.5)
        assertEquals(Color.Black, result); assertEquals(1f, result.alpha, 0f)
    }
    @Test fun `weak ink on light and dark surfaces reaches requested text contrast`() {
        listOf(Color.White, Color.Black).forEach { background ->
            val result = contrastInk(Color.Gray, listOf(background), 7.0)
            assertTrue(contrast(result, background) >= 7.0)
        }
    }
    @Test fun `derived ink passes across every supplied similar surface`() {
        val backgrounds = listOf(Color(0xFFF2E8DC), Color(0xFFE6DAC8))
        val result = contrastInk(Color(0xFFB69E86), backgrounds, 4.5)
        backgrounds.forEach { assertTrue(contrast(result, it) >= 4.5) }
    }
    @Test fun `all supported theme feedback seeds can produce accessible text on reading surfaces`() {
        ThemePreference.values().forEach { theme -> listOf(false, true).forEach { systemDark ->
            val colors = previewThemeColors(theme, systemDark)
            val backgrounds = listOf(colors.background, colors.surface, colors.surfaceVariant)
            val ink = contrastInk(colors.error, backgrounds, 4.5)
            backgrounds.forEach { assertTrue("$theme dark=$systemDark", contrast(ink, it) >= 4.5) }
        } }
    }
    @Test fun `system theme follows light and dark selection while fixed themes stay stable`() {
        assertNotEquals(previewThemeColors(ThemePreference.SYSTEM, false).background, previewThemeColors(ThemePreference.SYSTEM, true).background)
        ThemePreference.values().filter { it != ThemePreference.SYSTEM }.forEach {
            assertEquals(it.name, previewThemeColors(it, false).background, previewThemeColors(it, true).background)
        }
    }
}
