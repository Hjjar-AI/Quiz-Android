package com.mukhtabir.android.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat

private val variableFonts = setOf("noto-arabic", "inter", "outfit", "cairo")

/** Weight roles follow the actual face, including the Amiri fallback on API 23–25. */
internal fun supportsAppFontWeights(name: String): Boolean =
    name in variableFonts && Build.VERSION.SDK_INT >= 26

/** Static fonts expose their real regular face. Variable weights require API 26. */
@OptIn(ExperimentalTextApi::class)
internal fun appFontFamily(resource: Int, name: String): FontFamily {
    if (resource == 0) return FontFamily.Default
    if (name !in variableFonts) return FontFamily(Font(resource, weight = FontWeight.Normal))
    // Android 6/7 cannot select variation axes; retain a bundled readable regular face.
    if (Build.VERSION.SDK_INT < 26) return FontFamily(Font(com.mukhtabir.android.R.font.amiri))
    return FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(resource, weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    })
}

internal fun nativeReadingTypeface(context: Context, resource: Int, name: String): Typeface {
    if (resource == 0) return Typeface.DEFAULT
    if (name in variableFonts) {
        if (Build.VERSION.SDK_INT < 26) return ResourcesCompat.getFont(context, com.mukhtabir.android.R.font.amiri) ?: Typeface.DEFAULT
        return android.graphics.Paint().apply {
            typeface = ResourcesCompat.getFont(context, resource) ?: Typeface.DEFAULT
            fontVariationSettings = "'wght' 400"
        }.typeface
    }
    return ResourcesCompat.getFont(context, resource) ?: Typeface.DEFAULT
}
