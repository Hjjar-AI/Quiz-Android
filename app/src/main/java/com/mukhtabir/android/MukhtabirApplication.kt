package com.mukhtabir.android

import android.app.Application
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.core.ServerSettings
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.NetworkMonitor

class MukhtabirApplication : Application() {
    val serverSettings: ServerSettings by lazy { ServerSettings(applicationContext) }
    val preferences: AppPreferences by lazy { AppPreferences(applicationContext) }
    private val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(applicationContext) }
    private var cachedContainer: AppContainer? = null
    private var containerUrl: String? = null

    val container: AppContainer
        get() {
            val url = requireNotNull(serverSettings.read())
            if (cachedContainer == null || containerUrl != url) {
                cachedContainer?.authRepository?.detachServer()
                cachedContainer = AppContainer(applicationContext, url, preferences, networkMonitor)
                containerUrl = url
            }
            return requireNotNull(cachedContainer)
        }

    fun stopServerSession() {
        cachedContainer?.authRepository?.detachServer()
        cachedContainer = null
        containerUrl = null
    }
}
