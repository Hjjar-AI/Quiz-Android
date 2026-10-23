package com.mukhtabir.android.ui.history

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.HttpViewModelTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class HistoryViewModelTest : HttpViewModelTest() {
    private val row = HistoryItemDto(7, mode = "study", totalQuestions = 2, answeredCount = 2, correctCount = 1, accuracy = 50.0, timeSpent = 60, completedAt = "2026-10-23T12:00:00Z")
    private fun page(number: Int = 1, pages: Int = 3, rows: List<HistoryItemDto> = listOf(row)) = envelope(Json.encodeToString(HistoryPageDto.serializer(), HistoryPageDto(rows, 5, number, 2, pages)))
    private fun ready(vm: HistoryViewModel) = await(vm.state) { !it.isLoading && !it.detailLoading }
    private fun model(scope: HistoryScope = HistoryScope.OWN, handle: SavedStateHandle = SavedStateHandle()): HistoryViewModel {
        val vm = own(HistoryViewModel(HistoryService(api), scope, 41, 2, handle)); ready(vm); return vm
    }
    private fun listRequests() = requests.filter { it.requestUrl?.encodedPath == "/api/v1/history/" }
    @Test fun `personal history always filters signed in account and ignores user edits`() {
        respond = { page() }; val vm = model()
        vm.setUserFilter("99"); vm.applyUserFilter()
        assertEquals("41", listRequests().single().requestUrl?.queryParameter("user_id"))
        assertEquals("", vm.state.value.userFilter); assertEquals(listOf(row), vm.state.value.items)
    }
    @Test fun `all user history initially omits user filter`() {
        respond = { page() }; model(HistoryScope.ALL)
        assertNull(listRequests().single().requestUrl?.queryParameter("user_id"))
    }
    @Test fun `unsubmitted draft filter never affects pagination`() {
        respond = { page(it.requestUrl!!.queryParameter("page")!!.toInt()) }
        val vm = model(HistoryScope.ALL); vm.setUserFilter("99"); vm.nextPage(); ready(vm)
        assertEquals(2, vm.state.value.page); assertNull(listRequests().last().requestUrl?.queryParameter("user_id"))
    }
    @Test fun `applying valid user filter canonicalizes it and resets page`() {
        respond = { page() }; val vm = model(HistoryScope.ALL)
        vm.setUserFilter(" 0099 "); vm.applyUserFilter(); ready(vm)
        assertEquals("99", vm.state.value.appliedUserFilter)
        assertEquals("99", listRequests().last().requestUrl?.queryParameter("user_id"))
        assertEquals("1", listRequests().last().requestUrl?.queryParameter("page"))
    }
    @Test fun `invalid filter retains displayed rows and sends no request`() {
        respond = { page() }; val vm = model(HistoryScope.ALL); val count = requests.size
        listOf("0", "-1", "abc", "999999999999999999999999").forEach {
            vm.setUserFilter(it); vm.applyUserFilter(); assertTrue(vm.state.value.invalidUserFilter)
            assertEquals(listOf(row), vm.state.value.items)
        }
        assertEquals(count, requests.size)
    }
    @Test fun `clearing applied filter returns to all user history`() {
        respond = { page() }; val vm = model(HistoryScope.ALL)
        vm.setUserFilter("99"); vm.applyUserFilter(); ready(vm)
        vm.clearUserFilter(); ready(vm)
        assertEquals("", vm.state.value.appliedUserFilter); assertNull(listRequests().last().requestUrl?.queryParameter("user_id"))
    }
    @Test fun `failed refresh retains readable history and exposes error`() {
        respond = { page() }; val vm = model(); respond = { errorResponse() }; vm.load(); ready(vm)
        assertEquals(listOf(row), vm.state.value.items); assertNotNull(vm.state.value.error)
        respond = { page() }; vm.retry(); ready(vm); assertNull(vm.state.value.error)
    }
    @Test fun `out of range page is reconciled to last available page`() {
        respond = { request -> val requested = request.requestUrl!!.queryParameter("page")!!.toInt(); page(requested, 2) }
        val vm = model(); vm.load(8); ready(vm)
        assertEquals(2, vm.state.value.page)
        assertEquals(listOf("1", "8", "2"), listRequests().map { it.requestUrl?.queryParameter("page") })
    }
    @Test fun `page size changes clamp nonpositive sizes and reload once`() {
        respond = { page() }; val vm = model(); vm.applyPageSize(0); ready(vm)
        assertEquals("1", listRequests().last().requestUrl?.queryParameter("per_page"))
        val count = requests.size; vm.applyPageSize(-5); assertEquals(count, requests.size)
    }
    @Test fun `unknown detail identity cannot trigger request`() {
        respond = { page() }; val vm = model(); val count = requests.size
        vm.toggleDetails(999); assertEquals(count, requests.size); assertNull(vm.state.value.expandedId)
    }
    @Test fun `foreign detail payload cannot be displayed under selected history row`() {
        respond = { if (it.requestUrl?.encodedPath == "/api/v1/history/") page() else envelope("""{"id":8,"mode":"study","total_questions":2,"answered_count":2,"correct_count":1,"accuracy":50.0,"time_spent":60,"completed_at":"2026-10-23T12:00:00Z"}""") }
        val vm = model(); vm.toggleDetails(7); ready(vm)
        assertEquals(7L, vm.state.value.expandedId); assertNull(vm.state.value.detail); assertNotNull(vm.state.value.detailError)
    }
    @Test fun `detail read retries explicitly and collapse clears detail state`() {
        var detailFails = true
        respond = { if (it.requestUrl?.encodedPath == "/api/v1/history/") page() else if (detailFails) errorResponse() else envelope("""{"id":7,"mode":"study","total_questions":2,"answered_count":2,"correct_count":1,"accuracy":50.0,"time_spent":60,"completed_at":"2026-10-23T12:00:00Z"}""") }
        val vm = model(); vm.toggleDetails(7); ready(vm); assertNotNull(vm.state.value.detailError)
        detailFails = false; vm.retryDetails(); ready(vm)
        assertEquals(7L, vm.state.value.detail?.id); assertNull(vm.state.value.detailError)
        val count = requests.size; vm.toggleDetails(7)
        assertNull(vm.state.value.expandedId); assertNull(vm.state.value.detail); assertEquals(count, requests.size)
    }
    @Test fun `saved account scoped filters preserve draft separately from applied query`() {
        respond = { page() }; val handle = SavedStateHandle(); val first = model(HistoryScope.ALL, handle)
        first.setUserFilter("12"); first.applyUserFilter(); ready(first); first.setUserFilter("34")
        val restored = model(HistoryScope.ALL, handle)
        assertEquals("34", restored.state.value.userFilter); assertEquals("12", restored.state.value.appliedUserFilter)
        assertEquals("12", listRequests().last().requestUrl?.queryParameter("user_id"))
    }
    @Test fun `busy history read suppresses duplicate refreshes`() {
        respond = { page() }; val vm = model(); val gate = holdResponse(page())
        vm.load()
        try {
            assertTrue(gate.awaitRequest()); assertTrue(vm.state.value.isLoading)
            vm.load(); vm.nextPage(); assertEquals(2, listRequests().size)
        } finally { gate.release() }
        ready(vm); assertEquals(listOf(row), vm.state.value.items)
    }
}
