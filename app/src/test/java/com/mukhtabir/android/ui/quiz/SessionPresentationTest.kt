package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.core.SessionDraftKind
import com.mukhtabir.android.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class SessionPresentationTest {
    private val page = SessionQuestionPageDto(0, 2, SessionQuestionDto(13, "Question", listOf("A", "B")))
    private fun ready() = QuizUiState(isLoading = false, downloaded = true, mode = TestMode.STUDY,
        session = ExamSessionDto("session-1", "study", listOf(13, 14)), questionPage = page)
    private val draft = SessionDraft("session-1", "study", 13, 0, SessionDraftKind.ANSWER, answer = 1, confidence = 3)

    @Test fun `pending storage and uncertain writes block navigation and pause`() {
        for (state in listOf(ready().copy(draftStorageFailed = true), ready().copy(recoveryRequired = true),
            ready().copy(pendingDraft = draft))) {
            val ui = state.toSessionPresentation()
            assertFalse(ui.canNavigate)
            assertFalse(ui.canRequestPause)
            assertFalse(ui.canPause)
        }
    }
    @Test fun `missing learning confidence and unrevealed recall block question navigation`() {
        assertFalse(ready().copy(selectedAnswer = 1, confidence = null).toSessionPresentation().canNavigate)
        assertFalse(ready().copy(mode = TestMode.RECALL, choicesRevealed = false).toSessionPresentation().canNavigate)
        assertFalse(ready().copy(mode = TestMode.RECALL, choicesRevealed = true, selectedAnswer = null).toSessionPresentation().canNavigate)
        assertTrue(ready().copy(mode = TestMode.RECALL, choicesRevealed = true, selectedAnswer = 1).toSessionPresentation().canNavigate)
    }
    @Test fun `queued changes allow pause confirmation but require sync before exiting`() {
        val ui = ready().copy(queuedAnswers = 2).toSessionPresentation()
        assertTrue(ui.canRequestPause)
        assertFalse(ui.canPause)
        assertTrue(ready().copy(queuedAnswers = 2, locallyFinished = true).toSessionPresentation().canPause)
    }
    @Test fun `only unfinished downloaded study can finish locally`() {
        assertTrue(ready().toSessionPresentation().canFinishLocally)
        assertFalse(ready().copy(mode = TestMode.EXAM).toSessionPresentation().canFinishLocally)
        assertFalse(ready().copy(mode = TestMode.RECALL).toSessionPresentation().canFinishLocally)
        assertFalse(ready().copy(downloaded = false).toSessionPresentation().canFinishLocally)
        assertFalse(ready().copy(locallyFinished = true).toSessionPresentation().canFinishLocally)
    }
    @Test fun `stale question indices flags and answers cannot escape current session bounds`() {
        val ui = ready().copy(questionPage = page.copy(index = 20), flaggedIndices = setOf(-1, 0, 9),
            answeredIndices = setOf(-1, 1, 9)).toSessionPresentation()
        assertEquals(1, ui.index)
        assertEquals(setOf(0), ui.flagged)
        assertEquals(setOf(1), ui.answered)
    }
    @Test fun `draft acknowledgement distinguishes same answer from changed confidence and question identity`() {
        val acknowledged = page.copy(savedAnswer = 1, savedConfidence = 3)
        assertTrue(draft.matches(acknowledged) && draft.acknowledged(acknowledged))
        assertFalse(draft.acknowledged(acknowledged.copy(savedConfidence = 2)))
        assertFalse(draft.matches(acknowledged.copy(question = acknowledged.question.copy(id = 99))))
        assertFalse(draft.baselineUnchanged(acknowledged))
        assertTrue(draft.baselineUnchanged(page))
    }
    @Test fun `recall acknowledgement requires revealed choices and the saved pre-answer`() {
        val recall = draft.copy(mode = "recall", kind = SessionDraftKind.REVEAL, preAnswer = "  Recalled  ")
        val hidden = page.copy(savedPreAnswer = "Recalled", question = page.question.copy(choicesHidden = true))
        assertFalse(recall.acknowledged(hidden))
        assertTrue(recall.acknowledged(hidden.copy(question = hidden.question.copy(choicesHidden = false))))
        assertFalse(recall.acknowledged(hidden.copy(savedPreAnswer = "Different")))
    }
}
