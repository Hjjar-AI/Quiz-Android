package com.mukhtabir.android.ui.demo

import android.app.Application
import android.os.SystemClock
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DemoSessionTest {
    private fun start(mode: DemoMode = DemoMode.STUDY) = DemoSession.start(mode, listOf(4, 2, 8), 41)

    @Test
    fun `study locks confirmed answers and keeps the other slots unanswered`() {
        val saved = start().answer(3)
        assertEquals(listOf(3, -1, -1), saved.answers)
        assertEquals(1, saved.answered)
        assertSame(saved, saved.answer(0))
        assertEquals(listOf(3, 1, -1), saved.copy(index = 1).answer(1).answers)
    }

    @Test
    fun `exam selection can change before the deadline`() {
        val session = start(DemoMode.EXAM).answer(3).answer(0)
        assertEquals(listOf(0, -1, -1), session.answers)
        assertFalse(session.finished)
        assertFalse(session.expired)
    }

    @Test
    fun `recall refuses answers until choices are revealed`() {
        val session = start(DemoMode.RECALL)
        assertSame(session, session.answer(3))
        val revealed = session.copy(revealed = setOf(0), recalled = listOf("My answer", "", ""))
        val answered = revealed.answer(3)
        assertEquals(listOf(3, -1, -1), answered.answers)
        assertEquals("My answer", answered.recalled[0])
        assertSame(answered, answered.answer(1))
        val second = answered.copy(index = 1)
        assertSame(second, second.answer(1))
    }

    @Test
    fun `invalid choices and indices leave the session untouched`() {
        val session = start()
        assertSame(session, session.answer(-1))
        assertSame(session, session.answer(4))
        for (index in listOf(-1, session.total)) {
            val invalid = session.copy(index = index)
            assertSame(invalid, invalid.answer(0))
        }
    }

    @Test
    fun `finished and review sessions cannot accept more answers`() {
        for (page in listOf(DemoPage.HOME, DemoPage.RESULTS, DemoPage.REVIEW)) {
            val session = start(DemoMode.EXAM).copy(page = page)
            assertSame(session, session.answer(2))
        }
    }

    @Test
    fun `exam timeout preserves saved work and rejects a late answer`() {
        val saved = start(DemoMode.EXAM).answer(3).copy(flagged = setOf(1), index = 1)
        ShadowSystemClock.advanceBy(Duration.ofMillis(DemoSession.EXAM_MILLIS))
        assertTrue(saved.deadlineReached())
        val expired = saved.answer(1)
        assertTrue(expired.finished)
        assertTrue(expired.expired)
        assertEquals(listOf(3, -1, -1), expired.answers)
        assertEquals(setOf(1), expired.flagged)
    }

    @Test
    fun `exam deadline survives leaving the screen and does not restart on resume`() {
        val paused = start(DemoMode.EXAM)
        ShadowSystemClock.advanceBy(Duration.ofMinutes(3))
        assertEquals(300_000L, paused.remainingMillis())
        assertFalse(paused.deadlineReached())
        ShadowSystemClock.advanceBy(Duration.ofMinutes(5))
        assertTrue(paused.deadlineReached())
    }

    @Test
    fun `study and recall remain available after the exam-sized time window`() {
        val study = start()
        val recall = start(DemoMode.RECALL).copy(revealed = setOf(0))
        ShadowSystemClock.advanceBy(Duration.ofMillis(DemoSession.EXAM_MILLIS + 1))
        assertFalse(study.deadlineReached())
        assertFalse(recall.deadlineReached())
        assertEquals(1, study.answer(3).answered)
        assertEquals(1, recall.answer(3).answered)
    }

    @Test
    fun `restored exam with an impossible future monotonic deadline expires safely`() {
        val restored = start(DemoMode.EXAM).copy(deadline = SystemClock.elapsedRealtime() + DemoSession.EXAM_MILLIS + 1)
        assertTrue(restored.deadlineReached())
        assertTrue(restored.answer(0).expired)
        assertEquals(0, restored.answer(0).answered)
    }

    @Test
    fun `finish retains question identity notes flags and unanswered slots for review`() {
        val saved = start(DemoMode.RECALL).copy(revealed = setOf(0), flagged = setOf(2),
            recalled = listOf("Remembered answer", "", "")).answer(3).withConfidence(2)
        val finished = saved.finish()
        assertTrue(finished.finished)
        assertFalse(finished.expired)
        assertEquals(saved.copy(page = DemoPage.RESULTS), finished)
        assertEquals(listOf(4, 2, 8), finished.questionIds)
        assertEquals(mapOf(0 to 2), finished.confidence)
    }

    @Test
    fun `learning requires an explicit valid confidence after answering each question`() {
        for (mode in listOf(DemoMode.STUDY, DemoMode.RECALL)) {
            val session = start(mode).copy(revealed = setOf(0, 1))
            assertFalse(session.canAdvance())
            assertSame(session, session.withConfidence(3))
            val answered = session.answer(3)
            assertFalse(answered.canAdvance())
            assertTrue(answered.confidence.isEmpty())
            for (invalid in listOf(-1, 0, 4)) assertSame(answered, answered.withConfidence(invalid))
            val confident = answered.withConfidence(2)
            assertTrue(confident.canAdvance())
            assertEquals(mapOf(0 to 2), confident.confidence)
            val second = confident.copy(index = 1).answer(1)
            assertFalse(second.canAdvance())
            assertEquals(mapOf(0 to 2, 1 to 3), second.withConfidence(3).confidence)
            assertEquals(mapOf(0 to 2), confident.confidence)
        }
    }

    @Test
    fun `exam navigation stays available without confidence and rejects confidence writes`() {
        val exam = start(DemoMode.EXAM)
        assertTrue(exam.canAdvance())
        assertSame(exam, exam.withConfidence(1))
        val answered = exam.answer(2)
        assertTrue(answered.canAdvance())
        assertSame(answered, answered.withConfidence(3))
        assertTrue(answered.confidence.isEmpty())
    }

    @Test
    fun `confidence cannot mutate finished snapshots or an invalid question index`() {
        val answered = start().answer(3).withConfidence(1)
        for (page in listOf(DemoPage.HOME, DemoPage.RESULTS, DemoPage.REVIEW)) {
            val frozen = answered.copy(page = page)
            assertSame(frozen, frozen.withConfidence(3))
            assertEquals(mapOf(0 to 1), frozen.confidence)
        }
        for (index in listOf(-1, answered.total)) {
            val invalid = answered.copy(index = index)
            assertFalse(invalid.canAdvance())
            assertSame(invalid, invalid.withConfidence(3))
        }
    }
}
