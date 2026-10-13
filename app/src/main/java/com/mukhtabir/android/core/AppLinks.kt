package com.mukhtabir.android.core

import android.content.Intent
import android.net.Uri

/** Numeric links carry the configured API origin so IDs cannot silently cross servers. */
data class AppLink(val kind: String, val id: Long, val server: String) {
    val route: String get() = if (kind == "questions") "more/questions/$id" else "more/master_exams/open/$id"
    fun uri(): Uri = Uri.Builder().scheme("mukhtabir").authority(kind).appendPath(id.toString())
        .appendQueryParameter("server", server).build()
    companion object {
        fun parse(intent: Intent?): AppLink? {
            return runCatching {
                if (intent?.action != Intent.ACTION_VIEW) return@runCatching null
                val uri = intent.data ?: return@runCatching null
                if (uri.scheme != "mukhtabir" || uri.host !in setOf("questions", "exams") ||
                    uri.fragment != null || uri.userInfo != null || uri.port != -1 || uri.pathSegments.size != 1 ||
                    uri.queryParameterNames != setOf("server") || uri.getQueryParameters("server").size != 1) return@runCatching null
                val id = uri.pathSegments.single().takeIf { it.matches(Regex("[1-9][0-9]{0,18}")) }?.toLongOrNull() ?: return@runCatching null
                val server = ServerSettings.normalize(uri.getQueryParameter("server").orEmpty()) ?: return@runCatching null
                AppLink(requireNotNull(uri.host), id, server)
            }.getOrNull()
        }
    }
}
