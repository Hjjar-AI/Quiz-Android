package com.mukhtabir.android.data.remote

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OfflinePack(val id: String,val token: String,val questions: List<QuestionDto>,val images: Map<String,String> = emptyMap(),
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("images_included") val imagesIncluded: Boolean = true,
    @SerialName("image_question_ids") val imageQuestionIds: List<Long> = emptyList(),
    @SerialName("content_revision") val contentRevision: String? = null)
@Serializable
data class OfflineAnswer(val questionId: Long,val answer: Int,val confidence: Int,val answeredAt: String? = null)
@Serializable
data class OfflineMaterial(val pack: OfflinePack,val completionId: String? = null,
    val answers: List<OfflineAnswer> = emptyList(),val index: Int = 0,val startedAtMillis: Long = 0,
    val frozenTime: Int? = null,val result: SessionResultsDto? = null,
    val summary: OfflinePackSummary? = null, val clockOffsetMillis: Long = 0,
    val completedAt: String? = null,
    val displayTitle: String? = null, val lastStudiedAt: String? = null, val sourceLabels: List<String> = emptyList(), val flaggedIndices: Set<Int> = emptySet())

fun OfflinePack.newMaterial(): OfflineMaterial {
    val serverTime = issuedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    return OfflineMaterial(this, clockOffsetMillis = serverTime?.minus(System.currentTimeMillis()) ?: 0)
}

@Serializable
data class OfflinePackSummary(val id: String, val title: String, val questionCount: Int,
    val answeredCount: Int, val finished: Boolean, val synced: Boolean,
    val sources: List<String> = emptyList(), val lastStudiedAt: String? = null)

fun OfflineMaterial.summaryOnly(): OfflineMaterial {
    if (summary != null) return this
    return OfflineMaterial(OfflinePack(pack.id, "", emptyList()), summary = OfflinePackSummary(
        pack.id, displayTitle?.takeIf(String::isNotBlank)
            ?: sourceLabels.joinToString(" · ").take(160).takeIf(String::isNotBlank)
            ?: pack.questions.mapNotNull { it.sourceDocument?.takeIf(String::isNotBlank) }.distinct().joinToString(" · ").take(160).takeIf(String::isNotBlank)
            ?: pack.questions.mapNotNull { it.categoryName?.takeIf(String::isNotBlank) }.distinct().joinToString(" · ").take(160),
        pack.questions.size, answers.size, frozenTime != null, result != null,
        sources = (sourceLabels + pack.questions.flatMap { listOfNotNull(it.sourceDocument?.takeIf(String::isNotBlank), it.source?.takeIf(String::isNotBlank), it.categoryName?.takeIf(String::isNotBlank)) }).distinct().take(5),
        lastStudiedAt = lastStudiedAt))
}

@Serializable
data class OfflineBankProgress(val id: String, val upperBound: Long? = null, val after: Long = 0,
    val pendingIds: List<Long> = emptyList(), val total: Int = 0, val downloaded: Int = 0,
    val excluded: Int = 0, val catalogFinished: Boolean = false, val complete: Boolean = false,
    val selection: OfflineSelection = OfflineSelection(), val includeImages: Boolean = true)

@Serializable
data class OfflineSelection(val book: String? = null, val categories: List<Long> = emptyList(), val tags: List<String> = emptyList())

data class OfflineFilterOptions(val books: List<SourceBookDto>, val categories: List<CategoryDto>, val tags: List<TagDto>)

@Serializable
data class OfflineCatalogPage(@SerialName("question_ids") val questionIds: List<Long>,
    @SerialName("upper_bound") val upperBound: Long,
    @SerialName("next_after") val nextAfter: Long? = null, val total: Int, val excluded: Int,
    val selection: OfflineSelection = OfflineSelection())
