package com.mukhtabir.android.data.remote

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class,
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class DtoContractTest {
    private lateinit var api: ApiClient
    private lateinit var server: MockWebServer
    private val fixtures by lazy {
        Json.parseToJsonElement(requireNotNull(javaClass.getResource("/contracts/android_payloads.json")).readText()).jsonObject
    }
    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        api = ApiClient(ApplicationProvider.getApplicationContext<Context>(), server.url("/api/v1/").toString())
    }
    @After fun tearDown() { server.shutdown() }
    private fun <T> decode(key: String, serializer: DeserializationStrategy<T>): T = api.decode(serializer, fixtures.getValue(key))
    private fun response(data: String) = server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"code":200,"data":$data}"""))
    private fun request() = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS)) { "Expected API request missing" }
    private fun csrf() = response("""{"token":"sample-csrf"}""")

    @Test fun `analytics maps fractional aggregates nullable colours and exact tag names`() {
        val value = decode("analytics", AnalyticsSummaryDto.serializer())
        assertEquals(62.5, value.userPerformance.single().accuracy, 0.0)
        assertEquals(8, value.userPerformance.single().questionsAnswered)
        assertEquals(40.0, value.confidenceStats.fragileRatio, 0.0)
        assertEquals(3, value.confidenceStats.wrongOpen)
        assertEquals(7L, value.categoryCoverage.single().categoryId)
        assertNull(value.categoryCoverage.single().color)
        assertEquals(62.5, value.categoryCoverage.single().averageAccuracy, 0.0)
        assertEquals("Mood, anxiety", value.tagCoverage.single().tagName)
        assertEquals(33.3, value.difficultyStats.single().averageCorrectRate, 0.0)
        assertEquals(72.5, value.activeUsers.single().avgSessionDuration, 0.0)
        assertEquals(37.5, value.weakCategories.single().getValue("accuracy").jsonPrimitive.double, 0.0)
    }
    @Test fun `mastery retains distinct coverage accuracy and mastery score`() {
        val value = decode("mastery", CategoryMasteryDto.serializer())
        val row = value.categories.single()
        assertEquals(1, value.totalCategories)
        assertEquals(37.5, row.coverage, 0.0)
        assertEquals(75.0, row.accuracy, 0.0)
        assertEquals(52.5, row.masteryScore, 0.0)
        assertFalse(row.mastered)
    }
    @Test fun `activity retains session-only days and learning aggregates`() {
        val value = decode("streak", StreakHistoryDto.serializer())
        assertEquals(1, value.days.first().sessions)
        assertEquals(0, value.days.first().questions)
        assertEquals(3, value.totalSessions)
        assertEquals(5, value.totalQuestions)
        assertEquals(4, value.uniqueQuestions)
        assertEquals(3, value.uniqueConcepts)
        assertEquals(2, value.dueReviews)
        assertEquals(12.5, value.masteryGain, 0.0)
    }
    @Test fun `admin page retains flags pagination and augmented latest exam fields`() {
        val value = decode("users", UserPageDto.serializer())
        val user = value.items.single()
        assertEquals(2, value.page)
        assertEquals(21, value.total)
        assertEquals(2, value.totalPages)
        assertEquals(20, value.perPage)
        assertFalse(user.isActive)
        assertTrue(user.mustChangePassword)
        assertNull(user.expiresAt)
        assertNull(user.lastLogin)
        assertNull(user.authorRank)
        assertEquals(2.5, user.trustScore, 0.0)
        assertEquals(62.5, requireNotNull(user.latestExamAccuracy), 0.0)
        assertEquals("Mood, anxiety", user.latestExamTag)
        assertEquals("متعلّم", user.displayName)
        assertEquals(listOf("tests.start"), user.capabilities)
    }
    @Test fun `active account map converts JSON keys to user ids`() {
        val account = decode("active_accounts", ActiveAccountsDto.serializer()).users.getValue(41L)
        assertEquals("متعلّم", account.name)
        assertNull(account.ip)
        assertEquals("2026-10-21T10:00:00Z", account.lastSeen)
    }
    @Test fun `permission overrides preserve explicit denial and role resolution`() {
        val value = decode("permissions", UserCapabilitiesDto.serializer())
        assertEquals(false, value.overrides["admin.users"])
        assertEquals(true, value.overrides["tests.download_full_bank"])
        assertFalse("admin.users" in value.resolved)
        val roles = decode("roles", RoleCapabilitiesDto.serializer())
        assertEquals(listOf("tests.start"), roles.roles["member"])
        assertEquals("Study", roles.groups.single().label)
    }
    @Test fun `moderation separates author reporter and nullable report metadata`() {
        val value = decode("flags", ModerationFlagPageDto.serializer())
        val row = value.items.single()
        assertEquals(2, value.page)
        assertEquals(2, value.totalPages)
        assertEquals(13L, row.questionId)
        assertEquals("author", row.questionAuthor)
        assertEquals("reader", row.flaggerUsername)
        assertEquals("Ambiguous option", row.reason)
        assertEquals("2026-10-21T09:30:00Z", row.createdAt)
        val nullable = api.decode(ModerationFlagDto.serializer(), Json.parseToJsonElement(
            """{"id":9,"question_id":13,"reason":null,"created_at":null}"""))
        assertNull(nullable.reason)
        assertNull(nullable.createdAt)
    }
    @Test fun `groups retain private visibility and leaderboard fractions`() {
        val group = decode("groups", GroupListDto.serializer()).items.single()
        assertFalse(group.showInLeaderboard)
        assertEquals(3, group.memberCount)
        val board = decode("leaderboard", GroupLeaderboardDto.serializer())
        assertEquals(30, board.days)
        assertEquals(group.id, board.group.id)
        assertEquals(62.5, board.rows.single().accuracy, 0.0)
        assertEquals(41L, board.rows.single().userId)
        assertEquals(8, board.rows.single().questionsAnswered)
    }
    @Test fun `planner preserves revisions empty-day counts exact scope and nullable end`() {
        val value = decode("planner", PlannerDto.serializer())
        assertEquals(5L, value.id)
        assertEquals(8, value.version)
        assertEquals(12, value.targetQuestionsPerDay)
        assertEquals(listOf(7L, 9L), value.targetCategories)
        assertEquals(listOf("Mood, anxiety", "القلق"), value.targetTags)
        assertEquals(0, value.dailyProgress["2026-10-20"])
        assertEquals(12, value.dailyProgress[value.today])
        assertNull(value.endDate)
    }
    @Test fun `runtime settings retain string values and revision`() {
        val value = decode("settings", RuntimeSettingsDto.serializer())
        assertEquals(6, value.version)
        assertEquals("0", value.defaultRenewalDays)
        assertEquals("45", value.examDurationMinutes)
    }
    @Test fun `current empty analytics and moderation responses decode without invented rows`() {
        assertTrue(api.decode(AnalyticsSummaryDto.serializer(), JsonObject(emptyMap())).userPerformance.isEmpty())
        assertTrue(api.decode(ModerationFlagPageDto.serializer(), JsonObject(emptyMap())).items.isEmpty())
        val minimal = api.decode(UserDto.serializer(), Json.parseToJsonElement("""{"id":41,"username":"reader"}"""))
        assertNull(minimal.latestExamAccuracy)
        assertEquals("reader", minimal.displayName)
    }
    @Test fun `permission reset encodes null override rather than false denial`() {
        val data = api.encode(UserCapabilitiesUpdateRequest.serializer(), UserCapabilitiesUpdateRequest(mapOf("admin.users" to null))).jsonObject
        assertEquals(JsonNull, data.getValue("capabilities").jsonObject.getValue("admin.users"))
    }
    @Test fun `planner write sends displayed identity revision and exact scope`() = runTest {
        csrf(); response(fixtures.getValue("planner").toString())
        PlannerService(api).update(PlannerWriteRequest(8, 5, 12, listOf(7, 9), listOf("Mood, anxiety"), "2026-10-01"))
        assertEquals("GET", request().method)
        val request = request()
        assertEquals("POST", request.method)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(8, body.getValue("expected_version").jsonPrimitive.int)
        assertEquals(5L, body.getValue("expected_id").jsonPrimitive.long)
        assertEquals(JsonArray(listOf(JsonPrimitive("Mood, anxiety"))), body["target_tags"])
    }
    @Test fun `visibility writes explicitly false for a private member`() = runTest {
        csrf(); response("null")
        GroupService(api).setVisibility(7, false)
        request()
        val request = request()
        assertEquals("POST", request.method)
        assertEquals(false, Json.parseToJsonElement(request.body.readUtf8()).jsonObject.getValue("show_in_leaderboard").jsonPrimitive.boolean)
    }
    @Test fun `moderation bulk tags preserve comma-containing names and explicit removal`() = runTest {
        csrf(); response("""{"count":2}""")
        assertEquals(2, ModerationService(api).tags(listOf(13, 14), listOf("Mood, anxiety"), listOf("old")).count)
        request()
        val request = request()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(JsonArray(listOf(JsonPrimitive(13), JsonPrimitive(14))), body["question_ids"])
        assertEquals(JsonArray(listOf(JsonPrimitive("Mood, anxiety"))), body["add_tags"])
        assertEquals(JsonArray(listOf(JsonPrimitive("old"))), body["remove_tags"])
    }
    @Test fun `import unlock interprets current numeric window and bounds invalid metadata`() = runTest {
        response("""{"active":true,"expires_in":125}""")
        assertEquals(ImportLimitStatus(true, 125), TransferService(api).importLimitStatus())
        response("""{"active":false,"expires_in":-4}""")
        assertEquals(ImportLimitStatus(false, 0), TransferService(api).importLimitStatus())
        response("""{"active":true,"expires_in":900}""")
        assertEquals(ImportLimitStatus(true, 600), TransferService(api).importLimitStatus())
    }
}
