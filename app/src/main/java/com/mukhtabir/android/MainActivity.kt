package com.mukhtabir.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.ThemePreference
import com.mukhtabir.android.ui.MukhtabirApp
import com.mukhtabir.android.ui.auth.ServerSetupScreen
import com.mukhtabir.android.ui.components.AppLaunchLayout
import com.mukhtabir.android.ui.components.StartupBrand
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.MukhtabirTheme

class MainActivity : ComponentActivity() {
    private var pendingLink by mutableStateOf<com.mukhtabir.android.core.AppLink?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLink = com.mukhtabir.android.core.AppLink.parse(intent)
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLink = com.mukhtabir.android.core.AppLink.parse(intent)

        val app = application as MukhtabirApplication
        val preferences = app.preferences
        setContent {
            val theme by preferences.theme.collectAsLifecycleState()
            val display by preferences.display.collectAsLifecycleState()
            val language by preferences.language.collectAsLifecycleState()
            var editingServer by rememberSaveable { mutableStateOf(app.serverSettings.read()==null) }
            // Unconfigured installs do not enter MukhtabirApp's saved-login splash.
            // Save completion through rotation and language recreation.
            var welcomeSplashFinished by rememberSaveable { mutableStateOf(app.serverSettings.read() != null) }
            var requestedFileSlot by rememberSaveable { mutableStateOf<Int?>(null) }
            val darkTheme = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                in com.mukhtabir.android.core.AppPreferences.DARK_THEMES -> true
                else -> false
            }
            MukhtabirTheme(darkTheme = darkTheme, preference = theme, display = display) {
                val barColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    val transparent = android.graphics.Color.TRANSPARENT
                    enableEdgeToEdge(
                        statusBarStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                        navigationBarStyle = if (darkTheme) SystemBarStyle.dark(barColor) else SystemBarStyle.light(barColor, barColor),
                    )
                }
                if (!welcomeSplashFinished) Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        AppLaunchLayout {
                            StartupBrand(
                                modifier = Modifier.align(Alignment.CenterHorizontally), onFinished = { welcomeSplashFinished = true })
                        }
                    }
                } else if(editingServer || app.serverSettings.read()==null) Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        ServerSetupScreen(app.serverSettings,app.serverSettings.read().orEmpty(),
                            onSaved={changed ->
                                if(changed) app.stopServerSession()
                                // A new task discards all old account/server ViewModels and saved navigation.
                                val restart = Intent(this@MainActivity,MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                pendingLink?.takeIf { it.server == app.serverSettings.read() }?.let {
                                    restart.action=Intent.ACTION_VIEW; restart.data=it.uri()
                                }
                                startActivity(restart)
                                finish()
                            },onCancel=if(app.serverSettings.read()!=null) ({editingServer=false;requestedFileSlot=null}) else null,
                            initialFileSlot=requestedFileSlot, language=language,
                            onLanguageChange={code -> preferences.setLanguage(code); recreate()})
                    }
                } else MukhtabirApp(container = app.container,
                    onChangeServer={requestedFileSlot=null;editingServer=true},
                    onSelectServerFile={slot -> requestedFileSlot=slot;editingServer=true}, pendingLink=pendingLink,
                    onConsumeLink={pendingLink=null; intent.data=null})
            }
        }
    }
}
