package com.termkit.app.navigation

sealed class NavRoutes(val route: String) {
    data object Hosts : NavRoutes("hosts")
    data object HostForm : NavRoutes("host_form?hostId={hostId}") {
        fun createRoute(hostId: String? = null) = "host_form?hostId=${hostId ?: ""}"
    }
    data object Terminal : NavRoutes("terminal")
    data object Sftp : NavRoutes("sftp")
    data object Settings : NavRoutes("settings")
}
