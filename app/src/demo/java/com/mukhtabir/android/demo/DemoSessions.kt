package com.mukhtabir.android.demo

import com.mukhtabir.android.data.remote.*
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class DemoSessions(private val data: DemoStore, private val fixtures: DemoFixtures) : LearnerSessions {
    override suspend fun start(mode: TestMode, request: StartSessionRequest): ExamSessionDto = withContext(Dispatchers.IO) {
        data.changeOnline { snapshot ->
            val matching = fixtures.questions.matching(
                categories = request.categoryIds, tags = request.tagsFilter, difficulty = request.difficulty,
                verifiedOnly = request.verifiedOnly, book = request.sourceDocument,
                include = when {
                    request.useBookmarks -> snapshot.bookmarks
                    request.useSrs -> snapshot.reviewIds()
                    else -> null
                },
            ).map { it.id }
            val ids = if (request.questionIds.isEmpty()) matching.take(request.limit.coerceIn(0, SAMPLE_LIMIT))
                else request.questionIds.distinct().also { if (!matching.containsAll(it)) throw invalidDemoRequest() }
            if (ids.isEmpty() || ids.size > SAMPLE_LIMIT) throw invalidDemoRequest()
            if (snapshot.sessions.values.any { it.session.mode == mode.path }) throw demoConflict()
            val session = ExamSessionDto(
                sessionId = UUID.randomUUID().toString(), mode = mode.path, questionIds = ids,
                tag = request.sessionLabel, startedAt = Instant.now().toString(),
                durationMinutes = if (mode == TestMode.EXAM) 8 else null,
            )
            snapshot.withSession(DemoSession(session)) to session
        }
    }

    override suspend fun question(mode: TestMode, sessionId: String): SessionQuestionPageDto = withContext(Dispatchers.IO) {
        data.readOnline { snapshot ->
            val item = snapshot.session(mode, sessionId)
            item.page(item.session.currentIndex)
        }
    }

    override suspend fun questions(mode: TestMode, sessionId: String): SessionQuestionBundleDto = withContext(Dispatchers.IO) {
        data.readOnline { snapshot ->
            val item = snapshot.session(mode, sessionId)
            SessionQuestionBundleDto(sessionId, item.session.questionIds.indices.map { item.page(it, downloaded = true) })
        }
    }

    override suspend fun submit(mode: TestMode, request: SubmitAnswerRequest): AnswerSubmissionDto = withContext(Dispatchers.IO) {
        data.changeOnline(DemoFailure.ANSWER.takeIf { request.answer != null }) { snapshot ->
            val item = snapshot.session(mode, request.sessionId)
            val session = item.session
            val index = session.currentIndex
            if (!session.isActive || (request.expectedIndex != null && request.expectedIndex != index) ||
                (request.expectedSlot != null && request.expectedSlot != (item.answers[index] ?: SessionAnswerBaseline()))) {
                throw demoConflict()
            }
            val question = fixtures.byId.getValue(session.questionIds[index])
            val old = item.answers[index] ?: SessionAnswerBaseline()
            val answer = request.answer
            val preAnswer = request.preAnswer?.trim()?.takeIf(String::isNotBlank) ?: old.preAnswer
            if (answer != null && (answer !in 1..question.choices.size || (mode == TestMode.RECALL && preAnswer == null))) {
                throw invalidDemoRequest()
            }
            val target = when (request.action) {
                "same" -> index
                "previous" -> index - 1
                "next" -> index + 1
                "goto" -> request.targetIndex ?: throw invalidDemoRequest()
                else -> throw invalidDemoRequest()
            }
            if (target !in session.questionIds.indices) throw invalidDemoRequest()
            if (mode == TestMode.RECALL && target != index && (preAnswer == null || (answer ?: old.answer) == null)) {
                throw invalidDemoRequest()
            }
            val new = old.copy(answer = answer ?: old.answer, confidence = request.confidence ?: old.confidence,
                preAnswer = preAnswer, errorReason = request.errorReason ?: old.errorReason)
            val confirmed = new.answer?.let { it == question.correctAnswer }.takeUnless { mode == TestMode.EXAM }
            val result = AnswerSubmissionDto(target, question.explanation.takeIf { confirmed != null }, confirmed)
            val updated = item.copy(session = session.copy(currentIndex = target),
                answers = if (new == old) item.answers else item.answers + (index to new))
            snapshot.withSession(updated) to result
        }
    }

    override suspend fun finish(mode: TestMode, sessionId: String): SessionResultsDto = withContext(Dispatchers.IO) {
        data.changeOnline(DemoFailure.FINISH) { snapshot ->
            snapshot.completions[sessionId]?.result?.takeIf { it.mode == mode.path }?.let {
                return@changeOnline snapshot to it
            }
            val item = snapshot.session(mode, sessionId)
            if (!item.session.isActive) throw demoConflict()
            val completedAt = Instant.now()
            val startedAt = item.createdAt
            val elapsed = item.session.elapsed(completedAt).let { seconds ->
                item.session.durationMinutes?.times(60)?.let { seconds.coerceAtMost(it) } ?: seconds
            }
            val questions = item.session.questionIds.map(fixtures.byId::getValue)
            val result = buildDemoResult(sessionId, snapshot.nextHistoryId, mode.path, questions,
                questions.indices.map(item.answers::get), elapsed)
            val entry = DemoCompletion(result, startedAt, completedAt.toString(), item.session.tag)
            snapshot.copy(sessions = snapshot.sessions - sessionId, flags = snapshot.flags - sessionId,
                completions = snapshot.completions + (sessionId to entry), nextHistoryId = snapshot.nextHistoryId + 1) to result
        }
    }

    override suspend fun completedResult(mode: TestMode, sessionId: String): SessionResultsDto = withContext(Dispatchers.IO) {
        data.readOnline { snapshot ->
            snapshot.completions[sessionId]?.result?.takeIf { it.mode == mode.path } ?: throw demoMissing()
        }
    }

    override suspend fun pause(mode: TestMode, sessionId: String) = withContext(Dispatchers.IO) {
        data.changeOnline { snapshot ->
            val item = snapshot.session(mode, sessionId)
            if (!item.session.isActive) throw demoConflict()
            val paused = item.session.copy(startedAt = null, accumulatedTime = item.session.elapsed(Instant.now()), isActive = false)
            snapshot.withSession(item.copy(session = paused)) to Unit
        }
    }

    override suspend fun resume(mode: TestMode, sessionId: String?): SessionProgressDto = withContext(Dispatchers.IO) {
        data.changeOnline { snapshot ->
            val item = snapshot.findSession(mode, sessionId) ?: throw demoMissing()
            if (item.session.isActive) snapshot to item.progress()
            else {
                val resumed = item.copy(session = item.session.copy(startedAt = Instant.now().toString(), isActive = true))
                snapshot.withSession(resumed) to resumed.progress()
            }
        }
    }

    override suspend fun status(mode: TestMode, sessionId: String?): SessionProgressDto = withContext(Dispatchers.IO) {
        data.readOnline { it.findSession(mode, sessionId)?.progress() ?: SessionProgressDto() }
    }

    override suspend fun discard(mode: TestMode, sessionId: String) = withContext(Dispatchers.IO) {
        data.changeOnline { snapshot ->
            snapshot.session(mode, sessionId)
            snapshot.copy(sessions = snapshot.sessions - sessionId, flags = snapshot.flags - sessionId) to Unit
        }
    }

    private fun DemoSession.page(index: Int, downloaded: Boolean = false): SessionQuestionPageDto {
        val raw = fixtures.byId.getValue(session.questionIds[index])
        val saved = answers[index]
        val exam = session.mode == TestMode.EXAM.path
        val hidden = session.mode == TestMode.RECALL.path && saved?.preAnswer.isNullOrBlank()
        val feedback = !exam && !hidden && (downloaded || saved?.answer != null)
        val translations = if (hidden) JsonObject(emptyMap()) else if (feedback) raw.translations
            else JsonObject(raw.translations.mapValues { (_, value) -> JsonObject((value as JsonObject) - "explanation") })
        val stem = if (hidden) raw.localizedForDisplay().question else raw.question
        val question = SessionQuestionDto(raw.id, stem, if (hidden) emptyList() else raw.choices,
            correctAnswer = raw.correctAnswer.takeIf { feedback }, explanation = raw.explanation.takeIf { feedback },
            translations = translations, case = raw.case, choicesHidden = hidden)
        return SessionQuestionPageDto(index, session.questionIds.size, question, savedAnswer = saved?.answer,
            savedIsCorrect = saved?.answer?.let { it == raw.correctAnswer }.takeUnless { exam },
            savedErrorReason = saved?.errorReason, savedConfidence = saved?.confidence,
            savedPreAnswer = saved?.preAnswer, tag = session.tag)
    }

    private fun DemoSession.progress() = SessionProgressDto(
        sessionId = session.sessionId, questionIds = session.questionIds, currentIndex = session.currentIndex,
        answers = JsonObject(answers.mapNotNull { (index, slot) -> slot.answer?.let { index.toString() to JsonPrimitive(it) } }.toMap()),
        tag = session.tag, startedAt = session.startedAt, accumulatedTime = session.accumulatedTime,
        isActive = session.isActive, durationMinutes = session.durationMinutes,
    )

    private fun DemoData.withSession(item: DemoSession) = copy(sessions = sessions + (item.session.sessionId to item))
    private fun DemoData.findSession(mode: TestMode, id: String?) =
        (if (id == null) sessions.values.firstOrNull { it.session.mode == mode.path } else sessions[id])
            ?.takeIf { it.session.mode == mode.path }
    private fun DemoData.session(mode: TestMode, id: String) = findSession(mode, id) ?: throw demoMissing()
    private fun ExamSessionDto.elapsed(now: Instant): Int =
        (accumulatedTime.toLong() + (startedAt?.let { Duration.between(Instant.parse(it), now).seconds.coerceAtLeast(0) } ?: 0))
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
