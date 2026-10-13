package com.mukhtabir.android.core

import com.mukhtabir.android.data.remote.TestMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot channel for "start a quiz with these exact questions".
 *
 * The Home screen's Study Now card, the Mistakes list, and the
 * Fragile list all need to start a session with a caller-supplied
 * question-id list. The quiz setup screen exists for the
 * user-driven flow where the source and refinements are chosen
 * interactively; a "start immediately with these ids" tap does not
 * fit that flow.
 *
 * Rather than encode a variable-length id list into a navigation
 * route — Compose Navigation cannot carry a `List<Long>` without a
 * custom `NavType` — the caller drops a [QuizPreset] into this
 * holder and then navigates to the quiz route. The quiz screen
 * consumes the preset on entry, starts the session, and clears the
 * slot. If the user arrives at the quiz screen without a preset
 * (the normal tab tap), the holder is empty and the setup screen
 * renders as before.
 *
 * Threading
 * ---------
 * All access is from the main thread — every caller and reader is a
 * Compose composable or a view-model coroutine started on the main
 * dispatcher. The holder does not add its own synchronization.
 *
 * Lifetime
 * --------
 * A single instance lives on [AppContainer] and therefore survives
 * for the whole process. The app shell calls [clear] whenever the
 * authenticated user becomes null, so a logout or expired session
 * cannot leak a pending question selection into the next login.
 */
class QuizPresetHolder {
    private val _preset = MutableStateFlow<QuizPreset?>(null)
    val preset = _preset.asStateFlow()

    private val _setupMode = MutableStateFlow<TestMode?>(null)
    val setupMode = _setupMode.asStateFlow()
    fun configure(mode: TestMode) { _preset.value = null; _setupMode.value = mode }
    fun consumeSetupMode(): TestMode? = _setupMode.value.also { _setupMode.value = null }

    fun set(value: QuizPreset) {
        _setupMode.value = null
        _preset.value = value
    }

    fun clear() {
        _preset.value = null
        _setupMode.value = null
    }

    /**
     * Read the pending preset and clear the slot in one call.
     *
     * Deliberately atomic with respect to the caller: a subsequent
     * `set()` from another screen cannot race with a read that
     * already claimed the value. The quiz screen calls this once on
     * entry and starts the session from the returned value — if the
     * user backgrounds and returns, the slot is already empty and
     * the setup screen renders.
     */
    fun consume(): QuizPreset? = _preset.value.also { _preset.value = null }
}

/**
 * A pending session to start.
 *
 * [mode] drives the mode-specific timer behaviour on the backend
 * (exam ends at zero; study/recall roll into overtime) — see
 * `apps/exams/services/exam_service.py`.
 *
 * [questionIds] is the exact ordered list the caller supplied; the
 * backend validates each id against the caller's visibility rules
 * and returns 400 if any id is unavailable.
 *
 * [sessionLabel] is the display label persisted to
 * `StudySession.tag` / `TestHistory.tag` and shown in the user's
 * history. Callers pick a localized label.
 */
data class QuizPreset(
    val mode: TestMode,
    val questionIds: List<Long>,
    val sessionLabel: String,
)
