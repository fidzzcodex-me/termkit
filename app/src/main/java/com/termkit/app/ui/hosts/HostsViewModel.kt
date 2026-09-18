package com.termkit.app.ui.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.Host
import com.termkit.app.data.repo.HostRepository
import com.termkit.app.data.ssh.SshSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HostsUiState(
    val hosts: List<Host> = emptyList(),
    val query: String = "",
    val activeHostId: String? = null,
    val pendingDeleteHost: Host? = null,
    val pendingSwitchHost: Host? = null,
    val message: String? = null,
    val navigateToTerminal: Boolean = false
)

class HostsViewModel(
    private val repository: HostRepository,
    private val sessionManager: SshSessionManager
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val pendingDelete = MutableStateFlow<Host?>(null)
    private val pendingSwitch = MutableStateFlow<Host?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val navigateToTerminal = MutableStateFlow(false)

    val uiState: StateFlow<HostsUiState> = combine(
        repository.hosts,
        query,
        sessionManager.status,
        pendingDelete,
        pendingSwitch,
        message,
        navigateToTerminal
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val hosts = values[0] as List<Host>
        val q = values[1] as String
        val status = values[2] as ConnectionStatus
        val filtered = hosts
            .filter { q.isBlank() || it.name.contains(q, true) || it.hostname.contains(q, true) }
            .sortedWith(compareBy({ !it.isFavorite }, { it.name.lowercase() }))
        HostsUiState(
            hosts = filtered,
            query = q,
            activeHostId = (status as? ConnectionStatus.Connected)?.host?.id,
            pendingDeleteHost = values[3] as Host?,
            pendingSwitchHost = values[4] as Host?,
            message = values[5] as String?,
            navigateToTerminal = values[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HostsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFavoriteToggle(host: Host) {
        viewModelScope.launch { repository.toggleFavorite(host.id) }
    }

    fun requestDelete(host: Host) {
        pendingDelete.value = host
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val host = pendingDelete.value ?: return
        viewModelScope.launch {
            if (sessionManager.activeHost?.id == host.id) {
                sessionManager.disconnect()
            }
            repository.delete(host.id)
            pendingDelete.value = null
        }
    }

    fun requestConnect(host: Host) {
        val current = sessionManager.activeHost
        if (current != null && current.id != host.id) {
            pendingSwitch.value = host
        } else {
            connect(host)
        }
    }

    fun cancelSwitch() {
        pendingSwitch.value = null
    }

    fun confirmSwitch() {
        val host = pendingSwitch.value ?: return
        pendingSwitch.value = null
        connect(host)
    }

    private fun connect(host: Host) {
        viewModelScope.launch {
            sessionManager.connect(host)
            when (val result = sessionManager.status.value) {
                is ConnectionStatus.Connected -> {
                    repository.markConnected(host.id)
                    navigateToTerminal.value = true
                }
                is ConnectionStatus.Error -> message.value = result.message
                else -> Unit
            }
        }
    }

    fun consumeNavigation() {
        navigateToTerminal.value = false
    }

    fun consumeMessage() {
        message.value = null
    }
}
