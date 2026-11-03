package com.mukhtabir.android.ui.questions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ExactTagEditor

@Composable
internal fun QuestionTagFields(s: QuestionEditorState,vm: QuestionEditorViewModel,enabled: Boolean) {
    val matches = s.tagSuggestions.filter { it !in s.tags && it.contains(s.tagInput.trim(), ignoreCase = true) }
    AppSectionHeading(stringResource(R.string.additional_tags))
    ExactTagEditor(s.tags, s.tagInput, { vm.field("tagInput", it) }, vm::addTag, vm::removeTag, enabled,
        validation = s.tagValidation?.let { stringResource(it) }, suggestions = matches.take(20),
        suggestionsTitle = stringResource(R.string.editor_tags_suggestions,
            com.mukhtabir.android.util.formatNumber(matches.take(20).size), com.mukhtabir.android.util.formatNumber(matches.size)),
        suggestionsEnabled = enabled && !s.tagsLoading && s.tagsError == null,
        onClear = vm::clearTags, status = {
            AppOutlinedButton(vm::refreshTags, enabled = enabled && !s.tagsLoading) {
                Text(stringResource(if (s.tagsLoading) R.string.loading else R.string.editor_tags_refresh))
            }
            s.tagsError?.let { ErrorState(throwable = it, onRetry = if (enabled && !s.tagsLoading) vm::refreshTags else null) }
        })
}
