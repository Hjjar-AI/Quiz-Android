package com.mukhtabir.android.data.auth

import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.ApiClient
import com.mukhtabir.android.data.remote.UserDto

class AuthRepository(
    private val apiClient: ApiClient,
    private val sessionDraftStore: SessionDraftStore? = null,
    private val studyReminders: com.mukhtabir.android.core.StudyReminders? = null,
) {
    val sessionExpired = apiClient.sessionExpired
    val profileRefreshRequests = apiClient.profileRefreshRequests

    fun hasPersistedSession(): Boolean = apiClient.hasPersistedSession()

    suspend fun login(username: String, password: String): UserDto =
        apiClient.login(username = username, password = password).also { sessionDraftStore?.bind(it.id, it.uuid); runCatching { studyReminders?.bind(it.id) } }

    suspend fun currentUser(): UserDto = apiClient.currentUser().also { sessionDraftStore?.bind(it.id, it.uuid); runCatching { studyReminders?.bind(it.id) } }

    /** A profile read must never rebind private storage to a different returned account. */
    suspend fun refreshedUser(expectedUserId: Long): UserDto = apiClient.currentUser().also {check(it.id==expectedUserId)}

    suspend fun changePassword(currentPassword: String, newPassword: String) =
        apiClient.changePassword(currentPassword, newPassword)

    suspend fun logout() = apiClient.logout()

    fun detachServer() {
        sessionDraftStore?.revokeForServerChange()
        apiClient.detachServer()
    }

    fun clearSession() = apiClient.clearSession()

    fun expireOnlineSession() = apiClient.expireOnlineSession()
}
