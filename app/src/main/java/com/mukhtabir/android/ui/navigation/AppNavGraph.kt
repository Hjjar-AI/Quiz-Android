package com.mukhtabir.android.ui.navigation

import com.mukhtabir.android.ui.admin.VerificationStatsRoute
import com.mukhtabir.android.ui.settings.AdminSettingsRoute
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.PublicConfigDto
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.analytics.AnalyticsRoute
import com.mukhtabir.android.ui.about.AboutScreen
import com.mukhtabir.android.ui.about.LearnerInformation
import com.mukhtabir.android.ui.admin.ModerationRoute
import com.mukhtabir.android.ui.admin.ModerationMode
import com.mukhtabir.android.ui.admin.AdminAccountRoute
import com.mukhtabir.android.ui.admin.ActiveAccountsRoute
import com.mukhtabir.android.ui.admin.AdminUsersRoute
import com.mukhtabir.android.ui.admin.PermissionsRoute
import com.mukhtabir.android.ui.auth.ChangePasswordScreen
import com.mukhtabir.android.ui.bookmarks.BookmarksRoute
import com.mukhtabir.android.ui.categories.CategoriesRoute
import com.mukhtabir.android.ui.fragile.FragileRoute
import com.mukhtabir.android.ui.history.HistoryRoute
import com.mukhtabir.android.ui.history.HistoryScope
import com.mukhtabir.android.ui.home.HomeRoute
import com.mukhtabir.android.ui.mistakes.MistakesRoute
import com.mukhtabir.android.ui.groups.GroupsRoute
import com.mukhtabir.android.ui.knowledge.KnowledgeMapRoute
import com.mukhtabir.android.ui.knowledge.KnowledgeDetailRoute
import com.mukhtabir.android.ui.knowledge.ClinicalCasesRoute
import com.mukhtabir.android.ui.knowledge.ClinicalCaseDetailRoute
import com.mukhtabir.android.ui.knowledge.CaseStemRoute
import com.mukhtabir.android.ui.more.MoreScreen
import com.mukhtabir.android.ui.profile.ProfileScreen
import com.mukhtabir.android.ui.planner.PlannerRoute
import com.mukhtabir.android.ui.quiz.QuizRoute
import com.mukhtabir.android.ui.questions.QuestionImageRoute
import com.mukhtabir.android.ui.questions.QuestionEditorRoute
import com.mukhtabir.android.ui.questions.QuestionLibraryRoute
import com.mukhtabir.android.ui.settings.SettingsRoute

/** Route registration keeps capability guards adjacent to the destinations they protect. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun androidx.navigation.NavGraphBuilder.registerAppDestinations(
    container: AppContainer,
    user: UserDto,
    publicConfig: PublicConfigDto,
    navController: NavHostController,
    displayPreferences: com.mukhtabir.android.core.DisplayPreferences,
    navigationState: AppNavigationState,
    studyNowLabel: String,
    startQuestions: (TestMode, List<Long>, String) -> Unit,
    shareLink: (String, Long) -> Unit,
    onLogout: () -> Unit,
    onLocalSignOut: () -> Unit,
    onChangeServer: (() -> Unit)?,
    onSelectServerFile: ((Int) -> Unit)?,
    onProfileRefresh: () -> Unit,
    profileRefreshing: Boolean,
    profileError: Throwable?,
) {
    var questionBankRevision by navigationState.questionBankRevision
    var removedQuestionIds by navigationState.removedQuestionIds
    var questionActionBusy by navigationState.questionActionBusy
    var quizSessionActive by navigationState.quizSessionActive
    composable(AppRoutes.HOME) {
        HomeRoute(
            user = user,
            bookmarkService = container.bookmarkService,
            questionService = container.questionService,
            tipsService = container.tipsService,
            analyticsService = container.analyticsService,
            plannerService = container.plannerService,
            masterExamService = container.masterExamService,
            imageRepository = container.imageRepository,
            onOpenLibrary = { navController.navigateTopLevel(AppRoutes.MORE_QUESTIONS) },
            onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
            onOpenPlanner = { navController.navigateTopLevel(AppRoutes.MORE_PLANNER) },
            onOpenActivity = { navController.navigateTopLevel("more/activity") },
            onOpenStudyMode = { mode ->
                if (user.has("tests.start")) {
                    container.quizPresetHolder.configure(mode)
                    navController.navigateTopLevel(AppRoutes.QUIZZES)
                }
            },
            onOpenExam = { id -> navController.navigate("more/master_exams/open/$id") },
            onCreateQuestion = ({ navController.navigate(AppRoutes.QUESTION_CREATE) }).takeIf { user.has("questions.create") },
            canStartTests = user.has("tests.start"),
            canViewHistory = user.has("tests.view_own_history"),
            maxQuizQuestions = publicConfig.maxQuizQuestions,
            onStartStudyNow = { ids ->
                container.quizPresetHolder.set(
                    QuizPreset(
                        mode = TestMode.STUDY,
                        questionIds = ids,
                        sessionLabel = studyNowLabel,
                    ),
                )
                navController.navigateTopLevel(AppRoutes.QUIZZES)
            },
            onOpenQuizzes = { navController.navigateTopLevel(AppRoutes.QUIZZES) },
            onOpenBookmarks = { navController.navigateTopLevel(AppRoutes.BOOKMARKS) },
            onOpenAnalytics = { navController.navigateTopLevel(AppRoutes.MORE_ANALYTICS) },
            onOpenHistory = { navController.navigateTopLevel(AppRoutes.HISTORY) },
            onOpenMistakes = { navController.navigateTopLevel(AppRoutes.MORE_MISTAKES) },
            onOpenFragile = { navController.navigateTopLevel(AppRoutes.MORE_FRAGILE) },
            draftStore = container.sessionDraftStore,
            onOpenAssignedExam = { navController.navigateTopLevel("more/master_exams") },
            onOpenOfflineStudy = { navController.navigateTopLevel("more/offline") },
        )
    }
    if (user.has("tests.start")) {
        composable(AppRoutes.QUIZZES) {
            QuizRoute(
                draftStore = container.sessionDraftStore,
                preferences = container.preferences,
                userId = user.id,
                questionService = container.questionService,
                sessionService = container.testSessionService,
                bookmarkService = container.bookmarkService,
                categoryService = container.categoryService,
                blueprintService = container.blueprintService,
                imageRepository = container.imageRepository,
                quizPresetHolder = container.quizPresetHolder,
                canUseBlueprint = user.has("tests.use_blueprint"),
                maxQuizQuestions = publicConfig.maxQuizQuestions,
                onBack = { navController.popBackStack() },
                onSessionActiveChanged = { quizSessionActive = it },
                onOpenHistory = if(user.has("tests.view_own_history"))
                    ({ navController.navigateTopLevel(AppRoutes.HISTORY) }) else null,
            )
        }
    }
    composable(AppRoutes.BOOKMARKS) {
        BookmarksRoute(
            userId = user.id,
            service = container.bookmarkService,
            imageRepository = container.imageRepository,
            canStartSessions = user.has("tests.start"),
            maxQuestions = publicConfig.maxQuizQuestions,
            pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
            onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
            onStartQuestions = startQuestions,
            onDownloadOffline = { ids -> if(user.has("tests.start")) navController.navigate("more/offline?ids=${ids.joinToString(",")}") },
            onBack = { navController.popBackStack() },
        )
    }
    if (user.has("tests.view_own_history")) {
        composable(AppRoutes.HISTORY) {
            HistoryRoute(
                signedInUserId = user.id,
                pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
                service = container.historyService,
                imageRepository = container.imageRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
    composable(AppRoutes.MORE) {
        MoreScreen(
            user = user,
            onNavigate = { route -> navController.navigateTopLevel(route) },
        )
    }

    // ── More-nested destinations ────────────────────────────
    composable("more/offline?ids={ids}", arguments=listOf(navArgument("ids") {defaultValue="";type=NavType.StringType})) {entry ->
        com.mukhtabir.android.ui.offline.OfflineStudyRoute(container.offlineStudyService,container.sessionDraftStore,user.id,
            localOnly=false,canSync=user.has("tests.start"),maxQuestions=publicConfig.maxQuizQuestions,
            initialIds=entry.arguments?.getString("ids").orEmpty(),onBack={navController.popBackStack()},onBusy={questionActionBusy=it})
    }
    composable("more/sessions") {
        com.mukhtabir.android.ui.sessions.DeviceSessionsRoute(container.deviceSessionsService,user.id,
            onBack={navController.popBackStack()},onBusy={questionActionBusy=it},onCurrentRevoked=onLocalSignOut)
    }
    composable("more/notices") {
        com.mukhtabir.android.ui.notices.NoticesRoute(container.masterExamService, container.studyReminders, user.id,
            onOpenExam = { id -> navController.navigate("more/master_exams/open/$id") }, onBack = { navController.popBackStack() })
    }
    composable("more/master_exams/open/{examId}", arguments = listOf(navArgument("examId") { type = NavType.LongType })) { entry ->
        com.mukhtabir.android.ui.masterexams.MasterExamsRoute(user.id, container.masterExamService, container.imageRepository, container.sessionDraftStore,
            onShare={id -> shareLink("exams",id)}, initialExamId = requireNotNull(entry.arguments?.getLong("examId")), onBack = { navController.popBackStack() })
    }
    composable("more/master_exams") {
        com.mukhtabir.android.ui.masterexams.MasterExamsRoute(user.id, container.masterExamService, container.imageRepository, container.sessionDraftStore,
            onShare={id -> shareLink("exams",id)},
            onManage = if (user.has("master_exams.create") || user.has("master_exams.manage_own") || user.has("master_exams.manage_any"))
                ({ navController.navigate(AppRoutes.MASTER_MANAGE) }) else null,
            onBack = { navController.popBackStack() })
    }
    if (user.has("master_exams.create") || user.has("master_exams.manage_own") || user.has("master_exams.manage_any")) {
        composable(AppRoutes.MASTER_MANAGE) {
            com.mukhtabir.android.ui.masterexams.MasterExamManagementRoute(user, container.masterExamService,
                onCreate = { navController.navigate(AppRoutes.MASTER_CREATE) },
                onEdit = { id -> navController.navigate("more/master_exams/manage/$id") })
        }
    }
    if (user.has("master_exams.create")) {
        composable(AppRoutes.MASTER_CREATE) {
            com.mukhtabir.android.ui.masterexams.MasterExamEditorRoute(null, user, container.masterExamService,
                container.questionService, publicConfig.itemsPerPage, container.adminService, container.groupService, container.categoryService, publicConfig.maxChoices, draftStore = container.sessionDraftStore, onBack = { navController.popBackStack() },
                onBusyChanged = { questionActionBusy = it },
                onDraft = { draft -> navController.navigate("more/master_drafts/$draft") },
                onPreview = { exam -> navController.navigate("more/master_preview/$exam") },
                onResults = { exam -> navController.navigate("more/master_results/$exam") })
        }
    }
    if (user.has("master_exams.manage_own") || user.has("master_exams.manage_any")) {
        composable(AppRoutes.MASTER_EDIT, arguments = listOf(navArgument("examId") { type = NavType.LongType })) { entry ->
            com.mukhtabir.android.ui.masterexams.MasterExamEditorRoute(requireNotNull(entry.arguments?.getLong("examId")),
                user, container.masterExamService, container.questionService, publicConfig.itemsPerPage, container.adminService, container.groupService, container.categoryService, publicConfig.maxChoices, draftStore = container.sessionDraftStore,
                onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it },
                onDraft = { draft -> navController.navigate("more/master_drafts/$draft") },
                onPreview = { exam -> navController.navigate("more/master_preview/$exam") },
                onResults = { exam -> navController.navigate("more/master_results/$exam") })
        }
    }
    if (user.has("master_exams.drafts_library")) {
        composable("more/master_drafts") {
            com.mukhtabir.android.ui.masterexams.ExamDraftLibraryRoute(user, container.masterExamService, container.categoryService,
                publicConfig.maxChoices, publicConfig.itemsPerPage, draftStore = container.sessionDraftStore, onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it })
        }
        composable("more/master_drafts/{draftId}", arguments = listOf(navArgument("draftId") { type = NavType.LongType })) { entry ->
            com.mukhtabir.android.ui.masterexams.ExamDraftLibraryRoute(user, container.masterExamService, container.categoryService,
                publicConfig.maxChoices, publicConfig.itemsPerPage, draftStore = container.sessionDraftStore, initialId = requireNotNull(entry.arguments?.getLong("draftId")),
                onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it })
        }
    }
    if (com.mukhtabir.android.data.remote.canExportExamResults(user)) {
        composable("more/master_results") {
            com.mukhtabir.android.ui.masterexams.ExamResultsPicker(user, container.masterExamService) { exam -> navController.navigate("more/master_results/$exam") }
        }
        composable("more/master_results/{examId}", arguments = listOf(navArgument("examId") { type = NavType.LongType })) { entry ->
            com.mukhtabir.android.ui.masterexams.ExamReviewRoute(requireNotNull(entry.arguments?.getLong("examId")), user,
                container.masterExamService, container.transferService, container.imageRepository, false,
                onBack = { navController.popBackStack() }, onCsv = { navController.navigate("more/master_csv?examId=" + entry.arguments?.getLong("examId")) },onBusyChanged={questionActionBusy=it})
        }
    }
    if (user.has("master_exams.preview") && (user.has("master_exams.manage_own") || user.has("master_exams.manage_any"))) {
        composable("more/master_preview/{examId}", arguments = listOf(navArgument("examId") { type = NavType.LongType })) { entry ->
            com.mukhtabir.android.ui.masterexams.ExamReviewRoute(requireNotNull(entry.arguments?.getLong("examId")), user,
                container.masterExamService, container.transferService, container.imageRepository, true,
                onBack = { navController.popBackStack() }, onCsv = {},onBusyChanged={questionActionBusy=it})
        }
    }
    if (com.mukhtabir.android.ui.questions.canUseAuthoringDrafts(user)) composable("more/authoring_drafts") {
        com.mukhtabir.android.ui.questions.AuthoringDraftsRoute(user, container.sessionDraftStore) { key ->
            val id = key.substringAfter('-')
            val route = when (key.substringBefore('-')) {
                "question" -> if (id == "new") AppRoutes.QUESTION_CREATE else "more/questions/$id/edit"
                "image" -> "more/questions/$id/image"
                "exam" -> if (id == "new") AppRoutes.MASTER_CREATE else "more/master_exams/manage/$id"
                "draft" -> if (id == "new") "more/master_drafts" else "more/master_drafts/$id"
                else -> null
            }
            if (route != null) navController.navigate(route)
        }
    }
    if (user.has("admin.database") || user.has("admin.seed")) composable("more/admin/tools") {
        com.mukhtabir.android.ui.admin.OperatorToolsRoute(user, container.operatorService,
            onQuestion = { id -> navController.navigateContent("more/questions/$id") }, onBank = { navController.navigate(AppRoutes.MORE_QUESTIONS) },
            onBusyChanged = { questionActionBusy = it })
    }
    composable("more/help") { LearnerInformation(false) { navController.popBackStack() } }
    composable("more/privacy") { LearnerInformation(true) { navController.popBackStack() } }
    composable(AppRoutes.MORE_QUESTIONS) {
        QuestionLibraryRoute(
            defaultDifficulty = displayPreferences.difficulty,
            categoryService = container.categoryService,
            userId = user.id,
            bankRevision = questionBankRevision,
            removedQuestionIds = removedQuestionIds,
            maxQuestions = publicConfig.maxQuizQuestions,
            service = container.questionService,
            imageRepository = container.imageRepository,
            canReview = user.has("questions.verify"),
            canCreate = user.has("questions.create"),
            canStartSessions = user.has("tests.start"),
            pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
            onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
            onCreateQuestion = { navController.navigate(AppRoutes.QUESTION_CREATE) },
            onDownloadOffline = { ids -> if(user.has("tests.start")) navController.navigate("more/offline?ids=${ids.joinToString(",")}") },
            onStartQuestions = startQuestions,
            navigationStore=container.sessionDraftStore,
            detailStateOwner = container.apiBaseUrl,
            detailContent = { id, select, lock ->
                AppQuestionDetail(id, user, container, navController, questionBankRevision,
                    onBusyChanged = {},
                    onPaneLockChanged = { locked -> lock(locked); questionActionBusy = locked },
                    startQuestions = startQuestions, shareLink = shareLink,
                    onDuplicated = { copied -> questionBankRevision += 1; select(copied) },
                    onDeleted = { removed ->
                        removedQuestionIds = removedQuestionIds + removed
                        questionBankRevision += 1
                        select(null)
                    })
            },
        )
    }
    composable(
        route = AppRoutes.QUESTION_DETAIL,
        arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
    ) { entry ->
        AppQuestionDetail(
            questionId = requireNotNull(entry.arguments?.getLong("questionId")), user = user, container = container,
            navController = navController, bankRevision = questionBankRevision,
            onBusyChanged = { questionActionBusy = it }, startQuestions = startQuestions, shareLink = shareLink,
            onDuplicated = { id ->
                questionBankRevision += 1
                navController.navigate("more/questions/$id") {
                    popUpTo(AppRoutes.QUESTION_DETAIL) { inclusive = true }
                }
            },
            onDeleted = { id ->
                removedQuestionIds = removedQuestionIds + id
                questionBankRevision += 1
                if (!navController.popBackStack(AppRoutes.MORE_QUESTIONS, false)) {
                    navController.navigate(AppRoutes.MORE_QUESTIONS) {
                        popUpTo(AppRoutes.QUESTION_DETAIL) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
    }
    if (user.has("questions.edit_any") || user.has("questions.upload_image")) {
        composable(AppRoutes.QUESTION_IMAGE,arguments=listOf(navArgument("questionId") { type=NavType.LongType })) { entry ->
            QuestionImageRoute(questionId=requireNotNull(entry.arguments?.getLong("questionId")),userId=user.id,
                questions=container.questionService,images=container.imageRepository,
                canEditAny=user.has("questions.edit_any"),canUploadOwn=user.has("questions.upload_image"),draftStore=container.sessionDraftStore,
                onBack={navController.popBackStack()},onChanged={questionBankRevision+=1},
                onBusyChanged={questionActionBusy=it})
        }
    }
    if (user.has("questions.create")) {
        composable(AppRoutes.QUESTION_CREATE) {
            QuestionEditorRoute(
                questionId = null,
                questionService = container.questionService,
                categoryService = container.categoryService,
                onBack = { navController.popBackStack() },
                maxChoices = publicConfig.maxChoices,
                userId = user.id,
                canCreate = true,
                canEditOwn = user.has("questions.edit_own"),
                canEditAny = user.has("questions.edit_any"),
                draftStore = container.sessionDraftStore,
            ) { id ->
                questionBankRevision += 1
                navController.navigate("more/questions/$id") { popUpTo(AppRoutes.QUESTION_CREATE) { inclusive = true } }
            }
        }
    }
    if (user.has("questions.edit_any") || user.has("questions.edit_own")) {
        composable(AppRoutes.QUESTION_EDIT, arguments = listOf(navArgument("questionId") { type = NavType.LongType })) { entry ->
            QuestionEditorRoute(
                questionId = requireNotNull(entry.arguments?.getLong("questionId")),
                questionService = container.questionService,
                categoryService = container.categoryService,
                onBack = { navController.popBackStack() },
                maxChoices = publicConfig.maxChoices,
                userId = user.id,
                canCreate = user.has("questions.create"),
                canEditOwn = user.has("questions.edit_own"),
                canEditAny = user.has("questions.edit_any"),
                draftStore = container.sessionDraftStore,
            ) { id ->
                questionBankRevision += 1
                navController.navigate("more/questions/$id") { popUpTo(AppRoutes.QUESTION_DETAIL) { inclusive = true } }
            }
        }
    }
    composable(AppRoutes.MORE_ANALYTICS) {
        AnalyticsRoute(
            service = container.analyticsService,
            userId = user.id,
            canViewAll = user.has("analytics.view_all"),
            canViewActiveUsers = user.has("admin.active_users"),
            onBack = { navController.popBackStack() },
        )
    }
    composable(AppRoutes.MORE_MISTAKES) {
        MistakesRoute(
            userId = user.id,
            service = container.questionService,
            imageRepository = container.imageRepository,
            canStartSessions = user.has("tests.start"),
            maxQuestions = publicConfig.maxQuizQuestions,
            pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
            onStartQuestions = startQuestions,
            onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
        )
    }
    composable(AppRoutes.MORE_FRAGILE) {
        FragileRoute(
            userId = user.id,
            service = container.questionService,
            imageRepository = container.imageRepository,
            canStartSessions = user.has("tests.start"),
            maxQuestions = publicConfig.maxQuizQuestions,
            pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
            onStartQuestions = startQuestions,
            onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
        )
    }
    composable(AppRoutes.MORE_PROFILE) {
        ProfileScreen(
            user = user,
            onChangePassword = { navController.navigate(AppRoutes.MORE_CHANGE_PASSWORD) },
            onAbout = { navController.navigateTopLevel(AppRoutes.MORE_ABOUT) },
            onSignOut = onLogout,
            onAdminUsers = { navController.navigateTopLevel(AppRoutes.MORE_ADMIN_USERS) },
            onAdminSettings = { navController.navigateTopLevel(AppRoutes.MORE_ADMIN_SETTINGS) },
            onAdminPermissions = { navController.navigateTopLevel(AppRoutes.MORE_ADMIN_PERMISSIONS) },
            onRefresh=onProfileRefresh, refreshing=profileRefreshing, refreshError=profileError,
        )
    }
    composable(AppRoutes.MORE_CHANGE_PASSWORD) {
        ChangePasswordScreen(
            repository = container.authRepository,
            onDone = { navController.popBackStack() },
            onSignOut = onLocalSignOut,
            forced = false,
            onBusyChanged = { questionActionBusy = it },
        )
    }
    composable(AppRoutes.MORE_CATEGORIES) {
        CategoriesRoute(container.categoryService, user.has("categories.manage"), container.sessionDraftStore, user.id,
            onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it })
    }
    composable(AppRoutes.MORE_KNOWLEDGE_MAP) {
        KnowledgeMapRoute(container.questionService, user.has("tests.start"), user.id,
            onOpenDetail={id -> navController.navigateContent("more/knowledge/$id")}) { ids, label ->
            startQuestions(TestMode.STUDY, ids, label)
        }
    }
    composable(AppRoutes.KNOWLEDGE_DETAIL,arguments=listOf(navArgument("knowledgeId") { type=NavType.LongType })) { entry ->
        KnowledgeDetailRoute(id=requireNotNull(entry.arguments?.getLong("knowledgeId")),userId=user.id,
            service=container.questionService,canStart=user.has("tests.start"),maxQuestions=publicConfig.maxQuizQuestions,
            onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
            onReview={ids,label -> startQuestions(TestMode.STUDY,ids,label)})
    }
    composable(AppRoutes.MORE_CASES) {
        ClinicalCasesRoute(userId=user.id,service=container.questionService,pageSize=displayPreferences.perPage.takeIf {it>0} ?: publicConfig.itemsPerPage,onBack={navController.popBackStack()},
            onOpen={key -> navController.navigateContent("more/cases/${android.net.Uri.encode(key)}")})
    }
    composable(AppRoutes.CASE_DETAIL,arguments=listOf(navArgument("caseKey") { type=NavType.StringType })) { entry ->
        ClinicalCaseDetailRoute(key=requireNotNull(entry.arguments?.getString("caseKey")),userId=user.id,
            service=container.questionService,canStart=user.has("tests.start"),maxQuestions=publicConfig.maxQuizQuestions,
            canEditStemAny=user.has("questions.edit_case_stem_any"),canEditStemOwn=user.has("questions.edit_case_stem_own"),
            onEditStem={navController.navigate("more/cases/${android.net.Uri.encode(requireNotNull(entry.arguments?.getString("caseKey")))}/stem")},
            onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
            onReview={ids,label -> startQuestions(TestMode.STUDY,ids,label)})
    }
    if(user.has("questions.manage_tags")) composable("more/admin/tag_management") {
        com.mukhtabir.android.ui.admin.TagManagementRoute(container.contentManagementService, container.questionService,
            user.id, user.has("questions.manage_tags"), container.sessionDraftStore, onBack={navController.popBackStack()},
            onBusy={questionActionBusy=it}, onChanged={questionBankRevision++})
    }
    com.mukhtabir.android.ui.admin.ContentKind.values().forEach { kind ->
        if(com.mukhtabir.android.ui.admin.mayManageContent(user, kind)) {
            val route=if(kind==com.mukhtabir.android.ui.admin.ContentKind.KNOWLEDGE) "more/admin/knowledge" else "more/admin/cases"
            composable(route) {
                com.mukhtabir.android.ui.admin.ContentManagementRoute(kind, container.contentManagementService, container.categoryService,
                    user, displayPreferences.perPage.takeIf { it>0 } ?: publicConfig.itemsPerPage, store=container.sessionDraftStore,
                    onBack={navController.popBackStack()}, onBusy={questionActionBusy=it}, onChanged={questionBankRevision++},
                    onQuestion={navController.navigateContent("more/questions/$it")}, onReadKnowledge={navController.navigateContent("more/knowledge/$it")},
                    onStem={navController.navigate("more/cases/${android.net.Uri.encode(it)}/stem")})
            }
        }
    }
    if(user.has("questions.edit_case_stem_any") || user.has("questions.edit_case_stem_own")) {
        composable(AppRoutes.CASE_STEM,arguments=listOf(navArgument("caseKey") { type=NavType.StringType })) { entry ->
            CaseStemRoute(key=requireNotNull(entry.arguments?.getString("caseKey")),userId=user.id,
                service=container.questionService,canEditAny=user.has("questions.edit_case_stem_any"),
                canEditOwn=user.has("questions.edit_case_stem_own"),store=container.sessionDraftStore,onBack={navController.popBackStack()},
                onChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
        }
    }
    composable(AppRoutes.MORE_PLANNER) {
        PlannerRoute(container.plannerService, container.categoryService, container.questionService, user.id,
            container.sessionDraftStore, onBack={navController.popBackStack()}, onBusy={questionActionBusy=it})
    }
    composable(AppRoutes.MORE_GROUPS) {
        GroupsRoute(container.groupService, user.id)
    }
    listOf(com.mukhtabir.android.data.remote.ManagedKind.GROUP,com.mukhtabir.android.data.remote.ManagedKind.BLUEPRINT).forEach { kind ->
        if(user.has(kind.capability)) composable(if(kind==com.mukhtabir.android.data.remote.ManagedKind.GROUP) "more/admin/groups" else "more/admin/blueprints") {
            com.mukhtabir.android.ui.admin.ManagedContentRoute(kind,container.managedContentService,
                container.categoryService,container.adminService,user.id,user.has(kind.capability),user.has("admin.users"),
                publicConfig.itemsPerPage,onBack={navController.popBackStack()},onBusy={questionActionBusy=it})
        }
    }
    if (user.has("admin.database")) composable("more/admin/transfers?verified={verified}",arguments=listOf(navArgument("verified") {type=NavType.BoolType;defaultValue=false})) {entry ->
        com.mukhtabir.android.ui.transfer.TransferRoute(container.transferService, container.adminService, container.questionService,
            container.categoryService, user, displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage, onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it },initialVerified=entry.arguments?.getBoolean("verified")==true)
    }
    if (com.mukhtabir.android.data.remote.canExportExamResults(user)) composable("more/master_csv?examId={examId}", arguments = listOf(navArgument("examId") { type = NavType.LongType; defaultValue = 0L })) { entry ->
        com.mukhtabir.android.ui.transfer.ExamCsvRoute(container.transferService, container.questionService,
            container.masterExamService, user, onBusyChanged = { questionActionBusy = it }, initialExamId = entry.arguments?.getLong("examId")?.takeIf { it > 0 })
    }
    composable("more/activity") {
        com.mukhtabir.android.ui.analytics.ActivityRoute(container.analyticsService, user.id, container.preferences)
    }
    composable(AppRoutes.MORE_SETTINGS) {
        SettingsRoute(container.preferences, container.apiBaseUrl, container.serverSettings.isFileConfigured(), onChangeServer,
            savedServerFiles = container.serverSettings.importedFiles(), activeFileSlot = container.serverSettings.activeImportedSlot(),
            onSelectServerFile = onSelectServerFile)
    }
    if (user.has("admin.verification_stats")) {
        composable(AppRoutes.MORE_VERIFICATION_STATS) {
            VerificationStatsRoute(container.analyticsService, user.id, user.has("admin.verification_stats"),onExport=({navController.navigate("more/admin/transfers?verified=true")}).takeIf {user.has("admin.database")})
        }
    }
    if (user.has("admin.settings")) {
        composable(AppRoutes.MORE_ADMIN_SETTINGS) {
            AdminSettingsRoute(container.adminService, if (container.serverSettings.isFileConfigured()) "" else container.apiBaseUrl, canManageSystem = user.has("admin.settings"), store=container.sessionDraftStore, userId=user.id,
                onBusyChanged = { questionActionBusy = it })
        }
    }
    composable(AppRoutes.MORE_ABOUT) {
        AboutScreen(onHelp={navController.navigateTopLevel("more/help")}, onPrivacy={navController.navigateTopLevel("more/privacy")})
    }
    if (user.has("admin.flags")) {
        composable(AppRoutes.MORE_ADMIN_FLAGS) {
            ModerationRoute(mode=ModerationMode.FLAGS,service=container.moderationService,
                pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("admin.flags"),
                onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
        }
    }
    if (user.has("questions.bulk_tag")) {
        composable(AppRoutes.MORE_ADMIN_TAGS) {
            ModerationRoute(mode=ModerationMode.TAGS,service=container.moderationService,
                pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("questions.bulk_tag"),
                onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
        }
    }
    if (user.has("questions.bulk_verify")) {
        composable(AppRoutes.MORE_ADMIN_VERIFICATION) {
            ModerationRoute(mode=ModerationMode.VERIFICATION,service=container.moderationService,
                pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("questions.bulk_verify"),
                onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
        }
    }
    if (user.has("admin.active_users")) {
        composable(AppRoutes.MORE_ADMIN_ACTIVE_USERS) {
            ActiveAccountsRoute(service=container.adminService,userId=user.id,canView=user.has("admin.active_users"),
                canManageUsers=user.has("admin.users"),onBack={navController.popBackStack()},
                onOpenUser={id -> navController.navigate("more/admin/users/$id/account")})
        }
    }
    if (user.has("admin.users")) {
        composable(AppRoutes.MORE_ADMIN_ACCOUNT,arguments=listOf(navArgument("userId") { type=NavType.LongType })) { entry ->
            AdminAccountRoute(id=requireNotNull(entry.arguments?.getLong("userId")),actingUserId=user.id,
                service=container.adminService,canManage=user.has("admin.users"),onBack={navController.popBackStack()},
                onOwnPassword={navController.navigate(AppRoutes.MORE_CHANGE_PASSWORD)},onBusyChanged={questionActionBusy=it},
                onPermissions=({navController.navigate(AppRoutes.MORE_ADMIN_PERMISSIONS+"?user="+entry.arguments?.getLong("userId"))}).takeIf {user.has("admin.permissions")})
        }
        composable(AppRoutes.MORE_ADMIN_USERS) {
            AdminUsersRoute(
                service = container.adminService,
                pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
                roles = publicConfig.roles,
                canManage = user.has("admin.users"),
                actingUserId = user.id,
                onOpenUser = { id -> navController.navigate("more/admin/users/$id/account") },
                onPermissions=({id:Long->navController.navigate(AppRoutes.MORE_ADMIN_PERMISSIONS+"?user=$id")}).takeIf {user.has("admin.permissions")},
                onBusyChanged={questionActionBusy=it},
            )
        }
    }
    if (user.has("admin.permissions")) {
        composable(AppRoutes.MORE_ADMIN_PERMISSIONS+"?user={targetUser}",arguments=listOf(navArgument("targetUser"){type=NavType.LongType;defaultValue=0L})) {entry ->
            PermissionsRoute(container.adminService, canManage=user.has("admin.permissions"), actingUserId=user.id, pageSize=publicConfig.itemsPerPage, roles=publicConfig.roles, onBusyChanged={questionActionBusy=it},onBack={navController.popBackStack()},initialUserId=entry.arguments?.getLong("targetUser")?.takeIf {it>0})
        }
    }
    if (user.has("tests.view_all_history")) {
        composable(AppRoutes.MORE_ADMIN_HISTORY) {
            HistoryRoute(
                signedInUserId = user.id,
                scope = HistoryScope.ALL,
                pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
                service = container.historyService,
                imageRepository = container.imageRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
