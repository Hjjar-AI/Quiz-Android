package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.*

/** Existing operator endpoints only; no automatic replay or phone-side database manipulation. */
class OperatorService(private val api: ApiClient) {
    suspend fun info(): JsonObject = api.get(ApiEndpoints.Database.INFO) as JsonObject
    suspend fun quality(): JsonObject = api.get(ApiEndpoints.Database.QUALITY) as JsonObject
    suspend fun backups(): JsonObject = api.get(ApiEndpoints.Database.BACKUPS) as JsonObject
    suspend fun act(action: String, backup: String?, password: String): JsonObject {
        val path = when (action) {
            "backup" -> ApiEndpoints.Database.BACKUP
            "flag" -> ApiEndpoints.Database.QUALITY
            "restore" -> ApiEndpoints.Database.RESTORE
            "clear" -> ApiEndpoints.Database.CLEAR
            "ranks" -> ApiEndpoints.Database.REFRESH_RANKS
            "seed" -> ApiEndpoints.Database.SEED
            else -> error("Unknown operator action")
        }
        val data = buildJsonObject {
            if (action == "restore") put("backup_name", requireNotNull(backup))
            if (action in setOf("restore", "clear")) put("admin_password", password)
        }
        return api.post(path, data) as JsonObject
    }
}
