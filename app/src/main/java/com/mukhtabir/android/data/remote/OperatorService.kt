package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.*

/** Existing operator endpoints only; no automatic replay or phone-side database manipulation. */
class OperatorService(private val api: ApiClient) {
    suspend fun info(): JsonObject = api.get("database/info/") as JsonObject
    suspend fun quality(): JsonObject = api.get("database/data-quality/") as JsonObject
    suspend fun backups(): JsonObject = api.get("database/backups/") as JsonObject
    suspend fun act(action: String, backup: String?, password: String): JsonObject {
        val path = when (action) {
            "backup" -> "database/backup/"
            "flag" -> "database/data-quality/"
            "restore" -> "database/restore/"
            "clear" -> "database/clear/"
            "ranks" -> "admin/refresh-author-ranks/"
            "seed" -> "admin/seed-sample-questions/"
            else -> error("Unknown operator action")
        }
        val data = buildJsonObject {
            if (action == "restore") put("backup_name", requireNotNull(backup))
            if (action in setOf("restore", "clear")) put("admin_password", password)
        }
        return api.post(path, data) as JsonObject
    }
}
