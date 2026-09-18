package com.termkit.app.data.model

sealed class ConnectionStatus {
    data object Offline : ConnectionStatus()
    data object Connecting : ConnectionStatus()
    data class Connected(val host: Host) : ConnectionStatus()
    data object Reconnecting : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
