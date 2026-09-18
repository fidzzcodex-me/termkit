package com.termkit.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.termkit.app.BuildConfig
import com.termkit.app.TermkitApp
import com.termkit.app.data.model.AppTheme
import com.termkit.app.data.model.TerminalFontSize
import com.termkit.app.ui.components.ConfirmDialog

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as TermkitApp
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(app.settingsRepository, app.hostRepository, app.sessionManager) }
    })
    val settings by viewModel.settings.collectAsState()
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SettingsSection(title = "Tampilan") {
                SettingsThemeRow(current = settings.theme, onSelect = viewModel::setTheme)
                SettingsFontSizeRow(current = settings.terminalFontSize, onSelect = viewModel::setFontSize)
            }
            SettingsSection(title = "Sesi") {
                ListItem(
                    headlineContent = { Text("Tetap nyalakan layar saat terhubung") },
                    trailingContent = {
                        Switch(checked = settings.keepScreenOnWhileConnected, onCheckedChange = viewModel::setKeepScreenOn)
                    }
                )
                ListItem(
                    headlineContent = { Text("Putuskan semua koneksi") },
                    modifier = Modifier.clickable { showDisconnectConfirm = true }
                )
            }
            SettingsSection(title = "Data") {
                ListItem(
                    headlineContent = { Text("Hapus semua data host") },
                    supportingContent = { Text("Menghapus semua host tersimpan secara permanen") },
                    modifier = Modifier.clickable { showClearConfirm = true }
                )
            }
            SettingsSection(title = "Tentang") {
                ListItem(headlineContent = { Text("Termkit") }, supportingContent = { Text("Versi ${BuildConfig.VERSION_NAME}") })
                ListItem(
                    headlineContent = { Text("Catatan keamanan") },
                    supportingContent = {
                        Text("Password dan private key disimpan lokal di perangkat ini tanpa enkripsi tambahan. Gunakan hanya di perangkat yang kamu percaya.")
                    }
                )
            }
        }
    }

    if (showDisconnectConfirm) {
        ConfirmDialog(
            title = "Putuskan semua koneksi",
            message = "Sesi SSH yang aktif akan diputus.",
            confirmLabel = "Putuskan",
            onConfirm = { viewModel.disconnectAll(); showDisconnectConfirm = false },
            onDismiss = { showDisconnectConfirm = false }
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Hapus semua host",
            message = "Semua data host yang tersimpan akan dihapus permanen.",
            confirmLabel = "Hapus semua",
            onConfirm = { viewModel.clearHosts(); showClearConfirm = false },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsThemeRow(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    ListItem(
        headlineContent = { Text("Tema") },
        supportingContent = {
            Row {
                AppTheme.values().forEach { theme ->
                    FilterChip(
                        selected = theme == current,
                        onClick = { onSelect(theme) },
                        label = { Text(themeLabel(theme)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SettingsFontSizeRow(current: TerminalFontSize, onSelect: (TerminalFontSize) -> Unit) {
    ListItem(
        headlineContent = { Text("Ukuran font terminal") },
        supportingContent = {
            Row {
                TerminalFontSize.values().forEach { size ->
                    FilterChip(
                        selected = size == current,
                        onClick = { onSelect(size) },
                        label = { Text(fontSizeLabel(size)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    )
}

private fun themeLabel(theme: AppTheme) = when (theme) {
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
    AppTheme.SYSTEM -> "System"
}

private fun fontSizeLabel(size: TerminalFontSize) = when (size) {
    TerminalFontSize.SMALL -> "Kecil"
    TerminalFontSize.NORMAL -> "Normal"
    TerminalFontSize.LARGE -> "Besar"
}
