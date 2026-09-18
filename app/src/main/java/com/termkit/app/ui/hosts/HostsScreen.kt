package com.termkit.app.ui.hosts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.termkit.app.TermkitApp
import com.termkit.app.ui.components.ConfirmDialog
import com.termkit.app.ui.components.EmptyState

@Composable
fun HostsScreen(
    onAddHost: () -> Unit,
    onEditHost: (String) -> Unit,
    onConnected: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TermkitApp
    val viewModel: HostsViewModel = viewModel(factory = viewModelFactory {
        initializer { HostsViewModel(app.hostRepository, app.sessionManager) }
    })
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.navigateToTerminal) {
        if (state.navigateToTerminal) {
            onConnected()
            viewModel.consumeNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hosts") },
                actions = {
                    IconButton(onClick = onAddHost) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah host")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Cari host") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (state.hosts.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Dns,
                    title = "Belum ada host",
                    description = "Tambahkan server pertama kamu untuk mulai connect"
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(state.hosts, key = { it.id }) { host ->
                        HostCard(
                            host = host,
                            isActive = state.activeHostId == host.id,
                            onClick = { viewModel.requestConnect(host) },
                            onEdit = { onEditHost(host.id) },
                            onDelete = { viewModel.requestDelete(host) },
                            onFavorite = { viewModel.onFavoriteToggle(host) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    state.pendingDeleteHost?.let { host ->
        ConfirmDialog(
            title = "Hapus host",
            message = "Hapus \"${host.name}\"? Tindakan ini tidak bisa dibatalkan.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    state.pendingSwitchHost?.let { host ->
        ConfirmDialog(
            title = "Ganti host aktif",
            message = "Kamu masih terhubung ke host lain. Putuskan dan connect ke \"${host.name}\"?",
            confirmLabel = "Ganti",
            onConfirm = viewModel::confirmSwitch,
            onDismiss = viewModel::cancelSwitch
        )
    }
}
