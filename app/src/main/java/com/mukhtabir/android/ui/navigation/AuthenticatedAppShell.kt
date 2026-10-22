package com.mukhtabir.android.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.core.ShellNavigation
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.PublicConfigDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppBackBar
import com.mukhtabir.android.ui.components.AppBanner
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTopBar
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.navigation.AppDrawerContent
import com.mukhtabir.android.ui.theme.AppShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TabItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val unselectedIcon: ImageVector,
    val capability: String? = null,
)

private val tabs = listOf(
    TabItem(AppRoutes.HOME, R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
    TabItem(AppRoutes.MORE_QUESTIONS, R.string.nav_library_short, Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    TabItem(AppRoutes.QUIZZES, R.string.quizzes, Icons.Filled.Quiz, Icons.Outlined.Quiz, "tests.start"),
    TabItem(AppRoutes.BOOKMARKS, R.string.bookmarks, Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AuthenticatedAppShell(
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
    val displayPreferences by container.preferences.display.collectAsLifecycleState()

    val navController = rememberNavController()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val shareLink: (String,Long) -> Unit = {kind,id ->
        val link=com.mukhtabir.android.core.AppLink(kind,id,container.apiBaseUrl).uri().toString()
        appContext.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,link),null))
    }
    val navigationState = remember { AppNavigationState() }
    var questionActionBusy by navigationState.questionActionBusy
    var quizSessionActive by navigationState.quizSessionActive
    var selectedTab by rememberSaveable(user.id) { mutableStateOf(AppRoutes.HOME) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(currentRoute) {
        when {
            tabs.any { it.route == currentRoute } -> selectedTab = requireNotNull(currentRoute)
            currentRoute?.startsWith("more/questions/") == true -> Unit // Keep the originating tab.
            currentRoute != null -> selectedTab = when {
                ShellNavigation.matches(currentRoute, "more/cases") || ShellNavigation.matches(currentRoute, "more/knowledge") ||
                    currentRoute == AppRoutes.MORE_QUESTIONS -> AppRoutes.MORE_QUESTIONS
                currentRoute.startsWith("more/master") || currentRoute == AppRoutes.HISTORY -> AppRoutes.QUIZZES
                else -> AppRoutes.HOME
            }
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val focusedRoute = ShellNavigation.isFocused(currentRoute)
    val profileSensitiveRoutes = setOf(AppRoutes.MORE_CATEGORIES, AppRoutes.MORE_ADMIN_SETTINGS, AppRoutes.MORE_ADMIN_PERMISSIONS+"?user={targetUser}")
    DisposableEffect(questionActionBusy,quizSessionActive,currentRoute) {
        onProfileApplicationAllowed(!questionActionBusy && !quizSessionActive && !ShellNavigation.preservesDraft(currentRoute) && currentRoute !in profileSensitiveRoutes &&
            currentRoute?.startsWith("more/master_exams/open/")!=true)
        onDispose { }
    }
    RefreshOnResume { onProfileRefresh() }
    LaunchedEffect(currentRoute) {
        if(!questionActionBusy && !quizSessionActive && !ShellNavigation.preservesDraft(currentRoute)) onProfileRefresh()
    }
    val drawerNavigationAllowed = !questionActionBusy && !quizSessionActive && !focusedRoute
    LaunchedEffect(drawerNavigationAllowed) { if (!drawerNavigationAllowed) drawerState.close() }
    val openDrawer: () -> Unit = { if (drawerNavigationAllowed) drawerScope.launch { drawerState.open() } }
    val isOnline by container.networkMonitor.isOnline.collectAsLifecycleState()
    val expiryDays by produceState(user.remainingDays(), user) {
        while (true) { value=user.remainingDays(); delay(60_000) }
    }
    val studyNowLabel = stringResource(R.string.study_now_title)
    val startQuestions: (TestMode, List<Long>, String) -> Unit = { mode, ids, label ->
        if (user.has("tests.start") && ids.isNotEmpty()) {
            container.quizPresetHolder.set(QuizPreset(mode, ids, label))
            navController.navigateTopLevel(AppRoutes.QUIZZES)
        }
    }

    if (pendingLink != null && currentRoute in setOf(AppRoutes.HOME, AppRoutes.MORE) && !questionActionBusy && !quizSessionActive) {
        val sameServer = pendingLink.server == container.apiBaseUrl
        AppConfirmationDialog(onDismissRequest=onConsumeLink,
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val useRail = maxWidth >= AppLayoutTokens.navigationRailBreakpoint * scale && !focusedRoute && !quizSessionActive
    val pageBackground = com.mukhtabir.android.ui.theme.LocalAppBackground.current
    Scaffold(
        modifier = (if (pageBackground != null) Modifier.background(pageBackground) else Modifier).imePadding(),
        containerColor = if (pageBackground != null) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (currentRoute != null && tabs.none { it.route == currentRoute }) {
                val title = ShellNavigation.title(currentRoute)
                AppBackBar(stringResource(title), enabled = !questionActionBusy) {
                    backDispatcher?.onBackPressed()
                }
            } else if (!quizSessionActive) {
                AppTopBar(stringResource(R.string.app_name), enabled = drawerNavigationAllowed, onMenu = openDrawer,
                    onSettings = { if (drawerNavigationAllowed) navController.navigateTopLevel(AppRoutes.MORE_SETTINGS) })
            }
        },
        bottomBar = {
            if (!quizSessionActive && !useRail) AppNavigationBar(navController, user, enabled = !questionActionBusy, selectedTab = selectedTab)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Row(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding)) {
        if (useRail) AppNavigationRail(navController, user, enabled = !questionActionBusy,
            selectedTab = selectedTab, scale = scale)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (pendingLink != null && currentRoute !in setOf(AppRoutes.HOME, AppRoutes.MORE)) {
                AppBanner(stringResource(R.string.link_pending))
            }
            if (!isOnline) AppBanner(stringResource(R.string.offline_banner), kind = AppStatusKind.WARNING)
            if (expiryDays != null && requireNotNull(expiryDays) <= 7) {
                AppBanner(stringResource(R.string.account_expiry_warning,
                    com.mukhtabir.android.util.formatNumber(requireNotNull(expiryDays).coerceAtLeast(0))),
                    kind = AppStatusKind.WARNING)
            }
        NavHost(
            navController = navController,
            startDestination = AppRoutes.HOME,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            registerAppDestinations(
                container = container, user = user, publicConfig = publicConfig,
                navController = navController, displayPreferences = displayPreferences,
                navigationState = navigationState, studyNowLabel = studyNowLabel,
                startQuestions = startQuestions, shareLink = shareLink,
                onLogout = onLogout, onLocalSignOut = onLocalSignOut,
                onChangeServer = onChangeServer, onSelectServerFile = onSelectServerFile,
                onProfileRefresh = onProfileRefresh, profileRefreshing = profileRefreshing, profileError = profileError,
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
            .selectableGroup().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            tabs.filter { item -> item.capability?.let(user::has) ?: true }.forEach { item ->
                val selected = selectedTab == item.route
                Box(Modifier.weight(1f)
                    .heightIn(min=AppLayoutTokens.bottomNavigationHeight)
                    .selectable(selected=selected,enabled=enabled,role=androidx.compose.ui.semantics.Role.Tab,
                        onClick={navController.navigateTopLevel(item.route)})
                    .padding(vertical=AppLayoutTokens.secondaryFrameInset)) {
                    Column(Modifier.fillMaxWidth()
                        .heightIn(min=AppLayoutTokens.secondaryVisualHeight)
                        .padding(horizontal=4.dp,vertical=1.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides
                            if(selected) androidx.compose.material3.MaterialTheme.colorScheme.primary
                            else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) {
                            AppDestinationIcon(item, selected, enabled)
                            Text(stringResource(item.labelRes),style=androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

/** Same destinations/gates/handlers as bottom tabs, with scrollable labels at large text. */
@Composable
private fun AppNavigationRail(navController: NavHostController, user: UserDto, enabled: Boolean,
    selectedTab: String, scale: Float) {
    NavigationRail(Modifier.width(AppLayoutTokens.navigationRailWidth * scale).fillMaxHeight()
        .verticalScroll(rememberScrollState()), containerColor = MaterialTheme.colorScheme.surface) {
        tabs.filter { item -> item.capability?.let(user::has) ?: true }.forEach { item ->
            val selected = selectedTab == item.route
            NavigationRailItem(selected = selected, enabled = enabled,
                onClick = { if (enabled) navController.navigateTopLevel(item.route) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    selectedTextColor = MaterialTheme.colorScheme.primary),
                icon = { AppDestinationIcon(item, selected, enabled) },
                label = { Text(stringResource(item.labelRes), textAlign = androidx.compose.ui.text.style.TextAlign.Center) })
        }
    }
}

/** Both navigation layouts use the same glyph, selected pill and disabled treatment. */
@Composable
private fun AppDestinationIcon(item: TabItem, selected: Boolean, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val foreground = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
    Box(Modifier.background(if (selected) scheme.secondaryContainer
        else androidx.compose.ui.graphics.Color.Transparent, AppShapes.indicator)
        .padding(horizontal = AppLayoutTokens.pagePadding), contentAlignment = Alignment.Center) {
        Icon(if (selected) item.icon else item.unselectedIcon, contentDescription = null,
            tint = foreground.copy(alpha = if (enabled) 1f
                else com.mukhtabir.android.ui.components.AppControlTokens.disabledContentAlpha),
            modifier = Modifier.size(AppLayoutTokens.secondaryIconSize))
    }
}
