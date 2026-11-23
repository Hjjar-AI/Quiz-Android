package com.mukhtabir.android.demo

import com.mukhtabir.android.data.remote.*
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DemoOffline(private val store: DemoStore, private val fixtures: DemoFixtures) : LearnerOffline {
    override suspend fun filterOptions() = withContext(Dispatchers.IO) {
        store.readOnline {
            OfflineFilterOptions(listOf(SourceBookDto(fixtures.book, fixtures.questions.size)), fixtures.categories, fixtures.questions.tagStats())
        }
    }

    override suspend fun catalog(after: Long, upperBound: Long?, selection: OfflineSelection): OfflineCatalogPage = withContext(Dispatchers.IO) {
        store.readOnline {
            val max = upperBound ?: fixtures.questions.maxOf { it.id }
            val items = fixtures.questions.matching(selection.categories, selection.tags, book = selection.book)
                .map { question -> question.id }.filter { id -> id <= max }
            OfflineCatalogPage(items.filter { id -> id > after }, max, total = items.size, excluded = 0, selection = selection)
        }
    }

    override suspend fun downloadPermission() = withContext(Dispatchers.IO) { store.readOnline { fixtures.user } }

    override suspend fun download(ids: List<Long>, fullBank: Boolean, includeImages: Boolean): OfflinePack = withContext(Dispatchers.IO) {
        store.changeOnline { data ->
            if (fullBank || ids.isEmpty() || ids.size > SAMPLE_LIMIT || ids.distinct() != ids) throw invalidDemoRequest()
            val rows = ids.map { fixtures.byId[it] ?: throw demoMissing() }
            val id = UUID.randomUUID().toString()
            data to OfflinePack(id, id, rows, issuedAt = Instant.now().toString(), imagesIncluded = includeImages,
                contentRevision = "demo-fixtures")
        }
    }

    override suspend fun upload(material: OfflineMaterial): SessionResultsDto = withContext(Dispatchers.IO) {
        store.changeOnline(DemoFailure.UPLOAD) { data ->
            val id = material.completionId ?: throw invalidDemoRequest()
            val elapsed = material.frozenTime ?: throw invalidDemoRequest()
            val uploaded = DemoUpload(material.pack.id, material.answers.sortedBy { it.questionId }, elapsed, material.completedAt)
            data.completions[id]?.let { prior ->
                if (prior.upload != uploaded) throw demoConflict()
                return@changeOnline data to prior.result
            }
            val saved = data.offline.firstOrNull { it.pack.id == material.pack.id } ?: throw demoMissing()
            val questions = saved.pack.questions
            val byQuestion = material.answers.associateBy { it.questionId }
            if (byQuestion.size != material.answers.size || byQuestion.keys != questions.map { it.id }.toSet() ||
                saved.completionId != id || saved.frozenTime != elapsed || saved.answers != material.answers || saved.completedAt != material.completedAt) {
                throw invalidDemoRequest()
            }
            val answers = questions.map { question ->
                val answer = byQuestion.getValue(question.id)
                if (answer.answer !in 1..question.choices.size || answer.confidence !in 1..3) throw invalidDemoRequest()
                SessionAnswerBaseline(answer.answer, answer.confidence)
            }
            val completedAt = material.completedAt ?: Instant.now().toString()
            val result = buildDemoResult(id, data.nextHistoryId, TestMode.STUDY.path, questions, answers, elapsed)
            val completion = DemoCompletion(result, null, completedAt, material.displayTitle ?: fixtures.book, uploaded)
            data.copy(completions = data.completions + (id to completion), nextHistoryId = data.nextHistoryId + 1) to result
        }
    }
}
