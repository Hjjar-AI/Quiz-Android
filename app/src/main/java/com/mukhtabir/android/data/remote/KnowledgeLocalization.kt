package com.mukhtabir.android.data.remote

import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Knowledge translations normalize regional keys to lowercase in Django. */
fun KnowledgeDetailDto.localizedForDisplay(localeTag: String = Locale.getDefault().toLanguageTag()): KnowledgeDetailDto {
    val translated = translationFor(translations, localeTag)
    fun text(key: String, fallback: String): String =
        (translated?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: fallback
    return copy(title=text("title",title),learningObjective=text("learning_objective",learningObjective),
        canonicalAnswer=text("canonical_answer",canonicalAnswer))
}
