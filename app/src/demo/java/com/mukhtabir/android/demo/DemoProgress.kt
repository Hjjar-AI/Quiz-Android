package com.mukhtabir.android.demo

import com.mukhtabir.android.data.remote.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DemoHistory(private val store: DemoStore) : LearnerHistory {
    override suspend fun list(page: Int, perPage: Int, userId: Long?): HistoryPageDto = withContext(Dispatchers.IO) {
        store.readOnline { data ->
            if (userId != null && userId != SAMPLE_USER_ID) unsupportedDemoAction()
            val rows = data.completions.values.sortedByDescending { it.completedAt }.map { completion ->
                val result = completion.result
                HistoryItemDto(requireNotNull(result.historyId), user = SAMPLE_USER_ID, mode = requireNotNull(result.mode),
                    tag = completion.tag, totalQuestions = result.totalQuestions, answeredCount = result.answeredCount,
                    correctCount = result.correctCount, accuracy = result.accuracy, timeSpent = result.totalTime,
                    completedAt = completion.completedAt)
            }
            val size = perPage.coerceAtLeast(1)
            val pages = ((rows.size - 1).coerceAtLeast(0) / size) + 1
            val requested = page.coerceIn(1, pages)
            HistoryPageDto(rows.drop((requested - 1) * size).take(size), rows.size, requested, size, pages)
        }
    }

    override suspend fun detail(id: Long): HistoryDetailDto = withContext(Dispatchers.IO) {
        store.readOnline { data ->
            val completion = data.completions.values.firstOrNull { it.result.historyId == id } ?: throw demoMissing()
            val result = completion.result
            HistoryDetailDto(id, requireNotNull(result.mode), completion.tag, result.totalQuestions, result.answeredCount,
                result.correctCount, result.accuracy, result.totalTime, completion.startedAt, completion.completedAt, result.results)
        }
    }
}

internal class DemoPlanner(private val store: DemoStore, private val fixtures: DemoFixtures) : LearnerPlanner {
    override suspend fun get(): PlannerDto = withContext(Dispatchers.IO) { store.readOnline { it.plannerForToday() } }

    override suspend fun update(request: PlannerWriteRequest): PlannerDto = withContext(Dispatchers.IO) {
        store.changeOnline { data ->
            val prior = data.plannerForToday()
            if (request.expectedId != prior.id || request.expectedVersion != prior.version) throw demoConflict()
            val start = request.startDate ?: prior.startDate
            val end = request.endDate
            if (request.targetQuestionsPerDay !in 1..200 || (end != null && end < start) ||
                !fixtures.categories.map { it.id }.containsAll(request.targetCategories)) throw invalidDemoRequest()
            val next = PlannerDto(prior.id.takeIf { it > 0 } ?: data.nextPlannerId, prior.version + 1, request.targetQuestionsPerDay, request.targetCategories,
                fixtures.canonicalTags(request.targetTags), start, end, today = prior.today)
            val updated = data.copy(planner = next, nextPlannerId = maxOf(data.nextPlannerId, next.id + 1))
            updated to updated.plannerForToday()
        }
    }

    override suspend fun delete(id: Long, version: Int): Unit = withContext(Dispatchers.IO) {
        store.changeOnline { data ->
            val prior = data.planner ?: throw demoMissing()
            if (id != prior.id || version != prior.version) throw demoConflict()
            data.copy(planner = null) to Unit
        }
    }

    override suspend fun recordProgress() = withContext(Dispatchers.IO) {
        store.changeOnline { it to Unit }
    }

    private fun DemoData.plannerForToday(): PlannerDto {
        val today = LocalDate.now().toString()
        val plan = planner ?: return PlannerDto(0, 0, startDate = today, today = today)
        val tags = fixtures.localizedTags(plan.targetTags)
        val allowed = fixtures.questions.matching(plan.targetCategories, tags).map { it.id }.toSet()
        val progress = completions.values.groupBy { it.day() }
            .filterKeys { it >= plan.startDate && (plan.endDate?.let { end -> it <= end } ?: true) }
            .mapValues { (_, entries) -> entries.sumOf { entry -> entry.result.results.count {
                it.userAnswer != null && it.questionId in allowed
            } } }
        return plan.copy(today = today, dailyProgress = progress, targetTags = tags)
    }
}

internal class DemoAnalytics(private val store: DemoStore, private val fixtures: DemoFixtures) : LearnerAnalytics {
    private suspend fun <T> read(block: (DemoData) -> T) = withContext(Dispatchers.IO) { store.readOnline(block) }

    override suspend fun summary(days: Int) = read { data ->
        val rows = data.inDays(days)
        val attempts = rows.flatMap { it.result.results }.filter { it.userAnswer != null }
        val fragile = attempts.count { it.isCorrect && (it.confidenceScore ?: 3) < 3 }
        AnalyticsSummaryDto(
            userPerformance = rows.groupBy { it.day() }.toSortedMap().map { (day, records) ->
                val answers = records.flatMap { it.result.results }.filter { it.userAnswer != null }
                PerformancePointDto(day, answers.accuracy(), answers.size)
            },
            confidenceStats = ConfidenceStatsDto(attempts.size, attempts.count { it.isCorrect } - fragile,
                fragile, data.latestAnswers().count { !it.isCorrect }, if (attempts.isEmpty()) 0.0 else 100.0 * fragile / attempts.size),
            categoryCoverage = fixtures.categories.map { category ->
                val questions = fixtures.questions.filter { it.category == category.id }
                CategoryCoverageDto(category.id, category.name, questions.size, questions.count { it.verified },
                    attempts.filter { it.categoryId == category.id }.accuracy(), category.color)
            },
            tagCoverage = fixtures.questions.tagStats().map { TagCoverageDto(it.name, it.count, it.verifiedCount) },
            difficultyStats = attempts.groupBy { it.difficulty.orEmpty() }.map { (difficulty, answers) ->
                DifficultyStatsDto(difficulty, answers.size, answers.accuracy())
            },
        )
    }

    override suspend fun categoryMastery(minAttempts: Int?, top: Int?) = read { data ->
        val attempts = data.completions.values.flatMap { it.result.results }.filter { it.userAnswer != null }
        val rows = fixtures.categories.mapNotNull { category ->
            val answers = attempts.filter { it.categoryId == category.id }
            if (answers.size < (minAttempts ?: 1)) return@mapNotNull null
            val accuracy = answers.accuracy()
            val total = fixtures.questions.count { it.category == category.id }
            val unique = answers.distinctBy { it.questionId }.size
            CategoryMasteryItemDto(category.id, category.name, category.color, unique, total, 100.0 * unique / total,
                answers.size, answers.count { !it.isCorrect }, accuracy, accuracy, accuracy >= 80)
        }.sortedByDescending { it.accuracy }
        CategoryMasteryDto(rows.take(top?.coerceAtLeast(0) ?: rows.size), rows.size)
    }

    override suspend fun streakHistory(days: Int) = read { data -> data.streak(days) }

    suspend fun learnerProfile() = read { data ->
        val streak = data.streak(365)
        val lastExam = data.completions.values.filter { it.result.mode == TestMode.EXAM.path }.maxByOrNull { it.completedAt }
        fixtures.user.copy(currentStreak = streak.currentStreak, longestStreak = streak.longestStreak,
            lastStudyDate = streak.days.lastOrNull()?.date,
            latestExamAccuracy = lastExam?.result?.accuracy, latestExamTag = lastExam?.tag)
    }

    override suspend fun activeUsers(days: Int): List<ActiveUsersPointDto> = unsupportedDemoAction()
    override suspend fun difficultyCalibration(): ReportRowsDto = unsupportedDemoAction()
    override suspend fun authorFlagRate(): ReportRowsDto = unsupportedDemoAction()
    override suspend fun cohortComparison(): ReportRowsDto = unsupportedDemoAction()
    override suspend fun retention(): ReportRowsDto = unsupportedDemoAction()
    override suspend fun examDuration(): ExamDurationReportDto = unsupportedDemoAction()
}

private fun DemoCompletion.day() = Instant.parse(completedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()

private fun DemoData.inDays(days: Int): List<DemoCompletion> {
    val since = LocalDate.now().minusDays(days.coerceAtLeast(1).toLong() - 1L).toString()
    return completions.values.filter { it.day() >= since }
}

private fun DemoData.streak(days: Int): StreakHistoryDto {
    val rows = inDays(days)
    val dates = rows.groupBy { it.day() }.toSortedMap()
    val activity = dates.map { (day, records) -> StreakDayDto(day, records.size, records.sumOf { it.result.answeredCount }) }
    var longest = 0
    var current = 0
    var previous: LocalDate? = null
    for (day in activity) {
        val date = LocalDate.parse(day.date)
        current = if (previous?.plusDays(1) == date) current + 1 else 1
        longest = maxOf(longest, current)
        previous = date
    }
    if (previous?.isBefore(LocalDate.now().minusDays(1)) != false) current = 0
    val items = rows.flatMap { it.result.results }.filter { it.userAnswer != null }
    return StreakHistoryDto(activity, current, longest, activity.size, rows.size,
        items.size, items.distinctBy { it.questionId }.size, dueReviews = reviewIds().size)
}

private fun List<SessionResultItemDto>.accuracy(): Double = if (isEmpty()) 0.0 else 100.0 * count { it.isCorrect } / size
