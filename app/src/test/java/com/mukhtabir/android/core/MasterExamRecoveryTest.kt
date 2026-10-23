package com.mukhtabir.android.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.data.remote.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class MasterExamRecoveryTest {
    private lateinit var store: SessionDraftStore
    private val page = SessionQuestionPageDto(0, 1, SessionQuestionDto(7, "Frozen question", listOf("A", "B")))
    private val answer = MasterExamRecovery(3, "attempt", MasterExamAnswerRequest(7, 2, 3), page)
    @Before fun setUp() { store = SessionDraftStore(ApplicationProvider.getApplicationContext<Context>()); store.bind(41).getOrThrow() }
    @Test fun `frozen answer and question round trip without losing attempt identity`() {
        store.writeMaster(41, answer).getOrThrow(); assertEquals(answer, store.readMaster(41).getOrThrow())
    }
    @Test fun `destination only recovery and pending finish remain distinct`() {
        val destination = MasterExamRecovery(3); store.writeMaster(41, destination).getOrThrow(); assertEquals(destination, store.readMaster(41).getOrThrow())
        val finish = MasterExamRecovery(3, "attempt", finishPending = true); store.writeMaster(41, finish).getOrThrow(); assertEquals(finish, store.readMaster(41).getOrThrow())
    }
    @Test fun `foreign question identity cannot replace valid answer recovery`() {
        store.writeMaster(41, answer).getOrThrow()
        assertTrue(store.writeMaster(41, answer.copy(page = page.copy(question = page.question.copy(id = 8)))).isFailure)
        assertEquals(answer, store.readMaster(41).getOrThrow())
    }
    @Test fun `answer and confidence must match current question bounds`() {
        listOf(MasterExamAnswerRequest(7, 0, 3), MasterExamAnswerRequest(7, 3, 3), MasterExamAnswerRequest(7, 2, 0), MasterExamAnswerRequest(7, 2, 4)).forEach {
            assertTrue(store.writeMaster(41, answer.copy(draft = it)).isFailure)
        }
        assertNull(store.readMaster(41).getOrThrow())
    }
    @Test fun `answer recovery requires question snapshot and nonblank attempt`() {
        assertTrue(store.writeMaster(41, answer.copy(page = null)).isFailure)
        assertTrue(store.writeMaster(41, answer.copy(sessionId = " ")).isFailure)
        assertTrue(store.writeMaster(41, answer.copy(page = page.copy(index = -1))).isFailure)
    }
    @Test fun `finish recovery requires attempt and destination recovery rejects stray snapshot`() {
        assertTrue(store.writeMaster(41, MasterExamRecovery(3, finishPending = true)).isFailure)
        assertTrue(store.writeMaster(41, MasterExamRecovery(3, page = page)).isFailure)
        assertTrue(store.writeMaster(41, MasterExamRecovery(0)).isFailure)
    }
    @Test fun `clearing recovery preserves ordinary answer draft`() {
        val ordinary = SessionDraft("session", "study", 7, 0, SessionDraftKind.ANSWER, answer = 1)
        store.write(41, ordinary).getOrThrow(); store.writeMaster(41, answer).getOrThrow(); store.clearMaster(41).getOrThrow()
        assertNull(store.readMaster(41).getOrThrow()); assertEquals(ordinary, store.read(41).getOrThrow())
    }
    @Test fun `another account cannot clear or read master recovery`() {
        store.writeMaster(41, answer).getOrThrow()
        assertTrue(store.readMaster(42).isFailure); assertTrue(store.clearMaster(42).isFailure)
        assertEquals(answer, store.readMaster(41).getOrThrow())
    }
}
