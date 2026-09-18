package com.termkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.termkit.app.data.model.AppSettings
import com.termkit.app.data.model.AppTheme
import com.termkit.app.navigation.NavRoutes
import com.termkit.app.navigation.TermkitNavHost
import com.termkit.app.ui.theme.TermkitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as TermkitApp
            val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            TermkitTheme(darkTheme = darkTheme) {
                TermkitAppRoot()
            }
        }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(NavRoutes.Hosts.route, "Hosts", Icons.Filled.Dns),
    BottomTab(NavRoutes.Terminal.route, "Terminal", Icons.Filled.Terminal),
    BottomTab(NavRoutes.Sftp.route, "SFTP", Icons.Filled.Folder),
    BottomTab(NavRoutes.Settings.route, "Settings", Icons.Filled.Settings)
)

@Composable
private fun TermkitAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(NavRoutes.Hosts.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        TermkitNavHost(navController = navController, modifier = Modifier.padding(padding))
    }
}
