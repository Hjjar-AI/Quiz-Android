package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppSection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.QuestionLanguageSelector
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.QuestionImage
import java.util.Locale

@Composable
internal fun QuizQuestionContent(
    state: QuizUiState,
    imageRepository: ImageRepository,
    viewModel: QuizViewModel,
) {
    val nativeView = androidx.compose.ui.platform.LocalView.current

    val page = requireNotNull(state.questionPage)
    var explanationShown by remember(state.session?.sessionId, page.question.id) { mutableStateOf(false) }
    var readingLocale by rememberSaveable(state.session?.sessionId) { mutableStateOf<String?>(null) }
    val showExplanation = state.mode.isLearningMode() && (state.showFeedback || state.locallyFinished)
    val localized = page.question.localizedForDisplay(
        localeTag = readingLocale ?: Locale.getDefault().toLanguageTag(),
        feedbackTranslations = state.feedbackTranslations,
        feedbackExplanation = state.explanation,
        showExplanation = showExplanation,
    )
    QuestionLanguageSelector(page.question.translations, page.question.choices.size, readingLocale,
        { readingLocale = it }, hasCase = page.question.case?.stem?.isNotBlank() == true, caseTranslations = page.question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
        explanationAvailable = showExplanation && (!state.explanation.isNullOrBlank() || !page.question.explanation.isNullOrBlank()),
        explanationTranslated = !localized.explanation.isNullOrBlank(), action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val flagged = page.index in state.flaggedIndices
                val flagLabel = stringResource(if (flagged) R.string.flow_unflag else R.string.flow_flag)
                com.mukhtabir.android.ui.components.AppIconButton(viewModel::toggleFlag, enabled = !state.isLoading,
                    modifier = Modifier.semantics { selected = flagged; contentDescription = flagLabel }) {
                    Icon(if (flagged) Icons.Filled.Flag else Icons.Outlined.Flag, contentDescription = null)
                }
                QuestionBookmark(state, viewModel)
            }
        })
    if (state.flagError) {
        Text(stringResource(R.string.flow_flag_error), color = MaterialTheme.colorScheme.error)
        TextButton(viewModel::retryFlags) { Text(stringResource(R.string.retry)) }
    }
    ClinicalCaseContext(localized.case)
    com.mukhtabir.android.ui.components.QuestionTextCard(localized.text, Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.titleMedium)
    if (page.question.id in state.bookmarkErrors) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.quiz_bookmark_error), Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            TextButton({ viewModel.refreshQuestionBookmark(page.question.id, force = true) },
                enabled = page.question.id !in state.bookmarkBusyIds) { Text(stringResource(R.string.retry)) }
        }
    }
    QuestionImage(page.question.imageUrl, imageRepository,
        downloadedImage = viewModel.downloadedImage(page.question.imageUrl))

    if (state.mode == TestMode.RECALL && !state.choicesRevealed) {
        Text(stringResource(R.string.recall_instruction))
        OutlinedTextField(
            value = state.preAnswer,
            onValueChange = viewModel::updatePreAnswer,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recall_your_answer)) },
            minLines = 3,
            enabled = !state.isLoading,
        )
        Button(
            onClick = viewModel::revealRecallChoices,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.preAnswer.isNotBlank() && !state.isLoading,
        ) { Text(stringResource(R.string.reveal_choices)) }
    } else {
        localized.choices.forEachIndexed { index, choice ->
            val answer = index + 1
            SelectableButton(
                selected = state.selectedAnswer == answer,
                enabled = !state.isLoading && !(state.mode.isLearningMode() && state.showFeedback),
                onClick = {
                    if (viewModel.selectAnswer(answer)) {
                        if (viewModel.soundEnabled) nativeView.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        if (viewModel.hapticsEnabled) com.mukhtabir.android.ui.components.performAppHaptic(nativeView)
                    }
                },
                text = choice,
                choiceNumber = answer,
                neutralSelection = state.mode == TestMode.EXAM,
                answerCorrect = when {
                    !showExplanation -> null
                    page.question.correctAnswer == answer -> true
                    state.selectedAnswer == answer -> state.isCorrect
                    else -> null
                },
                markdown = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.mode.isLearningMode()) com.mukhtabir.android.ui.components.BilingualQuestionComparison(
        com.mukhtabir.android.data.remote.QuestionDto(id = page.question.id, question = page.question.text,
            choices = page.question.choices, translations = page.question.translations, case = page.question.case,
            explanation = state.explanation ?: page.question.explanation),
        choicesVisible = state.mode != TestMode.RECALL || state.choicesRevealed,
        explanationVisible = showExplanation, feedbackTranslations = state.feedbackTranslations)

    if (showExplanation) {
        localized.explanation?.takeIf { it.isNotBlank() }?.let {
            StudyExplanation(it, page.question.id, explanationShown) { explanationShown = true }
        }
    }

    if (state.mode.isLearningMode() && state.selectedAnswer != null) {
        androidx.compose.runtime.key(page.question.id) {
            AppSection(stringResource(R.string.confidence_prompt),
                initiallyExpanded = state.confidence == null, revealError = state.confidence == null,
                subtitle = state.confidence?.let { confidence -> stringResource(when (confidence) {
                    1 -> R.string.confidence_guessing
                    2 -> R.string.confidence_uncertain
                    else -> R.string.confidence_certain
                }) }) {
                QuizSelectionRow {
                    ConfidenceButton(1, R.string.confidence_guessing, state, viewModel)
                    ConfidenceButton(2, R.string.confidence_uncertain, state, viewModel)
                    ConfidenceButton(3, R.string.confidence_certain, state, viewModel)
                }
            }
        }
    }

    if (state.mode.isLearningMode() && state.isCorrect == false) {
        androidx.compose.runtime.key(page.question.id) {
            AppSection(stringResource(R.string.reflection_title),
                subtitle = stringResource(when (state.errorReason ?: "unknown") {
                    "misread" -> R.string.reflection_misread
                    "confused" -> R.string.reflection_confused
                    "guessed" -> R.string.reflection_guessed
                    else -> R.string.reflection_unknown
                })) {
                ReflectionButtons(viewModel, state.isLoading || state.errorReason != null, state.errorReason ?: "unknown")
            }
        }
    }

}

@Composable
private fun ConfidenceButton(
    value: Int,
    label: Int,
    state: QuizUiState,
    viewModel: QuizViewModel,
) {
    SelectableButton(
        selected = state.confidence == value,
        enabled = !state.isLoading,
        onClick = { viewModel.selectConfidence(value) },
        text = stringResource(label),
    )
}

@Composable
private fun ReflectionButtons(viewModel: QuizViewModel, disabled: Boolean, selectedReason: String) {
    val reasons = listOf(
        "unknown" to R.string.reflection_unknown,
        "misread" to R.string.reflection_misread,
        "confused" to R.string.reflection_confused,
        "guessed" to R.string.reflection_guessed,
    )
    reasons.forEach { (reason, label) ->
        SelectableButton(selected = selectedReason == reason, enabled = !disabled,
            onClick = { viewModel.recordReflection(reason) }, text = stringResource(label),
            modifier = Modifier.fillMaxWidth())
    }
}
