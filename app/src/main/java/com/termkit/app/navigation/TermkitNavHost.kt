package com.termkit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.termkit.app.ui.hosts.HostFormScreen
import com.termkit.app.ui.hosts.HostsScreen
import com.termkit.app.ui.settings.SettingsScreen
import com.termkit.app.ui.sftp.SftpScreen
import com.termkit.app.ui.terminal.TerminalScreen

@Composable
fun TermkitNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = NavRoutes.Hosts.route, modifier = modifier) {
        composable(NavRoutes.Hosts.route) {
            HostsScreen(
                onAddHost = { navController.navigate(NavRoutes.HostForm.createRoute()) },
                onEditHost = { hostId -> navController.navigate(NavRoutes.HostForm.createRoute(hostId)) },
                onConnected = {
                    navController.navigate(NavRoutes.Terminal.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = NavRoutes.HostForm.route,
            arguments = listOf(navArgument("hostId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val hostId = backStackEntry.arguments?.getString("hostId").orEmpty()
            HostFormScreen(
                hostId = hostId.ifBlank { null },
                onDone = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.Terminal.route) { TerminalScreen() }
        composable(NavRoutes.Sftp.route) { SftpScreen() }
        composable(NavRoutes.Settings.route) { SettingsScreen() }
    }
}
