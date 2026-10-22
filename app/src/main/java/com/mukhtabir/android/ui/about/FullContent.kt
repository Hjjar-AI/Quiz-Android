package com.mukhtabir.android.ui.about

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.alignReadingText
import com.mukhtabir.android.ui.components.applyReadingStyle
import com.mukhtabir.android.ui.theme.readingMaxWidth
import com.mukhtabir.android.ui.theme.readingParagraphMargin
import com.mukhtabir.android.ui.theme.readingTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Offline snapshot of the published bilingual frontend reference, with native guidance kept separate. */
@Composable
internal fun FullContent(privacy: Boolean) {
    val context = LocalContext.current
    val language = java.util.Locale.getDefault().language.takeIf { it == "ar" } ?: "en"
    var data by remember(privacy, language) { mutableStateOf<JsonObject?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(privacy, language) {
        data = withContext(Dispatchers.IO) { context.assets.open("content/${if (privacy) "privacy" else "manual"}.$language.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject } }
    }
    val sectionRows = (data?.get("sections") as? JsonArray).orEmpty().map { it.jsonObject }
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Text(stringResource(if (privacy) R.string.content_policy_note else R.string.content_manual_note))
        AppTextField(query, { query = it }, label = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth())
        data?.get("lastUpdate")?.jsonPrimitive?.content?.let { Text(it) }
        listOf("introLead", "lead").forEach { key -> data?.get(key)?.jsonPrimitive?.content?.let { PolicyHtml(it) } }
        sectionRows.filter { query.isBlank() || it["title"]?.jsonPrimitive?.content.orEmpty().contains(query, true) || it["content"]?.jsonPrimitive?.content.orEmpty().contains(query, true) }.forEach { section ->
            AppSection(section["title"]?.jsonPrimitive?.content.orEmpty(), initiallyExpanded = query.isNotBlank()) {
                PolicyHtml(section["content"]?.jsonPrimitive?.content.orEmpty())
            }
        }
        (data?.get("summaryItems") as? JsonArray).orEmpty().forEach { PolicyHtml("• " + it.jsonPrimitive.content) }
        data?.get("contactIntro")?.jsonPrimitive?.content?.let { PolicyHtml(it) }
        (data?.get("contactItems") as? JsonArray).orEmpty().forEach { PolicyHtml(it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()) }
        listOf("footerMain", "footerDisclaimer", "footerNote").forEach { key -> data?.get(key)?.jsonPrimitive?.content?.let { PolicyHtml(it) } }
    }
}

@Composable
private fun PolicyHtml(html: String) {
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val links = MaterialTheme.colorScheme.primary.toArgb()
    val style = readingTextStyle(MaterialTheme.typography.bodyLarge)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val typeface = com.mukhtabir.android.ui.theme.LocalNativeTypeface.current
    // Native TextView renders no scripts. Keep only safe external links and make tables linear and readable.
    val safe = remember(html) { html.replace(Regex("(?is)<(script|style|iframe)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?i)href=([\"'])(?!https?://|mailto:)[^\"']*\\1"), "")
        .replace(Regex("(?i)</t[dh]>"), "</span> · ").replace(Regex("(?i)<t[dh][^>]*>"), "<span>")
        .replace(Regex("(?i)</tr>"), "<br>") }
    // HTML already has paragraph breaks; add only the requested extra spacing.
    val paragraphMargin = readingParagraphMargin()
    val extraGap = with(density) {
        ((paragraphMargin - com.mukhtabir.android.ui.theme.readingParagraphBaseMargin).coerceAtLeast(0.dp) * fontScale.coerceAtLeast(1f)).roundToPx()
    }
    val rendered = remember(safe, extraGap) {
        android.text.SpannableString(HtmlCompat.fromHtml(safe, HtmlCompat.FROM_HTML_MODE_LEGACY)).apply {
            if (extraGap > 0) Regex("\n\n").findAll(toString()).forEach { paragraph ->
                setSpan(android.text.style.LineHeightSpan { _, _, _, _, _, metrics ->
                    metrics.descent += extraGap
                    metrics.bottom += extraGap
                }, paragraph.range.first, paragraph.range.first + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AndroidView(factory = { TextView(it).apply { setTextIsSelectable(true); movementMethod = LinkMovementMethod.getInstance() } }, update = {
            if (it.tag !== rendered) { it.text = rendered; it.tag = rendered }
            it.typeface = typeface
            it.setTextColor(color); it.setLinkTextColor(links); it.applyReadingStyle(style, density)
            it.alignReadingText()
        }, modifier = Modifier.widthIn(max = readingMaxWidth()).fillMaxWidth())
    }
}
