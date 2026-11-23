package com.mukhtabir.android.core

import kotlinx.serialization.Serializable

/** Small encrypted navigation hints; no question text or images. */
@Serializable
data class AuthorNavigation(val categories: List<Long> = emptyList(), val tags: List<String> = emptyList(), val lastQuestionId: Long? = null)
