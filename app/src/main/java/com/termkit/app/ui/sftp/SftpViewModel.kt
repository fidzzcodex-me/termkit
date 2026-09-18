package com.termkit.app.ui.sftp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.SftpEntry
import com.termkit.app.data.ssh.SshSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SftpLoadState {
    data object Idle : SftpLoadState()
    data object Loading : SftpLoadState()
    data class Error(val message: String) : SftpLoadState()
}

data class SftpUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Offline,
    val currentPath: String = "/",
    val entries: List<SftpEntry> = emptyList(),
    val loadState: SftpLoadState = SftpLoadState.Idle,
    val pendingDelete: SftpEntry? = null,
    val pendingRename: SftpEntry? = null,
    val showCreateFolder: Boolean = false,
    val busyActionPath: String? = null,
    val message: String? = null
)

class SftpViewModel(private val sessionManager: SshSessionManager) : ViewModel() {

    private val currentPath = MutableStateFlow("/")
    private val entries = MutableStateFlow<List<SftpEntry>>(emptyList())
    private val loadState = MutableStateFlow<SftpLoadState>(SftpLoadState.Idle)
    private val pendingDelete = MutableStateFlow<SftpEntry?>(null)
    private val pendingRename = MutableStateFlow<SftpEntry?>(null)
    private val showCreateFolder = MutableStateFlow(false)
    private val busyActionPath = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SftpUiState> = combine(
        sessionManager.status,
        currentPath,
        entries,
        loadState,
        pendingDelete,
        pendingRename,
        showCreateFolder,
        busyActionPath,
        message
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SftpUiState(
            connectionStatus = values[0] as ConnectionStatus,
            currentPath = values[1] as String,
            entries = values[2] as List<SftpEntry>,
            loadState = values[3] as SftpLoadState,
            pendingDelete = values[4] as SftpEntry?,
            pendingRename = values[5] as SftpEntry?,
            showCreateFolder = values[6] as Boolean,
            busyActionPath = values[7] as String?,
            message = values[8] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SftpUiState())

    init {
        viewModelScope.launch {
            sessionManager.status.collect { status ->
                if (status is ConnectionStatus.Connected) {
                    currentPath.value = "/"
                    refresh()
                } else {
                    entries.value = emptyList()
                }
            }
        }
    }

    fun refresh() {
        val path = currentPath.value
        viewModelScope.launch {
            loadState.value = SftpLoadState.Loading
            try {
                entries.value = sessionManager.listDirectory(path)
                loadState.value = SftpLoadState.Idle
            } catch (error: Exception) {
                loadState.value = SftpLoadState.Error(error.message ?: "Gagal memuat direktori")
            }
        }
    }

    fun open(entry: SftpEntry) {
        if (!entry.isDirectory) return
        currentPath.value = entry.path
        refresh()
    }

    fun navigateUp() {
        val path = currentPath.value
        if (path == "/") return
        val parent = path.substringBeforeLast("/").ifBlank { "/" }
        currentPath.value = parent
        refresh()
    }

    fun requestDelete(entry: SftpEntry) {
        pendingDelete.value = entry
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val entry = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            busyActionPath.value = entry.path
            try {
                sessionManager.delete(entry.path, entry.isDirectory)
                refresh()
            } catch (error: Exception) {
                message.value = error.message ?: "Gagal menghapus"
            } finally {
                busyActionPath.value = null
            }
        }
    }

    fun requestRename(entry: SftpEntry) {
        pendingRename.value = entry
    }

    fun cancelRename() {
        pendingRename.value = null
    }

    fun confirmRename(newName: String) {
        val entry = pendingRename.value ?: return
        pendingRename.value = null
        if (newName.isBlank()) return
        val newPath = entry.path.substringBeforeLast("/") + "/" + newName
        viewModelScope.launch {
            busyActionPath.value = entry.path
            try {
                sessionManager.rename(entry.path, newPath)
                refresh()
            } catch (error: Exception) {
                message.value = error.message ?: "Gagal mengganti nama"
            } finally {
                busyActionPath.value = null
            }
        }
    }

    fun showCreateFolderDialog() {
        showCreateFolder.value = true
    }

    fun dismissCreateFolderDialog() {
        showCreateFolder.value = false
    }

    fun createFolder(name: String) {
        showCreateFolder.value = false
        if (name.isBlank()) return
        val path = if (currentPath.value == "/") "/$name" else "${currentPath.value}/$name"
        viewModelScope.launch {
            try {
                sessionManager.createDirectory(path)
                refresh()
            } catch (error: Exception) {
                message.value = error.message ?: "Gagal membuat folder"
            }
        }
    }

    fun upload(context: Context, uri: Uri, fileName: String) {
        val path = if (currentPath.value == "/") "/$fileName" else "${currentPath.value}/$fileName"
        viewModelScope.launch {
            busyActionPath.value = path
            try {
                sessionManager.uploadFromUri(context, uri, path)
                refresh()
            } catch (error: Exception) {
                message.value = error.message ?: "Gagal mengunggah file"
            } finally {
                busyActionPath.value = null
            }
        }
    }

    fun download(context: Context, entry: SftpEntry, uri: Uri) {
        viewModelScope.launch {
            busyActionPath.value = entry.path
            try {
                sessionManager.downloadToUri(context, entry.path, uri)
                message.value = "Berhasil mengunduh ${entry.name}"
            } catch (error: Exception) {
                message.value = error.message ?: "Gagal mengunduh file"
            } finally {
                busyActionPath.value = null
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
