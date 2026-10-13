package com.mukhtabir.android.ui.questions

import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** Translated choices follow stable base-choice identities through moves/removals. */
@kotlinx.serialization.Serializable
data class QuestionTranslationDraft(
    val question: String = "",
    val explanation: String = "",
    val choices: Map<String, String> = emptyMap(),
    // Preserve legacy surplus entries until the author explicitly aligns or removes this translation.
    val extraChoices: List<String> = emptyList(),
)

internal fun translationDrafts(raw: JsonObject, choices: List<QuestionChoiceDraft>): Map<String, QuestionTranslationDraft> =
    raw.mapValues { (_, element) ->
        val content = element as? JsonObject ?: error("Invalid translation content")
        fun text(key: String) = (content[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val translatedChoices = (content["choices"] as? JsonArray).orEmpty().map { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }
        QuestionTranslationDraft(text("question"),text("explanation"),
            choices.mapIndexed { index, choice -> choice.id to translatedChoices.getOrElse(index) { "" } }.toMap(),
            translatedChoices.drop(choices.size))
    }

internal fun translatedChoices(draft: QuestionTranslationDraft, choices: List<QuestionChoiceDraft>): List<String> =
    choices.map { draft.choices[it.id].orEmpty() }

internal fun translationPayload(drafts: Map<String, QuestionTranslationDraft>, choices: List<QuestionChoiceDraft>): JsonObject =
    JsonObject(drafts.mapNotNull { (locale, draft) ->
        val aligned = translatedChoices(draft,choices).map(String::trim) + draft.extraChoices.map(String::trim)
        if(draft.question.isBlank() && draft.explanation.isBlank() && aligned.all(String::isBlank)) null
        else locale to buildJsonObject {
            put("question",draft.question.trim())
            put("explanation",draft.explanation.trim())
            put("choices",JsonArray(if(aligned.all(String::isBlank)) emptyList() else aligned.map(::JsonPrimitive)))
        }
    }.toMap())

internal fun normalizedTranslationLocale(value: String): String {
    val parts=value.trim().replace('_','-').split('-')
    return (parts.first().lowercase(Locale.ROOT) + if(parts.size==2) "-${parts[1].uppercase(Locale.ROOT)}" else "")
        .takeIf { parts.size<=2 && it.matches(Regex("[a-z]{2,3}(?:-[A-Z]{2})?")) }.orEmpty()
}
