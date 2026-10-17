package com.mukhtabir.android.ui.demo

import android.os.SystemClock
import java.io.Serializable

internal enum class DemoMode { STUDY, EXAM, RECALL }
internal enum class DemoPage { HOME, SESSION, RESULTS, REVIEW }
internal enum class DemoDialog { FINISH, DISCARD }

/** Public sample state only; serializable for the activity's saved-state registry. */
internal data class DemoSession(
    val mode: DemoMode = DemoMode.STUDY,
    val page: DemoPage = DemoPage.HOME,
    val index: Int = 0,
    val answers: List<Int> = emptyList(),
    val recalled: List<String> = emptyList(),
    val revealed: Set<Int> = emptySet(),
    val flagged: Set<Int> = emptySet(),
    val deadline: Long = 0L,
    val expired: Boolean = false,
) : Serializable {
    val total get() = answers.size
    val answered get() = answers.count { it >= 0 }
    val finished get() = page == DemoPage.RESULTS || page == DemoPage.REVIEW
    fun remainingMillis(): Long = deadline - SystemClock.elapsedRealtime()
    fun deadlineReached(): Boolean = mode == DemoMode.EXAM && (remainingMillis() <= 0 || remainingMillis() > EXAM_MILLIS)
    fun finish(timedOut: Boolean = false) = copy(page = DemoPage.RESULTS, expired = timedOut)
    fun answer(choice: Int): DemoSession = when {
        page != DemoPage.SESSION -> this
        deadlineReached() -> finish(true)
        mode != DemoMode.EXAM && answers[index] >= 0 -> this
        mode == DemoMode.RECALL && index !in revealed -> this
        else -> copy(answers = answers.mapIndexed { i, value -> if (i == index) choice else value })
    }
    companion object {
        const val EXAM_MILLIS = 480_000L
        fun start(mode: DemoMode, count: Int) = DemoSession(mode = mode, page = DemoPage.SESSION,
            answers = List(count) { -1 }, recalled = List(count) { "" },
            deadline = SystemClock.elapsedRealtime() + EXAM_MILLIS)
    }
}
