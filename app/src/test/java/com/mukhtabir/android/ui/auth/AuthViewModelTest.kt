package com.mukhtabir.android.ui.auth

import android.app.Application
import com.mukhtabir.android.data.auth.AuthRepository
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.HttpViewModelTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class AuthViewModelTest : HttpViewModelTest() {
    private val user = UserDto(41, "learner", capabilities = listOf("view_questions"))
    private fun userResponse(value: UserDto = user) = envelope(Json.encodeToString(UserDto.serializer(), value))
    private fun ready(vm: AuthViewModel) = await(vm.state) { !it.isLoading && !it.isRestoring && !it.profileRefreshing }
    private fun model(): AuthViewModel { val vm = own(AuthViewModel(AuthRepository(api))); ready(vm); return vm }
    private fun login(vm: AuthViewModel) {
        respond = { envelope("""{"user":${Json.encodeToString(UserDto.serializer(), user)}}""") }
        vm.updateUsername(" learner "); vm.updatePassword(" secret "); vm.login(); ready(vm)
    }
    @Test fun `no saved session opens login without network traffic`() {
        val vm = model(); assertNull(vm.state.value.user); assertFalse(vm.state.value.restorePending); assertTrue(requests.isEmpty())
    }
    @Test fun `blank credentials cannot dispatch login`() {
        val vm = model(); vm.login(); vm.updateUsername("learner"); vm.login(); vm.updatePassword(" "); vm.login()
        assertTrue(requests.isEmpty()); assertNull(vm.state.value.user)
    }
    @Test fun `login trims username preserves password and clears it only after confirmation`() {
        val vm = model(); login(vm)
        val request = requests.single { it.method == "POST" }
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("learner", body.getValue("username").jsonPrimitive.content); assertEquals(" secret ", body.getValue("password").jsonPrimitive.content)
        assertEquals(user, vm.state.value.user); assertEquals("", vm.state.value.password)
    }
    @Test fun `failed login exposes error and retains editable credentials`() {
        val vm = model(); respond = { errorResponse(400) }; vm.updateUsername("learner"); vm.updatePassword("secret"); vm.login(); ready(vm)
        assertNull(vm.state.value.user); assertNotNull(vm.state.value.loginError); assertEquals("secret", vm.state.value.password)
        vm.updatePassword("corrected"); assertNull(vm.state.value.loginError)
    }
    @Test fun `profile refresh applies current same account permissions`() {
        val vm = model(); login(vm); val fresh = user.copy(fullName = "Updated", capabilities = emptyList())
        respond = { userResponse(fresh) }; vm.refreshUser(); ready(vm)
        assertEquals(fresh, vm.state.value.user); assertNull(vm.state.value.profileError)
    }
    @Test fun `profile refresh never switches to another returned account`() {
        val vm = model(); login(vm); respond = { userResponse(user.copy(id = 99)) }; vm.refreshUser(); ready(vm)
        assertEquals(user, vm.state.value.user); assertNotNull(vm.state.value.profileError)
    }
    @Test fun `active editor defers profile application until explicitly allowed`() {
        val vm = model(); login(vm); vm.setProfileApplicationAllowed(false)
        val fresh = user.copy(fullName = "Updated"); respond = { userResponse(fresh) }; vm.refreshUser(); ready(vm)
        assertEquals(user, vm.state.value.user); vm.setProfileApplicationAllowed(true); assertEquals(fresh, vm.state.value.user)
    }
    @Test fun `profile read failure retains current user and capabilities`() {
        val vm = model(); login(vm); respond = { errorResponse() }; vm.refreshUser(); ready(vm)
        assertEquals(user, vm.state.value.user); assertNotNull(vm.state.value.profileError)
    }
    @Test fun `accepted forced password change removes gate even if profile read fails`() {
        val vm = model(); respond = { envelope("""{"user":${Json.encodeToString(UserDto.serializer(), user.copy(mustChangePassword = true))}}""") }
        vm.updateUsername("learner"); vm.updatePassword("secret"); vm.login(); ready(vm); assertTrue(vm.state.value.user!!.mustChangePassword)
        respond = { errorResponse() }; vm.completeForcedPasswordChange(); ready(vm)
        assertFalse(vm.state.value.user!!.mustChangePassword); assertNotNull(vm.state.value.profileError)
    }
    @Test fun `forget clears deferred profile and cannot restore it into signed out state`() {
        val vm = model(); login(vm); vm.setProfileApplicationAllowed(false); respond = { userResponse(user.copy(fullName = "Pending")) }
        vm.refreshUser(); ready(vm); vm.forgetSession(); vm.setProfileApplicationAllowed(true)
        assertNull(vm.state.value.user); assertEquals("", vm.state.value.password); assertFalse(vm.state.value.isRestoring)
    }
    @Test fun `logout failure still clears local auth state without replay`() {
        val vm = model(); login(vm); respond = { errorResponse() }; vm.logout(); ready(vm)
        assertNull(vm.state.value.user); assertEquals("", vm.state.value.password)
        assertEquals(1, requests.count { it.requestUrl?.encodedPath == "/api/v1/auth/logout/" })
    }
    @Test fun `busy login cannot dispatch a duplicate write`() {
        val vm = model()
        val gate = holdResponse(envelope("""{"user":${Json.encodeToString(UserDto.serializer(), user)}}"""))
        vm.updateUsername("learner"); vm.updatePassword("secret"); vm.login()
        try {
            assertTrue(gate.awaitRequest()); assertTrue(vm.state.value.isLoading)
            vm.login(); assertEquals(1, requests.count { it.method == "POST" })
        } finally { gate.release() }
        ready(vm); assertEquals(user, vm.state.value.user)
    }
    private fun seedSavedSession() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val url = server.url("/api/v1/")
        PersistentCookieJar(context).saveFromResponse(url, listOf(okhttp3.Cookie.parse(url, "sessionid=saved-session; Path=/")!!))
        api = ApiClient(context, url.toString())
    }
    @Test fun `failed saved session read blocks login while retaining cookies for explicit retry`() {
        seedSavedSession(); respond = { errorResponse() }; val vm = model()
        assertTrue(vm.state.value.restorePending); assertNotNull(vm.state.value.restoreError); assertNull(vm.state.value.user)
        val count = requests.size; vm.updateUsername("other"); vm.updatePassword("secret"); vm.login()
        assertEquals(count, requests.size); assertTrue(api.hasPersistedSession())
        respond = { userResponse() }; vm.restoreSession(); ready(vm)
        assertEquals(user, vm.state.value.user); assertFalse(vm.state.value.restorePending)
    }
    @Test fun `forbidden saved session read clears persisted authentication`() {
        seedSavedSession(); respond = { errorResponse(403) }; val vm = model()
        assertFalse(api.hasPersistedSession()); assertFalse(vm.state.value.restorePending)
        assertNull(vm.state.value.user); assertNotNull(vm.state.value.loginError)
    }
}
