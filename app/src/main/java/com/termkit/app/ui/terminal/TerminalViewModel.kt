package com.termkit.app.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termkit.app.data.model.AppSettings
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.repo.SettingsRepository
import com.termkit.app.data.ssh.SshSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_BUFFER_CHARS = 200_000

data class TerminalUiState(
    val status: ConnectionStatus = ConnectionStatus.Offline,
    val output: String = "",
    val input: String = "",
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val settings: AppSettings = AppSettings()
)

class TerminalViewModel(
    private val sessionManager: SshSessionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val output = MutableStateFlow("")
    private val input = MutableStateFlow("")
    private val history = MutableStateFlow<List<String>>(emptyList())
    private val historyIndex = MutableStateFlow(-1)

    val uiState: StateFlow<TerminalUiState> = combine(
        sessionManager.status,
        output,
        input,
        history,
        historyIndex,
        settingsRepository.settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        TerminalUiState(
            status = values[0] as ConnectionStatus,
            output = values[1] as String,
            input = values[2] as String,
            history = values[3] as List<String>,
            historyIndex = values[4] as Int,
            settings = values[5] as AppSettings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TerminalUiState())

    init {
        viewModelScope.launch {
            sessionManager.terminalOutput.collect { chunk ->
                val combined = output.value + chunk
                output.value = if (combined.length > MAX_BUFFER_CHARS) combined.takeLast(MAX_BUFFER_CHARS) else combined
            }
        }
    }

    fun onInputChange(value: String) {
        input.value = value
    }

    fun send() {
        val command = input.value
        if (command.isBlank()) return
        history.value = history.value + command
        historyIndex.value = history.value.size
        input.value = ""
        viewModelScope.launch { sessionManager.sendCommand(command) }
    }

    fun historyUp() {
        val list = history.value
        if (list.isEmpty()) return
        val nextIndex = (historyIndex.value - 1).coerceAtLeast(0)
        historyIndex.value = nextIndex
        input.value = list[nextIndex]
    }

    fun historyDown() {
        val list = history.value
        if (list.isEmpty()) return
        val nextIndex = historyIndex.value + 1
        if (nextIndex >= list.size) {
            historyIndex.value = list.size
            input.value = ""
        } else {
            historyIndex.value = nextIndex
            input.value = list[nextIndex]
        }
    }

    fun clear() {
        output.value = ""
    }

    fun disconnect() {
        viewModelScope.launch { sessionManager.disconnect() }
    }
}
