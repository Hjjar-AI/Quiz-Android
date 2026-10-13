package com.mukhtabir.android.core

import android.content.Context
import android.content.SharedPreferences
import android.os.StatFs
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.mukhtabir.android.data.remote.OfflineBankProgress
import com.mukhtabir.android.data.remote.OfflineMaterial
import com.mukhtabir.android.data.remote.OfflinePackSummary
import com.mukhtabir.android.data.remote.summaryOnly
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredOfflinePack(val summary: OfflinePackSummary, val filename: String, val checksum: String)

/** Immutable encrypted files plus a durably committed encrypted index/checkpoint.
 * New files keep their original filenames: EncryptedFile authenticates that filename.
 * Never delete old files on a failed commit (the durable old index may still reference them).
 */
internal class OfflinePackStorage(context: Context, profile: String, private val preferences: SharedPreferences) {
    private val context = context.applicationContext
    private val directory = File(this.context.noBackupFilesDir, "offline-$profile")
    private val json = Json { ignoreUnknownKeys = true }
    private val indexSerializer = ListSerializer(StoredOfflinePack.serializer())
    private val materialSerializer = ListSerializer(OfflineMaterial.serializer())
    private val references = mutableMapOf<String, Pair<OfflineMaterial, StoredOfflinePack>>()

    private fun index(): List<StoredOfflinePack>? = preferences.getString("offline_index", null)?.let {
        json.decodeFromString(indexSerializer, it).also { entries ->
            check(entries.map { entry -> entry.summary.id }.distinct().size == entries.size)
        }
    }

    private fun encrypted(file: File): EncryptedFile = EncryptedFile.Builder(file, context,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()

    private fun file(name: String): File {
        check(name.matches(Regex("[0-9a-f-]{36}\\.bin")))
        return File(directory, name)
    }

    fun read(): List<OfflineMaterial> {
        val entries = index()
        if (entries == null) return preferences.getString("offline_material", null)?.let {
            json.decodeFromString(materialSerializer, it)
        } ?: emptyList()
        return entries.map { entry ->
            file(entry.filename) // Validate the path; a missing pack fails only when that pack is opened.
            OfflineMaterial(com.mukhtabir.android.data.remote.OfflinePack(entry.summary.id, "", emptyList()), summary = entry.summary)
        }
    }

    fun readPack(id: String): OfflineMaterial {
        val entries = index() ?: return read().first { it.pack.id == id }
        val entry = entries.first { it.summary.id == id }
        val bytes = encrypted(file(entry.filename)).openFileInput().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                check(output.size().toLong() + count <= MAX_PACK_BYTES)
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        }
        check(checksum(bytes) == entry.checksum)
        val row = json.decodeFromString(OfflineMaterial.serializer(), bytes.toString(Charsets.UTF_8))
        check(row.pack.id == id && row.summary == null)
        references.clear()
        references[id] = row to entry
        return row
    }

    fun progress(): OfflineBankProgress? = preferences.getString("offline_bank_progress", null)?.let {
        json.decodeFromString(OfflineBankProgress.serializer(), it)
    }

    fun write(rows: List<OfflineMaterial>, progress: OfflineBankProgress?) {
        check(rows.map { it.pack.id }.distinct().size == rows.size)
        check(directory.isDirectory || directory.mkdirs())
        val prior = index().orEmpty().associateBy { it.summary.id }
        val created = mutableListOf<File>()
        var commitAttempted = false
        try {
            val next = rows.map { row ->
                val old = prior[row.pack.id]
                if (row.summary != null) {
                    requireNotNull(old) // A display summary must never overwrite its full material.
                } else {
                    val reference = references[row.pack.id]
                    if (reference != null && reference.first === row && reference.second == old) old
                    else {
                        val bytes = json.encodeToString(OfflineMaterial.serializer(), row).toByteArray(Charsets.UTF_8)
                        val digest = checksum(bytes)
                        // Retry after an uncertain commit reuses already written immutable content.
                        if (old != null && old.checksum == digest && file(old.filename).isFile) old
                        else {
                            if (bytes.size > MAX_PACK_BYTES || StatFs(directory.path).availableBytes < bytes.size.toLong() + RESERVE_BYTES) {
                                throw OfflineCacheCapacityException()
                            }
                            val filename = "${UUID.randomUUID()}.bin"
                            val target = file(filename)
                            try {
                                encrypted(target).openFileOutput().use { output -> output.write(bytes) }
                                java.io.FileOutputStream(target, true).use { output -> output.fd.sync() }
                            } catch (failure: Exception) { target.delete(); throw failure }
                            created += target
                            StoredOfflinePack(requireNotNull(row.summaryOnly().summary), filename, digest)
                        }
                    }
                }
            }
            val editor = preferences.edit().remove("offline_material")
            if (next.isEmpty()) editor.remove("offline_index")
            else editor.putString("offline_index", json.encodeToString(indexSerializer, next))
            if (progress == null) editor.remove("offline_bank_progress")
            else editor.putString("offline_bank_progress", json.encodeToString(OfflineBankProgress.serializer(), progress))
            commitAttempted = true
            check(editor.commit())
            references.clear()
            rows.zip(next).filter { it.first.summary == null }.forEach { (row, entry) -> references[row.pack.id] = row to entry }
            val kept = next.map { it.filename }.toSet()
            runCatching { directory.listFiles()?.filter { it.isFile && it.name !in kept }?.forEach { it.delete() } }
        } catch (failure: Exception) {
            if (!commitAttempted) created.forEach { it.delete() }
            throw failure
        }
    }

    fun clearFiles() {
        references.clear()
        runCatching { directory.deleteRecursively() }
    }

    private fun checksum(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_PACK_BYTES = 16 * 1024 * 1024
        const val RESERVE_BYTES = 16L * 1024 * 1024
    }
}
