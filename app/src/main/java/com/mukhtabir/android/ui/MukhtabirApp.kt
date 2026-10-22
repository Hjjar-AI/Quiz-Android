package com.mukhtabir.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.data.remote.PublicConfigDto
import com.mukhtabir.android.ui.about.LearnerInformation
import com.mukhtabir.android.ui.auth.AuthViewModel
import com.mukhtabir.android.ui.auth.ChangePasswordScreen
import com.mukhtabir.android.ui.auth.LoginScreen
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import com.mukhtabir.android.ui.components.AppLaunchHeader
import com.mukhtabir.android.ui.components.AppLaunchLayout
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.OfflineLaunchActions
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.StartupBrand
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.navigation.AuthenticatedAppShell
import kotlinx.coroutines.delay

@Composable
fun MukhtabirApp(container: AppContainer,onChangeServer: (() -> Unit)? = null,
    onSelectServerFile: ((Int) -> Unit)? = null,
    pendingLink: com.mukhtabir.android.core.AppLink? = null,onConsumeLink: () -> Unit = {}, onDemo: (() -> Unit)? = null) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(container.authRepository),
    )
    val authState by authViewModel.state.collectAsLifecycleState()
    var startupFinished by remember(container) { mutableStateOf(false) }
    LaunchedEffect(authState.isRestoring) { if(authState.isRestoring) startupFinished=false }
    RefreshOnResume { authViewModel.refreshUser(force=false) }
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
                    AppSection(stringResource(R.string.startup_more_options),
                        enabled = !authState.isLoading, expandedOverride = showRecoveryOptions,
                        onExpandedChange = { showRecoveryOptions = it }) {
                        AppOutlinedButton(authViewModel::logout, enabled = !authState.isLoading) {
                            Text(stringResource(R.string.sign_out))
                        }
                        onChangeServer?.let { change ->
                            AppOutlinedButton(change,enabled=!authState.isLoading) { Text(stringResource(R.string.server_change)) }
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

                else -> AuthenticatedAppShell(
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
            StartupBrand(onFinished = onFinished)
            AppLoadingIndicator()
            Text(stringResource(R.string.launch_checking_session),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        OfflineLaunchActions(onDownloaded = onOffline, onDemo = onDemo)
    }
}
