package com.mukhtabir.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.ui.graphics.vector.ImageVector
import com.mukhtabir.android.core.MaterialIcons

/**
 * Resolve a [MaterialIcons] name string to a Compose [ImageVector].
 *
 * The navigation catalog stores icon names as strings rather than
 * ImageVector references because it is a plain data object (easier to
 * diff and to build from a JSON config later if the app ever
 * supports a server-driven nav). This lookup is the single place the
 * string-to-vector mapping lives.
 *
 * Unknown names fall through to [Icons.Outlined.Inbox] rather than
 * throwing — a typo in the catalog shows as a neutral placeholder,
 * which is more useful than a crash.
 */
fun iconFor(name: String): ImageVector = when (name) {
    MaterialIcons.TAGS -> Icons.Filled.Label
    MaterialIcons.VERIFIED -> Icons.Filled.Verified
    MaterialIcons.HOME -> Icons.Filled.Home
    MaterialIcons.QUIZ -> Icons.Filled.Quiz
    MaterialIcons.BOOKMARK -> Icons.Filled.Bookmark
    MaterialIcons.HISTORY -> Icons.Filled.History
    MaterialIcons.MORE_HORIZ -> Icons.Filled.MoreHoriz
    MaterialIcons.CATEGORIES -> Icons.Filled.Category
    MaterialIcons.QUESTIONS -> Icons.Filled.ListAlt
    MaterialIcons.MISTAKES -> Icons.Filled.Error
    MaterialIcons.FRAGILE -> Icons.Filled.WarningAmber
    MaterialIcons.ANALYTICS -> Icons.Filled.Analytics
    MaterialIcons.PROFILE -> Icons.Filled.Person
    MaterialIcons.SETTINGS -> Icons.Filled.Settings
    MaterialIcons.LOCK -> Icons.Filled.Lock
    MaterialIcons.INFO -> Icons.Filled.Info
    MaterialIcons.MAP -> Icons.Filled.Map
    MaterialIcons.PLANNER -> Icons.Filled.CalendarMonth
    MaterialIcons.GROUPS -> Icons.Filled.Groups
    else -> Icons.Outlined.Inbox
}
