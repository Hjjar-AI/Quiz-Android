package com.mukhtabir.android.data.remote

import com.mukhtabir.android.core.has
import java.io.File
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/** Server-provided filename is display metadata, never a filesystem path. */
data class DownloadInfo(val filename: String, val mimeType: String)

data class ImportLimitStatus(val active: Boolean, val expiresIn: Int)

class TransferService(private val api: ApiClient) {
    private fun importLimitStatus(value: JsonElement): ImportLimitStatus {
        val data = value as JsonObject
        return ImportLimitStatus((data["active"] as? JsonPrimitive)?.booleanOrNull == true,
            (data["expires_in"] as? JsonPrimitive)?.intOrNull?.coerceIn(0, 600) ?: 0)
    }

    suspend fun importLimitStatus(): ImportLimitStatus = importLimitStatus(api.get("database/import/unlock/"))

    suspend fun unlockImportLimit(password: String): ImportLimitStatus = importLimitStatus(
        api.post("database/import/unlock/", buildJsonObject { put("admin_password", password) }))

    suspend fun exportExam(file: File, examId: Long, kind: String): DownloadInfo {
        require(examId > 0 && kind in setOf("summary", "matrix"))
        return api.download(ApiEndpoints.MasterExams.action(examId, "results/$kind.csv"), file)
    }

    suspend fun importFile(file: File, name: String, kind: String, options: Map<String, String>): JsonObject {
        require(kind in setOf("simple", "telegram", "state"))
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", name, file.asRequestBody("application/octet-stream".toMediaType()))
        options.forEach { (key, value) -> body.addFormDataPart(key, value) }
        val path = when (kind) {
            "telegram" -> "database/import/telegram/"
            "state" -> "database/import/state/"
            else -> "database/import/"
        }
        return api.postMultipart(path, body.build()) as JsonObject
    }

    suspend fun export(file: File, format: String, verified: Boolean, state: Boolean,
        images: Boolean, filters: Map<String, String>, body: JsonElement): DownloadInfo {
        require(format in setOf("json", "csv", "xlsx", "pdf"))
        if (state) {
            require(format in setOf("json", "xlsx"))
            return api.download("database/export/state/", file, filters + mapOf(
                "format" to format, "include_images" to images.toString(), "verified_only" to verified.toString()))
        }
        return api.download("database/export/$format/" + if (verified) "verified/" else "", file, body = body)
    }
}

fun canExportExamResults(user: UserDto): Boolean = listOf("master_exams.view_results_any", "master_exams.view_results_own",
    "master_exams.manage_any", "master_exams.manage_own").any(user::has)
