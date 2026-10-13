package com.mukhtabir.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.auth.AuthRepository
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Auth state.
 *
 * [sessionExpiredMessage] is a one-shot flag the App reads to show a
 * snackbar. It is set when the ApiClient's sessionExpired flow emits
 * (a 401 came back from any request) and cleared by the App after the
 * snackbar is shown. It is NOT a persistent error — the user is
 * already back on the login screen by the time it is displayed.
 */
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val restorePending: Boolean = false,
    val restoreError: Throwable? = null,
    val loginError: Throwable? = null,
    val user: UserDto? = null,
    val profileRefreshing: Boolean = false,
    val profileError: Throwable? = null,
    val sessionExpiredMessage: String? = null,
)

class AuthViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var authJob: Job? = null
    private var profileJob: Job? = null
    private var pendingProfile: UserDto? = null
    private var profileApplicationAllowed = true
    private var lastProfileRefresh = 0L
    private var profileGeneration = 0L

    init {
        // Collect the sessionExpired flow BEFORE restoreSession so an
        // expiry that fires during restoration is not missed.
        viewModelScope.launch {
            repository.sessionExpired.collect {reason ->
                authJob?.cancel()
                profileJob?.cancel()
                pendingProfile=null
                _state.value = AuthUiState(
                    isRestoring = false,
                    sessionExpiredMessage = reason,
                )
            }
        }
        viewModelScope.launch { repository.profileRefreshRequests.collect { force -> refreshUser(force=force) } }
        restoreSession()
    }

    fun restoreSession() {
        if (authJob?.isActive == true || _state.value.isLoading) return
        if (!repository.hasPersistedSession()) {
            _state.update { it.copy(isRestoring = false, restorePending = false, restoreError = null) }
            return
        }
        _state.update { it.copy(isRestoring = true, restorePending = false, restoreError = null, loginError = null) }
        authJob = viewModelScope.launch {
            runCatching { repository.currentUser() }
                .onSuccess { user ->
                    ensureActive()
                    _state.update { it.copy(isRestoring = false, user = user) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    ensureActive()
                    val status = (error as? ApiException)?.statusCode
                    if (status == 403) {
                        repository.clearSession()
                        _state.value = AuthUiState(isRestoring = false, loginError = error)
                    } else if (status == 401 || !repository.hasPersistedSession()) {
                        // ApiClient already handles a server 401, including the
                        // stronger ACCOUNT_EXPIRED cleanup. Do not erase downloads
                        // a second time here. A locally expired cookie also only
                        // revokes the online session.
                        if (status != 401) repository.expireOnlineSession()
                        _state.value = AuthUiState(isRestoring = false, loginError = error)
                    } else {
                        // No authenticated screen opens until /me succeeds; keep
                        // valid saved cookies and encrypted drafts for read Retry.
                        _state.update { it.copy(isRestoring = false, restorePending = true,
                            restoreError = error, user = null) }
                    }
                }
        }
    }

    /**
     * Defer navigation-changing profile updates while an editor/write/session is active.
     * The server continues to enforce current permissions on every request.
     */
    fun setProfileApplicationAllowed(allowed: Boolean) {
        profileApplicationAllowed=allowed
        if(allowed) pendingProfile?.let {fresh ->
            if(_state.value.user?.id==fresh.id) _state.update {it.copy(user=fresh,profileError=null)}
            pendingProfile=null
        }
    }

    fun refreshUser(force: Boolean = true) {
        val expectedId=_state.value.user?.id ?: return
        if(_state.value.isLoading || _state.value.isRestoring) return
        val now=android.os.SystemClock.elapsedRealtime()
        if(!force && (profileJob?.isActive==true || now-lastProfileRefresh<30_000)) return
        val generation=++profileGeneration
        profileJob?.cancel()
        lastProfileRefresh=now
        _state.update {it.copy(profileRefreshing=true,profileError=null)}
        profileJob = viewModelScope.launch {
            try {
                val fresh=repository.refreshedUser(expectedId)
                ensureActive()
                if(_state.value.user?.id!=expectedId || fresh.id!=expectedId) return@launch
                if(profileApplicationAllowed) _state.update {it.copy(user=fresh,profileError=null)}
                else pendingProfile=fresh
            } catch(error: CancellationException) {throw error}
            catch(error: Exception) {
                ensureActive()
                if(_state.value.user?.id==expectedId) _state.update {it.copy(profileError=error)}
            } finally {
                if(profileGeneration==generation && _state.value.user?.id==expectedId) _state.update {it.copy(profileRefreshing=false)}
            }
        }
    }

    /**
     * Leave the forced-change gate immediately after the server has
     * accepted the new password, then reconcile the rest of the profile.
     * This avoids trapping the user on the form if the follow-up /me call
     * loses connectivity after the password was already changed.
     */
    fun completeForcedPasswordChange() {
        _state.update { current ->
            current.copy(user = current.user?.copy(mustChangePassword = false))
        }
        refreshUser()
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, loginError = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loginError = null) }
    }

    fun login() {
        val snapshot = _state.value
        if (snapshot.username.isBlank() || snapshot.password.isBlank() || snapshot.isLoading ||
            snapshot.isRestoring || snapshot.restorePending || authJob?.isActive == true
        ) return
        profileJob?.cancel()
        pendingProfile=null
        _state.update { it.copy(isLoading = true, isRestoring = false, loginError = null) }
        authJob = viewModelScope.launch {
            runCatching { repository.login(snapshot.username.trim(), snapshot.password) }
                .onSuccess { user ->
                    ensureActive()
                    _state.update { it.copy(isLoading = false, password = "", user = user) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    ensureActive()
                    _state.update { it.copy(isLoading = false, loginError = error) }
                }
        }
    }

    fun forgetSession() {
        authJob?.cancel(); profileJob?.cancel(); pendingProfile=null
        repository.clearSession()
        _state.value = AuthUiState(isRestoring=false)
    }

    fun logout() {
        if (_state.value.isLoading || authJob?.isActive == true) return
        profileJob?.cancel(); pendingProfile=null
        _state.update { it.copy(isLoading = true, loginError = null) }
        authJob = viewModelScope.launch {
            runCatching { repository.logout() }.onFailure { error ->
                if (error is CancellationException) throw error
                ensureActive()
            }
            ensureActive()
            repository.clearSession()
            _state.value = AuthUiState(username = _state.value.username, isRestoring = false)
        }
    }

    /** Clear the one-shot session-expired message after the App shows it. */
    fun consumeSessionExpiredMessage() {
        _state.update { it.copy(sessionExpiredMessage = null) }
    }

    class Factory(
        private val repository: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(repository) as T
        }
    }
}
