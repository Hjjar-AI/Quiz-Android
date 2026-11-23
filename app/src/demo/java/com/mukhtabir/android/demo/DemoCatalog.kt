package com.mukhtabir.android.demo

import com.mukhtabir.android.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun invalidDemoRequest() = ApiException(400, "")
internal fun demoMissing() = ApiException(404, "")
internal fun demoConflict() = ApiException(409, "")
internal fun unsupportedDemoAction(): Nothing = throw ApiException(403, "")

internal fun List<QuestionDto>.matching(
    categories: List<Long> = emptyList(), tags: List<String> = emptyList(), difficulty: String? = null,
    verifiedOnly: Boolean = false, book: String? = null, include: Set<Long>? = null,
): List<QuestionDto> = filter { question ->
    (categories.isEmpty() || question.category in categories) &&
        (tags.isEmpty() || question.tags.any { it in tags }) &&
        (difficulty.isNullOrBlank() || question.difficulty == difficulty) &&
        (!verifiedOnly || question.verified) &&
        (book.isNullOrBlank() || question.sourceDocument == book) &&
        (include == null || question.id in include)
}

internal fun DemoData.latestAnswers(): List<SessionResultItemDto> =
    completions.values.sortedByDescending { it.completedAt }.flatMap { it.result.results }
        .filter { it.userAnswer != null }.distinctBy { it.questionId }

internal fun DemoData.reviewIds(): Set<Long> = latestAnswers()
    .filter { !it.isCorrect || (it.confidenceScore ?: 3) < 3 }.map { it.questionId }.toSet()

internal fun List<QuestionDto>.tagStats(): List<TagDto> = flatMap { it.tags }.distinct().sorted().map { name ->
    TagDto(name, count { name in it.tags }, count { name in it.tags && it.verified })
}

internal fun List<QuestionDto>.questionPage(page: Int, perPage: Int): QuestionPageDto {
    val size = perPage.coerceAtLeast(1)
    val pages = ((this.size - 1).coerceAtLeast(0) / size) + 1
    val requested = page.coerceIn(1, pages)
    return QuestionPageDto(drop((requested - 1) * size).take(size), this.size, requested, size, pages)
}

internal class DemoCatalog(private val store: DemoStore, private val fixtures: DemoFixtures) : LearnerQuestions {
    private suspend fun <T> read(block: (DemoData) -> T) = withContext(Dispatchers.IO) { store.readOnline(block) }
    private suspend fun write(block: (DemoData) -> DemoData) = withContext(Dispatchers.IO) {
        store.changeOnline { block(it) to Unit }
    }

    override suspend fun get(questionId: Long) = read { fixtures.byId[questionId] ?: throw demoMissing() }
    override suspend fun sourceBookFilters(name: String) = read {
        val rows = fixtures.questions.matching(book = name)
        SourceBookFiltersDto(name, fixtures.categories.filter { category -> rows.any { it.category == category.id } }, rows.tagStats())
    }
    override suspend fun sourceBooks() = read { SourceBookListDto(listOf(SourceBookDto(fixtures.book, fixtures.questions.size))) }
    override suspend fun tags() = read { fixtures.questions.tagStats().let { TagListDto(it, it.size) } }

    override suspend fun list(page: Int, perPage: Int, search: String?, categoryId: Long?, difficulty: String?,
        verifiedOnly: Boolean, tag: String?) = read {
        fixtures.questions.matching(listOfNotNull(categoryId), listOfNotNull(tag), difficulty, verifiedOnly)
            .filter { question ->
                search.isNullOrBlank() || question.localizedForDisplay().let { localized ->
                    sequenceOf(localized.question, question.question, localized.case?.stem)
                        .filterNotNull().any { it.contains(search, ignoreCase = true) }
                }
            }.questionPage(page, perPage)
    }

    override suspend fun unverified(page: Int, perPage: Int): QuestionPageDto = unsupportedDemoAction()
    override suspend fun verify(questionId: Long): Boolean = unsupportedDemoAction()

    override suspend fun availableCount(categoryIds: List<Long>, difficulty: String?, verifiedOnly: Boolean,
        useBookmarks: Boolean, useSrs: Boolean, tags: List<String>, blueprintId: Long?, sourceDocument: String?) = read { data ->
        if (blueprintId != null) unsupportedDemoAction()
        fixtures.questions.matching(categoryIds, tags, difficulty, verifiedOnly, sourceDocument,
            include = when {
                useBookmarks -> data.bookmarks
                useSrs -> data.reviewIds()
                else -> null
            }).size
    }

    override suspend fun attemptSummary() = read { data ->
        val rows = data.latestAnswers()
        AttemptSummaryDto(totalSeen = rows.size,
            everCorrect = data.completions.values.flatMap { it.result.results }.filter { it.isCorrect }.distinctBy { it.questionId }.size,
            wrongOpen = rows.count { !it.isCorrect }, fragileCorrect = rows.count { it.isCorrect && (it.confidenceScore ?: 3) < 3 },
            dueNow = data.reviewIds().size)
    }
    override suspend fun srsDueCount() = read { it.reviewIds().size }
    override suspend fun mistakes(page: Int, perPage: Int) = read { data ->
        data.latestAnswers().filter { !it.isCorrect }.mapNotNull { fixtures.byId[it.questionId] }.questionPage(page, perPage)
    }
    override suspend fun fragile(page: Int, perPage: Int) = read { data ->
        data.latestAnswers().filter { it.isCorrect && (it.confidenceScore ?: 3) < 3 }
            .mapNotNull { fixtures.byId[it.questionId] }.questionPage(page, perPage)
    }
    override suspend fun studyNow(limit: Int) = read { data ->
        val review = data.reviewIds()
        val ids = (review + fixtures.byId.keys).take(limit.coerceAtLeast(0))
        StudyNowDto(ids, ids.size)
    }

    override suspend fun ratings(ids: List<Long>) = read { data ->
        ids.filter { it in fixtures.byId }.associateWith { id ->
            val value = data.ratings[id] ?: 0
            QuestionRatingRowDto(id, value, value.toDouble(), if (value == 0) 0 else 1)
        }
    }
    override suspend fun rating(questionId: Long) = read { data ->
        val value = data.ratings[questionId] ?: 0
        QuestionRatingDto(value, value.toDouble(), if (value == 0) 0 else 1)
    }
    override suspend fun rate(questionId: Long, rating: Int) = write {
        if (questionId !in fixtures.byId || rating !in 1..5) throw invalidDemoRequest()
        it.copy(ratings = it.ratings + (questionId to rating))
    }
    override suspend fun reportState(questionId: Long) = read { data ->
        data.reports[questionId].let { ReportStateDto(it != null, it) }
    }
    override suspend fun report(questionId: Long, reason: String) = write {
        if (questionId !in fixtures.byId) throw demoMissing()
        it.copy(reports = it.reports + (questionId to reason))
    }

    override suspend fun delete(questionId: Long, version: Int): Unit = unsupportedDemoAction()
    override suspend fun duplicate(questionId: Long, operationId: String): QuestionDto = unsupportedDemoAction()
    override suspend fun duplicateReceipt(questionId: Long, operationId: String): QuestionDto = unsupportedDemoAction()
}

internal class DemoBookmarks(private val store: DemoStore, private val fixtures: DemoFixtures) : LearnerBookmarks {
    private suspend fun <T> read(block: (DemoData) -> T) = withContext(Dispatchers.IO) { store.readOnline(block) }

    override suspend fun list() = read { data ->
        val rows = fixtures.questions.matching(include = data.bookmarks)
        BookmarkListDto(rows, rows.size)
    }
    override suspend fun page(page: Int, perPage: Int) = read { fixtures.questions.matching(include = it.bookmarks).questionPage(page, perPage) }
    override suspend fun count() = read { it.bookmarks.size }
    override suspend fun state(questionId: Long) = read { questionId in it.bookmarks }
    override suspend fun set(questionId: Long, added: Boolean): Boolean = withContext(Dispatchers.IO) {
        store.changeOnline { data ->
            if (questionId !in fixtures.byId) throw demoMissing()
            data.copy(bookmarks = if (added) data.bookmarks + questionId else data.bookmarks - questionId) to added
        }
    }
}
