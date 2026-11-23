package com.mukhtabir.android.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ReportStateDto(val reported: Boolean, val reason: String? = null)
