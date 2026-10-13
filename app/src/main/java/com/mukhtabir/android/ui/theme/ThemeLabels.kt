package com.mukhtabir.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.core.ThemePreference

@Composable
fun themeLabel(theme: ThemePreference): String = when (theme) {
    ThemePreference.SYSTEM -> stringResource(R.string.system_default)
    ThemePreference.LIGHT -> stringResource(R.string.theme_light)
    ThemePreference.DARK -> stringResource(R.string.theme_dark)
    ThemePreference.FOOD_DELIVERY -> stringResource(R.string.theme_food_delivery)
    ThemePreference.FITNESS_TRACKER -> stringResource(R.string.theme_fitness_tracker)
    ThemePreference.WEATHER -> stringResource(R.string.theme_weather)
    ThemePreference.MATERIAL_YOU_LIGHT -> stringResource(R.string.theme_material_you_light)
    ThemePreference.MATERIAL_YOU_DARK -> stringResource(R.string.theme_material_you_dark)
    ThemePreference.IRIS -> stringResource(R.string.theme_iris)
    ThemePreference.BLOSSOM -> stringResource(R.string.theme_blossom)
    ThemePreference.LAGOON -> stringResource(R.string.theme_lagoon)
    ThemePreference.SLATE -> stringResource(R.string.theme_slate)
    ThemePreference.INK -> stringResource(R.string.theme_ink)
    ThemePreference.AMBER -> stringResource(R.string.theme_amber)
    ThemePreference.RUBY -> stringResource(R.string.theme_ruby)
    ThemePreference.MIDNIGHT -> stringResource(R.string.theme_midnight)
    ThemePreference.ONYX -> stringResource(R.string.theme_onyx)
    ThemePreference.CONTRAST -> stringResource(R.string.theme_contrast)
}
