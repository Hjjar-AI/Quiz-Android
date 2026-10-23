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
        repo.setTarget(100)
        repo.setTarget(0)
        repo.setTarget(101)
        assertEquals(100, repo.state.target)
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
