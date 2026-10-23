package com.mukhtabir.android.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.data.remote.TransportMasterKeysShadow
import com.mukhtabir.android.data.remote.TransportPreferencesShadow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class SessionDraftStoreTest {
    private lateinit var context: Context
    private lateinit var store: SessionDraftStore
    private val draft = SessionDraft("session", "recall", 7, 0, SessionDraftKind.RECALL_TEXT, preAnswer = "Private draft")
    @Before fun setUp() { context = ApplicationProvider.getApplicationContext(); store = SessionDraftStore(context) }
    private fun bind() { store.bind(41, "account-a").getOrThrow() }
    @Test fun `unbound storage refuses private reads and writes`() {
        assertTrue(store.read(41).isFailure); assertTrue(store.write(41, draft).isFailure)
        assertTrue(store.readEditor(41, "question-new").isFailure)
    }
    @Test fun `same account binding preserves draft across store instances`() {
        bind(); store.write(41, draft).getOrThrow()
        val restored = SessionDraftStore(context); restored.bind(41, "account-a").getOrThrow()
        assertEquals(draft, restored.read(41).getOrThrow())
    }
    @Test fun `wrong account cannot read write or clear bound drafts`() {
        bind(); store.write(41, draft).getOrThrow()
        assertTrue(store.read(42).isFailure); assertTrue(store.write(42, draft).isFailure); assertTrue(store.clearDraft(42).isFailure)
        assertEquals(draft, store.read(41).getOrThrow())
    }
    @Test fun `account switch removes previous owner data`() {
        bind(); store.write(41, draft).getOrThrow(); store.writeEditor(41, "question-new", "private").getOrThrow()
        store.bind(42, "account-b").getOrThrow()
        assertNull(store.read(42).getOrThrow()); assertTrue(store.editorKeys(42).getOrThrow().isEmpty()); assertTrue(store.read(41).isFailure)
    }
    @Test fun `reused numeric account identity with different UUID clears old drafts`() {
        bind(); store.write(41, draft).getOrThrow(); store.bind(41, "replacement-account").getOrThrow()
        assertNull(store.read(41).getOrThrow())
    }
    @Test fun `expiry locks online drafts until authenticated binding restores them`() {
        bind(); store.write(41, draft).getOrThrow(); store.expireOnlineSession()
        assertTrue(store.read(41).isFailure); assertTrue(store.retryBinding(41).isFailure)
        bind(); assertEquals(draft, store.read(41).getOrThrow())
    }
    @Test fun `binding retry cannot impersonate an account never authenticated`() { assertTrue(store.retryBinding(41).isFailure) }
    @Test fun `invalid draft bounds preserve last valid draft`() {
        bind(); store.write(41, draft).getOrThrow()
        listOf(draft.copy(index = -1), draft.copy(questionId = 0), draft.copy(preAnswer = "x".repeat(1001))).forEach { assertTrue(store.write(41, it).isFailure) }
        assertEquals(draft, store.read(41).getOrThrow())
        assertTrue(store.write(41, draft.copy(preAnswer = "x".repeat(1000))).isSuccess)
    }
    @Test fun `editor keys are validated and enumerated deterministically`() {
        bind()
        listOf("question-new", "question-7", "image-" + "a".repeat(64)).forEach { store.writeEditor(41, it, "saved").getOrThrow() }
        listOf("../question-new", "question-../7", "question-x", "Question-new", "question--1").forEach { assertTrue(it, store.writeEditor(41, it, "bad").isFailure) }
        assertEquals(listOf("image-" + "a".repeat(64), "question-7", "question-new"), store.editorKeys(41).getOrThrow())
    }
    @Test fun `oversized editor update retains existing recovery content`() {
        bind(); store.writeEditor(41, "question-new", "keep").getOrThrow()
        assertTrue(store.writeEditor(41, "question-new", "x".repeat(512 * 1024 + 1)).isFailure)
        assertEquals("keep", store.readEditor(41, "question-new").getOrThrow())
    }
    @Test fun `editor count capacity rejects new entries but allows replacing an existing draft`() {
        bind(); (1..40).forEach { store.writeEditor(41, "question-$it", "draft").getOrThrow() }
        assertTrue(store.writeEditor(41, "question-41", "overflow").isFailure)
        store.writeEditor(41, "question-1", "updated").getOrThrow()
        assertEquals(40, store.editorKeys(41).getOrThrow().size)
        assertEquals("updated", store.readEditor(41, "question-1").getOrThrow())
    }
    @Test fun `finish recovery round trips only supported session identities and modes`() {
        bind(); val finish = PendingSessionFinish("session", "exam"); store.writeFinish(41, finish).getOrThrow()
        listOf(finish.copy(sessionId = ""), finish.copy(sessionId = "x".repeat(37)), finish.copy(mode = "other")).forEach { assertTrue(store.writeFinish(41, it).isFailure) }
        assertEquals(finish, store.readFinish(41).getOrThrow()); store.clearFinish(41).getOrThrow(); assertNull(store.readFinish(41).getOrThrow())
    }
    @Test fun `flags are session scoped and invalid updates do not erase valid flags`() {
        bind(); store.writeFlags(41, "first", setOf(0, 2)).getOrThrow()
        assertTrue(store.writeFlags(41, "first", setOf(-1)).isFailure)
        assertEquals(setOf(0, 2), store.readFlags(41, "first").getOrThrow()); assertTrue(store.readFlags(41, "second").getOrThrow().isEmpty())
    }
    @Test fun `author navigation keeps bounded recent choices and explicit last question`() {
        bind(); (1L..7L).forEach { store.recordAuthorChoices(41, it, listOf("tag-$it", "shared")).getOrThrow() }
        store.recordLastQuestion(41, 9).getOrThrow(); val saved = store.authorNavigation(41).getOrThrow()
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), saved.categories)
        assertEquals(5, saved.tags.size); assertEquals("tag-7", saved.tags.first()); assertEquals(9L, saved.lastQuestionId)
        assertTrue(store.recordLastQuestion(41, 0).isFailure)
    }
    @Test fun `logout clears private content and persists authentication gate`() {
        bind(); store.write(41, draft).getOrThrow(); store.clear()
        assertTrue(store.read(41).isFailure); assertTrue(ServerSettings(context).requiresProfileAuthentication())
        bind(); assertNull(store.read(41).getOrThrow()); assertFalse(ServerSettings(context).requiresProfileAuthentication())
    }
}
