package com.mukhtabir.android.ui.planner

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlannerValidationTest {
    @Test fun `leap days and year endpoints parse strictly`() {
        listOf("2024-02-29", "0001-01-01", "9999-12-31").forEach { assertEquals(LocalDate.parse(it), parsePlannerDate(it)) }
    }
    @Test fun `impossible dates are rejected rather than corrected`() {
        listOf("2023-02-29", "2024-04-31", "2024-00-01", "2024-13-01", "2024-01-00", "0000-01-01").forEach { assertNull(it, parsePlannerDate(it)) }
    }
    @Test fun `non canonical dates locale digits whitespace and times are rejected`() {
        listOf("2024-2-01", "24-02-01", "2024/02/01", " 2024-02-01", "2024-02-01T00:00:00", "٢٠٢٤-٠٢-٠١", "+10000-01-01", "").forEach { assertNull(it, parsePlannerDate(it)) }
    }
}
