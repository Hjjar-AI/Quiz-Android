package com.mukhtabir.android.ui.admin

import org.junit.Assert.*
import org.junit.Test

class ModerationTagDraftTest {
    @Test fun `line based tags preserve embedded commas and Arabic`() {
        assertEquals(listOf("heart, lung", "قلب"), moderationTagNames(" heart, lung \nقلب\n"))
    }
    @Test fun `blank lines and exact duplicates are removed in insertion order`() {
        assertEquals(listOf("Alpha", "alpha", "Beta"), moderationTagNames("\nAlpha\r\nalpha\n Alpha \nBeta\n\t"))
    }
    @Test fun `empty mutation is invalid`() { assertEquals(ModerationTagError.EMPTY, moderationTagError(emptyList(), emptyList())) }
    @Test fun `add only and remove only operations are valid`() {
        assertNull(moderationTagError(listOf("add"), emptyList()))
        assertNull(moderationTagError(emptyList(), listOf("remove")))
    }
    @Test fun `overlap requires explicit correction`() { assertEquals(ModerationTagError.OVERLAP, moderationTagError(listOf("same"), listOf("same"))) }
    @Test fun `tag length counts Unicode code points rather than UTF16 units`() {
        assertNull(moderationTagError(listOf("😀".repeat(50)), emptyList()))
        assertEquals(ModerationTagError.TOO_LONG, moderationTagError(emptyList(), listOf("😀".repeat(51))))
    }
    @Test fun `length error precedes overlap`() {
        val tag = "a".repeat(51)
        assertEquals(ModerationTagError.TOO_LONG, moderationTagError(listOf(tag), listOf(tag)))
    }
}
