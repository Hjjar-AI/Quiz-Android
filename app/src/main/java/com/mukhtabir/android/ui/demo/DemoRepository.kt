package com.mukhtabir.android.ui.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import com.mukhtabir.android.ui.planner.parsePlannerDate
import java.io.Serializable
import java.time.LocalDate

internal data class DemoRecord(val session: DemoSession, val date: String, val synced: Boolean = true) : Serializable
internal enum class DemoPool { ALL, BOOKMARKS, MISTAKES, FRAGILE }

/** Only public fixtures and demo actions. Never contains a real account, URL, cookie or file store. */
internal data class DemoData(
    val session: DemoSession = DemoSession(),
    val bookmarks: Set<Int> = setOf(2, 6),
    val target: Int = 10,
    val planCategories: Set<Int> = emptySet(),
    val planStart: String = LocalDate.now().toString(),
    val planEnd: String = "",
    val connected: Boolean = true,
    val readFailed: Boolean = false,
    val downloadedIds: List<Int> = listOf(1, 2, 3, 4),
    val ratings: Map<Int, Int> = emptyMap(),
    val nextId: Long = 3,
    val records: List<DemoRecord> = listOf(
        DemoRecord(DemoSession(id = 2, questionIds = listOf(2, 3, 4, 5), mode = DemoMode.EXAM,
            page = DemoPage.RESULTS, answers = listOf(1, 0, 3, -1), recalled = List(4) { "" }), LocalDate.now().minusDays(1).toString()),
        DemoRecord(DemoSession(id = 1, questionIds = listOf(1, 6, 7), page = DemoPage.RESULTS,
            answers = listOf(0, 1, 0), recalled = List(3) { "" }, confidence = mapOf(0 to 3, 1 to 1, 2 to 2)), LocalDate.now().minusDays(2).toString()),
    ),
) : Serializable

/** Fake repository shared by all demo destinations; state uses the saved-state registry, never account storage or an API. */
internal class DemoRepository(initial: DemoData = DemoData()) {
    var state by mutableStateOf(initial)
        private set
    val ids get() = demoQuestions.indices.map { it + 1 }
    fun question(id: Int) = demoQuestions[id - 1]
    fun category(id: Int) = question(id).category
    fun correct(session: DemoSession) = session.questionIds.indices.count {
        session.answers[it] == question(session.questionIds[it]).correct
    }
    fun toggleBookmark(id: Int) {
        if (id !in ids) return
        state = state.copy(bookmarks = if (id in state.bookmarks) state.bookmarks - id else state.bookmarks + id)
    }
    fun setTarget(value: Int) { if (value in 1..1000) state = state.copy(target = value) }
    fun savePlan(value: Int, categories: Set<Int>, start: String, end: String): Boolean {
        val first = parsePlannerDate(start) ?: return false
        val last = if (end.isBlank()) null else (parsePlannerDate(end) ?: return false)
        if (value !in 1..1000 || categories.any { it !in 1..2 } || (last != null && last < first) || !state.connected) return false
        state = state.copy(target = value, planCategories = categories, planStart = start, planEnd = end)
        return true
    }
    val planActive get() = LocalDate.now().toString() >= state.planStart &&
        (state.planEnd.isBlank() || LocalDate.now().toString() <= state.planEnd)
    val planIds get() = ids.filter { state.planCategories.isEmpty() || category(it) in state.planCategories }
    val planTodayAnswered get() = if (!planActive) 0 else state.records.filter { it.synced && it.date == LocalDate.now().toString() }
        .sumOf { record -> record.session.questionIds.indices.count { record.session.questionIds[it] in planIds && record.session.answers[it] >= 0 } }
    val currentStreak: Int get() {
        val dates = state.records.filter { it.synced && it.session.answered > 0 }.map { it.date }.toSet()
        var day = LocalDate.now()
        if (day.toString() !in dates) day = day.minusDays(1)
        var count = 0
        while (day.toString() in dates) { count++; day = day.minusDays(1) }
        return count
    }
    fun setConnected(connected: Boolean) {
        if (state.session.page == DemoPage.SESSION && !state.session.downloaded) return
        state = state.copy(connected = connected)
    }
    fun refresh() { state = state.copy(readFailed = !state.connected) }
    fun rate(id: Int, value: Int) {
        if (id in ids && value in 1..5 && state.connected) state = state.copy(ratings = state.ratings + (id to value))
    }
    fun poolIds(pool: DemoPool): List<Int> {
        if (pool == DemoPool.ALL) return ids
        if (pool == DemoPool.BOOKMARKS) return ids.filter { it in state.bookmarks }
        val latest = state.records.filter { it.synced }.flatMap { record ->
            record.session.questionIds.mapIndexedNotNull { index, id ->
                if (record.session.answers[index] < 0) null else Triple(id,
                    record.session.answers[index] == question(id).correct, record.session.confidence[index])
            }
        }.distinctBy { it.first }
        return latest.filter { if (pool == DemoPool.MISTAKES) !it.second else it.second && it.third?.let { level -> level in 1..2 } == true }.map { it.first }
    }
    fun download(selected: List<Int>) {
        if (!state.connected || state.session.page == DemoPage.SESSION) return
        val valid = selected.distinct().filter { it in ids }
        if (valid.isNotEmpty()) state = state.copy(downloadedIds = valid)
    }
    val pendingRecords get() = state.records.filter { !it.synced }
    fun sync() {
        if (state.connected) state = state.copy(records = state.records.map { it.copy(synced = true) }.take(20))
    }
    fun start(mode: DemoMode, selected: List<Int>, downloaded: Boolean = false) {
        // Starting another session must never silently overwrite paused work.
        if (state.session.page == DemoPage.SESSION) return
        if (downloaded && pendingRecords.size >= 10) return
        if ((!downloaded && !state.connected) || (downloaded && mode != DemoMode.STUDY)) return
        val valid = selected.distinct().filter { it in ids && (!downloaded || it in state.downloadedIds) }.take(8)
        if (valid.isEmpty()) return
        state = state.copy(session = DemoSession.start(mode, valid, state.nextId).copy(downloaded = downloaded), nextId = state.nextId + 1)
    }
    fun updateSession(session: DemoSession) {
        if (session.id != state.session.id) return
        val firstFinish = session.finished && state.records.none { it.session.id == session.id }
        state = state.copy(session = session, records = if (firstFinish)
            (listOf(DemoRecord(session.copy(page = DemoPage.RESULTS), LocalDate.now().toString(), synced = !session.downloaded)) + state.records)
                .let { rows ->
                    val confirmed = rows.filter { it.synced }.take(20).map { it.session.id }.toSet()
                    rows.filter { !it.synced || it.session.id in confirmed }
                }
            else state.records)
    }
    fun review(record: DemoRecord): Boolean {
        if (state.session.page == DemoPage.SESSION) return false
        state = state.copy(session = record.session.copy(index = 0, page = DemoPage.REVIEW))
        return true
    }
    fun discard() { state = state.copy(session = DemoSession()) }
    val todayAnswered get() = state.records.filter { it.synced && it.date == LocalDate.now().toString() }.sumOf { it.session.answered }
    val answered get() = state.records.filter { it.synced }.sumOf { it.session.answered }
    val accuracy get() = if (answered == 0) 0 else 100 * state.records.filter { it.synced }.sumOf { correct(it.session) } / answered

    companion object {
        val StateSaver = Saver<DemoRepository, DemoData>(save = { it.state }, restore = { DemoRepository(it) })
    }
}
