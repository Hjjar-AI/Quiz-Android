package com.mukhtabir.android.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.ErrorState

@Composable
internal fun QuestionTagFields(s: QuestionEditorState,vm: QuestionEditorViewModel,enabled: Boolean) {
    var clearing by rememberSaveable { mutableStateOf(false) }
    if(clearing) AlertDialog(onDismissRequest={clearing=false},
        title={Text(stringResource(R.string.editor_tags_clear))},
        text={Text(stringResource(R.string.editor_tags_clear_confirm))},
        confirmButton={TextButton({vm.clearTags();clearing=false},enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({clearing=false}){Text(stringResource(R.string.cancel))}})
    Text(stringResource(R.string.additional_tags),style=MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.editor_tags_exact_hint))
    s.tags.forEach { name ->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Text(name,Modifier.weight(1f))
            TextButton({vm.removeTag(name)},enabled=enabled) { Text(stringResource(R.string.editor_tag_remove,name)) }
        }
    }
    if(s.tags.isEmpty()) Text(stringResource(R.string.none))
    OutlinedButton({clearing=true},enabled=enabled && s.tags.isNotEmpty()){Text(stringResource(R.string.editor_tags_clear))}
    OutlinedTextField(s.tagInput,{vm.field("tagInput",it)},enabled=enabled,singleLine=true,
        label={Text(stringResource(R.string.editor_tag_name))},modifier=Modifier.fillMaxWidth())
    s.tagValidation?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error) }
    OutlinedButton({vm.addTag()},enabled=enabled){Text(stringResource(R.string.editor_tag_add))}
    OutlinedButton(vm::refreshTags,enabled=enabled && !s.tagsLoading) {
        Text(stringResource(if(s.tagsLoading) R.string.loading else R.string.editor_tags_refresh))
    }
    s.tagsError?.let { ErrorState(throwable=it,onRetry=if(enabled && !s.tagsLoading) vm::refreshTags else null) }
    val matches=s.tagSuggestions.filter { it !in s.tags && it.contains(s.tagInput.trim(),ignoreCase=true) }
    if(matches.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.editor_tags_suggestions,matches.take(20).size,matches.size))
        matches.take(20).forEach { name ->
            OutlinedButton({vm.addTag(name)},enabled=enabled && !s.tagsLoading && s.tagsError==null) { Text(name) }
        }
    }
}
