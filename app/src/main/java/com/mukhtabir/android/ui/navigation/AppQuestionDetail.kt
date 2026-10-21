package com.mukhtabir.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.mukhtabir.android.core.AppContainer
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.questions.QuestionDetailRoute

/** Both layouts share permission checks, services and the same recovery-aware reader. */
@Composable
internal fun AppQuestionDetail(questionId: Long, user: UserDto, container: AppContainer,
    navController: NavHostController, bankRevision: Int,
    onBusyChanged: (Boolean) -> Unit, onPaneLockChanged: (Boolean) -> Unit = {},
    startQuestions: (TestMode, List<Long>, String) -> Unit,
    shareLink: (String, Long) -> Unit, onDuplicated: (Long) -> Unit, onDeleted: (Long) -> Unit) {
    QuestionDetailRoute(
        questionId = questionId, userId = user.id, bankRevision = bankRevision,
        questionService = container.questionService, bookmarkService = container.bookmarkService,
        store = container.sessionDraftStore, imageRepository = container.imageRepository,
        canStartSessions = user.has("tests.start"), canDuplicate = user.has("questions.duplicate"),
        canDeleteOwn = user.has("questions.delete_own"), canDeleteAny = user.has("questions.delete_any"),
        canEditAny = user.has("questions.edit_any"), canEditOwn = user.has("questions.edit_own"),
        canReviewQuestion = user.has("questions.verify") || user.has("questions.bulk_verify"),
        canUploadImageOwn = user.has("questions.upload_image"),
        onMutationBusyChanged = onBusyChanged, onPaneLockChanged = onPaneLockChanged,
        onShare = { shareLink("questions", it) }, onStart = startQuestions,
        onEdit = { navController.navigate("more/questions/$it/edit") },
        onManageImage = { navController.navigate("more/questions/$it/image") },
        onOpenKnowledge = { navController.navigateContent("more/knowledge/$it") },
        onOpenCase = { navController.navigateContent("more/cases/${android.net.Uri.encode(it)}") },
        onDuplicated = onDuplicated, onDeleted = onDeleted,
    )
}
