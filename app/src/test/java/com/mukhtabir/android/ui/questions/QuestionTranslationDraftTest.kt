package com.mukhtabir.android.ui.questions

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class QuestionTranslationDraftTest {
    private val a = QuestionChoiceDraft("a", "A")
    private val b = QuestionChoiceDraft("b", "B")
    private val choices = listOf(a, b)
    private fun raw(value: String) = Json.parseToJsonElement(value).jsonObject
    @Test fun `translated options follow stable identities when base choices move`() {
        val draft = translationDrafts(raw("""{"ar":{"question":"سؤال","choices":["أ","ب"]}}"""), choices).getValue("ar")
        assertEquals(listOf("ب", "أ"), translatedChoices(draft, listOf(b, a)))
    }
    @Test fun `new choice gets empty translation and removed choice is not emitted`() {
        val draft = QuestionTranslationDraft(choices = mapOf("a" to "Uno", "b" to "Dos"))
        assertEquals(listOf("Dos", ""), translatedChoices(draft, listOf(b, QuestionChoiceDraft("c", "C"))))
    }
    @Test fun `surplus stored translation options survive payload round trip`() {
        val draft = translationDrafts(raw("""{"en":{"question":" Q ","choices":[" A ","B"," C "]}}"""), choices)
        assertEquals(listOf(" C "), draft.getValue("en").extraChoices)
        assertEquals(listOf("A", "B", "C"), translationPayload(draft, choices).getValue("en").jsonObject.getValue("choices").jsonArray.map { it.jsonPrimitive.content })
    }
    @Test fun `blank translation is omitted`() {
        assertTrue(translationPayload(mapOf("en" to QuestionTranslationDraft("  ", "\n", mapOf("a" to " "))), choices).isEmpty())
    }
    @Test fun `text only translation emits empty choices and trimmed fields`() {
        val result = translationPayload(mapOf("en" to QuestionTranslationDraft(" Q ", " E ")), choices).getValue("en").jsonObject
        assertEquals("Q", result.getValue("question").jsonPrimitive.content)
        assertEquals("E", result.getValue("explanation").jsonPrimitive.content)
        assertTrue(result.getValue("choices").jsonArray.isEmpty())
    }
    @Test fun `partial options retain positions for validation`() {
        val result = translationPayload(mapOf("en" to QuestionTranslationDraft(choices = mapOf("b" to " B "))), choices)
        assertEquals(listOf("", "B"), result.getValue("en").jsonObject.getValue("choices").jsonArray.map { it.jsonPrimitive.content })
    }
    @Test fun `missing optional fields recover as empty text without losing locale`() {
        val draft = translationDrafts(raw("""{"en":{}}"""), choices).getValue("en")
        assertEquals("", draft.question); assertEquals("", draft.explanation)
        assertEquals(listOf("", ""), translatedChoices(draft, choices))
    }
    @Test fun `malformed translation object fails instead of discarding content`() {
        assertThrows(IllegalStateException::class.java) { translationDrafts(raw("""{"en":[]}"""), choices) }
    }
    @Test fun `locale accepts language and region with normalized casing`() {
        mapOf(" EN_us " to "en-US", "ar" to "ar", "fil_ph" to "fil-PH").forEach { (value, expected) -> assertEquals(expected, normalizedTranslationLocale(value)) }
    }
    @Test fun `unsupported locale shapes are rejected`() {
        listOf("", "e", "english", "en-US-extra", "zh-Hant", "en-123", "en US", "../en").forEach { assertEquals(it, "", normalizedTranslationLocale(it)) }
    }
}
