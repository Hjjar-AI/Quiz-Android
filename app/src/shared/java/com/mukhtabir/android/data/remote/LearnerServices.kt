package com.mukhtabir.android.data.remote

/** Service contracts used by screens shared by both applications. */
interface LearnerQuestions {
    suspend fun get(questionId: Long): QuestionDto
    suspend fun sourceBookFilters(name: String): SourceBookFiltersDto
    suspend fun sourceBooks(): SourceBookListDto
    suspend fun tags(): TagListDto
    suspend fun list(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
        categoryId: Long? = null,
        difficulty: String? = null,
        verifiedOnly: Boolean = false,
        tag: String? = null,
    ): QuestionPageDto
    suspend fun unverified(page: Int = 1, perPage: Int = 20): QuestionPageDto
    suspend fun verify(questionId: Long): Boolean
    suspend fun availableCount(
        categoryIds: List<Long> = emptyList(),
        difficulty: String? = null,
        verifiedOnly: Boolean = false,
        useBookmarks: Boolean = false,
        useSrs: Boolean = false,
        tags: List<String> = emptyList(),
        blueprintId: Long? = null,
        sourceDocument: String? = null,
    ): Int
    suspend fun attemptSummary(): AttemptSummaryDto
    suspend fun srsDueCount(): Int
    suspend fun mistakes(page: Int = 1, perPage: Int = 20): QuestionPageDto
    suspend fun fragile(page: Int = 1, perPage: Int = 20): QuestionPageDto
    suspend fun studyNow(limit: Int = 20): StudyNowDto
    suspend fun ratings(ids: List<Long>): Map<Long, QuestionRatingRowDto>
    suspend fun rating(questionId: Long): QuestionRatingDto
    suspend fun rate(questionId: Long, rating: Int)
    suspend fun reportState(questionId: Long): ReportStateDto
    suspend fun report(questionId: Long, reason: String)
    suspend fun delete(questionId: Long, version: Int)
    suspend fun duplicate(questionId: Long, operationId: String): QuestionDto
    suspend fun duplicateReceipt(questionId: Long, operationId: String): QuestionDto
}

interface LearnerSessions {
    suspend fun start(mode: TestMode, request: StartSessionRequest): ExamSessionDto
    suspend fun question(mode: TestMode, sessionId: String): SessionQuestionPageDto
    suspend fun submit(mode: TestMode, request: SubmitAnswerRequest): AnswerSubmissionDto
    suspend fun questions(mode: TestMode, sessionId: String): SessionQuestionBundleDto
    suspend fun finish(mode: TestMode, sessionId: String): SessionResultsDto
    suspend fun completedResult(mode: TestMode, sessionId: String): SessionResultsDto
    suspend fun pause(mode: TestMode, sessionId: String)
    suspend fun resume(mode: TestMode, sessionId: String? = null): SessionProgressDto
    suspend fun status(mode: TestMode, sessionId: String? = null): SessionProgressDto
    suspend fun discard(mode: TestMode, sessionId: String)
}

interface LearnerBookmarks {
    suspend fun list(): BookmarkListDto
    suspend fun page(page: Int = 1, perPage: Int = 10): QuestionPageDto
    suspend fun count(): Int
    suspend fun state(questionId: Long): Boolean
    suspend fun set(questionId: Long, added: Boolean): Boolean
}

interface LearnerCategories {
    suspend fun list(): CategoryListDto
}

interface LearnerBlueprints {
    suspend fun list(): BlueprintListDto
}

interface LearnerImages {
    suspend fun load(url: String): ByteArray
}

interface LearnerHistory {
    suspend fun list(
        page: Int = 1,
        perPage: Int = 20,
        userId: Long? = null,
    ): HistoryPageDto
    suspend fun detail(id: Long): HistoryDetailDto
}

interface LearnerAnalytics {
    suspend fun summary(days: Int = 30): AnalyticsSummaryDto
    suspend fun activeUsers(days: Int = 30): List<ActiveUsersPointDto>
    suspend fun categoryMastery(
        minAttempts: Int? = null,
        top: Int? = null,
    ): CategoryMasteryDto
    suspend fun streakHistory(days: Int = 30): StreakHistoryDto
    suspend fun difficultyCalibration(): ReportRowsDto
    suspend fun authorFlagRate(): ReportRowsDto
    suspend fun cohortComparison(): ReportRowsDto
    suspend fun retention(): ReportRowsDto
    suspend fun examDuration(): ExamDurationReportDto
}

interface LearnerPlanner {
    suspend fun get(): PlannerDto
    suspend fun update(request: PlannerWriteRequest): PlannerDto
    suspend fun delete(id: Long, version: Int)
    suspend fun recordProgress()
}

interface LearnerTips {
    suspend fun forLocale(locale: String): TipsResponseDto
}

interface LearnerNotices {
    suspend fun notices(): MasterExamNoticesDto
}

interface LearnerOffline {
    suspend fun filterOptions(): OfflineFilterOptions
    suspend fun catalog(after: Long, upperBound: Long?, selection: OfflineSelection = OfflineSelection()): OfflineCatalogPage
    suspend fun downloadPermission(): UserDto
    suspend fun download(ids: List<Long>, fullBank: Boolean = false, includeImages: Boolean = true): OfflinePack
    suspend fun upload(material: OfflineMaterial): SessionResultsDto
}
