@file:Suppress("DEPRECATION")

package com.mukhtabir.android.core

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference { SYSTEM, LIGHT, DARK, IRIS, BLOSSOM, LAGOON, SLATE, INK, AMBER, RUBY, MIDNIGHT, ONYX, CONTRAST, FOOD_DELIVERY, FITNESS_TRACKER, WEATHER, MATERIAL_YOU_LIGHT, MATERIAL_YOU_DARK }

data class DisplayPreferences(val perPage: Int = 0, val difficulty: String = "", val compact: Boolean = false,
    val font: String = "default", val autoAdvance: Boolean = false, val sound: Boolean = true,
    val haptics: Boolean = false, val activityHeatmap: Boolean = false)

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("mukhtabir_preferences", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme = _theme.asStateFlow()

    private val _language = MutableStateFlow(preferences.getString(KEY_LANGUAGE, LANGUAGE_ARABIC) ?: LANGUAGE_ARABIC)
    val language = _language.asStateFlow()

    private val _display = MutableStateFlow(DisplayPreferences(
        perPage = preferences.getInt("per_page", 0).takeIf { it in setOf(10, 20, 50, 100) } ?: 0,
        difficulty = preferences.getString("difficulty", "").orEmpty().takeIf { it in setOf("easy", "medium", "hard") }.orEmpty(),
        compact = preferences.getBoolean("compact", false), font = preferences.getString("font", "default").takeIf { it in SUPPORTED_FONTS } ?: "default",
        autoAdvance = preferences.getBoolean("auto_advance", false), sound = preferences.getBoolean("sound", true),
        haptics = preferences.getBoolean("haptics", false),
        activityHeatmap = preferences.getBoolean("activity_heatmap", false)))
    val display = _display.asStateFlow()
    fun setDisplay(value: DisplayPreferences) {
        require(value.perPage in setOf(0, 10, 20, 50, 100) && value.difficulty in setOf("", "easy", "medium", "hard") && value.font in SUPPORTED_FONTS)
        preferences.edit().putInt("per_page", value.perPage).putString("difficulty", value.difficulty)
            .putBoolean("compact", value.compact).putString("font", value.font)
            .putBoolean("auto_advance", value.autoAdvance).putBoolean("sound", value.sound)
            .putBoolean("haptics", value.haptics).putBoolean("activity_heatmap", value.activityHeatmap).apply()
        _display.value = value
    }
    fun resetDisplay() = setDisplay(_display.value.copy(perPage = 0, difficulty = "", compact = false))

    fun setTheme(value: ThemePreference) {
        preferences.edit().putString(KEY_THEME, value.name).apply()
        _theme.value = value
    }

    fun setLanguage(value: String) {
        val normalized = value.takeIf { it in SUPPORTED_LANGUAGES } ?: LANGUAGE_SYSTEM
        preferences.edit().putString(KEY_LANGUAGE, normalized).apply()
        _language.value = normalized
    }

    private fun readTheme(): ThemePreference = runCatching {
        ThemePreference.valueOf(preferences.getString(KEY_THEME, null) ?: ThemePreference.IRIS.name)
    }.getOrDefault(ThemePreference.IRIS)

    companion object {
        val DARK_THEMES = setOf(ThemePreference.DARK, ThemePreference.MIDNIGHT, ThemePreference.ONYX, ThemePreference.FITNESS_TRACKER, ThemePreference.WEATHER, ThemePreference.MATERIAL_YOU_DARK)
        val SUPPORTED_FONTS = setOf("default", "system", "noto-arabic", "tajawal", "cairo", "ibm-plex-arabic", "amiri", "inter", "outfit")
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ARABIC = "ar"
        val SUPPORTED_LANGUAGES = setOf(LANGUAGE_SYSTEM, "ar", "en")
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"

        fun wrapContext(context: Context): Context {
            val preferences = context.getSharedPreferences("mukhtabir_preferences", Context.MODE_PRIVATE)
            val code = preferences.getString(KEY_LANGUAGE, LANGUAGE_ARABIC) ?: LANGUAGE_ARABIC
            val locale = if (code == LANGUAGE_SYSTEM) {
                Resources.getSystem().configuration.locale
            } else {
                Locale.forLanguageTag(code)
            }
            Locale.setDefault(locale)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        }
    }
}
