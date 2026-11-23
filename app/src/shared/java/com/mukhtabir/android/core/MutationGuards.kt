package com.mukhtabir.android.core

import com.mukhtabir.android.data.remote.ApiException

fun Throwable.isRejectedWrite(): Boolean = this is ApiException && statusCode in setOf(400, 401, 403, 404, 409, 410, 422, 429)
