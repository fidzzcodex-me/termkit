package com.termkit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termkit.app.data.model.AppSettings
import com.termkit.app.data.model.AppTheme
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.TerminalFontSize
import com.termkit.app.data.repo.HostRepository
import com.termkit.app.data.repo.SettingsRepository
import com.termkit.app.data.ssh.SshSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val hostRepository: HostRepository,
    private val sessionManager: SshSessionManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setFontSize(size: TerminalFontSize) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun disconnectAll() {
        viewModelScope.launch { sessionManager.disconnect() }
    }

    fun clearHosts() {
        viewModelScope.launch {
            if (sessionManager.status.value is ConnectionStatus.Connected) {
                sessionManager.disconnect()
            }
            hostRepository.clearAll()
        }
    }
}
