package com.mukhtabir.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ActiveAccountDto
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ActiveAccountRow(val id: Long,val account: ActiveAccountDto)
internal data class ActiveAccountsState(val items: List<ActiveAccountRow> = emptyList(),val loading: Boolean = false,
    val loaded: Boolean = false,val error: Throwable? = null,val refreshedAt: Long? = null)

internal class ActiveAccountsViewModel(private val service: AdminService,private val canView: Boolean): ViewModel() {
    private val _state=MutableStateFlow(ActiveAccountsState())
    val state=_state.asStateFlow()
    init { refresh() }
    fun refresh() {
        if(!canView || _state.value.loading) return
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                val result=service.activeAccounts()
                coroutineContext.ensureActive()
                _state.update { it.copy(items=result.users.map { (id,account) -> ActiveAccountRow(id,account) }.sortedBy { row -> row.id },
                    loading=false,loaded=true,refreshedAt=System.currentTimeMillis()) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) { coroutineContext.ensureActive();_state.update { it.copy(loading=false,error=failure) } }
        }
    }
    class Factory(private val service: AdminService,private val canView: Boolean): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T = ActiveAccountsViewModel(service,canView) as T
    }
}

@Composable
fun ActiveAccountsRoute(service: AdminService,userId: Long,canView: Boolean,canManageUsers: Boolean,
    onBack: () -> Unit,onOpenUser: (Long) -> Unit) {
    val vm: ActiveAccountsViewModel=viewModel(key="active-accounts-$userId-$canView",factory=ActiveAccountsViewModel.Factory(service,canView))
    val s by vm.state.collectAsLifecycleState()
    RefreshOnResume(vm::refresh)
    if(!canView) { Text(stringResource(R.string.error_forbidden));return }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.active_accounts_title))
            AppTextButton(onBack){Text(stringResource(R.string.cancel))}
            Text(stringResource(R.string.active_accounts_window))
            AppOutlinedButton(vm::refresh,enabled=!s.loading){Text(stringResource(R.string.refresh))}
            if(s.loaded) Text(stringResource(R.string.active_accounts_count, com.mukhtabir.android.util.formatNumber(s.items.size)))
            s.refreshedAt?.let { Text(stringResource(R.string.active_accounts_refreshed,DateFormat.getDateTimeInstance().format(Date(it)))) }
        }
        if(s.loading) item { AppLoadingIndicator() }
        s.error?.let { failure -> item {
            ErrorState(throwable=failure,onRetry=if(!s.loading) vm::refresh else null)
            if(s.loaded) Text(stringResource(R.string.active_accounts_stale))
        } }
        if(s.loaded && !s.loading && s.error==null && s.items.isEmpty()) item { EmptyState(stringResource(R.string.active_accounts_empty)) }
        items(s.items,key={it.id}) { row ->
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(row.account.name,style=MaterialTheme.typography.titleMedium)
                    AppMetadataRow(listOfNotNull(
                        stringResource(R.string.account_identity,row.id,com.mukhtabir.android.util.roleLabel(row.account.role)),
                        row.account.ip?.let { stringResource(R.string.active_accounts_ip,it) },
                        row.account.lastSeen?.let { stringResource(R.string.active_accounts_last_seen,it) }))
                    if(canManageUsers) AppOutlinedButton({onOpenUser(row.id)}){Text(stringResource(R.string.account_tools_title))}
                }
            }
        }
    }
}
