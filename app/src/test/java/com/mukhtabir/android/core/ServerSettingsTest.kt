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
class ServerSettingsTest {
    private lateinit var settings: ServerSettings
    @Before fun setUp() { settings = ServerSettings(ApplicationProvider.getApplicationContext<Context>()) }
    @Test fun `new connection creates profile requiring authentication`() {
        settings.save("https://first.example").getOrThrow()
        assertEquals("https://first.example/api/v1/", settings.read())
        assertNotEquals("legacy", settings.profile()); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `same normalized address preserves authenticated profile`() {
        settings.save("https://first.example").getOrThrow(); val profile = settings.profile()
        settings.markProfileAuthenticated(profile, settings.read()).getOrThrow()
        settings.save("https://FIRST.example/api/v1").getOrThrow()
        assertEquals(profile, settings.profile()); assertFalse(settings.requiresProfileAuthentication())
    }
    @Test fun `switching back restores known profile with authentication gate`() {
        settings.save("https://first.example").getOrThrow(); val first = settings.profile()
        settings.save("https://second.example").getOrThrow(); assertNotEquals(first, settings.profile())
        settings.save("https://first.example").getOrThrow(); assertEquals(first, settings.profile()); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `explicit same server address change retains private profile`() {
        settings.save("https://first.example").getOrThrow(); val first = settings.profile()
        settings.save("https://renamed.example", sameServer = true).getOrThrow()
        assertEquals(first, settings.profile()); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `same server cannot merge already distinct profiles`() {
        settings.save("https://first.example").getOrThrow(); settings.save("https://second.example").getOrThrow(); val second = settings.profile()
        assertTrue(settings.save("https://first.example", sameServer = true).isFailure)
        assertEquals(second, settings.profile()); assertEquals("https://second.example/api/v1/", settings.read())
    }
    @Test fun `stale authentication completion cannot unlock another server`() {
        settings.save("https://first.example").getOrThrow(); val profile = settings.profile(); val address = settings.read()
        settings.save("https://second.example").getOrThrow()
        assertTrue(settings.markProfileAuthenticated(profile, address).isFailure); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `imported slots store public addresses without selecting or authenticating`() {
        settings.save("https://first.example").getOrThrow(); val profile = settings.profile()
        settings.saveImportedFile(2, "https://imported.example").getOrThrow()
        assertEquals(listOf(ImportedServerFile(2, "https://imported.example/api/v1/")), settings.importedFiles())
        assertEquals(profile, settings.profile()); assertEquals("https://first.example/api/v1/", settings.read())
        assertFalse(settings.isFileConfigured()); assertNull(settings.activeImportedSlot()); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `file selection requires matching slot and endpoint`() {
        settings.saveImportedFile(1, "https://first.example").getOrThrow()
        assertTrue(settings.save("https://other.example", fromFile = true, importedSlot = 1).isFailure)
        assertNull(settings.read())
        settings.save("https://first.example", fromFile = true, importedSlot = 1).getOrThrow()
        assertEquals(1, settings.activeImportedSlot()); assertTrue(settings.isFileConfigured()); assertTrue(settings.requiresProfileAuthentication())
    }
    @Test fun `slot bounds and invalid connections leave registry untouched`() {
        settings.saveImportedFile(3, "https://third.example").getOrThrow(); val before = settings.importedFiles()
        assertTrue(settings.saveImportedFile(0, "https://first.example").isFailure)
        assertTrue(settings.saveImportedFile(4, "https://first.example").isFailure)
        assertTrue(settings.saveImportedFile(3, "https://u:p@example.org").isFailure)
        assertEquals(before, settings.importedFiles())
    }
    @Test fun `replacing selected slot does not silently switch active server`() {
        settings.saveImportedFile(1, "https://first.example").getOrThrow()
        settings.save("https://first.example", fromFile = true, importedSlot = 1).getOrThrow()
        val profile = settings.profile(); settings.saveImportedFile(1, "https://second.example").getOrThrow()
        assertEquals("https://first.example/api/v1/", settings.read()); assertEquals(profile, settings.profile()); assertNull(settings.activeImportedSlot())
    }
}
