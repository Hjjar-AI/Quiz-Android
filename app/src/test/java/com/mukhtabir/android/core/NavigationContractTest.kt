package com.mukhtabir.android.core

import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.UserDto
import org.junit.Assert.*
import org.junit.Test

class NavigationContractTest {
    @Test fun `route matching strips queries and respects segment boundaries`() {
        assertTrue(ShellNavigation.matches("more/questions/13?tab=answers", "more/questions"))
        assertFalse(ShellNavigation.matches("more/questions_extra", "more/questions"))
        assertFalse(ShellNavigation.matches(null, "more/questions"))
    }
    @Test fun `knowledge detail uses the knowledge parent rather than generic more chrome`() {
        assertEquals("more/knowledge_map", ShellNavigation.parent("more/knowledge/7")?.route)
        assertEquals("more/master_exams/manage", ShellNavigation.parent("more/master_exams/manage/7")?.route)
        assertEquals(R.string.edit_question, ShellNavigation.title("more/questions/{questionId}/edit"))
    }
    @Test fun `editor templates preserve drafts and hide shell navigation`() {
        for (route in listOf("more/questions/{questionId}/edit", "more/cases/{caseKey}/stem",
            "more/master_drafts/{draftId}", "more/admin/transfers?verified=true")) {
            assertTrue(route, ShellNavigation.preservesDraft(route))
            assertTrue(route, ShellNavigation.isFocused(route))
        }
        assertFalse(ShellNavigation.isFocused("more/questions"))
        assertTrue(ShellNavigation.isFocused("more/master_exams/open/7"))
    }
    @Test fun `drawer does not infer admin permissions from role labels`() {
        val user = UserDto(41, "reader", role = "admin")
        val routes = NavigationCatalog.visibleFor(user).map { it.route }
        assertFalse("more/admin/users" in routes)
        assertFalse("more/admin/transfers" in routes)
        assertFalse("history" in routes)
        assertTrue("more/questions" in routes)
    }
    @Test fun `drawer permits independently granted learner and operator destinations`() {
        val user = UserDto(41, "reader", role = "member", capabilities = listOf("tests.start", "tests.view_own_history", "admin.users"))
        val routes = NavigationCatalog.visibleFor(user).map { it.route }
        assertTrue("quizzes" in routes)
        assertTrue("history" in routes)
        assertTrue("more/admin/users" in routes)
        assertFalse("more/admin/permissions" in routes)
    }
}
