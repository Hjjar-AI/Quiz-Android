package com.mukhtabir.android.ui.demo

import android.app.Application
import android.os.Bundle
import android.os.Parcel
import androidx.compose.runtime.saveable.SaverScope
import java.io.Serializable
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class DemoRepositoryTest {
    @Test
    fun `seeded history has consistent scores totals and no credit today`() {
        val repo = DemoRepository()
        assertEquals(2, repo.state.records.size)
        assertEquals(listOf(2, 2), repo.state.records.map { repo.correct(it.session) })
        assertEquals(6, repo.answered)
        assertEquals(66, repo.accuracy)
        assertEquals(0, repo.todayAnswered)
        assertTrue(repo.state.records.all { it.date < LocalDate.now().toString() })
    }

    @Test
    fun `bookmark changes are shared across destinations and tolerate unknown ids`() {
        val repo = DemoRepository()
        repo.toggleBookmark(2)
        repo.toggleBookmark(4)
        repo.toggleBookmark(0)
        repo.toggleBookmark(999)
        assertEquals(setOf(4, 6), repo.state.bookmarks)
        repo.start(DemoMode.STUDY, repo.ids.filter { it in repo.state.bookmarks })
        assertEquals(listOf(4, 6), repo.state.session.questionIds)
        assertEquals(setOf(4, 6), repo.state.bookmarks)
    }

    @Test
    fun `filtered selection keeps chosen order and removes duplicates and invalid ids`() {
        val repo = DemoRepository()
        repo.start(DemoMode.EXAM, listOf(4, 999, 2, 4, -1, 8))
        assertEquals(listOf(4, 2, 8), repo.state.session.questionIds)
        assertEquals(listOf(-1, -1, -1), repo.state.session.answers)
        assertEquals(DemoMode.EXAM, repo.state.session.mode)
    }

    @Test
    fun `empty selection does not consume a session identity or replace a result`() {
        val repo = DemoRepository()
        repo.review(repo.state.records.first())
        val before = repo.state
        repo.start(DemoMode.STUDY, listOf(0, -1, 999))
        assertSame(before, repo.state)
    }

    @Test
    fun `starting or reviewing cannot overwrite paused answers notes and flags`() {
        val repo = DemoRepository()
        repo.start(DemoMode.RECALL, listOf(4, 2))
        repo.updateSession(repo.state.session.copy(recalled = listOf("My recall", ""), revealed = setOf(0), flagged = setOf(1)).answer(3))
        val before = repo.state
        repo.start(DemoMode.EXAM, listOf(1, 8))
        assertFalse(repo.review(repo.state.records.first()))
        assertSame(before, repo.state)
    }

    @Test
    fun `finishing updates all summary counts and keeps history frozen exactly once`() {
        val repo = DemoRepository()
        repo.start(DemoMode.EXAM, listOf(4, 2, 8))
        val answered = repo.state.session.answer(3).copy(index = 1).answer(0)
        repo.updateSession(answered)
        assertEquals(6, repo.answered)
        assertEquals(0, repo.todayAnswered)
        val finished = answered.finish()
        repo.updateSession(finished)
        assertEquals(3, repo.state.records.size)
        assertEquals(8, repo.answered)
        assertEquals(2, repo.todayAnswered)
        assertEquals(62, repo.accuracy)
        val frozen = repo.state.records.first()
        assertEquals(1, repo.correct(frozen.session))
        assertEquals(LocalDate.now().toString(), frozen.date)
        repo.updateSession(finished.copy(page = DemoPage.REVIEW, index = 2))
        repo.updateSession(finished)
        assertEquals(3, repo.state.records.size)
        assertEquals(frozen, repo.state.records.first())
        assertEquals(2, repo.todayAnswered)
    }

    @Test
    fun `unanswered finish contributes a session but no answered-question accuracy credit`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repo.start(DemoMode.STUDY, listOf(4, 2))
        repo.updateSession(repo.state.session.finish())
        assertEquals(1, repo.state.records.size)
        assertEquals(0, repo.answered)
        assertEquals(0, repo.todayAnswered)
        assertEquals(0, repo.accuracy)
    }

    @Test
    fun `discard removes only active work and ignores late updates from it`() {
        val repo = DemoRepository()
        repo.toggleBookmark(4)
        repo.setTarget(15)
        repo.start(DemoMode.STUDY, listOf(4, 2))
        val late = repo.state.session.answer(3).finish()
        val history = repo.state.records
        repo.discard()
        repo.updateSession(late)
        assertEquals(DemoPage.HOME, repo.state.session.page)
        assertEquals(history, repo.state.records)
        assertEquals(setOf(2, 4, 6), repo.state.bookmarks)
        assertEquals(15, repo.state.target)
    }

    @Test
    fun `a previous session response cannot modify the next session`() {
        val repo = DemoRepository()
        repo.start(DemoMode.STUDY, listOf(4))
        val old = repo.state.session.answer(3).finish()
        repo.updateSession(old)
        repo.start(DemoMode.STUDY, listOf(8))
        val next = repo.state
        repo.updateSession(old)
        assertSame(next, repo.state)
        assertTrue(next.session.id > old.id)
    }

    @Test
    fun `history opens the saved selection and answers without recording another finish`() {
        val repo = DemoRepository()
        val record = repo.state.records.first()
        assertTrue(repo.review(record))
        assertEquals(record.session.copy(index = 0, page = DemoPage.REVIEW), repo.state.session)
        repo.updateSession(repo.state.session.copy(index = 1, page = DemoPage.RESULTS))
        assertEquals(2, repo.state.records.size)
        assertEquals(6, repo.answered)
    }

    @Test
    fun `planner validates bounds and reaches its goal only on completed answers`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repo.setTarget(1000)
        repo.setTarget(0)
        repo.setTarget(1001)
        assertEquals(1000, repo.state.target)
        repo.setTarget(1)
        repo.start(DemoMode.STUDY, listOf(4))
        repo.updateSession(repo.state.session.answer(3))
        assertTrue(repo.todayAnswered < repo.state.target)
        repo.updateSession(repo.state.session.finish())
        assertTrue(repo.todayAnswered >= repo.state.target)
    }

    @Test
    fun `history retention is bounded and session ids stay unique`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repeat(23) {
            repo.start(DemoMode.STUDY, listOf(4))
            repo.updateSession(repo.state.session.answer(3).finish())
        }
        assertEquals(20, repo.state.records.size)
        assertEquals(20, repo.state.records.map { it.session.id }.distinct().size)
        assertEquals(20, repo.answered)
        assertEquals(100, repo.accuracy)
    }

    @Test
    fun `saved-state saver restores a parcelled repository with notes history and next identity`() {
        val repo = DemoRepository()
        repo.toggleBookmark(4)
        repo.setTarget(15)
        repo.start(DemoMode.RECALL, listOf(4, 8))
        repo.updateSession(repo.state.session.copy(revealed = setOf(0), flagged = setOf(1),
            recalled = listOf("A recalled answer", "")).answer(3))
        val saver = DemoRepository.StateSaver
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = value is Serializable }
        val saved = requireNotNull(with(saver) { scope.save(repo) })
        val restored = requireNotNull(saver.restore(parcelRoundTrip(saved)))
        assertEquals(repo.state, restored.state)
        restored.updateSession(restored.state.session.finish())
        assertEquals(3, restored.state.records.size)
        assertEquals(2, repo.state.records.size)
        restored.start(DemoMode.STUDY, listOf(2))
        assertEquals(repo.state.nextId, restored.state.session.id)
    }

    @Test
    fun `a fresh demo resets only sample data without sharing repository mutations`() {
        val old = DemoRepository()
        old.toggleBookmark(2)
        old.setTarget(55)
        old.start(DemoMode.STUDY, listOf(1))
        old.updateSession(old.state.session.answer(0).finish())
        val fresh = DemoRepository()
        assertEquals(setOf(2, 6), fresh.state.bookmarks)
        assertEquals(10, fresh.state.target)
        assertEquals(2, fresh.state.records.size)
        assertEquals(DemoPage.HOME, fresh.state.session.page)
    }

    @Test
    fun `disconnected reads keep sample data and require an explicit retry after reconnect`() {
        val repo = DemoRepository()
        repo.rate(2, 4)
        val original = repo.state
        repo.setConnected(false)
        repo.refresh()
        assertTrue(repo.state.readFailed)
        repo.start(DemoMode.STUDY, listOf(1))
        repo.download(listOf(8))
        repo.rate(2, 1)
        assertEquals(DemoPage.HOME, repo.state.session.page)
        assertEquals(original.nextId, repo.state.nextId)
        assertEquals(original.downloadedIds, repo.state.downloadedIds)
        assertEquals(original.ratings, repo.state.ratings)
        assertEquals(original.records, repo.state.records)
        assertEquals(original.bookmarks, repo.state.bookmarks)
        repo.setConnected(true)
        assertTrue(repo.state.readFailed)
        repo.refresh()
        assertFalse(repo.state.readFailed)
        repo.start(DemoMode.STUDY, listOf(1))
        assertEquals(DemoPage.SESSION, repo.state.session.page)
    }

    @Test
    fun `active online work locks connection and collection replacement until discarded`() {
        val repo = DemoRepository()
        repo.start(DemoMode.STUDY, listOf(1))
        repo.updateSession(repo.state.session.answer(0).withConfidence(2))
        val active = repo.state
        repo.setConnected(false)
        repo.download(listOf(8))
        repo.start(DemoMode.STUDY, listOf(2), downloaded = true)
        assertSame(active, repo.state)
        repo.discard()
        repo.setConnected(false)
        assertFalse(repo.state.connected)
        assertEquals(active.downloadedIds, repo.state.downloadedIds)
    }

    @Test
    fun `download selection retains exact valid order and an invalid replacement preserves the collection`() {
        val repo = DemoRepository()
        repo.download(listOf(8, 4, 8, 0, 999))
        assertEquals(listOf(8, 4), repo.state.downloadedIds)
        val downloaded = repo.state
        repo.download(listOf(0, 999))
        assertSame(downloaded, repo.state)
        repo.setConnected(false)
        repo.start(DemoMode.EXAM, listOf(8), downloaded = true)
        repo.start(DemoMode.RECALL, listOf(8), downloaded = true)
        assertEquals(DemoPage.HOME, repo.state.session.page)
        repo.start(DemoMode.STUDY, listOf(8, 1, 4, 8), downloaded = true)
        assertEquals(listOf(8, 4), repo.state.session.questionIds)
        assertTrue(repo.state.session.downloaded)
        repo.setConnected(true)
        assertTrue(repo.state.connected)
        assertEquals(listOf(8, 4), repo.state.session.questionIds)
    }

    @Test
    fun `offline completion remains frozen without progress credit until explicit idempotent upload`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repo.setConnected(false)
        repo.start(DemoMode.STUDY, listOf(1), downloaded = true)
        val finished = repo.state.session.answer(0).withConfidence(1).finish()
        repo.updateSession(finished)
        val frozen = repo.pendingRecords.single()
        assertEquals(0, repo.answered)
        assertEquals(0, repo.todayAnswered)
        assertEquals(0, repo.planTodayAnswered)
        assertTrue(repo.poolIds(DemoPool.FRAGILE).isEmpty())
        repo.updateSession(finished.copy(page = DemoPage.REVIEW))
        repo.sync()
        assertEquals(listOf(frozen), repo.pendingRecords)
        repo.setConnected(true)
        assertEquals(listOf(frozen), repo.pendingRecords)
        repo.sync()
        assertTrue(repo.pendingRecords.isEmpty())
        assertEquals(frozen.copy(synced = true), repo.state.records.single())
        assertEquals(1, repo.answered)
        assertEquals(1, repo.todayAnswered)
        assertEquals(1, repo.planTodayAnswered)
        assertEquals(listOf(1), repo.poolIds(DemoPool.FRAGILE))
        val confirmed = repo.state.records
        repo.sync()
        repo.updateSession(finished)
        assertEquals(confirmed, repo.state.records)
        assertEquals(1, repo.answered)
    }

    @Test
    fun `pending capacity blocks another downloaded start without dropping any saved completion`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repo.setConnected(false)
        repeat(10) {
            repo.start(DemoMode.STUDY, listOf(1), downloaded = true)
            repo.updateSession(repo.state.session.answer(0).withConfidence(3).finish())
        }
        val full = repo.state
        repo.start(DemoMode.STUDY, listOf(2), downloaded = true)
        assertSame(full, repo.state)
        assertEquals(10, repo.pendingRecords.size)
        assertEquals(10, repo.pendingRecords.map { it.session.id }.distinct().size)
        repo.setConnected(true)
        repo.sync()
        repo.start(DemoMode.STUDY, listOf(2), downloaded = true)
        assertEquals(DemoPage.SESSION, repo.state.session.page)
        assertEquals(full.nextId, repo.state.session.id)
    }

    @Test
    fun `confirmed history retention preserves older pending completions and newest answer ordering`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        repo.start(DemoMode.STUDY, listOf(1), downloaded = true)
        repo.updateSession(repo.state.session.answer(0).withConfidence(1).finish())
        val pending = repo.pendingRecords.single()
        repeat(23) {
            repo.start(DemoMode.STUDY, listOf(1))
            repo.updateSession(repo.state.session.answer(1).withConfidence(3).finish())
        }
        assertEquals(21, repo.state.records.size)
        assertEquals(listOf(pending), repo.pendingRecords)
        assertTrue(repo.state.records.zipWithNext().all { (a, b) -> a.session.id > b.session.id })
        assertEquals(listOf(1), repo.poolIds(DemoPool.MISTAKES))
        assertTrue(repo.poolIds(DemoPool.FRAGILE).isEmpty())
    }

    @Test
    fun `review notebooks follow the latest answered result and ignore unanswered newer attempts`() {
        val repo = DemoRepository()
        assertEquals(listOf(3, 7), repo.poolIds(DemoPool.MISTAKES))
        assertEquals(listOf(6), repo.poolIds(DemoPool.FRAGILE))
        repo.start(DemoMode.STUDY, listOf(6, 3))
        repo.updateSession(repo.state.session.answer(0).withConfidence(3).copy(index = 1).answer(2).withConfidence(3).finish())
        assertEquals(listOf(6, 7), repo.poolIds(DemoPool.MISTAKES))
        assertTrue(repo.poolIds(DemoPool.FRAGILE).isEmpty())
        repo.start(DemoMode.STUDY, listOf(6))
        repo.updateSession(repo.state.session.finish())
        assertEquals(listOf(6, 7), repo.poolIds(DemoPool.MISTAKES))
        repo.start(DemoMode.STUDY, listOf(6))
        repo.updateSession(repo.state.session.answer(1).withConfidence(2).finish())
        assertEquals(listOf(7), repo.poolIds(DemoPool.MISTAKES))
        assertEquals(listOf(6), repo.poolIds(DemoPool.FRAGILE))
    }

    @Test
    fun `planner rejects invalid dates categories and offline saves without changing its saved goal`() {
        val repo = DemoRepository()
        val today = LocalDate.now().toString()
        assertTrue(repo.savePlan(1000, setOf(1), today, today))
        val saved = repo.state
        for (target in listOf(0, 1001)) assertFalse(repo.savePlan(target, setOf(1), today, ""))
        assertFalse(repo.savePlan(10, setOf(99), today, ""))
        assertFalse(repo.savePlan(10, emptySet(), "2026-02-30", ""))
        assertFalse(repo.savePlan(10, emptySet(), "2026-1-01", ""))
        assertFalse(repo.savePlan(10, emptySet(), today, LocalDate.now().minusDays(1).toString()))
        assertSame(saved, repo.state)
        repo.setConnected(false)
        val offline = repo.state
        assertFalse(repo.savePlan(5, emptySet(), today, ""))
        assertSame(offline, repo.state)
    }

    @Test
    fun `planner progress uses confirmed matching answers only within its active dates`() {
        val repo = DemoRepository(DemoData(records = emptyList()))
        val today = LocalDate.now()
        assertTrue(repo.savePlan(2, setOf(1), today.toString(), today.toString()))
        assertEquals(listOf(2, 3, 4), repo.planIds)
        repo.start(DemoMode.STUDY, listOf(1, 2, 3))
        repo.updateSession(repo.state.session.answer(0).withConfidence(3).copy(index = 1).answer(1).withConfidence(2))
        assertEquals(0, repo.planTodayAnswered)
        repo.updateSession(repo.state.session.finish())
        assertEquals(2, repo.todayAnswered)
        assertEquals(1, repo.planTodayAnswered)
        assertTrue(repo.planActive)
        assertEquals(1, repo.currentStreak)
        assertTrue(repo.savePlan(2, emptySet(), today.plusDays(1).toString(), ""))
        assertFalse(repo.planActive)
        assertEquals(0, repo.planTodayAnswered)
        assertTrue(repo.savePlan(2, emptySet(), today.minusDays(3).toString(), today.minusDays(1).toString()))
        assertFalse(repo.planActive)
        assertEquals(0, repo.planTodayAnswered)
        assertTrue(repo.savePlan(2, emptySet(), today.toString(), ""))
        assertEquals(2, repo.planTodayAnswered)
    }

    @Test
    fun `saved-state round trip retains offline completion confidence connection failure ratings and planner`() {
        val repo = DemoRepository()
        assertTrue(repo.savePlan(12, setOf(1), LocalDate.now().toString(), ""))
        repo.rate(2, 5)
        repo.download(listOf(2, 4))
        repo.setConnected(false)
        repo.refresh()
        repo.start(DemoMode.STUDY, listOf(2), downloaded = true)
        repo.updateSession(repo.state.session.answer(1).withConfidence(2).finish())
        val saver = DemoRepository.StateSaver
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = value is Serializable }
        val saved = requireNotNull(with(saver) { scope.save(repo) })
        val restored = requireNotNull(saver.restore(parcelRoundTrip(saved)))
        assertEquals(repo.state, restored.state)
        assertFalse(restored.state.connected)
        assertEquals(2, restored.pendingRecords.single().session.confidence[0])
        restored.setConnected(true)
        restored.sync()
        assertEquals(1, restored.planTodayAnswered)
        assertEquals(1, repo.pendingRecords.size)
        assertEquals(0, repo.planTodayAnswered)
    }

    @Suppress("DEPRECATION")
    private fun parcelRoundTrip(data: DemoData): DemoData {
        val parcel = Parcel.obtain()
        val restored = Parcel.obtain()
        try {
            parcel.writeBundle(Bundle().apply { putSerializable("demo", data) })
            val bytes = parcel.marshall()
            restored.unmarshall(bytes, 0, bytes.size)
            restored.setDataPosition(0)
            return requireNotNull(restored.readBundle(DemoData::class.java.classLoader)?.getSerializable("demo")) as DemoData
        } finally {
            parcel.recycle()
            restored.recycle()
        }
    }
}
