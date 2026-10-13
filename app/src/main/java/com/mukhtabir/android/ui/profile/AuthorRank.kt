package com.mukhtabir.android.ui.profile

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R

/**
 * Author-rank display registry.
 *
 * Mirrors `frontend/src/utils/authorRank.js`. The server decides the
 * rank (see `AuthorReputationService.refresh_user` and the thresholds
 * on `apps/users/models.py::User.AUTHOR_RANK_THRESHOLDS`) and ships
 * both the raw key (`author_rank`) and a pre-localized Arabic label
 * (`author_rank_label_ar`).
 *
 * The registry here owns the icon and colour metadata the server
 * does not know about, plus a per-rank localized label resource so
 * an English build does not have to display Arabic text when the
 * caller's locale has no other source for it.
 */
enum class AuthorRank(
    val key: String,
    @StringRes val labelRes: Int,
) {
    ADMIN("admin", R.string.rank_admin),
    AUTHORITY("authority", R.string.rank_authority),
    EXPERT("expert", R.string.rank_expert),
    CONTRIBUTOR("contributor", R.string.rank_contributor),
    APPRENTICE("apprentice", R.string.rank_apprentice),
    NEWCOMER("newcomer", R.string.rank_newcomer),
    ;

    companion object {
        /**
         * Map a rank string from the API to an enum value.
         *
         * Returns null for an unknown or missing key. Callers that
         * want a fallback should render `authorRankLabelAr` (the
         * server's own Arabic label) rather than fabricating a
         * rank — the server label is the honest source when we do
         * not recognize the key.
         *
         * `AuthorRank.entries` is qualified explicitly. An
         * unqualified `entries` inside a companion object resolves
         * inconsistently across Kotlin compiler versions; the
         * qualified form is unambiguous and constant.
         */
        fun fromKey(raw: String?): AuthorRank? {
            if (raw.isNullOrBlank()) return null
            return AuthorRank.entries.firstOrNull { it.key == raw }
        }
    }
}

/**
 * Accent colour for the rank badge.
 *
 * Deliberately derived from the current [MaterialTheme] palette
 * rather than hardcoded hex — this keeps the badge coherent with
 * whichever of the eleven themes the user has active.
 */
@Composable
fun AuthorRank.tint(): Color = when (this) {
    AuthorRank.ADMIN -> MaterialTheme.colorScheme.onSurface
    AuthorRank.AUTHORITY -> MaterialTheme.colorScheme.primary
    AuthorRank.EXPERT -> MaterialTheme.colorScheme.tertiary
    AuthorRank.CONTRIBUTOR -> MaterialTheme.colorScheme.secondary
    AuthorRank.APPRENTICE -> MaterialTheme.colorScheme.onSurfaceVariant
    AuthorRank.NEWCOMER -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Localized label for the rank. */
@Composable
fun AuthorRank.label(): String = stringResource(labelRes)

/**
 * Small inline rank badge.
 *
 * Renders nothing when [rank] is null. The caller is responsible
 * for the surrounding layout (e.g. a `Row` alongside the author
 * name); this composable only produces the text in the rank's
 * accent colour.
 */
@Composable
fun AuthorRankBadge(
    rank: AuthorRank?,
    modifier: Modifier = Modifier,
) {
    if (rank == null) return
    Text(
        text = rank.label(),
        color = rank.tint(),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier,
    )
}
