package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

/** Shared notebook presentation only; each owner retains requests, paging and practice intent. */
@Composable
fun PagedLearningQuestions(title: String, emptyTitle: String, emptyMessage: String,
    questions: List<QuestionDto>, page: Int, totalPages: Int, loading: Boolean,
    error: String?, hasUnknownError: Boolean, canPractice: Boolean,
    imageRepository: ImageRepository, onPractice: () -> Unit, onRetry: () -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onOpenQuestion: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(title, action = {
            AppButton(onPractice, enabled = canPractice && questions.isNotEmpty() && !loading && error == null && !hasUnknownError) {
                Text(stringResource(R.string.practise_these_n, formatNumber(questions.size)))
            }
        })
        if (loading) AppLoadingIndicator()
        if (error != null || hasUnknownError) ErrorState(message = error, onRetry = if (loading) null else onRetry)
        if (questions.isEmpty() && !loading && error == null && !hasUnknownError) {
            EmptyState(emptyTitle, message = emptyMessage)
        } else if (questions.isNotEmpty()) LazyColumn(Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            items(questions, key = { it.id }) { question ->
                QuestionListCard(question, imageRepository, onOpen = { onOpenQuestion(question.id) })
            }
            if (totalPages > 1) item {
                AppPagination(page, totalPages, onPrevious, onNext,
                    previousEnabled = !loading, nextEnabled = !loading, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
