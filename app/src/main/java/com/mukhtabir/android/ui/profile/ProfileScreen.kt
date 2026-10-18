package com.mukhtabir.android.ui.profile

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppTextButton

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.util.formatLongDate
import com.mukhtabir.android.core.has
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
    onChangePassword: () -> Unit,
    onAbout: () -> Unit,
    onSignOut: () -> Unit,
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
        AppTextButton(onRefresh,enabled=!refreshing) { Text(stringResource(R.string.profile_refresh)) }
        refreshError?.let {com.mukhtabir.android.ui.components.ErrorState(throwable=it)}
        AppPageHeading(stringResource(R.string.profile_title))

        // ── Identity card ──────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppLayoutTokens.cardInsets),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        modifier = Modifier.padding(AppLayoutTokens.cardInsets),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
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
            StatRow(stringResource(R.string.profile_role), com.mukhtabir.android.util.roleLabel(user.role))
            StatRow(stringResource(R.string.profile_questions_count), com.mukhtabir.android.util.formatNumber(user.questionsCount))
            StatRow(stringResource(R.string.profile_trust_score), "${"%.1f".format(user.trustScore)}%")
            StatRow(stringResource(R.string.profile_streak), stringResource(R.string.profile_streak_value, user.currentStreak, user.longestStreak))
            user.createdAt?.let { StatRow(stringResource(R.string.profile_joined), formatLongDate(it)) }
            user.lastLogin?.let { StatRow(stringResource(R.string.profile_last_login), formatLongDate(it)) }
        }

        // ── Subscription card (item 78) ────────────────────────────
        if (!user.bypassesExpiry && user.expiresAt != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
                ) {
                    Text(
                        text = stringResource(R.string.profile_subscription),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val days = user.remainingDays()
                    StatRow(
                        label = stringResource(R.string.profile_expires_on),
                        value = formatLongDate(user.expiresAt),
                    )
                    if (days != null) {
                        val daysText = when {
                            days > 0 -> stringResource(R.string.profile_days_left, days)
                            else -> stringResource(R.string.profile_expired)
                        }
                        StatRow(
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
            Card(modifier = Modifier.fillMaxWidth()) {
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
                                .clickable(onClick = action)
                                .heightIn(min = com.mukhtabir.android.ui.components.AppLayoutTokens.touchTarget)
                                .padding(horizontal = com.mukhtabir.android.ui.components.AppLayoutTokens.cardPadding, vertical = AppLayoutTokens.denseCardVerticalPadding),
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // ── Actions ────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileActionRow(
                    label = stringResource(R.string.nav_change_password),
                    onClick = onChangePassword,
                )
                Divider()
                ProfileActionRow(
                    label = stringResource(R.string.nav_about),
                    onClick = onAbout,
                )
                Divider()
                ProfileActionRow(
                    label = stringResource(R.string.sign_out),
                    onClick = onSignOut,
                    destructive = true,
                )
            }
        }

    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    com.mukhtabir.android.ui.components.AppListRow(Modifier.clickable(onClick = onClick)) {
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
