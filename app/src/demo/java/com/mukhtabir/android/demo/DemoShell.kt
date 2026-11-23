@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.shared.R as SharedR
import com.mukhtabir.android.ui.analytics.ActivityRoute
import com.mukhtabir.android.ui.analytics.AnalyticsRoute
import com.mukhtabir.android.ui.bookmarks.BookmarksRoute
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.fragile.FragileRoute
import com.mukhtabir.android.ui.history.HistoryRoute
import com.mukhtabir.android.ui.home.HomeRoute
import com.mukhtabir.android.ui.mistakes.MistakesRoute
import com.mukhtabir.android.ui.offline.OfflineStudyRoute
import com.mukhtabir.android.ui.planner.PlannerRoute
import com.mukhtabir.android.ui.profile.ProfileScreen
import com.mukhtabir.android.ui.questions.QuestionDetailRoute
import com.mukhtabir.android.ui.questions.QuestionLibraryRoute
import com.mukhtabir.android.ui.quiz.QuizRoute
import com.mukhtabir.android.ui.settings.ReadingComfortSettings
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Destination(val route: String, val label: Int) {
    LIBRARY("library", SharedR.string.question_library),
    HOME("home", SharedR.string.home),
    QUIZZES("quizzes", SharedR.string.quizzes),
    BOOKMARKS("bookmarks", SharedR.string.bookmarks),
    HISTORY("history", SharedR.string.history),
    MISTAKES("mistakes", SharedR.string.nav_mistakes),
    FRAGILE("fragile", SharedR.string.nav_fragile),
    ANALYTICS("analytics", SharedR.string.nav_analytics),
    ACTIVITY("activity", SharedR.string.activity_title),
    PLANNER("planner", SharedR.string.nav_planner),
    OFFLINE("offline", SharedR.string.offline_title),
    PROFILE("profile", SharedR.string.nav_profile),
    SETTINGS("settings", SharedR.string.nav_settings),
    CONNECTION("connection", R.string.demo_connection_title),
}

@Composable
internal fun DemoShell(runtime: DemoRuntime, onReset: suspend () -> Unit, onLanguageChanged: (String) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val display by runtime.preferences.display.collectAsLifecycleState()
    val pageSize = display.perPage.takeIf { it > 0 } ?: 4
    val user = runtime.fixtures.user
    var routeBusy by remember { mutableStateOf(false) }
    val go: (Destination) -> Unit = { destination ->
        if (entry?.destination?.route?.substringBefore('?') != destination.route &&
            !nav.popBackStack(destination.route, false)) {
            nav.navigate(destination.route) { launchSingleTop = true }
        }
    }
    val back: () -> Unit = { nav.popBackStack() }
    val openQuestion: (Long) -> Unit = { nav.navigate("question/$it") { launchSingleTop = true } }
    val openPractice: (TestMode, List<Long>, String) -> Unit = { mode, ids, label ->
        runtime.presets.set(QuizPreset(mode, ids, label))
        go(Destination.QUIZZES)
    }
    val download: (List<Long>) -> Unit = { ids ->
        nav.navigate("offline?ids=${ids.joinToString(",")}") { launchSingleTop = true }
    }

    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets)) {
                    AppPageHeading(stringResource(R.string.demo_launcher_name))
                    Destination.entries.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(destination.label)) },
                            selected = entry?.destination?.route?.substringBefore('?') == destination.route,
                            onClick = {
                                if (!routeBusy) {
                                    scope.launch {
                                        drawer.close()
                                        go(destination)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                AppScreenBar(stringResource(R.string.demo_launcher_name), navigation = {
                    AppIconButton({ scope.launch { drawer.open() } }, enabled = !routeBusy) {
                        Icon(Icons.Filled.Menu, stringResource(R.string.demo_navigation))
                    }
                }, actions = {
                    if (nav.previousBackStackEntry != null && entry?.destination?.route != Destination.QUIZZES.route) {
                        AppTextButton(back, enabled = !routeBusy) { Text(stringResource(SharedR.string.back)) }
                    }
                })
            },
        ) { padding ->
            NavHost(nav, startDestination = Destination.LIBRARY.route, modifier = Modifier
                .padding(padding).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
                composable(Destination.LIBRARY.route) {
                    QuestionLibraryRoute(runtime.questions, display.difficulty, runtime.categories, runtime.images,
                        canReview = false, canCreate = false, canStartSessions = true, pageSize = pageSize,
                        maxQuestions = SAMPLE_LIMIT, bankRevision = 0, userId = user.id, removedQuestionIds = emptySet(),
                        onOpenQuestion = openQuestion, onCreateQuestion = {}, onStartQuestions = openPractice,
                        onDownloadOffline = download, detailStateOwner = "demo",
                        detailContent = { id, _, onPaneLock ->
                            DemoQuestionDetail(runtime, id, openPractice, onPaneLock) { routeBusy = it }
                        })
                }
                composable("question/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { detail ->
                    DemoQuestionDetail(runtime, requireNotNull(detail.arguments).getLong("id"), openPractice) { routeBusy = it }
                }
                composable(Destination.QUIZZES.route) {
                    QuizRoute(runtime.questions, runtime.sessions, runtime.bookmarks, runtime.categories, runtime.blueprints,
                        runtime.images, runtime.presets, canUseBlueprint = false, maxQuizQuestions = SAMPLE_LIMIT,
                        userId = user.id, draftStore = runtime.store, preferences = runtime.preferences, onBack = back,
                        onOpenHistory = { go(Destination.HISTORY) }, onSessionActiveChanged = {})
                }
                composable(Destination.BOOKMARKS.route) {
                    BookmarksRoute(runtime.bookmarks, runtime.images, true, SAMPLE_LIMIT, pageSize, user.id,
                        openQuestion, openPractice, back, download)
                }
                composable(Destination.HOME.route) {
                    val label = stringResource(SharedR.string.study_now_title)
                    HomeRoute(user, runtime.bookmarks, runtime.questions, runtime.tips, runtime.analytics, runtime.planner,
                        runtime.notices, runtime.images, onOpenLibrary = { go(Destination.LIBRARY) },
                        onOpenQuestion = openQuestion, onOpenPlanner = { go(Destination.PLANNER) },
                        onOpenActivity = { go(Destination.ACTIVITY) },
                        onOpenStudyMode = { runtime.presets.configure(it); go(Destination.QUIZZES) },
                        onOpenExam = {}, onCreateQuestion = null, canStartTests = true, canViewHistory = true,
                        maxQuizQuestions = SAMPLE_LIMIT, onStartStudyNow = { openPractice(TestMode.STUDY, it, label) },
                        onOpenQuizzes = { go(Destination.QUIZZES) }, onOpenBookmarks = { go(Destination.BOOKMARKS) },
                        onOpenAnalytics = { go(Destination.ANALYTICS) }, onOpenHistory = { go(Destination.HISTORY) },
                        onOpenMistakes = { go(Destination.MISTAKES) }, onOpenFragile = { go(Destination.FRAGILE) },
                        draftStore = runtime.store, onOpenAssignedExam = {}, onOpenOfflineStudy = { go(Destination.OFFLINE) })
                }
                composable(Destination.HISTORY.route) {
                    HistoryRoute(user.id, pageSize = pageSize, service = runtime.history, imageRepository = runtime.images, onBack = back)
                }
                composable(Destination.MISTAKES.route) {
                    MistakesRoute(runtime.questions, runtime.images, true, SAMPLE_LIMIT, pageSize, user.id, openPractice, openQuestion)
                }
                composable(Destination.FRAGILE.route) {
                    FragileRoute(runtime.questions, runtime.images, true, SAMPLE_LIMIT, pageSize, user.id, openPractice, openQuestion)
                }
                composable(Destination.ANALYTICS.route) {
                    AnalyticsRoute(runtime.analytics, false, false, user.id, back)
                }
                composable(Destination.ACTIVITY.route) {
                    ActivityRoute(runtime.analytics, user.id, runtime.preferences)
                }
                composable(Destination.PLANNER.route) {
                    PlannerRoute(runtime.planner, runtime.categories, runtime.questions, user.id, runtime.store, back) { routeBusy = it }
                }
                composable("offline?ids={ids}", arguments = listOf(navArgument("ids") { defaultValue = "" })) { offline ->
                    OfflineStudyRoute(runtime.offline, runtime.store, user.id, localOnly = false, canSync = true,
                        maxQuestions = SAMPLE_LIMIT, initialIds = offline.arguments?.getString("ids").orEmpty(),
                        onBack = back, onBusy = { routeBusy = it })
                }
                composable(Destination.PROFILE.route) { DemoProfile(runtime) }
                composable(Destination.SETTINGS.route) { DemoSettings(runtime, onLanguageChanged) }
                composable(Destination.CONNECTION.route) { DemoConnections(runtime.store, onReset) }
            }
        }
    }
}

@Composable
private fun DemoQuestionDetail(
    runtime: DemoRuntime, id: Long, onPractice: (TestMode, List<Long>, String) -> Unit,
    onPaneLock: (Boolean) -> Unit = {}, onBusy: (Boolean) -> Unit,
) {
    QuestionDetailRoute(id, runtime.questions, runtime.bookmarks, runtime.store, runtime.images,
        canStartSessions = true, canEditAny = false, canEditOwn = false, canUploadImageOwn = false,
        canReviewQuestion = true, canDuplicate = false, canDeleteOwn = false, canDeleteAny = false,
        bankRevision = 0, userId = runtime.fixtures.user.id, onStart = onPractice, onEdit = {},
        onManageImage = {}, onOpenKnowledge = {}, onOpenCase = {}, onDuplicated = {}, onDeleted = {},
        onMutationBusyChanged = onBusy, onPaneLockChanged = onPaneLock)
}

@Composable
private fun DemoProfile(runtime: DemoRuntime) {
    var user by remember { mutableStateOf(runtime.fixtures.user) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = {
        if (!refreshing) {
            refreshing = true
            scope.launch {
                try {
                    user = runtime.analytics.learnerProfile()
                    error = null
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (failure: Exception) {
                    error = failure
                } finally {
                    refreshing = false
                }
            }
        }
    }
    RefreshOnResume(refresh)
    ProfileScreen(user, onChangePassword = null, onAbout = null, onSignOut = null,
        onAdminUsers = {}, onAdminSettings = {}, onAdminPermissions = {},
        onRefresh = refresh, refreshing = refreshing, refreshError = error)
}

@Composable
private fun DemoSettings(runtime: DemoRuntime, onLanguageChanged: (String) -> Unit) {
    val display by runtime.preferences.display.collectAsLifecycleState()
    val language by runtime.preferences.language.collectAsLifecycleState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(SharedR.string.nav_settings))
        AppLanguageChoices(language, onLanguageChanged)
        AppThemePicker(runtime.preferences)
        AppSwitchRow(stringResource(SharedR.string.preferences_compact), display.compact, {
            runtime.preferences.setDisplay(display.copy(compact = it))
        })
        ReadingComfortSettings(runtime.preferences, display)
    }
}

@Composable
private fun DemoConnections(store: DemoStore, onReset: suspend () -> Unit) {
    val connection by store.connection.collectAsLifecycleState()
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var resetFailed by remember { mutableStateOf(false) }
    if (confirm) AppConfirmationDialog(
        onDismissRequest = { if (!resetting) confirm = false },
        title = { Text(stringResource(R.string.demo_reset_progress)) },
        text = { Text(stringResource(R.string.demo_reset_progress_message)) },
        confirmButton = {
            AppDangerTextButton({
                resetting = true
                resetFailed = false
                scope.launch {
                    try {
                        onReset()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        resetting = false
                        confirm = false
                        resetFailed = true
                    }
                }
            }, enabled = !resetting) { Text(stringResource(R.string.demo_reset_progress)) }
        },
        dismissButton = {
            AppTextButton({ confirm = false }, enabled = !resetting) { Text(stringResource(SharedR.string.cancel)) }
        },
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.demo_connection_title))
        AppSwitchRow(stringResource(if (connection.connected) R.string.demo_connected else R.string.demo_disconnected),
            connection.connected, store::setConnected)
        Text(stringResource(R.string.demo_connection_hint))
        listOf(
            DemoFailure.READ to R.string.demo_lose_read,
            DemoFailure.ANSWER to R.string.demo_lose_answer,
            DemoFailure.FINISH to R.string.demo_lose_finish,
            DemoFailure.UPLOAD to R.string.demo_lose_upload,
        ).forEach { (failure, label) ->
            AppSwitchRow(stringResource(label), failure in connection.armed, { store.arm(failure, it) })
        }
        Text(stringResource(R.string.demo_loss_hint))
        if (resetFailed) AppStatus(stringResource(R.string.demo_reset_failed))
        AppDangerOutlinedButton({ confirm = true }) { Text(stringResource(R.string.demo_reset_progress)) }
    }
}
