package com.mukhtabir.android.ui.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import com.mukhtabir.android.testing.ContractTestTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class AppNavigationInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var nav: NavHostController
    private lateinit var restoration: StateRestorationTester
    @Before fun content() {
        restoration = StateRestorationTester(compose)
        restoration.setContent { ContractTestTheme {
            nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home") { Text("Home") }
                composable("more/questions") { Text("Library") }
                composable("more/questions/{id}") { Text("Detail") }
                composable("bookmarks") { Text("Bookmarks") }
            }
        } }
    }
    @Test fun `retapping a tab returns to its root without restoring its old detail`() {
        compose.runOnIdle { nav.navigateTopLevel("more/questions") }
        compose.runOnIdle { nav.navigateContent("more/questions/13") }
        compose.runOnIdle { nav.navigateTopLevel("more/questions") }
        compose.runOnIdle { assertEquals("more/questions", nav.currentDestination?.route) }
    }
    @Test fun `opening the same content twice reuses its existing stack entry`() {
        compose.runOnIdle { nav.navigateContent("more/questions/13") }
        val id = compose.runOnIdle { requireNotNull(nav.currentBackStackEntry).id }
        compose.runOnIdle { nav.navigateContent("more/questions/13") }
        compose.runOnIdle {
            assertEquals(id, nav.currentBackStackEntry?.id)
            nav.popBackStack()
            assertEquals("home", nav.currentDestination?.route)
        }
    }
    @Test fun `switching tabs removes stale detail and back returns to home`() {
        compose.runOnIdle { nav.navigateTopLevel("more/questions") }
        compose.runOnIdle { nav.navigateContent("more/questions/13") }
        compose.runOnIdle { nav.navigateTopLevel("bookmarks") }
        compose.runOnIdle {
            assertEquals("bookmarks", nav.currentDestination?.route)
            nav.popBackStack()
            assertEquals("home", nav.currentDestination?.route)
        }
    }
    @Test fun `saved navigation restores the detail identity and its parent stack`() {
        compose.runOnIdle { nav.navigateTopLevel("more/questions") }
        compose.runOnIdle { nav.navigateContent("more/questions/13") }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals("more/questions/{id}", nav.currentDestination?.route)
            assertEquals("13", nav.currentBackStackEntry?.arguments?.getString("id"))
            nav.popBackStack()
            assertEquals("more/questions", nav.currentDestination?.route)
        }
    }

}
