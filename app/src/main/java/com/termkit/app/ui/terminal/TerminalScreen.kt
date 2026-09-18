package com.termkit.app.ui.terminal

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.termkit.app.TermkitApp
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.data.model.TerminalFontSize
import com.termkit.app.ui.components.EmptyState
import com.termkit.app.ui.components.StatusChip

@Composable
fun TerminalScreen() {
    val app = LocalContext.current.applicationContext as TermkitApp
    val viewModel: TerminalViewModel = viewModel(factory = viewModelFactory {
        initializer { TerminalViewModel(app.sessionManager, app.settingsRepository) }
    })
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val activity = LocalContext.current as? Activity
    LaunchedEffect(state.status, state.settings.keepScreenOnWhileConnected) {
        val window = activity?.window ?: return@LaunchedEffect
        val shouldKeepOn = state.status is ConnectionStatus.Connected && state.settings.keepScreenOnWhileConnected
        if (shouldKeepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal") },
                actions = {
                    StatusChip(status = state.status, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Filled.Clear, contentDescription = "Bersihkan")
                    }
                    if (state.status is ConnectionStatus.Connected) {
                        IconButton(onClick = viewModel::disconnect) {
                            Icon(Icons.Filled.LinkOff, contentDescription = "Putuskan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.status is ConnectionStatus.Offline) {
            EmptyState(
                icon = Icons.Filled.Terminal,
                title = "Belum ada sesi",
                description = "Connect ke salah satu host dari tab Hosts untuk membuka terminal",
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                val fontSize = when (state.settings.terminalFontSize) {
                    TerminalFontSize.SMALL -> 12.sp
                    TerminalFontSize.NORMAL -> 14.sp
                    TerminalFontSize.LARGE -> 18.sp
                }
                Text(
                    text = state.output.ifBlank { "Menunggu output..." },
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(12.dp)
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::historyUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "History sebelumnya")
                    }
                    IconButton(onClick = viewModel::historyDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "History berikutnya")
                    }
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ketik perintah") },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = viewModel::send, enabled = state.input.isNotBlank()) {
                        Icon(Icons.Filled.Send, contentDescription = "Kirim")
                    }
                }
            }
        }
    }
}
