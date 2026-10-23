package com.mukhtabir.android.ui.offline

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import com.mukhtabir.android.testing.ContractTestTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.OfflineFileBuilderShadow
import com.mukhtabir.android.testing.OfflineFileShadow
import com.mukhtabir.android.testing.answerChoices
import java.io.File
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowStatFs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w480dp-h1000dp",
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class,
        OfflineFileBuilderShadow::class, OfflineFileShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class OfflineStudyScreenInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var store: SessionDraftStore
    private lateinit var server: MockWebServer
    private val packId = "22222222-2222-4222-8222-222222222222"
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        OfflineFileShadow.rejectWrites = false
        store = SessionDraftStore(context).also { it.bind(41).getOrThrow() }
        val directory = File(context.noBackupFilesDir, "offline-legacy").apply { mkdirs() }
        ShadowStatFs.registerStats(directory.path, 100_000, 100_000, 100_000)
        val pack = OfflinePack(packId, "sample-token", listOf(QuestionDto(13, "Offline question", listOf("A", "B"),
            correctAnswer = 1, explanation = "Offline explanation")))
        store.writeOffline(41, false, listOf(pack.newMaterial()), null).getOrThrow()
        server = MockWebServer().apply { start() }
        val service = OfflineStudyService(ApiClient(context, server.url("/api/v1/").toString()))
        compose.setContent { ContractTestTheme {
            OfflineStudyRoute(service, store, 41, localOnly = true, canSync = false, maxQuestions = 200, onBack = {})
        } }
    }
    @After fun tearDown() {
        compose.runOnIdle { compose.activity.viewModelStore.clear() }
        OfflineFileShadow.rejectWrites = false
        server.shutdown()
    }
    private fun text(id: Int) = compose.activity.getString(id)
    private fun clickReady(id: Int) {
        val match = hasText(text(id)) and isEnabled() and hasClickAction()
        compose.waitUntil(5_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(match).performScrollTo().performClick()
    }
    @Test fun `real offline screen records a local answer and completion without any request`() {
        clickReady(R.string.offline_open)
        clickReady(R.string.offline_start)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(answerChoices)
        compose.onAllNodes(answerChoices).onFirst().performClick()
        compose.waitUntil(5_000) { store.readOfflinePack(41, true, packId).getOrNull()?.answers?.size == 1 }
        val finish = hasText(text(R.string.finish)) and isEnabled() and hasClickAction()
        compose.waitUntil(5_000) { compose.onAllNodes(finish).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(finish).onLast().performClick()
        compose.onNodeWithText(text(R.string.finish_session_action)).performClick()
        compose.waitUntil(5_000) { store.readOfflinePack(41, true, packId).getOrNull()?.frozenTime != null }
        compose.runOnIdle {
            val material = store.readOfflinePack(41, true, packId).getOrThrow()
            assertEquals(1, material.answers.single().answer)
            assertNotNull(material.completionId)
            assertNull(material.result)
            assertEquals(0, server.requestCount)
        }
        compose.onNodeWithText(text(R.string.offline_sync)).assertDoesNotExist()
    }
    @Test fun `local-only screen hides online download and upload actions`() {
        clickReady(R.string.offline_open)
        compose.onNodeWithText(text(R.string.offline_sync)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.offline_check_updates)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, server.requestCount) }
    }
}
