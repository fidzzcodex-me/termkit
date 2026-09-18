package com.termkit.app.ui.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termkit.app.data.model.AuthMethod
import com.termkit.app.data.model.Host
import com.termkit.app.data.repo.HostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class HostFormUiState(
    val id: String = "",
    val name: String = "",
    val hostname: String = "",
    val port: String = "22",
    val username: String = "",
    val authMethod: AuthMethod = AuthMethod.PASSWORD,
    val password: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val isFavorite: Boolean = false,
    val nameError: String? = null,
    val hostnameError: String? = null,
    val portError: String? = null,
    val usernameError: String? = null,
    val authError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class HostFormViewModel(
    private val repository: HostRepository,
    private val hostId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostFormUiState())
    val uiState: StateFlow<HostFormUiState> = _uiState.asStateFlow()

    init {
        if (hostId != null) {
            viewModelScope.launch {
                val host = repository.hosts.first().firstOrNull { it.id == hostId }
                if (host != null) {
                    _uiState.value = HostFormUiState(
                        id = host.id,
                        name = host.name,
                        hostname = host.hostname,
                        port = host.port.toString(),
                        username = host.username,
                        authMethod = host.authMethod,
                        password = host.password,
                        privateKey = host.privateKey,
                        passphrase = host.passphrase,
                        isFavorite = host.isFavorite
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun onHostnameChange(value: String) {
        _uiState.value = _uiState.value.copy(hostname = value, hostnameError = null)
    }

    fun onPortChange(value: String) {
        if (value.length <= 5 && value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(port = value, portError = null)
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null)
    }

    fun onAuthMethodChange(value: AuthMethod) {
        _uiState.value = _uiState.value.copy(authMethod = value, authError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, authError = null)
    }

    fun onPrivateKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(privateKey = value, authError = null)
    }

    fun onPassphraseChange(value: String) {
        _uiState.value = _uiState.value.copy(passphrase = value)
    }

    fun save() {
        val state = _uiState.value
        var nameError: String? = null
        var hostnameError: String? = null
        var portError: String? = null
        var usernameError: String? = null
        var authError: String? = null

        if (state.name.isBlank()) nameError = "Nama tidak boleh kosong"
        if (state.hostname.isBlank()) hostnameError = "Hostname tidak boleh kosong"
        val portValue = state.port.toIntOrNull()
        if (portValue == null || portValue !in 1..65535) portError = "Port tidak valid"
        if (state.username.isBlank()) usernameError = "Username tidak boleh kosong"
        if (state.authMethod == AuthMethod.PASSWORD && state.password.isBlank()) {
            authError = "Password tidak boleh kosong"
        }
        if (state.authMethod == AuthMethod.PRIVATE_KEY && state.privateKey.isBlank()) {
            authError = "Private key tidak boleh kosong"
        }

        if (nameError != null || hostnameError != null || portError != null || usernameError != null || authError != null) {
            _uiState.value = state.copy(
                nameError = nameError,
                hostnameError = hostnameError,
                portError = portError,
                usernameError = usernameError,
                authError = authError
            )
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val host = Host(
                id = state.id.ifBlank { UUID.randomUUID().toString() },
                name = state.name.trim(),
                hostname = state.hostname.trim(),
                port = portValue!!,
                username = state.username.trim(),
                authMethod = state.authMethod,
                password = state.password,
                privateKey = state.privateKey,
                passphrase = state.passphrase,
                isFavorite = state.isFavorite
            )
            repository.upsert(host)
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
