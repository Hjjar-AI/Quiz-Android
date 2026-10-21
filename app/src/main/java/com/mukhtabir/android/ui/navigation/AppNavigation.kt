package com.mukhtabir.android.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

internal fun NavHostController.navigateTopLevel(route: String) {
    // A tab re-tap returns to its existing root; no restored detail stack is replayed.
    if (popBackStack(route, inclusive = false)) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

internal fun NavHostController.navigateContent(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { launchSingleTop = true }
}
