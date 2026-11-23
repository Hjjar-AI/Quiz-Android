package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TestSessionService(private val api: ApiClient) : LearnerSessions {
    override suspend fun start(mode: TestMode, request: StartSessionRequest): ExamSessionDto = api.decode(
        ExamSessionDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "start/${mode.path}"),
            api.encode(StartSessionRequest.serializer(), request),
        ),
    )

    override suspend fun question(mode: TestMode, sessionId: String): SessionQuestionPageDto = api.decode(
        SessionQuestionPageDto.serializer(),
        api.get(ApiEndpoints.test(mode, "question"), mapOf("session_id" to sessionId)),
    )

    override suspend fun submit(mode: TestMode, request: SubmitAnswerRequest): AnswerSubmissionDto = api.decode(
        AnswerSubmissionDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "answer"),
            api.encode(SubmitAnswerRequest.serializer(), request),
        ),
    )

    override suspend fun questions(mode: TestMode, sessionId: String): SessionQuestionBundleDto = api.decode(
        SessionQuestionBundleDto.serializer(),
        api.get(ApiEndpoints.test(mode, "questions"), mapOf("session_id" to sessionId)),
    )

    override suspend fun finish(mode: TestMode, sessionId: String): SessionResultsDto = api.decode(
        SessionResultsDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "results"),
            buildJsonObject { put("session_id", sessionId) },
        ),
    )

    override suspend fun completedResult(mode: TestMode, sessionId: String): SessionResultsDto {
        val result = api.decode(SessionResultsDto.serializer(), api.get(
            ApiEndpoints.test(mode, "results"), mapOf("session_id" to sessionId, "mode" to mode.path)))
        check(result.sourceSessionId == sessionId && result.mode == mode.path && (result.historyId ?: 0) > 0)
        return result
    }

    override suspend fun pause(mode: TestMode, sessionId: String) {
        api.post(
            ApiEndpoints.test(mode, "pause"),
            buildJsonObject { put("session_id", sessionId) },
        )
    }

    override suspend fun resume(mode: TestMode, sessionId: String?): SessionProgressDto = api.decode(
        SessionProgressDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "resume"),
            buildJsonObject {
                sessionId?.let { put("session_id", it) }
                put("mode", mode.path)
            },
        ),
    )

    override suspend fun status(mode: TestMode, sessionId: String?): SessionProgressDto = api.decode(
        SessionProgressDto.serializer(),
        api.get(
            ApiEndpoints.test(mode, "status"),
            mapOf(
                "session_id" to sessionId,
                "mode" to mode.path.takeIf { sessionId == null },
            ),
        ),
    )

    override suspend fun discard(mode: TestMode, sessionId: String) {
        api.delete(
            ApiEndpoints.test(mode, "discard"),
            query = mapOf("session_id" to sessionId),
        )
    }
}
