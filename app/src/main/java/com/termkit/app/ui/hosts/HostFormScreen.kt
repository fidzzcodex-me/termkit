package com.termkit.app.ui.hosts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.termkit.app.TermkitApp
import com.termkit.app.data.model.AuthMethod

@Composable
fun HostFormScreen(hostId: String?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as TermkitApp
    val viewModel: HostFormViewModel = viewModel(factory = viewModelFactory {
        initializer { HostFormViewModel(app.hostRepository, hostId) }
    })
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (hostId == null) "Host baru" else "Edit host") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nama host") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.hostname,
                onValueChange = viewModel::onHostnameChange,
                label = { Text("Hostname / IP") },
                isError = state.hostnameError != null,
                supportingText = { state.hostnameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.port,
                onValueChange = viewModel::onPortChange,
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.portError != null,
                supportingText = { state.portError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                isError = state.usernameError != null,
                supportingText = { state.usernameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = state.authMethod == AuthMethod.PASSWORD,
                    onClick = { viewModel.onAuthMethodChange(AuthMethod.PASSWORD) },
                    label = { Text("Password") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = state.authMethod == AuthMethod.PRIVATE_KEY,
                    onClick = { viewModel.onAuthMethodChange(AuthMethod.PRIVATE_KEY) },
                    label = { Text("Private key") }
                )
            }

            if (state.authMethod == AuthMethod.PASSWORD) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.authError != null,
                    supportingText = { state.authError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = state.privateKey,
                    onValueChange = viewModel::onPrivateKeyChange,
                    label = { Text("Private key (PEM)") },
                    isError = state.authError != null,
                    supportingText = { state.authError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                OutlinedTextField(
                    value = state.passphrase,
                    onValueChange = viewModel::onPassphraseChange,
                    label = { Text("Passphrase (opsional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (hostId == null) "Simpan host" else "Simpan perubahan")
                }
            }
        }
    }
}
