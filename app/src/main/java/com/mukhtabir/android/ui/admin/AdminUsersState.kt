@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.data.remote.UserDto

data class AdminUsersState(
    val users: List<UserDto> = emptyList(), val page: Int = 1, val totalPages: Int = 1,
    val search: String = "", val selected: UserDto? = null, val username: String = "",
    val fullName: String = "", val password: String = "", val adminPassword: String = "",
    val expiryDays: String = "60", val renewalDays: String = "0",
    val role: String = "member", val active: Boolean = true, val loading: Boolean = false,
    val error: Throwable? = null, val validation: Int? = null, val saved: Boolean = false,
    val needsReview: Boolean = false,val createUncertain: Boolean = false,val listFresh: Boolean = false,
    val batchIds: Set<Long> = emptySet(), val batchUnknown: Set<Long> = emptySet(),
    val batchReviewed: Boolean = false, val batchRows: List<UserDto> = emptyList(), val batchMissing: Set<Long> = emptySet(), val batchDesired: Boolean? = null, val batchSuccess: Int = 0, val batchFailed: Int = 0,
)
