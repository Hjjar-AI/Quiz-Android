package com.mukhtabir.android.ui.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import java.io.Serializable
import java.time.LocalDate

internal data class DemoRecord(val session: DemoSession, val date: String) : Serializable

/** Only public fixtures and demo actions. Never contains a real account, URL, cookie or file store. */
internal data class DemoData(
    val session: DemoSession = DemoSession(),
    val bookmarks: Set<Int> = setOf(2, 6),
    val target: Int = 10,
    val nextId: Long = 3,
    val records: List<DemoRecord> = listOf(
        DemoRecord(DemoSession(id = 2, questionIds = listOf(2, 3, 4, 5), mode = DemoMode.EXAM,
            page = DemoPage.RESULTS, answers = listOf(1, 0, 3, -1), recalled = List(4) { "" }), LocalDate.now().minusDays(1).toString()),
        DemoRecord(DemoSession(id = 1, questionIds = listOf(1, 6, 7), page = DemoPage.RESULTS,
            answers = listOf(0, 1, 0), recalled = List(3) { "" }), LocalDate.now().minusDays(2).toString()),
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
    fun setTarget(value: Int) { if (value in 1..100) state = state.copy(target = value) }
    fun start(mode: DemoMode, selected: List<Int>) {
        // Starting another session must never silently overwrite paused work.
        if (state.session.page == DemoPage.SESSION) return
        val valid = selected.distinct().filter { it in ids }.take(8)
        if (valid.isEmpty()) return
        state = state.copy(session = DemoSession.start(mode, valid, state.nextId), nextId = state.nextId + 1)
    }
    fun updateSession(session: DemoSession) {
        if (session.id != state.session.id) return
        val firstFinish = session.finished && state.records.none { it.session.id == session.id }
        state = state.copy(session = session, records = if (firstFinish)
            (listOf(DemoRecord(session.copy(page = DemoPage.RESULTS), LocalDate.now().toString())) + state.records).take(20)
            else state.records)
    }
    fun review(record: DemoRecord): Boolean {
        if (state.session.page == DemoPage.SESSION) return false
        state = state.copy(session = record.session.copy(index = 0, page = DemoPage.REVIEW))
        return true
    }
    fun discard() { state = state.copy(session = DemoSession()) }
    val todayAnswered get() = state.records.filter { it.date == LocalDate.now().toString() }.sumOf { it.session.answered }
    val answered get() = state.records.sumOf { it.session.answered }
    val accuracy get() = if (answered == 0) 0 else 100 * state.records.sumOf { correct(it.session) } / answered

    companion object {
        val StateSaver = Saver<DemoRepository, DemoData>(save = { it.state }, restore = { DemoRepository(it) })
    }
}
