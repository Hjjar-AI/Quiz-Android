package com.mukhtabir.android.ui.offline

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.core.LearnerStorage
import com.mukhtabir.android.core.OfflineCacheCapacityException
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.OfflineAnswer
import com.mukhtabir.android.data.remote.OfflineBankProgress
import com.mukhtabir.android.data.remote.OfflineMaterial
import com.mukhtabir.android.data.remote.OfflinePack
import com.mukhtabir.android.data.remote.LearnerOffline
import com.mukhtabir.android.data.remote.OfflineSelection
import com.mukhtabir.android.data.remote.OfflineFilterOptions
import com.mukhtabir.android.data.remote.summaryOnly
import java.util.UUID
import java.time.Instant
import com.mukhtabir.android.data.remote.newMaterial
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
data class OfflineStudyState(val materials: List<OfflineMaterial> = emptyList(),val selected: String? = null,
    val ids: String = "",val busy: Boolean = false,val pendingStorage: Boolean = false,val storageFailed: Boolean = false,val error: Throwable? = null,
    val message: Int? = null,val validation: Int? = null, val bank: OfflineBankProgress? = null, val bankDownloading: Boolean = false, val legacyStorage: Boolean = false, val bankAllowed: Boolean = false,
    val selection: OfflineSelection = OfflineSelection(), val includeImages: Boolean = true,
    val filterOptions: OfflineFilterOptions? = null, val filterError: Throwable? = null,
    val syncingIds: Set<String> = emptySet(), val syncErrors: Map<String, Throwable> = emptyMap()) {
    val current: OfflineMaterial? get() = materials.firstOrNull {it.pack.id==selected}
    val canDelete: Boolean get() = !busy && (!storageFailed || (pendingStorage && error is OfflineCacheCapacityException))
}
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class OfflineStudyViewModel(private val service: LearnerOffline,private val store: LearnerStorage,
    private val userId: Long,private val localOnly: Boolean,private val canSync: Boolean,
    private val maxQuestions: Int,initialIds: String) : ViewModel() {
    private val _state=MutableStateFlow(OfflineStudyState(ids=initialIds))
    val state=_state.asStateFlow()
    private var bankJob: Job? = null
    private val storageMutex = Mutex()
    init { read() }

    private suspend fun loadStorage() {
        val loaded = withContext(Dispatchers.IO) {
            store.readOffline(userId,localOnly).getOrThrow() to store.readOfflineBankProgress(userId,localOnly).getOrThrow()
        }
        _state.update { it.copy(materials=loaded.first, bank=loaded.second, selected=null, storageFailed=false, error=null,
            legacyStorage=loaded.first.any {row -> row.summary==null}) }
    }
    fun read() {
        if(_state.value.busy || _state.value.pendingStorage || _state.value.syncingIds.isNotEmpty()) return
        _state.update {it.copy(busy=true)}
        viewModelScope.launch {
            try {
                loadStorage()
                if(!localOnly) updateDownloadPermission()
            }
            catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {_state.update {it.copy(storageFailed=true,error=failure)}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    private suspend fun updateDownloadPermission() {
        try {
            val user=service.downloadPermission()
            check(user.id==userId)
            _state.update {it.copy(bankAllowed=user.has("tests.start") && user.has("tests.download_full_bank"),error=null)}
            if (_state.value.bankAllowed) {
                try { val options = service.filterOptions(); coroutineContext.ensureActive()
                    _state.update { it.copy(filterOptions = options, filterError = null) }
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { _state.update { it.copy(filterError = failure) } }
            }
        } catch(cancel: CancellationException) {throw cancel}
        catch(error: Exception) {_state.update {it.copy(bankAllowed=false,error=error)}}
    }
    fun refreshDownloadPermission() {
        if(localOnly || _state.value.busy || _state.value.pendingStorage) return
        _state.update {it.copy(busy=true)}
        viewModelScope.launch {
            try {updateDownloadPermission()}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun ids(value: String) {if(!_state.value.busy) _state.update {it.copy(ids=value,validation=null)}}
    fun includeImages(value: Boolean) { if (!_state.value.busy && _state.value.bank?.complete != false) _state.update { it.copy(includeImages = value) } }
    fun selectBook(value: String?) { updateSelection { it.copy(book = value) } }
    fun toggleCategory(id: Long) { updateSelection { it.copy(categories = if (id in it.categories) it.categories - id else it.categories + id) } }
    fun toggleTag(tag: String) { updateSelection { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) } }
    private fun updateSelection(transform: (OfflineSelection) -> OfflineSelection) {
        val s = _state.value
        if (!s.bankAllowed || s.busy || s.bank?.complete == false || s.filterOptions == null || s.filterError != null) return
        _state.update { it.copy(selection = transform(it.selection)) }
    }
    fun open(id: String) {
        if(_state.value.busy || _state.value.pendingStorage || _state.value.materials.none {it.pack.id==id}) return
        _state.update {it.copy(busy=true,error=null,message=null)}
        viewModelScope.launch {
            try {
                storageMutex.withLock {
                    val row=withContext(Dispatchers.IO) {store.readOfflinePack(userId,localOnly,id).getOrThrow()}
                    _state.update {it.copy(materials=it.materials.map {old -> if(old.pack.id==id) row else if(it.legacyStorage) old else old.summaryOnly()},selected=id)}
                }
            } catch(cancel: CancellationException) {throw cancel}
            catch(error: Exception) {_state.update {it.copy(error=error)}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun back() {
        if(!_state.value.busy) _state.update {it.copy(selected=null,
            materials=if(it.pendingStorage || it.legacyStorage) it.materials else it.materials.map {row -> row.summaryOnly()})}
        // Full payloads are released after every successful write; legacy rows stay until migrated.
    }
    private suspend fun persist(rows: List<OfflineMaterial>, downloading: Boolean = false,
        progress: OfflineBankProgress? = _state.value.bank, keepId: String? = _state.value.selected,
        transform: (suspend (List<OfflineMaterial>) -> List<OfflineMaterial>)? = null): Boolean = storageMutex.withLock {
        val currentRows = transform?.invoke(_state.value.materials) ?: rows
        val result=withContext(Dispatchers.IO) {store.writeOffline(userId,localOnly,currentRows,progress)}
        if (downloading && result.exceptionOrNull() is OfflineCacheCapacityException) {
            _state.update {it.copy(validation=R.string.offline_cache_full,error=null)}
            return@withLock false
        }
        val retainedId = if (downloading) keepId else _state.value.selected
        _state.update {it.copy(materials=if(result.isSuccess) currentRows.map {row -> if(row.pack.id==retainedId) row else row.summaryOnly()} else currentRows,
            bank=progress,legacyStorage=if(result.isSuccess) false else it.legacyStorage,storageFailed=result.isFailure,pendingStorage=result.isFailure,error=result.exceptionOrNull(),
            validation=R.string.offline_cache_full.takeIf {result.exceptionOrNull() is OfflineCacheCapacityException})}
        result.isSuccess
    }
    fun retryStorage() {
        if(_state.value.busy || (_state.value.syncingIds.isNotEmpty() && !_state.value.pendingStorage)) return
        _state.update {it.copy(busy=true)}
        viewModelScope.launch {
            try {
                if (!localOnly) withContext(Dispatchers.IO) {store.retryBinding(userId).getOrThrow()}
                if(_state.value.pendingStorage) persist(emptyList(), transform={ it }) else loadStorage()
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {_state.update {it.copy(storageFailed=true,error=failure)}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    private fun validPack(pack: OfflinePack, ids: List<Long>) {
        check(pack.questions.map {it.id}==ids && pack.questions.all {q -> !q.answersHidden && q.correctAnswer?.let {it in 1..q.choices.size}==true})
    }
    fun download() {
        val s=_state.value
        if(localOnly || !canSync || s.busy || s.storageFailed || s.syncingIds.isNotEmpty()) return
        val ids=s.ids.trim().split(Regex("[,،\\s]+")).map { raw ->
            raw.map { Character.digit(it,10).takeIf { digit -> digit>=0 }?.digitToChar() ?: it }.joinToString("").toLongOrNull()
        }
        if(ids.isEmpty() || ids.any {it==null || it<=0} || ids.distinct().size!=ids.size || ids.size>maxQuestions.coerceIn(1,200)) {
            _state.update {it.copy(validation=R.string.offline_ids_invalid)};return
        }
        val includeImages = s.bank?.takeIf { !it.complete }?.includeImages ?: s.includeImages
        _state.update {it.copy(busy=true,error=null,validation=null)}
        viewModelScope.launch {
            try {
                val pack=service.download(ids.filterNotNull(), includeImages=includeImages);coroutineContext.ensureActive()
                validPack(pack,ids.filterNotNull())
                if (pack.imagesIncluded != includeImages) {
                    _state.update { it.copy(validation = R.string.offline_download_options_unsupported) }
                    return@launch
                }
                if(persist(_state.value.materials+pack.newMaterial(),downloading=true,keepId=pack.id))
                    _state.update {it.copy(message=R.string.offline_downloaded,selected=pack.id)}
            } catch(cancel: CancellationException) {throw cancel}
            catch(error: Exception) {coroutineContext.ensureActive();_state.update {it.copy(error=error)}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun downloadBank() {
        val s=_state.value
        if(localOnly || !s.bankAllowed || s.busy || s.storageFailed || s.pendingStorage || s.syncingIds.isNotEmpty()) return
        _state.update {it.copy(busy=true,bankDownloading=true,error=null,validation=null,selected=null)}
        bankJob=viewModelScope.launch {
            try {
                // Reload committed checkpoints after pause/process recreation; no automatic resume.
                loadStorage()
                if(_state.value.bank?.complete!=false) {
                    if(!persist(_state.value.materials,progress=OfflineBankProgress(UUID.randomUUID().toString(), selection=s.selection, includeImages=s.includeImages))) return@launch
                }
                while(true) {
                    coroutineContext.ensureActive()
                    val progress=requireNotNull(_state.value.bank)
                    if(progress.pendingIds.isNotEmpty()) {
                        if(!downloadBankBatch(progress.pendingIds.take(maxQuestions.coerceIn(1,200)))) break
                    } else if(progress.catalogFinished) {
                        if(persist(_state.value.materials,progress=progress.copy(complete=true)))
                            _state.update {it.copy(message=R.string.offline_bank_completed)}
                        break
                    } else {
                        val page=service.catalog(progress.after,progress.upperBound,progress.selection)
                        if (page.selection != progress.selection) {
                            _state.update { it.copy(validation = R.string.offline_download_options_unsupported) }
                            break
                        }
                        check(page.total>=0 && page.excluded>=0)
                        check(page.upperBound>=progress.after && (progress.upperBound==null || page.upperBound==progress.upperBound))
                        check(page.questionIds.distinct().size==page.questionIds.size &&
                            page.questionIds.all {it>progress.after && it<=page.upperBound})
                        check(page.nextAfter==null || (page.nextAfter>progress.after && page.nextAfter<=page.upperBound))
                        val next=progress.copy(upperBound=page.upperBound,after=page.nextAfter ?: page.upperBound,
                            pendingIds=page.questionIds,total=if(progress.upperBound==null) page.total else progress.total,
                            excluded=progress.excluded+page.excluded,catalogFinished=page.nextAfter==null)
                        if(!persist(_state.value.materials,progress=next)) break
                    }
                }
            } catch(cancel: CancellationException) {throw cancel}
            catch(error: Exception) {
                coroutineContext.ensureActive()
                _state.update {it.copy(error=error,bankAllowed=it.bankAllowed && (error as? ApiException)?.statusCode !in setOf(401,403))}
            }
            finally {_state.update {it.copy(busy=false,bankDownloading=false)}}
        }
    }
    private suspend fun downloadBankBatch(ids: List<Long>): Boolean {
        check(_state.value.bankAllowed)
        val includeImages = requireNotNull(_state.value.bank).includeImages
        val pack = try { service.download(ids, fullBank=true, includeImages=includeImages) }
        catch(failure: ApiException) {
            // This POST only reads/snapshots content: splitting cannot create server-side study writes.
            if(failure.statusCode !in setOf(400,404,409)) throw failure
            if(ids.size>1) {
                val split=ids.size/2
                return downloadBankBatch(ids.take(split)) && downloadBankBatch(ids.drop(split))
            }
            val p=requireNotNull(_state.value.bank)
            check(p.pendingIds.take(ids.size)==ids)
            return persist(_state.value.materials,progress=p.copy(pendingIds=p.pendingIds.drop(ids.size),excluded=p.excluded+1))
        }
        coroutineContext.ensureActive()
        validPack(pack,ids)
        if (pack.imagesIncluded != includeImages) {
            _state.update { it.copy(validation = R.string.offline_download_options_unsupported) }
            return false
        }
        val p=requireNotNull(_state.value.bank)
        check(p.pendingIds.take(ids.size)==ids)
        return persist(_state.value.materials+pack.newMaterial().copy(sourceLabels=listOfNotNull(p.selection.book)),downloading=true,
            progress=p.copy(pendingIds=p.pendingIds.drop(ids.size),downloaded=p.downloaded+ids.size))
    }
    fun pauseBank() { if(_state.value.bankDownloading) bankJob?.cancel() }
    fun resetBank() {
        val s = _state.value
        if (s.busy || s.pendingStorage || s.storageFailed || s.syncingIds.isNotEmpty()) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { persist(emptyList(), progress = null, transform={ it }) }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun refreshPack() {
        val s = _state.value
        val row = s.current ?: return
        if (localOnly || !canSync || s.busy || s.storageFailed || s.pendingStorage || s.syncingIds.isNotEmpty() || row.summary != null) return
        _state.update { it.copy(busy = true, error = null, validation = null) }
        viewModelScope.launch {
            try {
                val ids = row.pack.questions.map { it.id }
                val fresh = service.download(ids, fullBank = s.bankAllowed, includeImages = row.pack.imagesIncluded)
                coroutineContext.ensureActive()
                validPack(fresh, ids)
                if (fresh.imagesIncluded != row.pack.imagesIncluded) {
                    _state.update { it.copy(validation = R.string.offline_download_options_unsupported) }
                    return@launch
                }
                val changed = if (row.pack.contentRevision != null && fresh.contentRevision != null)
                    row.pack.contentRevision != fresh.contentRevision || row.pack.images != fresh.images
                else row.pack.questions != fresh.questions || row.pack.images != fresh.images
                if (!changed) _state.update { it.copy(message = R.string.offline_up_to_date) }
                else if (persist(_state.value.materials + fresh.newMaterial().copy(displayTitle = row.displayTitle, sourceLabels = row.sourceLabels), downloading = true))
                    _state.update { it.copy(message = R.string.offline_updated_copy) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(error = failure) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun review() { val row = _state.value.current ?: return; if (row.frozenTime != null) go(-row.index) }
    private fun replace(row: OfflineMaterial, preserveResult: Boolean = true) {
        _state.update {it.copy(busy=true,error=null)}
        viewModelScope.launch {
            try {persist(emptyList(), transform={ current -> current.map { old ->
                if(old.pack.id==row.pack.id) row.copy(result=if(preserveResult) old.result ?: row.result else row.result) else old
            } })}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun toggleFlag(index: Int) {
        val s = _state.value
        val row = s.current ?: return
        if (s.busy || s.storageFailed || s.pendingStorage || row.completionId == null || row.frozenTime != null || index !in row.pack.questions.indices) return
        replace(row.copy(flaggedIndices = if (index in row.flaggedIndices) row.flaggedIndices - index else row.flaggedIndices + index))
    }

    fun rename(title: String): Boolean {
        val s = _state.value
        val row = s.current ?: return false
        val normalized = title.trim()
        if (s.busy || s.storageFailed || s.pendingStorage || row.summary != null || row.pack.id in s.syncingIds || normalized.length !in 1..120) return false
        replace(row.copy(displayTitle = normalized))
        return true
    }

    fun start() {
        val s=_state.value;val row=s.current ?: return
        if(s.busy || s.storageFailed || row.summary!=null || row.completionId!=null) return
        replace(row.copy(completionId=UUID.randomUUID().toString(),startedAtMillis=System.currentTimeMillis(),lastStudiedAt=Instant.now().toString()))
    }
    fun answer(choice: Int,confidence: Int) {
        val s=_state.value;val row=s.current ?: return
        val q=row.pack.questions.getOrNull(row.index) ?: return
        if(s.busy || s.storageFailed || row.completionId==null || row.frozenTime!=null || row.answers.any {it.questionId==q.id} || choice !in 1..q.choices.size || confidence !in 1..3) return
        val answeredAt = row.pack.issuedAt?.let { Instant.ofEpochMilli(System.currentTimeMillis()+row.clockOffsetMillis).toString() }
        replace(row.copy(answers=row.answers+OfflineAnswer(q.id,choice,confidence,answeredAt),lastStudiedAt=Instant.now().toString()))
    }
    fun go(offset: Int) {
        val s=_state.value;val row=s.current ?: return
        val next=row.index+offset
        if(s.busy || s.storageFailed || next !in row.pack.questions.indices) return
        replace(row.copy(index=next,lastStudiedAt=Instant.now().toString()))
    }
    fun finish() {
        val s=_state.value;val row=s.current ?: return
        if(s.busy || s.storageFailed || row.completionId==null || row.frozenTime!=null || row.answers.size!=row.pack.questions.size) return
        val elapsed=((System.currentTimeMillis()-row.startedAtMillis).coerceAtLeast(0)/1000).coerceAtMost(7*24*60*60).toInt()
        val completedAt = row.pack.issuedAt?.let {
            Instant.ofEpochMilli(System.currentTimeMillis() + row.clockOffsetMillis).toString()
        }
        replace(row.copy(frozenTime=elapsed,completedAt=completedAt))
    }
    fun sync() {
        val s=_state.value;val row=s.current ?: return
        if(localOnly || !canSync || s.busy || s.storageFailed || s.pendingStorage || row.frozenTime==null || row.result!=null || row.pack.id in s.syncingIds) return
        _state.update {it.copy(syncingIds=it.syncingIds + row.pack.id, syncErrors=it.syncErrors - row.pack.id)}
        viewModelScope.launch {
            try {
                val result=service.upload(row);coroutineContext.ensureActive()
                if (persist(emptyList(), transform={ current ->
                    val active = requireNotNull(current.firstOrNull { it.pack.id == row.pack.id })
                    val latest = if (active.summary == null) active else withContext(Dispatchers.IO) {
                        store.readOfflinePack(userId,localOnly,row.pack.id).getOrThrow()
                    }
                    current.map { old -> if(old.pack.id==row.pack.id) latest.copy(result=result) else old }
                })) _state.update {it.copy(message=if(it.selected==null || it.selected==row.pack.id) R.string.offline_synced else it.message)}
            } catch(cancel: CancellationException) {throw cancel}
            catch(error: Exception) {coroutineContext.ensureActive();_state.update {it.copy(syncErrors=it.syncErrors + (row.pack.id to error))}}
            finally {_state.update {it.copy(syncingIds=it.syncingIds - row.pack.id)}}
        }
    }
    fun restart() {
        val s=_state.value;val row=s.current ?: return
        if(s.busy || s.storageFailed || row.result==null || row.pack.id in s.syncingIds) return
        replace(OfflineMaterial(row.pack,clockOffsetMillis=row.clockOffsetMillis,displayTitle=row.displayTitle,lastStudiedAt=row.lastStudiedAt,sourceLabels=row.sourceLabels), preserveResult=false)
    }
    fun delete(id: String) {
        if(!_state.value.canDelete || id in _state.value.syncingIds) return
        _state.update {it.copy(busy=true)}
        viewModelScope.launch {
            try {
                if(persist(emptyList(),keepId=null,transform={ current -> current.filterNot {it.pack.id==id} })) _state.update {it.copy(selected=null)}
            } finally {_state.update {it.copy(busy=false)}}
        }
    }
    class Factory(private val service: LearnerOffline,private val store: LearnerStorage,private val userId: Long,
        private val localOnly: Boolean,private val canSync: Boolean,private val max: Int,private val ids: String): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T=
            OfflineStudyViewModel(service,store,userId,localOnly,canSync,max,ids) as T
    }
}
