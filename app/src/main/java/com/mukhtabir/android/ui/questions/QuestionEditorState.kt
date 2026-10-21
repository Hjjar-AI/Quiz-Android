package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.QuestionWriteRequest
import java.util.UUID
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto
import com.mukhtabir.android.data.remote.KnowledgeAssociationDto

enum class QuestionEditorValidationError {
    INVALID_FORM, TOO_MANY_CHOICES, TEXT_TOO_LONG, CHOICE_TOO_LONG, DUPLICATE_CHOICES,
    INVALID_ASSOCIATION, INVALID_SOURCE_PAGE, SOURCE_TOO_LONG, INVALID_LOCALE, TRANSLATION_REQUIRED, TRANSLATION_CHOICES,
}

// Identity, rather than a positional answer number, follows a choice as it moves.
@kotlinx.serialization.Serializable
data class QuestionChoiceDraft(val id: String = UUID.randomUUID().toString(), val text: String = "")

@kotlinx.serialization.Serializable
data class QuestionEditorState(
    val revisedDate: String = "",
    val needsReview: Boolean = false,
    val operationId: String? = null,
    @kotlinx.serialization.Transient val receiptMissing: Boolean = false,
    val reviewReady: Boolean = false,
    @kotlinx.serialization.Transient val reviewRows: List<QuestionDto> = emptyList(),
    val text: String = "",
    val choices: List<QuestionChoiceDraft> = List(2) { QuestionChoiceDraft() },
    val correctChoiceId: String? = null,
    val explanation: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val tagValidation: Int? = null,
    @kotlinx.serialization.Transient val tagSuggestions: List<String> = emptyList(),
    val tagsLoading: Boolean = false,
    @kotlinx.serialization.Transient val tagsError: Throwable? = null,
    val source: String = "",
    val sourceDocument: String = "",
    val sourcePage: String = "",
    val caseKey: String = "",
    val caseStem: String = "",
    val caseStemLocked: Boolean = false,
    val caseOrder: String = "",
    val knowledgeObject: Long? = null,
    val knowledgeTitle: String = "",
    val knowledgeObjective: String = "",
    val lookupKind: String? = null,
    val lookupQuery: String = "",
    val lookupBusy: Boolean = false,
    @kotlinx.serialization.Transient val lookupError: Throwable? = null,
    @kotlinx.serialization.Transient val caseResults: List<ClinicalCaseContextDto> = emptyList(),
    @kotlinx.serialization.Transient val knowledgeResults: List<KnowledgeAssociationDto> = emptyList(),
    val lookupTotal: Int = 0,
    @kotlinx.serialization.Transient val knowledgeCreation: KnowledgeCreationState? = null,
    val translations: Map<String, QuestionTranslationDraft> = emptyMap(),
    val difficulty: String = "medium",
    val category: Long? = null,
    @kotlinx.serialization.Transient val categories: List<CategoryDto> = emptyList(),
    val version: Int? = null,
    val maxChoices: Int = 8,
    val accessDenied: Boolean = false,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    @kotlinx.serialization.Transient val error: Throwable? = null,
    @kotlinx.serialization.Transient val validationError: QuestionEditorValidationError? = null,
    @kotlinx.serialization.Transient val validationAttempt: Int = 0,
    val savedId: Long? = null,
    val dirty: Boolean = false,
    val conflict: Boolean = false,
    @kotlinx.serialization.Transient val latest: QuestionDto? = null,
)

@kotlinx.serialization.Serializable
internal data class StoredQuestionEditor(val state: QuestionEditorState, val baseline: QuestionWriteRequest?,
    val baselinePage: String, val baselineOrder: String, val posting: Boolean = false)
