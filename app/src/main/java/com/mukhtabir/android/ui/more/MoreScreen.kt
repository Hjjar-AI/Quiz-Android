package com.mukhtabir.android.ui.more

import androidx.compose.runtime.Composable
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.navigation.AppDrawerContent

/** Kept for restored navigation from older app tasks; new navigation uses the drawer. */
@Composable
fun MoreScreen(user: UserDto, onNavigate: (String) -> Unit) {
    AppDrawerContent(user, currentRoute = "more", enabled = true, onNavigate = onNavigate)
}
