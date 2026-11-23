package com.mukhtabir.android.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.QuizPresetHolder
import com.mukhtabir.android.core.ThemePreference
import com.mukhtabir.android.data.remote.BlueprintListDto
import com.mukhtabir.android.data.remote.CategoryListDto
import com.mukhtabir.android.data.remote.LearnerBlueprints
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerImages
import com.mukhtabir.android.data.remote.LearnerNotices
import com.mukhtabir.android.data.remote.LearnerTips
import com.mukhtabir.android.data.remote.MasterExamNoticesDto
import com.mukhtabir.android.data.remote.TipsResponseDto
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.MukhtabirTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DemoActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = ViewModelProvider(this, viewModelFactory {
            initializer { DemoRuntime(AppPreferences.wrapContext(applicationContext)) }
        })[DemoRuntime::class.java]
        val preferences = runtime.preferences
        setContent {
            val theme by preferences.theme.collectAsLifecycleState()
            val display by preferences.display.collectAsLifecycleState()
            val dark = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                else -> theme in AppPreferences.DARK_THEMES
            }
            MukhtabirTheme(darkTheme = dark, preference = theme, display = display) {
                val barColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    val transparent = android.graphics.Color.TRANSPARENT
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(transparent)
                            else SystemBarStyle.light(transparent, transparent),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(barColor)
                            else SystemBarStyle.light(barColor, barColor),
                    )
                }
                DemoShell(runtime, onReset = {
                    runtime.reset()
                    restart()
                }, onLanguageChanged = { language ->
                    if (language != preferences.language.value) {
                        runtime.store.retire()
                        preferences.setLanguage(language)
                        restart()
                    }
                })
            }
        }
    }

    private fun restart() {
        startActivity(Intent(this, DemoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}

internal class DemoRuntime(context: Context) : ViewModel() {
    val preferences = AppPreferences(context)
    val fixtures = DemoFixtures(context)
    val store = DemoStore(context, fixtures::seed)
    val presets = QuizPresetHolder()
    val questions = DemoCatalog(store, fixtures)
    val bookmarks = DemoBookmarks(store, fixtures)
    val sessions = DemoSessions(store, fixtures)
    val history = DemoHistory(store)
    val planner = DemoPlanner(store, fixtures)
    val analytics = DemoAnalytics(store, fixtures)
    val offline = DemoOffline(store, fixtures)
    val categories = object : LearnerCategories {
        override suspend fun list() = withContext(Dispatchers.IO) {
            store.readOnline { CategoryListDto(fixtures.categories, fixtures.categories.size) }
        }
    }
    val blueprints = object : LearnerBlueprints {
        override suspend fun list() = BlueprintListDto()
    }
    val images = object : LearnerImages {
        override suspend fun load(url: String): ByteArray = throw demoMissing()
    }
    val notices = object : LearnerNotices {
        override suspend fun notices() = MasterExamNoticesDto()
    }
    val tips = object : LearnerTips {
        override suspend fun forLocale(locale: String) = TipsResponseDto(locale = locale)
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        store.reset(fixtures.seed())
    }
}
