package com.termkit.app.ui.sftp

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.termkit.app.TermkitApp
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.SftpEntry
import com.termkit.app.ui.components.ConfirmDialog
import com.termkit.app.ui.components.EmptyState
import com.termkit.app.ui.components.TextInputDialog

@Composable
fun SftpScreen() {
    val app = LocalContext.current.applicationContext as TermkitApp
    val context = LocalContext.current
    val viewModel: SftpViewModel = viewModel(factory = viewModelFactory {
        initializer { SftpViewModel(app.sessionManager) }
    })
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDownloadEntry by remember { mutableStateOf<SftpEntry?>(null) }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "berkas_unggahan"
            viewModel.upload(context, uri, name)
        }
    }

    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val entry = pendingDownloadEntry
        if (uri != null && entry != null) {
            viewModel.download(context, entry, uri)
        }
        pendingDownloadEntry = null
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("SFTP") },
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang")
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::navigateUp, enabled = state.currentPath != "/") {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Naik satu level")
                    }
                    Text(
                        text = state.currentPath,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.connectionStatus is ConnectionStatus.Connected) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(onClick = viewModel::showCreateFolderDialog) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "Folder baru")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FloatingActionButton(onClick = { uploadLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Unggah")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.connectionStatus !is ConnectionStatus.Connected -> {
                EmptyState(
                    icon = Icons.Filled.FolderOpen,
                    title = "Belum terhubung",
                    description = "Connect ke host terlebih dahulu untuk mengelola file",
                    modifier = Modifier.padding(padding)
                )
            }
            state.loadState is SftpLoadState.Loading && state.entries.isEmpty() -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.loadState is SftpLoadState.Error -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text((state.loadState as SftpLoadState.Error).message, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = viewModel::refresh) { Text("Coba lagi") }
                }
            }
            state.entries.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Folder,
                    title = "Folder kosong",
                    description = "Tidak ada berkas di direktori ini",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(state.entries, key = { it.path }) { entry ->
                        SftpEntryRow(
                            entry = entry,
                            isBusy = state.busyActionPath == entry.path,
                            onClick = { if (entry.isDirectory) viewModel.open(entry) },
                            onDownload = {
                                pendingDownloadEntry = entry
                                downloadLauncher.launch(entry.name)
                            },
                            onRename = { viewModel.requestRename(entry) },
                            onDelete = { viewModel.requestDelete(entry) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "Hapus ${if (entry.isDirectory) "folder" else "file"}",
            message = "Hapus \"${entry.name}\"? Tindakan ini tidak bisa dibatalkan.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    state.pendingRename?.let { entry ->
        TextInputDialog(
            title = "Ganti nama",
            label = "Nama baru",
            initialValue = entry.name,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::cancelRename
        )
    }

    if (state.showCreateFolder) {
        TextInputDialog(
            title = "Folder baru",
            label = "Nama folder",
            onConfirm = viewModel::createFolder,
            onDismiss = viewModel::dismissCreateFolderDialog
        )
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return cursor.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) it.getString(index) else null
        } else {
            null
        }
    }
}
