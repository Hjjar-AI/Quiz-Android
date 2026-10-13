package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.VerificationStatsDto
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.util.formatNumber
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class VerificationStatsState(
    val stats: VerificationStatsDto? = null,
    val loading: Boolean = false,
    val error: Throwable? = null,
)

private class VerificationStatsViewModel(
    private val service: AnalyticsService,
    private val canRead: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(VerificationStatsState())
    val state = _state.asStateFlow()

    fun refresh() {
        if (!canRead || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val stats = service.verificationStats()
                ensureActive()
                _state.update { it.copy(stats = stats, loading = false) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                ensureActive()
                _state.update { it.copy(loading = false, error = error) }
            }
        }
    }

    class Factory(private val service: AnalyticsService, private val canRead: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VerificationStatsViewModel(service, canRead) as T
    }
}

@Composable
fun VerificationStatsRoute(service: AnalyticsService, userId: Long, canRead: Boolean,onExport: (() -> Unit)? = null) {
    if (!canRead) {
        Text(stringResource(R.string.error_forbidden))
        return
    }
    val vm: VerificationStatsViewModel = viewModel(key = "verification-stats-$userId",
        factory = VerificationStatsViewModel.Factory(service, canRead))
    val state by vm.state.collectAsState()
    RefreshOnResume(vm::refresh)
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.verification_stats_title))
            onExport?.let {open->OutlinedButton(open,enabled=!state.loading){Text(stringResource(R.string.transfer_export))}}
            OutlinedButton(vm::refresh, enabled = !state.loading) { Text(stringResource(R.string.refresh)) }
            if (state.loading) AppLoadingIndicator()
            state.error?.let { ErrorState(throwable = it, onRetry = if (state.loading) null else vm::refresh,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        state.stats?.let { stats ->
            item {
                StatsCard {
                    Text(stringResource(R.string.verification_totals, formatNumber(stats.totalVerified), formatNumber(stats.totalUnverified)))
                    val ratio = (stats.verificationRate / 100).let { if (it.isFinite()) it.coerceIn(0.0, 1.0) else 0.0 }
                    Text(stringResource(R.string.verification_rate, percent(ratio)))
                    LinearProgressIndicator(ratio.toFloat(), Modifier.fillMaxWidth())
                }
            }
            item { StatsHeading(R.string.verification_by_user) }
            if (stats.byUser.isEmpty() && !state.loading && state.error==null) item { com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.verification_no_users)) }
            items(stats.byUser) { row ->
                Text(stringResource(R.string.verification_user_count,
                    row.verifiedBy?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown), formatNumber(row.count)))
            }
            item { StatsHeading(R.string.verification_by_category) }
            if (stats.byCategory.isEmpty() && !state.loading && state.error==null) item { com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.verification_no_categories)) }
            items(stats.byCategory) { row ->
                StatsCard {
                    Text(row.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.verification_category_counts, formatNumber(row.verified),
                        formatNumber((row.total - row.verified).coerceAtLeast(0)), formatNumber(row.total)))
                    val ratio = if (row.total > 0) (row.verified.toDouble() / row.total).coerceIn(0.0, 1.0) else 0.0
                    Text(stringResource(R.string.verification_rate, percent(ratio)))
                    LinearProgressIndicator(ratio.toFloat(), Modifier.fillMaxWidth())
                }
            }
            item { StatsHeading(R.string.verification_monthly) }
            if (stats.monthly.isEmpty() && !state.loading && state.error==null) item { com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.verification_no_months)) }
            val maximum = stats.monthly.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1L
            items(stats.monthly.asReversed()) { row ->
                StatsCard {
                    Text(stringResource(R.string.verification_user_count, row.month, formatNumber(row.count)))
                    LinearProgressIndicator((row.count.toDouble() / maximum).coerceIn(0.0, 1.0).toFloat(), Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun percent(value: Double): String = NumberFormat.getPercentInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
}.format(value)

@Composable
private fun StatsHeading(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) { content() }
    }
}
