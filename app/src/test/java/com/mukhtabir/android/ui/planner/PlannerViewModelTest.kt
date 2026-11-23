package com.mukhtabir.android.ui.planner

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
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
class PlannerViewModelTest : HttpViewModelTest() {
    private val plan = PlannerDto(7, version = 3, startDate = "2026-10-23", today = "2026-10-23")
    private val json = Json { encodeDefaults = true }
    private fun planResponse(value: PlannerDto = plan) = envelope(json.encodeToString(PlannerDto.serializer(), value))
    private fun defaults() { respond = { when (it.requestUrl?.encodedPath) {
        "/api/v1/study-planner/" -> planResponse()
        "/api/v1/study-planner/progress/" -> envelope("null")
        else -> envelope("""{"items":[]}""")
    } } }
    private fun store() = SessionDraftStore(ApplicationProvider.getApplicationContext<Context>()).also { it.bind(41).getOrThrow() }
    private fun ready(vm: PlannerViewModel) = await(vm.state) { (it.loaded || it.error != null) && !it.loading && !it.optionsLoading }
    private fun model(store: SessionDraftStore = store()): PlannerViewModel {
        val vm = own(PlannerViewModel(PlannerService(api), CategoryService(api), QuestionService(api), store, 41)); ready(vm); return vm
    }
    private fun updates() = requests.filter { it.requestUrl?.encodedPath == "/api/v1/study-planner/update/" }
    @Test fun `initialization records progress once and refresh preserves unsaved input`() {
        defaults(); val vm = model(); assertTrue(vm.state.value.loaded)
        assertEquals(3, vm.state.value.baselineVersion); vm.update("target", "25"); vm.load(); ready(vm)
        assertEquals("25", vm.state.value.target)
        assertEquals(1, requests.count { it.requestUrl?.encodedPath == "/api/v1/study-planner/progress/" })
    }
    @Test fun `invalid targets are rejected locally with repeated validation attempts`() {
        defaults(); val vm = model()
        listOf("0", "1001", "1.5", "huge", "").forEach { vm.update("target", it); vm.save(); assertEquals(PlannerValidationError.TARGET, vm.state.value.validationError) }
        assertEquals(5, vm.state.value.validationAttempt); assertTrue(updates().isEmpty())
    }
    @Test fun `impossible and reverse date ranges are rejected before dispatch`() {
        defaults(); val vm = model(); vm.update("start", "2026-02-30"); vm.save()
        assertEquals(PlannerValidationError.DATE, vm.state.value.validationError)
        vm.update("start", "2026-10-23"); vm.update("end", "2026-10-22"); vm.save()
        assertEquals(PlannerValidationError.DATE_ORDER, vm.state.value.validationError); assertTrue(updates().isEmpty())
    }
    @Test fun `pending tag input blocks save until explicitly added`() {
        defaults(); val vm = model(); vm.update("tagInput", "heart, lung"); vm.save()
        assertEquals(R.string.editor_tag_pending, vm.state.value.tagValidation); assertTrue(updates().isEmpty())
        vm.addTag(); assertEquals(listOf("heart, lung"), vm.state.value.tags); assertEquals("", vm.state.value.tagInput)
    }
    @Test fun `tags trim deduplicate and enforce Unicode code point limit`() {
        defaults(); val vm = model(); vm.addTag(" A "); vm.addTag("A"); assertEquals(listOf("A"), vm.state.value.tags)
        vm.addTag("😀".repeat(50)); assertEquals(2, vm.state.value.tags.size)
        vm.addTag("😀".repeat(51)); assertEquals(R.string.editor_tag_too_long, vm.state.value.tagValidation)
        vm.removeTag("A"); assertEquals(1, vm.state.value.tags.size)
    }
    @Test fun `unknown category cannot enter selection`() {
        defaults(); val vm = model(); vm.toggleCategory(999); assertTrue(vm.state.value.selectedCategories.isEmpty())
    }
    @Test fun `save sends revision identity and exact comma containing tags`() {
        defaults(); val vm = model(); vm.update("target", "25"); vm.addTag("heart, lung")
        val accepted = plan.copy(version = 4, targetQuestionsPerDay = 25, targetTags = listOf("heart, lung"))
        respond = { planResponse(accepted) }; vm.save(); ready(vm)
        val body = Json.parseToJsonElement(updates().single().body.readUtf8()).jsonObject
        assertEquals(3, body.getValue("expected_version").jsonPrimitive.int); assertEquals(7L, body.getValue("expected_id").jsonPrimitive.long)
        assertEquals(listOf("heart, lung"), body.getValue("target_tags").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(PlannerOutcome.SAVED, vm.state.value.outcome); assertEquals(4, vm.state.value.baselineVersion)
    }
    @Test fun `uncertain save preserves draft and prevents repeated write until review`() {
        defaults(); val vm = model(); vm.update("target", "25"); respond = { errorResponse() }
        vm.save(); ready(vm)
        assertTrue(vm.state.value.recoveryRequired); assertNotNull(vm.state.value.actionError); assertEquals("25", vm.state.value.target)
        vm.save(); assertEquals(1, updates().size)
    }
    @Test fun `accepted save survives failed follow up read`() {
        defaults(); val vm = model(); vm.update("target", "25")
        respond = { if (it.method == "POST") planResponse(plan.copy(version = 4, targetQuestionsPerDay = 25)) else errorResponse() }
        vm.save(); ready(vm)
        assertEquals(PlannerOutcome.SAVED, vm.state.value.outcome); assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.recoveryRequired); assertEquals("25", vm.state.value.target); assertEquals(1, updates().size)
    }
    @Test fun `changed revision requires explicit keep draft or server resolution`() {
        defaults(); val vm = model(); vm.update("target", "25")
        respond = { planResponse(plan.copy(version = 4, targetQuestionsPerDay = 30)) }; vm.load(); ready(vm)
        assertTrue(vm.state.value.conflict); vm.save(); assertTrue(updates().isEmpty())
        vm.resolvePlan(keep = true); assertFalse(vm.state.value.conflict); assertEquals("25", vm.state.value.target)
        vm.resolvePlan(keep = false); assertEquals("30", vm.state.value.target)
    }
    @Test fun `stored uncertain draft suppresses automatic progress and save replay`() {
        defaults(); val store = store()
        val saved = """
            {"target":"25","categories":[],"tags":["tag"],"tagInput":"",
             "start":"2026-10-23","end":"","pending":true,"version":3,"plannerId":7}
        """.trimIndent()
        store.writeEditor(41, "planner-new", saved).getOrThrow()
        val vm = model(store); assertTrue(vm.hasRecovery.value)
        assertEquals(0, requests.count { it.requestUrl?.encodedPath == "/api/v1/study-planner/progress/" })
        vm.restoreDraft(); ready(vm)
        assertEquals("25", vm.state.value.target); assertEquals(listOf("tag"), vm.state.value.tags); assertTrue(updates().isEmpty())
    }
    @Test fun `failed refresh preserves planner and edits instead of empty success`() {
        defaults(); val vm = model(); vm.update("target", "25"); respond = { errorResponse() }; vm.load(); ready(vm)
        assertEquals(plan, vm.state.value.planner); assertEquals("25", vm.state.value.target); assertNotNull(vm.state.value.error)
    }
    @Test fun `busy save freezes submitted values and suppresses duplicate mutation`() {
        defaults(); val vm = model(); vm.update("target", "25")
        val gate = holdResponse(planResponse(plan.copy(version = 4, targetQuestionsPerDay = 25)))
        vm.save()
        try {
            assertTrue(gate.awaitRequest()); vm.save(); vm.delete(); vm.update("target", "99")
            assertEquals("25", vm.state.value.target); assertEquals(1, updates().size)
        } finally { gate.release() }
        ready(vm); assertEquals(PlannerOutcome.SAVED, vm.state.value.outcome)
    }
}
