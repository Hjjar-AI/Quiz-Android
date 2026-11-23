package com.mukhtabir.android.ui.transfer

import com.mukhtabir.android.core.has
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.DownloadInfo
import com.mukhtabir.android.data.remote.TransferService
import com.mukhtabir.android.data.remote.UserDto
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal enum class ImportStatus { Pending, Limited, Uploading, Imported, Failed, Uncertain }

internal data class ImportEntry(
    val uri: Uri, val file: File, val name: String,
    val status: ImportStatus = ImportStatus.Pending,
    val result: JsonObject? = null, val error: Throwable? = null,
)

internal data class TransferState(
    val busy: Boolean = false, val error: Throwable? = null, val message: Int? = null,
    val queue: List<ImportEntry> = emptyList(), val importUnlockedUntil: Long = 0,
    val file: File? = null, val name: String = "", val result: JsonObject? = null,
    val analysis: JsonObject? = null, val uncertain: Boolean = false, val reviewed: Boolean = false,
    val reviewQuestions: List<com.mukhtabir.android.data.remote.QuestionDto> = emptyList(),
    val artifact: File? = null, val download: DownloadInfo? = null, val savedUri: Uri? = null,
)

internal class TransferViewModel(private val service: TransferService, private val questions: com.mukhtabir.android.data.remote.QuestionService, private val user: UserDto,
    context: Context) : ViewModel() {
    val inputs = TransferInputs()
    private val app = context.applicationContext
    private val owned = mutableListOf<File>()
    private var limitGeneration = 0L
    private var limitExpiry: Job? = null
    private val _state = MutableStateFlow(TransferState())
    val state = _state.asStateFlow()
    private fun applyImportLimit(status: com.mukhtabir.android.data.remote.ImportLimitStatus) {
        limitExpiry?.cancel()
        val seconds = if (status.active) status.expiresIn else 0
        _state.update { it.copy(importUnlockedUntil = if (seconds > 0) SystemClock.elapsedRealtime() + seconds * 1000L else 0) }
        if (seconds > 0) limitExpiry = viewModelScope.launch {
            delay(seconds * 1000L)
            _state.update { it.copy(importUnlockedUntil = 0) }
        }
    }

    fun refreshImportLimit() {
        if (!database() || _state.value.busy) return
        val generation = ++limitGeneration
        viewModelScope.launch {
            try {
                val status = service.importLimitStatus()
                ensureActive()
                if (generation == limitGeneration) applyImportLimit(status)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { /* Keep normal imports available if the status read fails. */ }
        }
    }

    fun unlockImports(password: String) {
        if (!database() || _state.value.busy || password.isEmpty()) return
        ++limitGeneration
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val status = service.unlockImportLimit(password)
                ensureActive()
                applyImportLimit(status)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                _state.update { it.copy(error = error) }
                // Recover the grant with a read after response loss; do not replay the POST.
                val statusCode = (error as? ApiException)?.statusCode
                if (statusCode == null || statusCode >= 500) {
                    try {
                        val status = service.importLimitStatus()
                        ensureActive()
                        applyImportLimit(status)
                        if (status.active) _state.update { it.copy(error = null) }
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { /* Retain the original failure. */ }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun temporary(): File = File.createTempFile("transfer-", ".tmp", app.cacheDir).also { owned.add(it) }
    private fun permitted(): Boolean = user.has("admin.database") || com.mukhtabir.android.data.remote.canExportExamResults(user)
    private fun database(): Boolean = user.has("admin.database")
    fun clearFile() {
        if (!database() || _state.value.busy || _state.value.uncertain) return
        _state.value.file?.delete()
        _state.value.queue.forEach { it.file.delete() }
        _state.update { it.copy(file = null, queue = emptyList(), name = "", analysis = null, result = null) }
        inputs.mapping.value = emptyMap(); inputs.resolutions.value = emptyMap()
    }
    fun invalidateAnalysis() { if (!_state.value.busy) _state.update { it.copy(analysis = null, result = null, reviewed = false) } }
    fun select(uri: Uri, kind: String) {
        if (!database() || _state.value.busy || _state.value.uncertain) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            var staged: File? = null
            try {
                val (target, name) = stage(uri, kind)
                staged = target
                ensureActive()
                _state.value.file?.delete()
                _state.update { it.copy(busy = false, file = target, name = name, analysis = null, result = null) }
            } catch (cancel: CancellationException) { staged?.delete(); throw cancel }
            catch (error: Exception) { staged?.delete(); ensureActive(); _state.update { it.copy(busy = false, error = if (error is IllegalArgumentException) ApiException(null, error.message.orEmpty()) else error) } }
        }
    }
    private suspend fun stage(uri: Uri, kind: String): Pair<File, String> {
        val target = temporary()
        try {
            val name = withContext(Dispatchers.IO) {
                val found = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: "import.json"
                val extensions = when (kind) { "telegram" -> setOf("json"); "state" -> setOf("json", "xlsx"); else -> setOf("json", "xlsx", "xls", "csv") }
                require(found.substringAfterLast('.', "").lowercase() in extensions) { "$found: ${app.getString(R.string.transfer_bad_file)}" }
                app.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 50L * 1024 * 1024) { "$found: ${app.getString(R.string.transfer_too_large)}" }
                        output.write(buffer, 0, count)
                    }
                } } ?: error(app.getString(R.string.transfer_bad_file))
                require(target.length() > 0) { app.getString(R.string.transfer_bad_file) }
                found.substringAfterLast('/').substringAfterLast('\\').take(180)
            }
            return target to name
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    fun selectMany(uris: List<Uri>) {
        if (!database() || _state.value.busy || _state.value.uncertain || inputs.kind.value != "simple") return
        val candidates = uris.distinct().filter { uri -> _state.value.queue.none { it.uri == uri } }
        if (candidates.isEmpty()) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                for (uri in candidates) {
                    ensureActive()
                    try {
                        val (file, name) = stage(uri, "simple")
                        ensureActive()
                        _state.update { it.copy(queue = it.queue + ImportEntry(uri, file, name)) }
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (error: Exception) {
                        ensureActive()
                        _state.update { it.copy(error = if (error is IllegalArgumentException) ApiException(null, error.message.orEmpty()) else error) }
                    }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun removeQueued(uri: Uri) {
        if (!database() || _state.value.busy || _state.value.uncertain) return
        _state.value.queue.firstOrNull { it.uri == uri }?.file?.delete()
        _state.update { it.copy(queue = it.queue.filterNot { entry -> entry.uri == uri }) }
    }

    private fun updateEntry(uri: Uri, change: (ImportEntry) -> ImportEntry) {
        _state.update { it.copy(queue = it.queue.map { entry -> if (entry.uri == uri) change(entry) else entry }) }
    }

    private fun importQueued() {
        val s = _state.value
        if (!database() || s.busy || s.uncertain || inputs.kind.value != "simple") return
        val pending = s.queue.filter { it.status in setOf(ImportStatus.Pending, ImportStatus.Limited) }
        if (pending.isEmpty()) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                for (entry in pending) {
                    ensureActive()
                    updateEntry(entry.uri) { it.copy(status = ImportStatus.Uploading, error = null) }
                    try {
                        val result = service.importFile(entry.file, entry.name, "simple", emptyMap())
                        ensureActive()
                        entry.file.delete()
                        updateEntry(entry.uri) { it.copy(status = ImportStatus.Imported, result = result, error = null) }
                        _state.update { it.copy(message = R.string.transfer_imported) }
                    } catch (cancel: CancellationException) {
                        updateEntry(entry.uri) { it.copy(status = ImportStatus.Uncertain) }
                        _state.update { it.copy(uncertain = true, reviewed = false, message = R.string.transfer_uncertain) }
                        throw cancel
                    } catch (error: Exception) {
                        ensureActive()
                        val uncertain = !(error is ApiException && error.statusCode in setOf(400, 401, 403, 404, 413, 415, 429))
                        updateEntry(entry.uri) { it.copy(status = when { error is ApiException && error.statusCode == 429 -> ImportStatus.Limited; uncertain -> ImportStatus.Uncertain; else -> ImportStatus.Failed }, error = error) }
                        _state.update { it.copy(error = error, uncertain = uncertain, reviewed = false,
                            message = if (uncertain) R.string.transfer_uncertain else R.string.transfer_batch_stopped) }
                        break
                    }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun importFile(kind: String, mode: String, strategy: String, preview: Boolean,
        mapping: JsonObject, resolutions: JsonObject, password: String) {
        if (kind == "simple" && !preview && _state.value.queue.isNotEmpty()) {
            importQueued()
            return
        }
        val s = _state.value
        val file = s.file ?: return
        if (!database() || s.busy || (!preview && s.uncertain) || kind !in setOf("simple", "telegram", "state")) return
        if (kind == "state" && (mode !in setOf("merge", "replace") || strategy !in setOf("keep_local", "use_imported", "review"))) return
        if (preview && kind != "state") return
        if (!preview && kind == "state") {
            val analysis = s.analysis ?: return
            val unknown = analysis["unknown_authors"] as? JsonArray ?: JsonArray(emptyList())
            if (unknown.any { mapping[it.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()] == null }) return
            val conflicts = analysis["conflicts"] as? JsonArray ?: JsonArray(emptyList())
            if (strategy == "review" && conflicts.any { resolutions[it.jsonObject["uuid"]?.jsonPrimitive?.content.orEmpty()] == null }) return
            if (mode == "replace" && password.isBlank()) return
        }
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val options = if (kind == "state") mapOf("mode" to mode, "conflict_strategy" to strategy,
                    "analyze" to preview.toString(), "mapping" to mapping.toString(), "conflict_resolutions" to resolutions.toString(),
                    "admin_password" to if (preview) "" else password) else emptyMap()
                val result = service.importFile(file, s.name, kind, options)
                ensureActive()
                if (preview) {
                    inputs.mapping.value = emptyMap(); inputs.resolutions.value = emptyMap()
                    _state.update { it.copy(busy = false, analysis = result, reviewed = s.uncertain) }
                }
                else {
                    file.delete()
                    _state.update { it.copy(busy = false, file = null, analysis = null, result = result, message = R.string.transfer_imported) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                ensureActive()
                val uncertain = !preview && !(error is ApiException && error.statusCode in setOf(400, 401, 403, 404, 413, 415, 429))
                _state.update { it.copy(busy = false, error = error, uncertain = s.uncertain || uncertain, reviewed = false,
                    message = if (uncertain) R.string.transfer_uncertain else null) }
            }
        }
    }
    fun reviewSimple() {
        if (!database() || _state.value.busy || !_state.value.uncertain) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { val found = questions.list(); ensureActive(); _state.update { it.copy(busy = false, reviewQuestions = found.items, reviewed = true) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(busy = false, error = error) } }
        }
    }
    fun acknowledgeReview() {
        val s = _state.value
        if (!database() || s.busy || !s.uncertain || !s.reviewed) return
        _state.update { it.copy(uncertain = false, analysis = null, reviewed = false, message = null,
            queue = it.queue.map { entry -> if (entry.status == ImportStatus.Uncertain) entry.copy(status = ImportStatus.Failed) else entry }) }
    }
    fun export(format: String, verified: Boolean, state: Boolean, images: Boolean,
        filters: Map<String, String>, body: JsonObject) {
        if (!database() || _state.value.busy) return
        _state.value.artifact?.delete()
        _state.update { it.copy(busy = true, error = null, message = null, artifact = null, download = null, savedUri = null) }
        viewModelScope.launch {
            var artifact: File? = null
            try {
                val target = temporary().also { artifact = it }
                val info = service.export(target, format, verified, state, images, filters, body)
                ensureActive()
                _state.update { it.copy(busy = false, artifact = target, download = info) }
            } catch (cancel: CancellationException) { artifact?.delete(); throw cancel }
            catch (error: Exception) { artifact?.delete(); ensureActive(); _state.update { it.copy(busy = false, error = error) } }
        }
    }
    fun exportExam(id: Long, kind: String, exams: com.mukhtabir.android.data.remote.MasterExamService) {
        if (!com.mukhtabir.android.data.remote.canExportExamResults(user) || _state.value.busy || id <= 0 || kind !in setOf("summary", "matrix")) return
        _state.value.artifact?.delete()
        _state.update { it.copy(busy = true, error = null, artifact = null, download = null, savedUri = null, message = null) }
        viewModelScope.launch {
            var artifact: File? = null
            try {
                val target = temporary().also { artifact = it }
                if (!user.has("master_exams.view_results_any") && !user.has("master_exams.manage_any")) {
                    val fresh = exams.authorDetail(id)
                    ensureActive()
                    if (fresh.primaryAttending != user.id && fresh.coAttendings.none { it.id == user.id }) throw ApiException(403, app.getString(R.string.error_forbidden))
                }
                val info = service.exportExam(target, id, kind)
                ensureActive()
                _state.update { it.copy(busy = false, artifact = target, download = info) }
            } catch (cancel: CancellationException) { artifact?.delete(); throw cancel }
            catch (error: Exception) { artifact?.delete(); ensureActive(); _state.update { it.copy(busy = false, error = error) } }
        }
    }

    fun save(uri: Uri) {
        val s = _state.value
        val file = s.artifact ?: return
        if (!permitted() || s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { output -> file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) { ensureActive(); val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count) }
                    } } ?: throw ApiException(null, app.getString(R.string.transfer_save_failed))
                }
                ensureActive()
                _state.update { it.copy(busy = false, savedUri = uri, message = R.string.transfer_saved) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { ensureActive(); _state.update { it.copy(busy = false, error = error) } }
        }
    }
    override fun onCleared() { owned.forEach { it.delete() } }
    class Factory(private val service: TransferService, private val questions: com.mukhtabir.android.data.remote.QuestionService, private val user: UserDto, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TransferViewModel(service, questions, user, context) as T
    }
}
