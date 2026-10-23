package com.mukhtabir.android.ui.sessions

import android.app.Application
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.HttpViewModelTest
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class DeviceSessionsViewModelTest : HttpViewModelTest() {
    private val target = "a".repeat(64)
    private val other = "b".repeat(64)
    private fun listing(ids: List<String> = listOf(target, other)) = envelope(Json.encodeToString(DeviceSessionsDto.serializer(), DeviceSessionsDto(ids.map { DeviceSessionDto(it, false, lastSeen = "2026-10-23T12:00:00Z") })))
    private fun ready(vm: DeviceSessionsViewModel) = await(vm.state) { !it.busy }
    private fun model(): DeviceSessionsViewModel { val vm = own(DeviceSessionsViewModel(DeviceSessionsService(api))); ready(vm); return vm }
    private fun posts() = requests.filter { it.method == "POST" }
    private fun success(current: Boolean = false) = envelope("""{"revoked":true,"is_current":$current}""")
    @Test fun `initial read failure remains distinguishable from an empty list`() {
        respond = { errorResponse() }; val vm = model()
        assertFalse(vm.state.value.loaded); assertNotNull(vm.state.value.error); assertTrue(posts().isEmpty())
    }
    @Test fun `failed refresh retains existing device rows`() {
        respond = { listing() }; val vm = model(); respond = { errorResponse() }; vm.refresh(); ready(vm)
        assertTrue(vm.state.value.loaded); assertEquals(2, vm.state.value.items.size); assertNotNull(vm.state.value.error)
    }
    @Test fun `blank oversized passwords and absent target cannot dispatch revocation`() {
        respond = { listing() }; val vm = model(); val count = requests.size
        vm.revoke(target); vm.password(" "); vm.revoke(target)
        vm.password("x".repeat(129)); vm.revoke(target)
        vm.password("valid"); vm.revoke("c".repeat(64))
        assertEquals(count, requests.size); assertTrue(posts().isEmpty())
    }
    @Test fun `revocation preflight detects an already absent target without a write`() {
        respond = { listing() }; val vm = model(); vm.password("secret")
        respond = { listing(listOf(other)) }; vm.revoke(target); ready(vm)
        assertTrue(posts().isEmpty()); assertEquals(R.string.device_session_absent, vm.state.value.message); assertEquals("", vm.state.value.password)
    }
    @Test fun `preflight failure does not mark an undispatched write uncertain`() {
        respond = { listing() }; val vm = model(); vm.password("secret"); respond = { errorResponse() }
        vm.revoke(target); ready(vm)
        assertNotNull(vm.state.value.error); assertNull(vm.state.value.pending); assertEquals("", vm.state.value.password); assertTrue(posts().isEmpty())
    }
    @Test fun `confirmed other device revocation clears password and refreshes list`() {
        val reads = AtomicInteger()
        respond = { if (it.method == "POST") success() else listing(if (reads.incrementAndGet() >= 3) listOf(other) else listOf(target, other)) }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertEquals(listOf(other), vm.state.value.items.map { it.id }); assertEquals(3, reads.get()); assertFalse(vm.state.value.signedOut)
        assertEquals(R.string.device_session_revoked, vm.state.value.message); assertEquals("", vm.state.value.password)
        val request = posts().single(); assertEquals("/api/v1/auth/sessions/$target/revoke/", request.requestUrl?.encodedPath)
        assertEquals("secret", Json.parseToJsonElement(request.body.readUtf8()).jsonObject.getValue("current_password").jsonPrimitive.content)
    }
    @Test fun `current device revocation signs out and blocks further activity`() {
        respond = { if (it.method == "POST") success(current = true) else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertTrue(vm.state.value.signedOut); val count = requests.size
        vm.refresh(); vm.revoke(other); assertEquals(count, requests.size); assertEquals(1, posts().size)
    }
    @Test fun `rejected revocation clears password without uncertain marker`() {
        respond = { if (it.method == "POST") errorResponse(403) else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertNotNull(vm.state.value.error); assertNull(vm.state.value.pending); assertFalse(vm.state.value.reviewReady); assertEquals("", vm.state.value.password)
        assertEquals(1, posts().size)
    }
    @Test fun `lost response blocks replay until explicit read and acknowledgement`() {
        respond = { if (it.method == "POST") okhttp3.mockwebserver.MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST) else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertEquals(target, vm.state.value.pending); assertEquals("", vm.state.value.password); assertFalse(vm.state.value.reviewReady)
        vm.acknowledgeReview(); assertEquals(target, vm.state.value.pending)
        vm.password("secret"); vm.revoke(target); assertEquals(1, posts().size)
        respond = { listing(listOf(other)) }; vm.refresh(); ready(vm)
        assertTrue(vm.state.value.reviewReady); assertEquals(target, vm.state.value.pending); assertEquals(1, posts().size)
        vm.acknowledgeReview(); assertNull(vm.state.value.pending); assertFalse(vm.state.value.reviewReady); assertEquals("", vm.state.value.password)
    }
    @Test fun `failed reconciliation cannot unlock uncertain revocation`() {
        respond = { if (it.method == "POST") errorResponse() else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        respond = { errorResponse() }; vm.refresh(); ready(vm); vm.acknowledgeReview()
        assertEquals(target, vm.state.value.pending); assertFalse(vm.state.value.reviewReady); assertEquals(1, posts().size)
    }
    @Test fun `confirmed revocation remains confirmed when subsequent refresh fails`() {
        val reads = AtomicInteger()
        respond = { if (it.method == "POST") success() else if (reads.incrementAndGet() >= 3) errorResponse() else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertEquals(R.string.device_session_revoked, vm.state.value.message); assertEquals(listOf(other), vm.state.value.items.map { it.id })
        assertNotNull(vm.state.value.error); assertNull(vm.state.value.pending); assertEquals(1, posts().size)
    }
    @Test fun `negative acknowledgement remains unresolved for explicit review`() {
        respond = { if (it.method == "POST") envelope("""{"revoked":false,"is_current":false}""") else listing() }
        val vm = model(); vm.password("secret"); vm.revoke(target); ready(vm)
        assertEquals(target, vm.state.value.pending); assertNull(vm.state.value.message); assertNotNull(vm.state.value.error)
    }
    @Test fun `busy session read blocks duplicate refresh and password changes`() {
        respond = { listing() }; val vm = model(); vm.password("retained")
        val gate = holdResponse(listing()); vm.refresh()
        try {
            assertTrue(gate.awaitRequest()); vm.refresh(); vm.password("changed"); vm.clearPassword(); vm.revoke(target)
            assertEquals("retained", vm.state.value.password); assertEquals(2, requests.size); assertTrue(posts().isEmpty())
        } finally { gate.release() }
        ready(vm)
    }
}
