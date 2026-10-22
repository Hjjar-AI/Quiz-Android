package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ErrorState

@Composable
internal fun QuestionTagFields(s: QuestionEditorState,vm: QuestionEditorViewModel,enabled: Boolean) {
    var clearing by rememberSaveable { mutableStateOf(false) }
    if(clearing) AppConfirmationDialog(onDismissRequest={clearing=false},
        title={Text(stringResource(R.string.editor_tags_clear))},
        text={Text(stringResource(R.string.editor_tags_clear_confirm))},
        confirmButton={AppTextButton({vm.clearTags();clearing=false},enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({clearing=false}){Text(stringResource(R.string.cancel))}})
    AppSectionHeading(stringResource(R.string.additional_tags))
    Text(stringResource(R.string.editor_tags_exact_hint))
    s.tags.forEach { name ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(name,Modifier.weight(1f))
            AppTextButton({vm.removeTag(name)},enabled=enabled) { Text(stringResource(R.string.editor_tag_remove,name)) }
        }
    }
    if(s.tags.isEmpty()) Text(stringResource(R.string.none))
    AppOutlinedButton({clearing=true},enabled=enabled && s.tags.isNotEmpty()){Text(stringResource(R.string.editor_tags_clear))}
    AppTextField(s.tagInput,{vm.field("tagInput",it)},enabled=enabled,singleLine=true,
        label={Text(stringResource(R.string.editor_tag_name))},modifier=Modifier.fillMaxWidth())
    s.tagValidation?.let { AppStatus(stringResource(it)) }
    AppOutlinedButton({vm.addTag()},enabled=enabled){Text(stringResource(R.string.editor_tag_add))}
    AppOutlinedButton(vm::refreshTags,enabled=enabled && !s.tagsLoading) {
        Text(stringResource(if(s.tagsLoading) R.string.loading else R.string.editor_tags_refresh))
    }
    s.tagsError?.let { ErrorState(throwable=it,onRetry=if(enabled && !s.tagsLoading) vm::refreshTags else null) }
    val matches=s.tagSuggestions.filter { it !in s.tags && it.contains(s.tagInput.trim(),ignoreCase=true) }
    if(matches.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Text(stringResource(R.string.editor_tags_suggestions, com.mukhtabir.android.util.formatNumber(matches.take(20).size), com.mukhtabir.android.util.formatNumber(matches.size)))
        matches.take(20).forEach { name ->
            AppOutlinedButton({vm.addTag(name)},enabled=enabled && !s.tagsLoading && s.tagsError==null) { Text(name) }
        }
    }
}
