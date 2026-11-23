package com.mukhtabir.android.core

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class LearnerSavedStateTest {
    @Test fun `empty handle has safe navigation defaults`() {
        val saved = LearnerSavedState(SavedStateHandle(), 41, "history")
        assertEquals(1, saved.page()); assertNull(saved.filter()); assertNull(saved.draftFilter()); assertNull(saved.expandedId())
    }
    @Test fun `same account restores independent applied and draft filters`() {
        val handle = SavedStateHandle(); val saved = LearnerSavedState(handle, 41, "history")
        saved.savePage(4); saved.saveFilter("12"); saved.saveDraftFilter("99"); saved.saveExpandedId(7)
        val restored = LearnerSavedState(handle, 41, "history")
        assertEquals(4, restored.page()); assertEquals("12", restored.filter()); assertEquals("99", restored.draftFilter()); assertEquals(7L, restored.expandedId())
    }
    @Test fun `account change clears all navigation metadata`() {
        val handle = SavedStateHandle(); val saved = LearnerSavedState(handle, 41, "history")
        saved.savePage(8); saved.saveFilter("42"); saved.saveExpandedId(3)
        val other = LearnerSavedState(handle, 42, "history")
        assertEquals(1, other.page()); assertNull(other.filter()); assertNull(other.expandedId()); assertFalse(handle.contains("history"))
    }
    @Test fun `separate destination keys never overwrite each other`() {
        val handle = SavedStateHandle(); val own = LearnerSavedState(handle, 41, "own"); val all = LearnerSavedState(handle, 41, "all")
        own.savePage(2); all.savePage(5)
        assertEquals(2, own.page()); assertEquals(5, all.page())
    }
    @Test fun `page is clamped on read and write`() {
        val handle = SavedStateHandle(mapOf("history" to Bundle().apply { putLong("user_id", 41); putInt("page", -9) }))
        val saved = LearnerSavedState(handle, 41, "history")
        assertEquals(1, saved.page()); saved.savePage(0); assertEquals(1, saved.page())
    }
    @Test fun `expanded identity must be positive and can be cleared`() {
        val saved = LearnerSavedState(SavedStateHandle(), 41, "history")
        saved.saveExpandedId(0); assertNull(saved.expandedId()); saved.saveExpandedId(-1); assertNull(saved.expandedId())
        saved.saveExpandedId(5); assertEquals(5L, saved.expandedId()); saved.saveExpandedId(null); assertNull(saved.expandedId())
    }
    @Test fun `updates replace bundles without mutating previous saved snapshots`() {
        val handle = SavedStateHandle(); val saved = LearnerSavedState(handle, 41, "history")
        saved.savePage(2); val previous = handle.get<Bundle>("history")!!
        saved.savePage(3)
        assertNotSame(previous, handle.get<Bundle>("history")); assertEquals(2, previous.getInt("page"))
        assertEquals(setOf("user_id", "page"), handle.get<Bundle>("history")!!.keySet())
    }
}
