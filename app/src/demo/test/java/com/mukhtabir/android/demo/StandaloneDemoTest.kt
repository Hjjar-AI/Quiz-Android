package com.mukhtabir.android.demo

import android.Manifest
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.PendingSessionFinish
import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.core.SessionDraftKind
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.OfflineAnswer
import com.mukhtabir.android.data.remote.PlannerWriteRequest
import com.mukhtabir.android.data.remote.StartSessionRequest
import com.mukhtabir.android.data.remote.SubmitAnswerRequest
import com.mukhtabir.android.data.remote.TestMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en-rUS")
class StandaloneDemoTest {
    @get:Rule val storage = TemporaryFolder()

    private val application get() = RuntimeEnvironment.getApplication()
    private val fixtures by lazy { DemoFixtures(application) }
    private val context by lazy {
        val directory = storage.newFolder()
        object : ContextWrapper(application) {
            override fun getFilesDir() = directory
        }
    }

    private fun reopen() = DemoStore(context, fixtures::seed)

    @Test fun `demo app owns its launcher and omits Internet permission`() {
        val packageName = application.packageName
        assertEquals("com.mukhtabir.android.demo", packageName)
        @Suppress("DEPRECATION")
        val info = application.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        assertFalse(Manifest.permission.INTERNET in info.requestedPermissions.orEmpty())
        val launch = requireNotNull(application.packageManager.getLaunchIntentForPackage(packageName))
        assertEquals(DemoActivity::class.java.name, launch.component?.className)
        assertTrue(launch.hasCategory(Intent.CATEGORY_LAUNCHER))
    }

    @Test fun `one shot read failure clears only after a connected read and offline blocks writes`() = runBlocking {
        val store = reopen()
        val bookmarks = DemoBookmarks(store, fixtures)
        val catalog = DemoCatalog(store, fixtures)
        val initial = catalog.list()
        store.arm(DemoFailure.READ, true)
        store.setConnected(false)
        assertTrue(runCatching { catalog.list() }.exceptionOrNull() is DemoConnectionException)
        assertTrue(DemoFailure.READ in store.connection.value.armed)
        assertTrue(runCatching { bookmarks.set(2, true) }.exceptionOrNull() is DemoConnectionException)

        store.setConnected(true)
        assertTrue(runCatching { catalog.list() }.exceptionOrNull() is DemoConnectionException)
        assertEquals(initial, catalog.list())
        assertFalse(bookmarks.state(2))
        assertTrue(store.connection.value.armed.isEmpty())
    }

    @Test fun `uncertain answer can be read after reopening and recall and exam keep reveal rules`() = runBlocking {
        val store = reopen()
        val sessions = DemoSessions(store, fixtures)
        val recall = sessions.start(TestMode.RECALL, StartSessionRequest(1, questionIds = listOf(1)))
        val hidden = sessions.question(TestMode.RECALL, recall.sessionId).question
        assertTrue(hidden.choicesHidden)
        assertTrue(hidden.choices.isEmpty())
        assertNull(hidden.correctAnswer)
        val correct = requireNotNull(fixtures.byId.getValue(1).correctAnswer)
        val answer = SubmitAnswerRequest(recall.sessionId, answer = correct, action = "same", expectedIndex = 0, confidence = 3)
        assertEquals(400, (runCatching { sessions.submit(TestMode.RECALL, answer) }.exceptionOrNull() as? ApiException)?.statusCode)

        sessions.submit(TestMode.RECALL, answer.copy(answer = null, preAnswer = "My prediction"))
        assertFalse(sessions.question(TestMode.RECALL, recall.sessionId).question.choicesHidden)
        store.arm(DemoFailure.ANSWER, true)
        assertTrue(runCatching { sessions.submit(TestMode.RECALL, answer) }.exceptionOrNull() is DemoConnectionException)

        val resumed = DemoSessions(reopen(), fixtures)
        val saved = resumed.question(TestMode.RECALL, recall.sessionId)
        assertEquals(correct, saved.savedAnswer)
        assertEquals("My prediction", saved.savedPreAnswer)
        assertEquals(true, saved.savedIsCorrect)

        val exam = resumed.start(TestMode.EXAM, StartSessionRequest(1, questionIds = listOf(1)))
        assertEquals(8, exam.durationMinutes)
        resumed.submit(TestMode.EXAM, answer.copy(sessionId = exam.sessionId))
        val examPage = resumed.questions(TestMode.EXAM, exam.sessionId).questions.single()
        assertEquals(correct, examPage.savedAnswer)
        assertNull(examPage.savedIsCorrect)
        assertNull(examPage.question.correctAnswer)
        assertNull(examPage.question.explanation)
    }

    @Test fun `finish response loss leaves one durable completion and preserves the recovery marker`() = runBlocking {
        val store = reopen()
        val sessions = DemoSessions(store, fixtures)
        val session = sessions.start(TestMode.STUDY, StartSessionRequest(1, questionIds = listOf(1)))
        val marker = PendingSessionFinish(session.sessionId, TestMode.STUDY.path)
        store.writeFinish(SAMPLE_USER_ID, marker).getOrThrow()
        val originalTotal = DemoHistory(store).list().total
        store.arm(DemoFailure.FINISH, true)

        assertTrue(runCatching { sessions.finish(TestMode.STUDY, session.sessionId) }.exceptionOrNull() is DemoConnectionException)
        val reopened = reopen()
        val retained = DemoSessions(reopened, fixtures)
        assertEquals(marker, reopened.readFinish(SAMPLE_USER_ID).getOrThrow())
        assertNull(retained.status(TestMode.STUDY, session.sessionId).sessionId)
        val receipt = retained.completedResult(TestMode.STUDY, session.sessionId)
        assertEquals(session.sessionId, receipt.sourceSessionId)
        assertEquals(receipt, retained.finish(TestMode.STUDY, session.sessionId))
        assertEquals(originalTotal + 1, DemoHistory(reopened).list().total)
    }

    @Test fun `local progress and drafts survive reopening and reset restores the sample but keeps settings`() = runBlocking {
        val first = reopen()
        val preferences = AppPreferences(context)
        preferences.setDisplay(preferences.display.value.copy(compact = true))
        DemoBookmarks(first, fixtures).set(2, true)
        DemoCatalog(first, fixtures).rate(2, 4)
        val draft = SessionDraft("demo-session", "recall", 2, 0, SessionDraftKind.RECALL_TEXT, preAnswer = "Recall")
        first.write(SAMPLE_USER_ID, draft).getOrThrow()

        val second = reopen()
        assertTrue(DemoBookmarks(second, fixtures).state(2))
        assertEquals(4, DemoCatalog(second, fixtures).rating(2).userRating)
        assertEquals(draft, second.read(SAMPLE_USER_ID).getOrThrow())
        assertTrue(second.read(SAMPLE_USER_ID + 1).isFailure)
        second.arm(DemoFailure.FINISH, true)
        second.setConnected(false)
        second.reset(fixtures.seed())
        assertTrue(second.read(SAMPLE_USER_ID).isFailure)

        val clean = reopen()
        assertFalse(DemoBookmarks(clean, fixtures).state(2))
        assertEquals(0, DemoCatalog(clean, fixtures).rating(2).userRating)
        assertNull(clean.read(SAMPLE_USER_ID).getOrThrow())
        assertEquals(DemoConnection(), clean.connection.value)
        assertTrue(AppPreferences(context).display.value.compact)
    }

    @Test fun `downloaded practice stays local and a lost upload receipt counts once across learner views`() = runBlocking {
        val store = reopen()
        val planner = DemoPlanner(store, fixtures)
        planner.update(PlannerWriteRequest(0, 0, 10, startDate = LocalDate.now().minusDays(1).toString()))
        val history = DemoHistory(store)
        val analytics = DemoAnalytics(store, fixtures)
        val originalHistory = history.list().total
        val originalAttempts = analytics.summary().confidenceStats.totalAttempted
        val originalProgress = planner.get().dailyProgress.values.sum()

        store.setConnected(false)
        val summary = store.readOffline(SAMPLE_USER_ID, localOnly = true).getOrThrow().single()
        val seeded = store.readOfflinePack(SAMPLE_USER_ID, localOnly = true, summary.pack.id).getOrThrow()
        val completed = seeded.copy(completionId = "demo-upload", frozenTime = 120, completedAt = Instant.now().toString(),
            answers = seeded.pack.questions.map { OfflineAnswer(it.id, requireNotNull(it.correctAnswer), 3) })
        store.writeOffline(SAMPLE_USER_ID, localOnly = true, listOf(completed), null).getOrThrow()
        assertTrue(runCatching { DemoOffline(store, fixtures).upload(completed) }.exceptionOrNull() is DemoConnectionException)

        store.setConnected(true)
        assertEquals(originalHistory, history.list().total)
        store.arm(DemoFailure.UPLOAD, true)
        assertTrue(runCatching { DemoOffline(store, fixtures).upload(completed) }.exceptionOrNull() is DemoConnectionException)

        val reopened = reopen()
        val offline = DemoOffline(reopened, fixtures)
        val result = offline.upload(completed)
        assertEquals(result, offline.upload(completed))
        val conflict = runCatching { offline.upload(completed.copy(frozenTime = 121)) }.exceptionOrNull()
        assertEquals(409, (conflict as? ApiException)?.statusCode)
        assertEquals(originalHistory + 1, DemoHistory(reopened).list().total)
        assertEquals(originalAttempts + completed.answers.size, DemoAnalytics(reopened, fixtures).summary().confidenceStats.totalAttempted)
        assertEquals(originalProgress + completed.answers.size, DemoPlanner(reopened, fixtures).get().dailyProgress.values.sum())
    }
}
