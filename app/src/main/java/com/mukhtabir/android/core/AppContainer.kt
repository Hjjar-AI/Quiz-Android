package com.mukhtabir.android.core

import android.content.Context
import com.mukhtabir.android.data.auth.AuthRepository
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiClient
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.BlueprintService
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ConfigService
import com.mukhtabir.android.data.remote.HistoryService
import com.mukhtabir.android.data.remote.GroupService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.PlannerService
import com.mukhtabir.android.data.remote.TestSessionService
import com.mukhtabir.android.data.remote.TipsService

/**
 * Manual dependency container.
 *
 * Constructed once by [com.mukhtabir.android.MukhtabirApplication] and
 * read from every composable that needs a service. A DI framework
 * (Hilt) would replace this when the app grows past the current
 * feature set; until then the explicit constructor is easier to
 * trace than a graph.
 *
 * [quizPresetHolder] is a state holder rather than a service: it
 * carries a one-shot "start a quiz with these ids" instruction from
 * the home / mistakes / fragile screens to the quiz tab.
 *
 * The Android [Context] is NOT retained. It is used only to build
 * [ApiClient] (which itself keeps the Application context via
 * `PersistentCookieJar`). Phase 3's `AppPreferences` will receive
 * its own context argument from `MukhtabirApplication`, not from
 * here — this container does not need to be a context holder.
 */
class AppContainer(
    context: Context,
    val apiBaseUrl: String,
    val preferences: AppPreferences = AppPreferences(context.applicationContext),
    val networkMonitor: NetworkMonitor = NetworkMonitor(context.applicationContext),
) {
    init {
        // A terminated process cannot execute the matrix reader's finally block.
        runCatching { context.applicationContext.cacheDir.listFiles()?.filter { it.name.startsWith("exam-matrix-") && it.isFile }?.forEach { it.delete() } }
    }
    val serverSettings = ServerSettings(context.applicationContext)
    val studyReminders = StudyReminders(context.applicationContext)
    val sessionDraftStore = SessionDraftStore(context.applicationContext)
    private val apiClient = ApiClient(
        context = context,
        baseUrl = apiBaseUrl,
        onSessionCleared = {
            // A late old-origin response must not erase archived state after an address change.
            if (ServerSettings(context.applicationContext).read() == apiBaseUrl) {
                sessionDraftStore.clear()
                runCatching { studyReminders.clear() }
            }
        },
        onSessionExpired = {
            if (ServerSettings(context.applicationContext).read() == apiBaseUrl) {
                sessionDraftStore.expireOnlineSession()
                runCatching { studyReminders.clear() }
            }
        },
    )

    val authRepository = AuthRepository(apiClient = apiClient, sessionDraftStore = sessionDraftStore, studyReminders = studyReminders)
    val configService = ConfigService(apiClient)
    val tipsService = TipsService(apiClient)
    val operatorService = com.mukhtabir.android.data.remote.OperatorService(apiClient)
    val transferService = com.mukhtabir.android.data.remote.TransferService(apiClient)
    val questionService = QuestionService(apiClient)
    val moderationService = com.mukhtabir.android.data.remote.ModerationService(apiClient)
    val bookmarkService = BookmarkService(apiClient)
    val contentManagementService = com.mukhtabir.android.data.remote.ContentManagementService(apiClient)
    val categoryService = CategoryService(apiClient)
    val blueprintService = BlueprintService(apiClient)
    val analyticsService = AnalyticsService(apiClient)
    val historyService = HistoryService(apiClient)
    val plannerService = PlannerService(apiClient)
    val groupService = GroupService(apiClient)
    val offlineStudyService = com.mukhtabir.android.data.remote.OfflineStudyService(apiClient)
    val deviceSessionsService = com.mukhtabir.android.data.remote.DeviceSessionsService(apiClient)
    val adminService = AdminService(apiClient)
    val managedContentService = com.mukhtabir.android.data.remote.ManagedContentService(apiClient)
    val masterExamService = com.mukhtabir.android.data.remote.MasterExamService(apiClient)
    val testSessionService = TestSessionService(apiClient)
    val imageRepository = ImageRepository(apiClient)

    val quizPresetHolder = QuizPresetHolder()
}
