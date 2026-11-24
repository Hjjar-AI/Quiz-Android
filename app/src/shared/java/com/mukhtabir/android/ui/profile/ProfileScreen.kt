package com.mukhtabir.android.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppDivider
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppListRow
import com.mukhtabir.android.ui.components.AppMetricRow
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLongDate
/**
 * Read-only profile screen (Phase 1, item 76).
 *
 * Renders every field on the extended [UserDto] that the server
 * already ships. The admin shortcut card only appears when the
 * caller holds at least one of the three admin capabilities the web
 * exposes on its profile page.
 *
 * Layout is a vertical scroll of cards. There is no edit affordance
 * — profile fields are managed by the system administrator on the
 * web side, matching the web's own read-only behaviour for
 * non-admin users.
 */
@Composable
fun ProfileScreen(
    user: UserDto,
    onChangePassword: (() -> Unit)?,
    onAbout: (() -> Unit)?,
    onSignOut: (() -> Unit)?,
    onAdminUsers: () -> Unit,
    onAdminSettings: () -> Unit,
    onAdminPermissions: () -> Unit,
    onRefresh: () -> Unit, refreshing: Boolean, refreshError: Throwable?,
) {
    val rank = AuthorRank.fromKey(user.authorRank)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppPageHeading(stringResource(R.string.profile_title), action = {
            AppIconButton(onRefresh, enabled = !refreshing) {
                Icon(Icons.Filled.Refresh, stringResource(R.string.profile_refresh))
            }
        })
        refreshError?.let {ErrorState(throwable=it)}

        // ── Identity card ──────────────────────────────────────────
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppLayoutTokens.cardInsets),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Box(Modifier.size(ProfileScreenGeometry.profileAvatarSize), contentAlignment = Alignment.Center) {
                        Text(
                            text = user.displayName.ifEmpty { "?" }.let { name ->
                                name.substring(0, name.offsetByCodePoints(0, 1)).uppercase()
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AuthorRankBadge(rank = rank)
                }
            }
        }

        AppSection(stringResource(R.string.profile_details)) {
            AppMetricRow(stringResource(R.string.profile_role), com.mukhtabir.android.util.roleLabel(user.role))
            AppMetricRow(stringResource(R.string.profile_questions_count), com.mukhtabir.android.util.formatNumber(user.questionsCount))
            AppMetricRow(stringResource(R.string.profile_trust_score), "${com.mukhtabir.android.util.formatDecimal(user.trustScore)}%")
            AppMetricRow(stringResource(R.string.profile_streak), stringResource(R.string.profile_streak_value, com.mukhtabir.android.util.formatNumber(user.currentStreak), com.mukhtabir.android.util.formatNumber(user.longestStreak)))
            user.createdAt?.let { AppMetricRow(stringResource(R.string.profile_joined), formatLongDate(it)) }
            user.lastLogin?.let { AppMetricRow(stringResource(R.string.profile_last_login), formatLongDate(it)) }
        }

        // ── Subscription card (item 78) ────────────────────────────
        if (!user.bypassesExpiry && user.expiresAt != null) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AppLayoutTokens.cardInsets),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
                ) {
                    Text(
                        text = stringResource(R.string.profile_subscription),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val days = user.remainingDays()
                    AppMetricRow(
                        label = stringResource(R.string.profile_expires_on),
                        value = formatLongDate(user.expiresAt),
                    )
                    if (days != null) {
                        val daysText = when {
                            days > 0 -> stringResource(R.string.profile_days_left, com.mukhtabir.android.util.formatNumber(days))
                            else -> stringResource(R.string.profile_expired)
                        }
                        AppMetricRow(
                            label = stringResource(R.string.profile_status),
                            value = daysText,
                        )
                    }
                }
            }
        }

        // ── Admin shortcuts (only if any capability is held) ───────
        val adminItems = buildList {
            if (user.has("admin.users")) add(
                stringResource(R.string.nav_admin_users) to onAdminUsers,
            )
            if (user.has("admin.settings")) add(
                stringResource(R.string.nav_admin_settings) to onAdminSettings,
            )
            if (user.has("admin.permissions")) add(
                stringResource(R.string.nav_admin_permissions) to onAdminPermissions,
            )
        }
        if (adminItems.isNotEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = AppLayoutTokens.cardVerticalPadding)) {
                    Text(
                        text = stringResource(R.string.profile_admin_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(AppLayoutTokens.cardInsets),
                    )
                    adminItems.forEach { (label, action) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = action)
                                .heightIn(min = AppLayoutTokens.touchTarget)
                                .padding(horizontal = AppLayoutTokens.cardPadding, vertical = AppLayoutTokens.denseCardVerticalPadding),
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        val actions = listOfNotNull(
            onChangePassword?.let { Triple(R.string.nav_change_password, it, false) },
            onAbout?.let { Triple(R.string.nav_about, it, false) },
            onSignOut?.let { Triple(R.string.sign_out, it, true) },
        )
        if (actions.isNotEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    actions.forEachIndexed { index, (label, action, destructive) ->
                        if (index > 0) AppDivider()
                        ProfileActionRow(stringResource(label), action, destructive)
                    }
                }
            }
        }

    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    AppListRow(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private object ProfileScreenGeometry {
    val profileAvatarSize = 64.dp
}
