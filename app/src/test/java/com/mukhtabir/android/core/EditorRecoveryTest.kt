package com.mukhtabir.android.core

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.data.remote.TransportMasterKeysShadow
import com.mukhtabir.android.data.remote.TransportPreferencesShadow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class EditorRecoveryTest {
    private lateinit var store: SessionDraftStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    @Before fun setUp() { store = SessionDraftStore(ApplicationProvider.getApplicationContext<Context>()); store.bind(41).getOrThrow() }
    @After fun tearDown() { scope.cancel() }
    private fun recovery(): EditorRecovery<String> = EditorRecovery(scope, store, 41, "question-new", String.serializer())
    private fun ready(value: EditorRecovery<String>) { runBlocking { withTimeout(5000) { value.ready.first { it } } } }
    @Test fun `empty recovery becomes ready without pending content`() {
        val value = recovery(); ready(value); assertFalse(value.blocked); assertNull(value.pending.value); assertNull(value.error.value)
    }
    @Test fun `stored draft blocks editing until explicitly restored`() {
        store.writeEditor(41, "question-new", Json.encodeToString(String.serializer(), "Retained")).getOrThrow()
        val value = recovery(); ready(value); assertTrue(value.blocked); assertEquals("Retained", value.pending.value)
        var accepted: String? = null; value.restore { accepted = it }
        assertEquals("Retained", accepted); assertNull(value.pending.value); assertFalse(value.blocked)
        assertNotNull(store.readEditor(41, "question-new").getOrThrow())
    }
    @Test fun `failed restore callback leaves pending draft for another attempt`() {
        store.writeEditor(41, "question-new", "\"Retained\"").getOrThrow(); val value = recovery(); ready(value)
        assertThrows(IllegalStateException::class.java) { value.restore { error("UI failed") } }
        assertEquals("Retained", value.pending.value); assertTrue(value.blocked)
    }
    @Test fun `corrupt stored draft stays blocked and is not silently discarded`() {
        store.writeEditor(41, "question-new", "broken").getOrThrow(); val value = recovery()
        runBlocking { withTimeout(5000) { value.error.first { it != null } } }
        assertFalse(value.ready.value); assertTrue(value.blocked); assertEquals("broken", store.readEditor(41, "question-new").getOrThrow())
    }
    @Test fun `persist and explicit clear round trip through bound store`() {
        val value = recovery(); ready(value)
        runBlocking { assertTrue(value.persist("Draft")) }
        assertEquals("Draft", Json.decodeFromString(String.serializer(), store.readEditor(41, "question-new").getOrThrow()!!))
        runBlocking { assertTrue(value.persist(null)) }; assertNull(store.readEditor(41, "question-new").getOrThrow())
    }
    @Test fun `expired store makes persistence fail visibly without losing old content`() {
        val value = recovery(); ready(value); runBlocking { assertTrue(value.persist("Old")) }; store.expireOnlineSession()
        runBlocking { assertFalse(value.persist("New")) }; assertTrue(value.blocked); assertNotNull(value.error.value)
        store.bind(41).getOrThrow(); assertEquals("\"Old\"", store.readEditor(41, "question-new").getOrThrow())
    }
    @Test fun `discard clears pending only after storage confirms removal`() {
        store.writeEditor(41, "question-new", "\"Retained\"").getOrThrow(); val value = recovery(); ready(value)
        value.discard(); runBlocking { withTimeout(5000) { value.pending.first { it == null } } }
        assertNull(store.readEditor(41, "question-new").getOrThrow()); assertFalse(value.blocked)
    }
}
