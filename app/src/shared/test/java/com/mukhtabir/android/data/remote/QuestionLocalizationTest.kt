package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class QuestionLocalizationTest {
    private fun raw(value: String) = Json.parseToJsonElement(value).jsonObject
    private val translations = raw("""{"ar":{"question":"سؤال","choices":["أ","ب"],"explanation":"شرح"},"ar-SY":{"question":"محلي","choices":["١","٢"]}}""")
    private fun question(content: JsonObject = translations) = QuestionDto(7, "Original", listOf("A", "B"), correctAnswer = 2, explanation = "Original feedback", translations = content)
    @Test fun `region wins over language with case and underscore normalization`() {
        assertEquals("ar-SY", translationLocale(translations, " AR_sy "))
        assertEquals("ar", translationLocale(translations, "ar-LB"))
        assertNull(translationLocale(translations, "fr")); assertNull(translationLocale(translations, " "))
    }
    @Test fun `malformed regional object falls back to language`() {
        assertEquals("ar", translationLocale(raw("""{"ar-SY":[],"ar":{}}"""), "ar-SY"))
    }
    @Test fun `translated question and options apply atomically and preserve identity`() {
        val original = question(); val displayed = original.localizedForDisplay("ar")
        assertEquals("سؤال", displayed.question); assertEquals(listOf("أ", "ب"), displayed.choices)
        assertEquals("شرح", displayed.explanation); assertEquals(7L, displayed.id); assertEquals(2, displayed.correctAnswer)
        assertEquals("Original", original.question); assertEquals(listOf("A", "B"), original.choices)
    }
    @Test fun `incomplete blank nonstring or wrong sized options retain original question`() {
        listOf("[]", "[\"أ\"]", "[\"أ\",\" \"]", "[1,2]", "[null,\"ب\"]").forEach { options ->
            val displayed = question(raw("""{"ar":{"question":"سؤال","choices":$options}}""")).localizedForDisplay("ar")
            assertEquals(options, "Original", displayed.question); assertEquals(listOf("A", "B"), displayed.choices)
            assertEquals("Original feedback", displayed.explanation)
        }
    }
    @Test fun `blank or nonstring question prevents partial translation`() {
        listOf("null", "3", "\" \"").forEach { text ->
            assertEquals("Original", question(raw("""{"ar":{"question":$text,"choices":["أ","ب"]}}""")).localizedForDisplay("ar").question)
        }
    }
    @Test fun `missing translated feedback stays unavailable in selected language`() {
        assertNull(question().localizedForDisplay("ar-SY").explanation)
        assertEquals("Original feedback", question().localizedForDisplay("fr").explanation)
    }
    @Test fun `hidden answers suppress both correctness and translated explanation`() {
        val result = question().copy(answersHidden = true).localizedForDisplay("ar")
        assertNull(result.correctAnswer); assertNull(result.explanation)
        assertEquals("سؤال", result.question)
    }
    @Test fun `session feedback is hidden until explicitly requested`() {
        val value = SessionQuestionDto(7, "Original", listOf("A", "B"), explanation = "Secret", translations = translations)
        assertNull(value.localizedForDisplay("ar").explanation)
        assertEquals("شرح", value.localizedForDisplay("ar", showExplanation = true).explanation)
    }
    @Test fun `confirmed feedback translation takes precedence over question translation`() {
        val value = SessionQuestionDto(7, "Original", listOf("A", "B"), translations = translations)
        assertEquals("Updated", value.localizedForDisplay("ar", raw("""{"ar":{"explanation":"Updated"}}"""), showExplanation = true).explanation)
        assertEquals("Server feedback", value.localizedForDisplay("fr", feedbackExplanation = "Server feedback", showExplanation = true).explanation)
    }
    @Test fun `case title and stem fall back independently`() {
        val context = ClinicalCaseContextDto(id = 3, title = "Title", stem = "Stem", translations = raw("""{"ar":{"title":"عنوان","stem":" "}}"""))
        val displayed = context.localizedForDisplay("ar")
        assertEquals("عنوان", displayed.title); assertEquals("Stem", displayed.stem); assertEquals(3L, displayed.id)
    }
    @Test fun `case translation locales exclude empty malformed and nonstring content`() {
        assertEquals(listOf("ar", "en"), caseTranslationLocales(raw("""{"ar":{"stem":"س"},"en":{"title":"Title"},"fr":{"title":3},"de":[],"es":{"stem":" "}}""")))
    }
    @Test fun `available question locales require complete paired text and deduplicate normalized keys`() {
        val raw = raw("""{"en_US":{"question":"Q","choices":["A","B"]},"en-us":{"question":"Q2","choices":["C","D"]},"ar":{"question":"س","choices":["أ"]},"fr":{}}""")
        assertEquals(listOf("en_US"), completeQuestionLocales(raw, 2))
        assertTrue(questionTranslationApplied(raw, 2, "en-US"))
        assertFalse(questionTranslationApplied(raw, 2, "ar"))
    }
    @Test fun `translated question also translates its case without mutating base snapshot`() {
        val original = question().copy(case = ClinicalCaseContextDto(stem = "Original case", translations = raw("""{"ar":{"stem":"حالة"}}""")))
        assertEquals("حالة", original.localizedForDisplay("ar").case?.stem)
        assertEquals("Original case", original.case?.stem)
    }
}
