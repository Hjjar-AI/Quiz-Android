package com.mukhtabir.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.core.ThemePreference

/** Theme character lives here; screens continue using shared Material shape roles. */
fun themeShapes(preference: ThemePreference): Shapes {
    val radii = when(preference) {
        ThemePreference.SYSTEM, ThemePreference.DARK -> listOf(6,10,12,20,24)
        ThemePreference.LIGHT -> listOf(4,6,10,14,18)
        ThemePreference.IRIS -> listOf(8,12,18,24,30)
        ThemePreference.BLOSSOM -> listOf(12,16,22,28,34)
        ThemePreference.LAGOON -> listOf(6,10,14,20,26)
        ThemePreference.SLATE -> listOf(2,4,8,12,16)
        ThemePreference.INK -> listOf(4,8,10,16,20)
        ThemePreference.AMBER -> listOf(6,10,16,22,28)
        ThemePreference.RUBY -> listOf(8,12,18,24,30)
        ThemePreference.MIDNIGHT -> listOf(4,8,12,18,24)
        ThemePreference.ONYX -> listOf(2,6,10,16,22)
        ThemePreference.CONTRAST -> listOf(0,2,4,8,12)
        ThemePreference.FOOD_DELIVERY -> listOf(8,10,16,20,26)
        ThemePreference.FITNESS_TRACKER -> listOf(8,12,15,24,28)
        ThemePreference.WEATHER -> listOf(8,12,14,18,24)
        ThemePreference.MATERIAL_YOU_LIGHT -> listOf(10,14,24,28,32)
        ThemePreference.MATERIAL_YOU_DARK -> listOf(10,14,22,26,30)
    }
    return Shapes(RoundedCornerShape(radii[0].dp),RoundedCornerShape(radii[1].dp),
        RoundedCornerShape(radii[2].dp),RoundedCornerShape(radii[3].dp),RoundedCornerShape(radii[4].dp))
}
