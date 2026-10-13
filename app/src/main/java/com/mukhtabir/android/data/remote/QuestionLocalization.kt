package com.mukhtabir.android.data.remote

import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One case-insensitive regional/language matching rule for read-only content. */
fun translationLocale(translations: JsonObject, localeTag: String): String? {
    val normalized = localeTag.trim().replace('_', '-').lowercase(Locale.ROOT)
    if (normalized.isBlank()) return null
    return listOf(normalized, normalized.substringBefore('-')).distinct().firstNotNullOfOrNull { key ->
        translations.entries.firstOrNull { it.key.replace('_', '-').lowercase(Locale.ROOT) == key && it.value is JsonObject }?.key
    }
}

fun translationFor(translations: JsonObject, localeTag: String): JsonObject? =
    translationLocale(translations, localeTag)?.let { translations[it] as? JsonObject }

private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)
    ?.takeIf { it.isString }?.contentOrNull?.takeIf(String::isNotBlank)

/** Case fields are independent and optional; never blank out an untranslated part. */
fun ClinicalCaseContextDto.localizedForDisplay(localeTag: String = Locale.getDefault().toLanguageTag()): ClinicalCaseContextDto {
    val content = translationFor(translations, localeTag) ?: return this
    return copy(title = content.text("title") ?: title, stem = content.text("stem") ?: stem)
}

fun caseTranslationLocales(translations: JsonObject): List<String> = translations.entries.filter { (_, raw) ->
    val content = raw as? JsonObject ?: return@filter false
    content.text("title") != null || content.text("stem") != null
}.map { it.key }.distinctBy { it.replace('_', '-').lowercase(Locale.ROOT) }.sorted()

private fun JsonObject.completeChoices(count: Int): List<String>? = (get("choices") as? JsonArray)
    ?.map { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.contentOrNull.orEmpty() }
    ?.takeIf { it.size == count && it.all(String::isNotBlank) }

fun completeQuestionLocales(translations: JsonObject, choiceCount: Int): List<String> =
    translations.entries.filter { (_, raw) ->
        val content = raw as? JsonObject ?: return@filter false
        content.text("question") != null && content.completeChoices(choiceCount) != null
    }.map { it.key }.distinctBy { it.replace('_', '-').lowercase(Locale.ROOT) }.sorted()

fun questionTranslationApplied(translations: JsonObject, choiceCount: Int, localeTag: String): Boolean {
    val content = translationFor(translations, localeTag) ?: return false
    return content.text("question") != null && content.completeChoices(choiceCount) != null
}

private data class ReadingText(val question: String, val choices: List<String>, val explanation: String?)

private fun resolveReadingText(question: String, choices: List<String>, explanation: String?,
    translations: JsonObject, localeTag: String, feedbackTranslations: JsonObject = JsonObject(emptyMap())): ReadingText {
    val original = ReadingText(question, choices, explanation)
    val content = translationFor(translations, localeTag) ?: return original
    // Apply stem and options together, never leave a translated stem over original options.
    val translatedQuestion = content.text("question") ?: return original
    val translatedChoices = content.completeChoices(choices.size) ?: return original
    val translatedExplanation = translationFor(feedbackTranslations, localeTag)?.text("explanation")
        ?: content.text("explanation")
    // Missing explanation remains unavailable in this language; Original is always selectable.
    return ReadingText(translatedQuestion, translatedChoices, translatedExplanation)
}

/** Display-only copy: never feed localized content back into the question editor. */
fun QuestionDto.localizedForDisplay(localeTag: String = Locale.getDefault().toLanguageTag()): QuestionDto {
    val text = resolveReadingText(question, choices, explanation, translations, localeTag)
    return copy(question = text.question, choices = text.choices, case = case?.localizedForDisplay(localeTag),
        correctAnswer = correctAnswer.takeUnless { answersHidden },
        explanation = text.explanation.takeUnless { answersHidden })
}

fun SessionQuestionDto.localizedForDisplay(localeTag: String = Locale.getDefault().toLanguageTag(),
    feedbackTranslations: JsonObject = JsonObject(emptyMap()), feedbackExplanation: String? = null,
    showExplanation: Boolean = false): SessionQuestionDto {
    val resolved = resolveReadingText(text, choices, if (showExplanation) feedbackExplanation ?: explanation else null,
        translations, localeTag, feedbackTranslations)
    return copy(text = resolved.question, choices = resolved.choices, case = case?.localizedForDisplay(localeTag),
        explanation = resolved.explanation.takeIf { showExplanation })
}

fun ClinicalCaseDetailDto.localizedForDisplay(localeTag: String = Locale.getDefault().toLanguageTag()): ClinicalCaseDetailDto {
    val content = translationFor(translations, localeTag) ?: return this
    return copy(title = content.text("title") ?: title, stem = content.text("stem") ?: stem)
}
