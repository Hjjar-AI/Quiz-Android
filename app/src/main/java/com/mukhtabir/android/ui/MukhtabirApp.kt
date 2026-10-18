package com.mukhtabir.android.ui

import com.mukhtabir.android.ui.components.AppLaunchLayout
import com.mukhtabir.android.ui.components.AppLaunchHeader
import com.mukhtabir.android.ui.components.OfflineLaunchActions
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.ui.components.AppTextButton

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import androidx.lifecycle.repeatOnLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import com.mukhtabir.android.core.ShellNavigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ListAlt
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import com.mukhtabir.android.ui.components.AppBackBar
import com.mukhtabir.android.ui.admin.VerificationStatsRoute
import com.mukhtabir.android.ui.settings.AdminSettingsRoute
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.PublicConfigDto
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.analytics.AnalyticsRoute
import com.mukhtabir.android.ui.about.AboutScreen
import com.mukhtabir.android.ui.about.LearnerInformation
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import com.mukhtabir.android.ui.admin.ModerationRoute
import com.mukhtabir.android.ui.admin.ModerationMode
import com.mukhtabir.android.ui.admin.AdminAccountRoute
import com.mukhtabir.android.ui.admin.ActiveAccountsRoute
import com.mukhtabir.android.ui.admin.AdminUsersRoute
import com.mukhtabir.android.ui.admin.PermissionsRoute
import com.mukhtabir.android.ui.auth.AuthViewModel
import com.mukhtabir.android.ui.auth.ChangePasswordScreen
import com.mukhtabir.android.ui.auth.LoginScreen
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
import com.mukhtabir.android.ui.more.AppDrawerContent
import com.mukhtabir.android.ui.more.MoreScreen
import com.mukhtabir.android.ui.components.AppTopBar
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.mukhtabir.android.ui.profile.ProfileScreen
import com.mukhtabir.android.ui.planner.PlannerRoute
import com.mukhtabir.android.ui.quiz.QuizRoute
import com.mukhtabir.android.ui.questions.QuestionImageRoute
import com.mukhtabir.android.ui.questions.QuestionDetailRoute
import com.mukhtabir.android.ui.questions.QuestionEditorRoute
import com.mukhtabir.android.ui.questions.QuestionLibraryRoute
import com.mukhtabir.android.ui.settings.SettingsRoute

private object Routes {
    const val HOME = "home"
    const val QUIZZES = "quizzes"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
    const val MORE = "more"

    // More-nested destinations.
    const val MORE_CATEGORIES = "more/categories"
    const val MORE_QUESTIONS = "more/questions"
    const val QUESTION_DETAIL = "more/questions/{questionId}"
    const val QUESTION_CREATE = "more/questions/create"
    const val QUESTION_IMAGE = "more/questions/{questionId}/image"
    const val QUESTION_EDIT = "more/questions/{questionId}/edit"
    const val MASTER_MANAGE = "more/master_exams/manage"
    const val MASTER_CREATE = "more/master_exams/manage/new"
    const val MASTER_EDIT = "more/master_exams/manage/{examId}"
    const val MORE_MISTAKES = "more/mistakes"
    const val MORE_FRAGILE = "more/fragile"
    const val MORE_ANALYTICS = "more/analytics"
    const val MORE_KNOWLEDGE_MAP = "more/knowledge_map"
    const val KNOWLEDGE_DETAIL = "more/knowledge/{knowledgeId}"
    const val MORE_CASES = "more/cases"
    const val CASE_DETAIL = "more/cases/{caseKey}"
    const val CASE_STEM = "more/cases/{caseKey}/stem"
    const val MORE_PLANNER = "more/planner"
    const val MORE_GROUPS = "more/groups"
    const val MORE_PROFILE = "more/profile"
    const val MORE_SETTINGS = "more/settings"
    const val MORE_ADMIN_SETTINGS = "more/admin/settings"
    const val MORE_CHANGE_PASSWORD = "more/change_password"
    const val MORE_ABOUT = "more/about"
    const val MORE_ADMIN_FLAGS = "more/admin/flags"
    const val MORE_ADMIN_TAGS = "more/admin/tags"
    const val MORE_VERIFICATION_STATS = "more/admin/verification_stats"
    const val MORE_ADMIN_VERIFICATION = "more/admin/verification"
    const val MORE_ADMIN_ACTIVE_USERS = "more/admin/active_users"
    const val MORE_ADMIN_ACCOUNT = "more/admin/users/{userId}/account"
    const val MORE_ADMIN_USERS = "more/admin/users"
    const val MORE_ADMIN_PERMISSIONS = "more/admin/permissions"
    const val MORE_ADMIN_HISTORY = "more/admin/history"
}

private data class TabItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val capability: String? = null,
)

private val tabs = listOf(
    TabItem(Routes.HOME, R.string.home, Icons.Filled.Home),
    TabItem(Routes.MORE_QUESTIONS, R.string.nav_library_short, Icons.Filled.ListAlt),
    TabItem(Routes.QUIZZES, R.string.quizzes, Icons.Filled.Quiz, "tests.start"),
    TabItem(Routes.BOOKMARKS, R.string.bookmarks, Icons.Filled.Bookmark),
)

@Composable
fun MukhtabirApp(container: AppContainer,onChangeServer: (() -> Unit)? = null,
    onSelectServerFile: ((Int) -> Unit)? = null,
    pendingLink: com.mukhtabir.android.core.AppLink? = null,onConsumeLink: () -> Unit = {}, onDemo: (() -> Unit)? = null) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(container.authRepository),
    )
    val authState by authViewModel.state.collectAsState()
    var startupFinished by remember(container) { mutableStateOf(false) }
    LaunchedEffect(authState.isRestoring) { if(authState.isRestoring) startupFinished=false }
    com.mukhtabir.android.ui.components.RefreshOnResume { authViewModel.refreshUser(force=false) }
    val profileLifecycle=androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(container,authState.user?.id,profileLifecycle) {
        if(authState.user!=null) profileLifecycle.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while(true) {authViewModel.refreshUser(force=false);delay(60_000)}
        }
    }
    var localOfflineOwner by remember(container) { mutableStateOf<Long?>(null) }
    val cachedOwner = remember(container, authState.user, authState.isRestoring, authState.restorePending, authState.sessionExpiredMessage) {
        container.sessionDraftStore.cachedOfflineOwner().getOrNull()
    }
    LaunchedEffect(cachedOwner) {if(localOfflineOwner!=null && cachedOwner!=localOfflineOwner) localOfflineOwner=null}
    LaunchedEffect(authState.user?.id) {
        if (authState.user != null && authState.user?.id != localOfflineOwner) localOfflineOwner=null
    }
    var showRecoveryOptions by rememberSaveable(container) { mutableStateOf(false) }
    var publicInformation by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(publicInformation != null) { publicInformation = null }
    val publicConfig by produceState(
        initialValue = PublicConfigDto(),
        key1 = container.configService,
    ) {
        value = runCatching { container.configService.getConfig() }.getOrDefault(value)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val sessionExpiredCopy = stringResource(if(authState.sessionExpiredMessage=="ACCOUNT_EXPIRED") R.string.api_account_expired else R.string.session_expired_message)

    LaunchedEffect(authState.sessionExpiredMessage) {
        if (authState.sessionExpiredMessage != null) {
            snackbarHostState.showSnackbar(sessionExpiredCopy)
            authViewModel.consumeSessionExpiredMessage()
        }
    }

    LaunchedEffect(authState.user) {
        if (authState.user == null) container.quizPresetHolder.clear()
    }

    val usesAppScaffold = publicInformation == null && localOfflineOwner == null && !authState.isRestoring && startupFinished &&
        authState.user != null && !authState.user!!.mustChangePassword
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (usesAppScaffold) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))) {
            when {
                localOfflineOwner != null -> com.mukhtabir.android.ui.offline.OfflineStudyRoute(
                    container.offlineStudyService,container.sessionDraftStore,requireNotNull(localOfflineOwner),
                    localOnly=true,canSync=false,maxQuestions=publicConfig.maxQuizQuestions,onBack={localOfflineOwner=null})
                publicInformation != null -> LearnerInformation(privacy=publicInformation=="privacy", onBack={ publicInformation=null })
                authState.isRestoring || !startupFinished -> StartupScreen(onFinished={startupFinished=true},
                    onOffline=cachedOwner?.let {owner -> ({localOfflineOwner=owner})}, onDemo=onDemo)

                authState.restorePending && authState.user == null -> AppLaunchLayout {
                    AppLaunchHeader()
                    AppCard(Modifier.fillMaxWidth()) {
                        AppCardBody(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.saved_login_retry),
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() })
                            if (authState.isLoading) AppLoadingIndicator()
                            ErrorState(throwable = authState.restoreError,
                                onRetry = if (authState.isLoading) null else authViewModel::restoreSession)
                        }
                    }
                    OfflineLaunchActions(
                        onDownloaded = cachedOwner?.let { owner -> ({ localOfflineOwner = owner }) },
                        onDemo = onDemo, enabled = !authState.isLoading, downloadedEnabled = true)
                    com.mukhtabir.android.ui.components.AppSection(stringResource(R.string.startup_more_options),
                        enabled = !authState.isLoading, expandedOverride = showRecoveryOptions,
                        onExpandedChange = { showRecoveryOptions = it }) {
                        OutlinedButton(authViewModel::logout, enabled = !authState.isLoading) {
                            Text(stringResource(R.string.sign_out))
                        }
                        onChangeServer?.let { change ->
                            OutlinedButton(change,enabled=!authState.isLoading) { Text(stringResource(R.string.server_change)) }
                        }
                    }
                }

                authState.user == null -> Box(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        state = authState,
                        onUsernameChange = authViewModel::updateUsername,
                        onPasswordChange = authViewModel::updatePassword,
                        onLogin = authViewModel::login,
                        onHelp = { publicInformation="help" },
                        onPrivacy = { publicInformation="privacy" },
                        onDemo = onDemo,
                        onChangeServer = onChangeServer,
                        onOffline = cachedOwner?.let {owner -> ({localOfflineOwner=owner})},
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                authState.user!!.mustChangePassword -> {
                    ChangePasswordScreen(
                        repository = container.authRepository,
                        onDone = authViewModel::completeForcedPasswordChange,
                        onSignOut = authViewModel::forgetSession,
                        forced = true,
                    )
                }

                else -> AuthenticatedApp(
                    container = container,
                    user = authState.user!!,
                    publicConfig = publicConfig,
                    onLogout = authViewModel::logout,
                    onLocalSignOut = authViewModel::forgetSession,
                    onChangeServer = onChangeServer,
                    onSelectServerFile = onSelectServerFile,
                    onProfileRefresh = {authViewModel.refreshUser()},
                    onProfileApplicationAllowed = authViewModel::setProfileApplicationAllowed,
                    profileRefreshing = authState.profileRefreshing, profileError=authState.profileError,
                    snackbarHostState = snackbarHostState,
                    pendingLink = pendingLink, onConsumeLink = onConsumeLink,
                )
            }
        }
    }
}

@Composable
private fun StartupScreen(onFinished: () -> Unit, onOffline: (() -> Unit)? = null, onDemo: (() -> Unit)? = null) {
    AppLaunchLayout(centerVertically = true) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.theme.AppTheme.spacing.content)) {
            com.mukhtabir.android.ui.components.StartupBrand(onFinished = onFinished)
            AppLoadingIndicator()
            Text(stringResource(R.string.launch_checking_session),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        OfflineLaunchActions(onDownloaded = onOffline, onDemo = onDemo)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AuthenticatedApp(
    container: AppContainer,
    user: UserDto,
    publicConfig: PublicConfigDto,
    onLogout: () -> Unit,
    onLocalSignOut: () -> Unit,
    onChangeServer: (() -> Unit)?,
    onSelectServerFile: ((Int) -> Unit)?,
    onProfileRefresh: () -> Unit, onProfileApplicationAllowed: (Boolean) -> Unit,
    profileRefreshing: Boolean, profileError: Throwable?,
    snackbarHostState: SnackbarHostState,
    pendingLink: com.mukhtabir.android.core.AppLink?, onConsumeLink: () -> Unit,
) {
    val displayPreferences by container.preferences.display.collectAsState()

    val navController = rememberNavController()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val shareLink: (String,Long) -> Unit = {kind,id ->
        val link=com.mukhtabir.android.core.AppLink(kind,id,container.apiBaseUrl).uri().toString()
        appContext.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,link),null))
    }
    var questionBankRevision by remember { mutableStateOf(0) }
    var removedQuestionIds by remember { mutableStateOf(emptySet<Long>()) }
    var questionActionBusy by remember { mutableStateOf(false) }
    var quizSessionActive by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable(user.id) { mutableStateOf(Routes.HOME) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(currentRoute) {
        when {
            tabs.any { it.route == currentRoute } -> selectedTab = requireNotNull(currentRoute)
            currentRoute?.startsWith("more/questions/") == true -> Unit // Keep the originating tab.
            currentRoute != null -> selectedTab = when {
                ShellNavigation.matches(currentRoute, "more/cases") || ShellNavigation.matches(currentRoute, "more/knowledge") ||
                    currentRoute == Routes.MORE_QUESTIONS -> Routes.MORE_QUESTIONS
                currentRoute.startsWith("more/master") || currentRoute == Routes.HISTORY -> Routes.QUIZZES
                else -> Routes.HOME
            }
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val focusedRoute = ShellNavigation.isFocused(currentRoute)
    val profileSensitiveRoutes = setOf(Routes.MORE_CATEGORIES, Routes.MORE_ADMIN_SETTINGS, Routes.MORE_ADMIN_PERMISSIONS+"?user={targetUser}")
    DisposableEffect(questionActionBusy,quizSessionActive,currentRoute) {
        onProfileApplicationAllowed(!questionActionBusy && !quizSessionActive && !ShellNavigation.preservesDraft(currentRoute) && currentRoute !in profileSensitiveRoutes &&
            currentRoute?.startsWith("more/master_exams/open/")!=true)
        onDispose { }
    }
    com.mukhtabir.android.ui.components.RefreshOnResume { onProfileRefresh() }
    LaunchedEffect(currentRoute) {
        if(!questionActionBusy && !quizSessionActive && !ShellNavigation.preservesDraft(currentRoute)) onProfileRefresh()
    }
    val drawerNavigationAllowed = !questionActionBusy && !quizSessionActive && !focusedRoute
    LaunchedEffect(drawerNavigationAllowed) { if (!drawerNavigationAllowed) drawerState.close() }
    val openDrawer: () -> Unit = { if (drawerNavigationAllowed) drawerScope.launch { drawerState.open() } }
    val isOnline by container.networkMonitor.isOnline.collectAsState()
    val expiryDays by produceState(user.remainingDays(), user) {
        while (true) { value=user.remainingDays(); delay(60_000) }
    }
    val studyNowLabel = stringResource(R.string.study_now_title)
    val startQuestions: (TestMode, List<Long>, String) -> Unit = { mode, ids, label ->
        if (user.has("tests.start") && ids.isNotEmpty()) {
            container.quizPresetHolder.set(QuizPreset(mode, ids, label))
            navController.navigateTopLevel(Routes.QUIZZES)
        }
    }

    if (pendingLink != null && currentRoute in setOf(Routes.HOME, Routes.MORE) && !questionActionBusy && !quizSessionActive) {
        val sameServer = pendingLink.server == container.apiBaseUrl
        com.mukhtabir.android.ui.components.AppConfirmationDialog(onDismissRequest=onConsumeLink,
            title={Text(stringResource(R.string.link_title))},
            text={Text(stringResource(if(sameServer) R.string.link_open_confirm else R.string.link_wrong_server, pendingLink.id))},
            confirmButton={if(sameServer) AppTextButton({
                navController.navigate(pendingLink.route) { launchSingleTop=true }; onConsumeLink()
            }) { Text(stringResource(R.string.notice_open_link)) }},
            dismissButton={AppTextButton(onConsumeLink) {Text(stringResource(R.string.cancel))}})
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerNavigationAllowed,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(user, currentRoute, enabled = drawerNavigationAllowed, preferences = container.preferences) { route ->
                    if (drawerNavigationAllowed) {
                        if (currentRoute != route) {
                            navController.navigateTopLevel(route)
                        }
                        drawerScope.launch { drawerState.close() }
                    }
                }
            }
        },
    ) {
    val pageBackground = com.mukhtabir.android.ui.theme.LocalAppBackground.current
    Scaffold(
        modifier = (if (pageBackground != null) Modifier.background(pageBackground) else Modifier).imePadding(),
        containerColor = if (pageBackground != null) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (currentRoute != null && tabs.none { it.route == currentRoute }) {
                val title = ShellNavigation.title(currentRoute)
                AppBackBar(stringResource(title), enabled = !questionActionBusy,
                    onMenu = openDrawer.takeIf { drawerNavigationAllowed }) {
                    backDispatcher?.onBackPressed()
                }
            } else if (!quizSessionActive) {
                AppTopBar(stringResource(R.string.app_name), enabled = drawerNavigationAllowed, onMenu = openDrawer,
                    onSettings = { if (drawerNavigationAllowed) navController.navigateTopLevel(Routes.MORE_SETTINGS) })
            }
        },
        bottomBar = {
            if (!quizSessionActive) AppNavigationBar(navController, user, enabled = !questionActionBusy, selectedTab = selectedTab)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding)) {
            if (pendingLink != null && currentRoute !in setOf(Routes.HOME, Routes.MORE)) {
                Text(stringResource(R.string.link_pending), Modifier.fillMaxWidth().padding(horizontal = com.mukhtabir.android.ui.components.AppLayoutTokens.pagePadding, vertical = 8.dp))
            }
            if (!isOnline) Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer) {
                Text(stringResource(R.string.offline_banner), Modifier.fillMaxWidth().padding(horizontal = com.mukhtabir.android.ui.components.AppLayoutTokens.pagePadding, vertical = 8.dp))
            }
            if (expiryDays != null && requireNotNull(expiryDays) <= 7) {
                Surface(color=androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer) {
                    Text(stringResource(R.string.account_expiry_warning, requireNotNull(expiryDays).coerceAtLeast(0)),
                        Modifier.fillMaxWidth().padding(horizontal = com.mukhtabir.android.ui.components.AppLayoutTokens.pagePadding, vertical = 8.dp))
                }
            }
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            composable(Routes.HOME) {
                HomeRoute(
                    user = user,
                    bookmarkService = container.bookmarkService,
                    questionService = container.questionService,
                    tipsService = container.tipsService,
                    analyticsService = container.analyticsService,
                    plannerService = container.plannerService,
                    masterExamService = container.masterExamService,
                    imageRepository = container.imageRepository,
                    onOpenLibrary = { navController.navigateTopLevel(Routes.MORE_QUESTIONS) },
                    onOpenQuestion = { id -> navController.navigateContent("more/questions/$id") },
                    onOpenPlanner = { navController.navigateTopLevel(Routes.MORE_PLANNER) },
                    onOpenActivity = { navController.navigateTopLevel("more/activity") },
                    onOpenStudyMode = { mode ->
                        if (user.has("tests.start")) {
                            container.quizPresetHolder.configure(mode)
                            navController.navigateTopLevel(Routes.QUIZZES)
                        }
                    },
                    onOpenExam = { id -> navController.navigate("more/master_exams/open/$id") },
                    onCreateQuestion = ({ navController.navigate(Routes.QUESTION_CREATE) }).takeIf { user.has("questions.create") },
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
                        navController.navigateTopLevel(Routes.QUIZZES)
                    },
                    onOpenQuizzes = { navController.navigateTopLevel(Routes.QUIZZES) },
                    onOpenBookmarks = { navController.navigateTopLevel(Routes.BOOKMARKS) },
                    onOpenAnalytics = { navController.navigateTopLevel(Routes.MORE_ANALYTICS) },
                    onOpenHistory = { navController.navigateTopLevel(Routes.HISTORY) },
                    onOpenMistakes = { navController.navigateTopLevel(Routes.MORE_MISTAKES) },
                    onOpenFragile = { navController.navigateTopLevel(Routes.MORE_FRAGILE) },
                    draftStore = container.sessionDraftStore,
                    onOpenAssignedExam = { navController.navigateTopLevel("more/master_exams") },
                    onOpenOfflineStudy = { navController.navigateTopLevel("more/offline") },
                )
            }
            if (user.has("tests.start")) {
                composable(Routes.QUIZZES) {
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
                            ({ navController.navigateTopLevel(Routes.HISTORY) }) else null,
                    )
                }
            }
            composable(Routes.BOOKMARKS) {
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
                composable(Routes.HISTORY) {
                    HistoryRoute(
                        signedInUserId = user.id,
                        pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
                        service = container.historyService,
                        imageRepository = container.imageRepository,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.MORE) {
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
                        ({ navController.navigate(Routes.MASTER_MANAGE) }) else null,
                    onBack = { navController.popBackStack() })
            }
            if (user.has("master_exams.create") || user.has("master_exams.manage_own") || user.has("master_exams.manage_any")) {
                composable(Routes.MASTER_MANAGE) {
                    com.mukhtabir.android.ui.masterexams.MasterExamManagementRoute(user, container.masterExamService,
                        onCreate = { navController.navigate(Routes.MASTER_CREATE) },
                        onEdit = { id -> navController.navigate("more/master_exams/manage/$id") })
                }
            }
            if (user.has("master_exams.create")) {
                composable(Routes.MASTER_CREATE) {
                    com.mukhtabir.android.ui.masterexams.MasterExamEditorRoute(null, user, container.masterExamService,
                        container.questionService, publicConfig.itemsPerPage, container.adminService, container.groupService, container.categoryService, publicConfig.maxChoices, draftStore = container.sessionDraftStore, onBack = { navController.popBackStack() },
                        onBusyChanged = { questionActionBusy = it },
                        onDraft = { draft -> navController.navigate("more/master_drafts/$draft") },
                        onPreview = { exam -> navController.navigate("more/master_preview/$exam") },
                        onResults = { exam -> navController.navigate("more/master_results/$exam") })
                }
            }
            if (user.has("master_exams.manage_own") || user.has("master_exams.manage_any")) {
                composable(Routes.MASTER_EDIT, arguments = listOf(navArgument("examId") { type = NavType.LongType })) { entry ->
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
                        "question" -> if (id == "new") Routes.QUESTION_CREATE else "more/questions/$id/edit"
                        "image" -> "more/questions/$id/image"
                        "exam" -> if (id == "new") Routes.MASTER_CREATE else "more/master_exams/manage/$id"
                        "draft" -> if (id == "new") "more/master_drafts" else "more/master_drafts/$id"
                        else -> null
                    }
                    if (route != null) navController.navigate(route)
                }
            }
            if (user.has("admin.database") || user.has("admin.seed")) composable("more/admin/tools") {
                com.mukhtabir.android.ui.admin.OperatorToolsRoute(user, container.operatorService,
                    onQuestion = { id -> navController.navigateContent("more/questions/$id") }, onBank = { navController.navigate(Routes.MORE_QUESTIONS) },
                    onBusyChanged = { questionActionBusy = it })
            }
            composable("more/help") { LearnerInformation(false) { navController.popBackStack() } }
            composable("more/privacy") { LearnerInformation(true) { navController.popBackStack() } }
            composable(Routes.MORE_QUESTIONS) {
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
                    onCreateQuestion = { navController.navigate(Routes.QUESTION_CREATE) },
                    onDownloadOffline = { ids -> if(user.has("tests.start")) navController.navigate("more/offline?ids=${ids.joinToString(",")}") },
                    onStartQuestions = startQuestions,
                    navigationStore=container.sessionDraftStore,
                )
            }
            composable(
                route = Routes.QUESTION_DETAIL,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
            ) { entry ->
                QuestionDetailRoute(
                    onShare={id -> shareLink("questions",id)},
                    onMutationBusyChanged = { questionActionBusy = it },
                    bankRevision = questionBankRevision,
                    canDuplicate = user.has("questions.duplicate"),
                    canDeleteOwn = user.has("questions.delete_own"),
                    canDeleteAny = user.has("questions.delete_any"),
                    onDuplicated = { id ->
                        questionBankRevision += 1
                        navController.navigate("more/questions/$id") {
                            popUpTo(Routes.QUESTION_DETAIL) { inclusive = true }
                        }
                    },
                    onDeleted = { id ->
                        removedQuestionIds = removedQuestionIds + id
                        questionBankRevision += 1
                        if (!navController.popBackStack(Routes.MORE_QUESTIONS, false)) {
                            navController.navigate(Routes.MORE_QUESTIONS) {
                                popUpTo(Routes.QUESTION_DETAIL) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    questionId = requireNotNull(entry.arguments?.getLong("questionId")),
                    questionService = container.questionService,
                    bookmarkService = container.bookmarkService,
                    store = container.sessionDraftStore,
                    imageRepository = container.imageRepository,
                    canStartSessions = user.has("tests.start"),
                    canEditAny = user.has("questions.edit_any"),
                    canEditOwn = user.has("questions.edit_own"),
                    userId = user.id,
                    onStart = startQuestions,
                    onEdit = { id -> navController.navigate("more/questions/$id/edit") },
                    canReviewQuestion = user.has("questions.verify") || user.has("questions.bulk_verify"),
                    canUploadImageOwn = user.has("questions.upload_image"),
                    onManageImage = { id -> navController.navigate("more/questions/$id/image") },
                    onOpenKnowledge={id -> navController.navigateContent("more/knowledge/$id")},
                    onOpenCase={key -> navController.navigateContent("more/cases/${android.net.Uri.encode(key)}")},
                )
            }
            if (user.has("questions.edit_any") || user.has("questions.upload_image")) {
                composable(Routes.QUESTION_IMAGE,arguments=listOf(navArgument("questionId") { type=NavType.LongType })) { entry ->
                    QuestionImageRoute(questionId=requireNotNull(entry.arguments?.getLong("questionId")),userId=user.id,
                        questions=container.questionService,images=container.imageRepository,
                        canEditAny=user.has("questions.edit_any"),canUploadOwn=user.has("questions.upload_image"),draftStore=container.sessionDraftStore,
                        onBack={navController.popBackStack()},onChanged={questionBankRevision+=1},
                        onBusyChanged={questionActionBusy=it})
                }
            }
            if (user.has("questions.create")) {
                composable(Routes.QUESTION_CREATE) {
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
                        navController.navigate("more/questions/$id") { popUpTo(Routes.QUESTION_CREATE) { inclusive = true } }
                    }
                }
            }
            if (user.has("questions.edit_any") || user.has("questions.edit_own")) {
                composable(Routes.QUESTION_EDIT, arguments = listOf(navArgument("questionId") { type = NavType.LongType })) { entry ->
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
                        navController.navigate("more/questions/$id") { popUpTo(Routes.QUESTION_DETAIL) { inclusive = true } }
                    }
                }
            }
            composable(Routes.MORE_ANALYTICS) {
                AnalyticsRoute(
                    service = container.analyticsService,
                    userId = user.id,
                    canViewAll = user.has("analytics.view_all"),
                    canViewActiveUsers = user.has("admin.active_users"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.MORE_MISTAKES) {
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
            composable(Routes.MORE_FRAGILE) {
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
            composable(Routes.MORE_PROFILE) {
                ProfileScreen(
                    user = user,
                    onChangePassword = { navController.navigate(Routes.MORE_CHANGE_PASSWORD) },
                    onAbout = { navController.navigateTopLevel(Routes.MORE_ABOUT) },
                    onSignOut = onLogout,
                    onAdminUsers = { navController.navigateTopLevel(Routes.MORE_ADMIN_USERS) },
                    onAdminSettings = { navController.navigateTopLevel(Routes.MORE_ADMIN_SETTINGS) },
                    onAdminPermissions = { navController.navigateTopLevel(Routes.MORE_ADMIN_PERMISSIONS) },
                    onRefresh=onProfileRefresh, refreshing=profileRefreshing, refreshError=profileError,
                )
            }
            composable(Routes.MORE_CHANGE_PASSWORD) {
                ChangePasswordScreen(
                    repository = container.authRepository,
                    onDone = { navController.popBackStack() },
                    onSignOut = onLocalSignOut,
                    forced = false,
                    onBusyChanged = { questionActionBusy = it },
                )
            }
            composable(Routes.MORE_CATEGORIES) {
                CategoriesRoute(container.categoryService, user.has("categories.manage"), container.sessionDraftStore, user.id,
                    onBack = { navController.popBackStack() }, onBusyChanged = { questionActionBusy = it })
            }
            composable(Routes.MORE_KNOWLEDGE_MAP) {
                KnowledgeMapRoute(container.questionService, user.has("tests.start"), user.id,
                    onOpenDetail={id -> navController.navigateContent("more/knowledge/$id")}) { ids, label ->
                    startQuestions(TestMode.STUDY, ids, label)
                }
            }
            composable(Routes.KNOWLEDGE_DETAIL,arguments=listOf(navArgument("knowledgeId") { type=NavType.LongType })) { entry ->
                KnowledgeDetailRoute(id=requireNotNull(entry.arguments?.getLong("knowledgeId")),userId=user.id,
                    service=container.questionService,canStart=user.has("tests.start"),maxQuestions=publicConfig.maxQuizQuestions,
                    onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                    onReview={ids,label -> startQuestions(TestMode.STUDY,ids,label)})
            }
            composable(Routes.MORE_CASES) {
                ClinicalCasesRoute(userId=user.id,service=container.questionService,pageSize=displayPreferences.perPage.takeIf {it>0} ?: publicConfig.itemsPerPage,onBack={navController.popBackStack()},
                    onOpen={key -> navController.navigateContent("more/cases/${android.net.Uri.encode(key)}")})
            }
            composable(Routes.CASE_DETAIL,arguments=listOf(navArgument("caseKey") { type=NavType.StringType })) { entry ->
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
                composable(Routes.CASE_STEM,arguments=listOf(navArgument("caseKey") { type=NavType.StringType })) { entry ->
                    CaseStemRoute(key=requireNotNull(entry.arguments?.getString("caseKey")),userId=user.id,
                        service=container.questionService,canEditAny=user.has("questions.edit_case_stem_any"),
                        canEditOwn=user.has("questions.edit_case_stem_own"),store=container.sessionDraftStore,onBack={navController.popBackStack()},
                        onChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
                }
            }
            composable(Routes.MORE_PLANNER) {
                PlannerRoute(container.plannerService, container.categoryService, container.questionService, user.id,
                    container.sessionDraftStore, onBack={navController.popBackStack()}, onBusy={questionActionBusy=it})
            }
            composable(Routes.MORE_GROUPS) {
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
            composable(Routes.MORE_SETTINGS) {
                SettingsRoute(container.preferences, container.apiBaseUrl, container.serverSettings.isFileConfigured(), onChangeServer,
                    savedServerFiles = container.serverSettings.importedFiles(), activeFileSlot = container.serverSettings.activeImportedSlot(),
                    onSelectServerFile = onSelectServerFile)
            }
            if (user.has("admin.verification_stats")) {
                composable(Routes.MORE_VERIFICATION_STATS) {
                    VerificationStatsRoute(container.analyticsService, user.id, user.has("admin.verification_stats"),onExport=({navController.navigate("more/admin/transfers?verified=true")}).takeIf {user.has("admin.database")})
                }
            }
            if (user.has("admin.settings")) {
                composable(Routes.MORE_ADMIN_SETTINGS) {
                    AdminSettingsRoute(container.adminService, if (container.serverSettings.isFileConfigured()) "" else container.apiBaseUrl, canManageSystem = user.has("admin.settings"), store=container.sessionDraftStore, userId=user.id,
                        onBusyChanged = { questionActionBusy = it })
                }
            }
            composable(Routes.MORE_ABOUT) {
                AboutScreen(onHelp={navController.navigateTopLevel("more/help")}, onPrivacy={navController.navigateTopLevel("more/privacy")})
            }
            if (user.has("admin.flags")) {
                composable(Routes.MORE_ADMIN_FLAGS) {
                    ModerationRoute(mode=ModerationMode.FLAGS,service=container.moderationService,
                        pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("admin.flags"),
                        onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                        onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
                }
            }
            if (user.has("questions.bulk_tag")) {
                composable(Routes.MORE_ADMIN_TAGS) {
                    ModerationRoute(mode=ModerationMode.TAGS,service=container.moderationService,
                        pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("questions.bulk_tag"),
                        onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                        onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
                }
            }
            if (user.has("questions.bulk_verify")) {
                composable(Routes.MORE_ADMIN_VERIFICATION) {
                    ModerationRoute(mode=ModerationMode.VERIFICATION,service=container.moderationService,
                        pageSize=publicConfig.itemsPerPage,userId=user.id,canManage=user.has("questions.bulk_verify"),
                        onBack={navController.popBackStack()},onOpenQuestion={id -> navController.navigateContent("more/questions/$id")},
                        onQuestionsChanged={questionBankRevision+=1},onBusyChanged={questionActionBusy=it})
                }
            }
            if (user.has("admin.active_users")) {
                composable(Routes.MORE_ADMIN_ACTIVE_USERS) {
                    ActiveAccountsRoute(service=container.adminService,userId=user.id,canView=user.has("admin.active_users"),
                        canManageUsers=user.has("admin.users"),onBack={navController.popBackStack()},
                        onOpenUser={id -> navController.navigate("more/admin/users/$id/account")})
                }
            }
            if (user.has("admin.users")) {
                composable(Routes.MORE_ADMIN_ACCOUNT,arguments=listOf(navArgument("userId") { type=NavType.LongType })) { entry ->
                    AdminAccountRoute(id=requireNotNull(entry.arguments?.getLong("userId")),actingUserId=user.id,
                        service=container.adminService,canManage=user.has("admin.users"),onBack={navController.popBackStack()},
                        onOwnPassword={navController.navigate(Routes.MORE_CHANGE_PASSWORD)},onBusyChanged={questionActionBusy=it},
                        onPermissions=({navController.navigate(Routes.MORE_ADMIN_PERMISSIONS+"?user="+entry.arguments?.getLong("userId"))}).takeIf {user.has("admin.permissions")})
                }
                composable(Routes.MORE_ADMIN_USERS) {
                    AdminUsersRoute(
                        service = container.adminService,
                        pageSize = displayPreferences.perPage.takeIf { it > 0 } ?: publicConfig.itemsPerPage,
                        roles = publicConfig.roles,
                        canManage = user.has("admin.users"),
                        actingUserId = user.id,
                        onOpenUser = { id -> navController.navigate("more/admin/users/$id/account") },
                        onPermissions=({id:Long->navController.navigate(Routes.MORE_ADMIN_PERMISSIONS+"?user=$id")}).takeIf {user.has("admin.permissions")},
                        onBusyChanged={questionActionBusy=it},
                    )
                }
            }
            if (user.has("admin.permissions")) {
                composable(Routes.MORE_ADMIN_PERMISSIONS+"?user={targetUser}",arguments=listOf(navArgument("targetUser"){type=NavType.LongType;defaultValue=0L})) {entry ->
                    PermissionsRoute(container.adminService, canManage=user.has("admin.permissions"), actingUserId=user.id, pageSize=publicConfig.itemsPerPage, roles=publicConfig.roles, onBusyChanged={questionActionBusy=it},onBack={navController.popBackStack()},initialUserId=entry.arguments?.getLong("targetUser")?.takeIf {it>0})
                }
            }
            if (user.has("tests.view_all_history")) {
                composable(Routes.MORE_ADMIN_HISTORY) {
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
        }
    }
    BackHandler(drawerState.isOpen) { drawerScope.launch { drawerState.close() } }
    }
}

@Composable
private fun AppNavigationBar(navController: NavHostController, user: UserDto, enabled: Boolean, selectedTab: String) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    if (ShellNavigation.isFocused(currentRoute)) return
    Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.material3.NavigationBarDefaults.windowInsets)
            .selectableGroup().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            tabs.filter { item -> item.capability?.let(user::has) ?: true }.forEach { item ->
                val selected = selectedTab == item.route
                Box(Modifier.weight(1f)
                    .heightIn(min=com.mukhtabir.android.ui.components.AppLayoutTokens.bottomNavigationHeight)
                    .selectable(selected=selected,enabled=enabled,role=androidx.compose.ui.semantics.Role.Tab,
                        onClick={navController.navigateTopLevel(item.route)})
                    .padding(vertical=com.mukhtabir.android.ui.components.AppLayoutTokens.secondaryFrameInset)) {
                    Column(Modifier.fillMaxWidth()
                        .heightIn(min=com.mukhtabir.android.ui.components.AppLayoutTokens.secondaryVisualHeight)
                        .background(if(selected) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent,androidx.compose.material3.MaterialTheme.shapes.medium)
                        .padding(horizontal=4.dp,vertical=1.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides
                            if(selected) androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                            else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) {
                            Icon(item.icon,contentDescription=null,
                                modifier=Modifier.size(com.mukhtabir.android.ui.components.AppLayoutTokens.secondaryIconSize))
                            Text(stringResource(item.labelRes),style=androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    // A tab re-tap returns to its existing root; no restored detail stack is replayed.
    if (popBackStack(route, inclusive = false)) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

private fun NavHostController.navigateContent(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { launchSingleTop = true }
}
