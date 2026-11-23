package com.mukhtabir.android.demo

import android.content.Context
import android.content.res.Configuration
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.shared.R as SharedR
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.serialization.json.*

internal class DemoFixtures(private val context: Context) {
    private fun inLanguage(tag: String): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) },
    )

    private val english = inLanguage("en")
    private val arabic = inLanguage("ar")
    private val names = listOf(SharedR.string.demo_category_circulation, SharedR.string.demo_category_systems)
    val categories = names.mapIndexed { index, title ->
        CategoryDto(index + 1L, name = context.getString(title), color = listOf("#6750A4", "#006B60")[index])
    }
    private val tags = demoQuestions.flatMap { it.tags }.distinct().associate { id ->
        english.getString(id) to context.getString(id)
    }
    fun canonicalTags(labels: List<String>) = labels.map { label ->
        tags.entries.firstOrNull { it.value == label }?.key ?: label
    }
    fun localizedTags(keys: List<String>) = keys.map { tags[it] ?: it }

    val book = english.getString(SharedR.string.demo_collection)
    val user = UserDto(
        id = SAMPLE_USER_ID,
        username = "demo",
        fullName = context.getString(R.string.sample_learner),
        role = "member",
        capabilities = listOf("tests.start", "tests.view_own_history"),
    )
    val questions = demoQuestions.mapIndexed { index, sample ->
        val category = categories[sample.category - 1]
        val case = sample.caseStem?.let { resource ->
            ClinicalCaseContextDto(stem = english.getString(resource), translations = buildJsonObject {
                putJsonObject("ar") { put("stem", arabic.getString(resource)) }
            })
        }
        QuestionDto(
            id = index + 1L,
            question = english.getString(sample.stem),
            choices = sample.choices.map(english::getString),
            correctAnswer = sample.correct + 1,
            explanation = english.getString(sample.explanation),
            translations = buildJsonObject {
                putJsonObject("ar") {
                    put("question", arabic.getString(sample.stem))
                    put("choices", JsonArray(sample.choices.map { JsonPrimitive(arabic.getString(it)) }))
                    put("explanation", arabic.getString(sample.explanation))
                }
            },
            sourceDocument = book,
            tags = sample.tags.map(context::getString),
            difficulty = listOf("easy", "medium", "hard")[index % 3],
            category = category.id,
            categoryName = category.name,
            categoryColor = category.color,
            verified = index % 3 != 2,
            case = case,
        )
    }
    val byId = questions.associateBy { it.id }

    fun seed(): DemoData {
        val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
        val first = listOf(1L, 2L, 3L).map(byId::getValue)
        val answers = listOf(SessionAnswerBaseline(first[0].correctAnswer, 3),
            SessionAnswerBaseline(1, 3), SessionAnswerBaseline(first[2].correctAnswer, 1))
        val result = buildDemoResult("sample-study", 1, TestMode.STUDY.path, first, answers, 105)
        val completion = DemoCompletion(result, yesterday.minusSeconds(105).toString(), yesterday.toString(), book)
        val pack = OfflinePack("sample-pack", "sample", questions.takeLast(4), imagesIncluded = false)
        return DemoData(completions = mapOf("sample-study" to completion), nextHistoryId = 2,
            offline = listOf(pack.newMaterial()))
    }
}

internal fun buildDemoResult(
    identity: String,
    historyId: Long,
    mode: String,
    questions: List<QuestionDto>,
    answers: List<SessionAnswerBaseline?>,
    elapsed: Int,
): SessionResultsDto {
    val rows = questions.mapIndexed { index, raw ->
        val question = raw.localizedForDisplay()
        val answer = answers[index]
        SessionResultItemDto(
            questionId = question.id, question = question.question, choices = question.choices,
            correctAnswer = question.correctAnswer, userAnswer = answer?.answer, finalAnswer = answer?.answer,
            isCorrect = answer?.answer != null && answer.answer == question.correctAnswer,
            confidenceScore = answer?.confidence, finalConfidenceScore = answer?.confidence,
            preAnswer = answer?.preAnswer, errorReason = answer?.errorReason,
            explanation = question.explanation, categoryId = question.category, categoryName = question.categoryName,
            categoryColor = question.categoryColor, tagNames = question.tags, difficulty = question.difficulty, case = question.case,
        )
    }
    val correct = rows.count { it.isCorrect }
    return SessionResultsDto(
        results = rows, correctCount = correct, totalQuestions = rows.size,
        accuracy = if (rows.isEmpty()) 0.0 else 100.0 * correct / rows.size,
        totalTime = elapsed, answeredCount = rows.count { it.userAnswer != null },
        confidenceCorrect = rows.count { it.isCorrect && it.confidenceScore == 3 },
        confidenceFragile = rows.count { it.isCorrect && (it.confidenceScore ?: 3) < 3 },
        sourceSessionId = identity, historyId = historyId, mode = mode,
    )
}
