package com.mukhtabir.android.ui.navigation

import androidx.compose.runtime.mutableStateOf

/** Shell-owned state shared with route callbacks; lifetime matches the authenticated shell. */
internal class AppNavigationState {
    val questionBankRevision = mutableStateOf(0)
    val removedQuestionIds = mutableStateOf(emptySet<Long>())
    val questionActionBusy = mutableStateOf(false)
    val quizSessionActive = mutableStateOf(false)
}
