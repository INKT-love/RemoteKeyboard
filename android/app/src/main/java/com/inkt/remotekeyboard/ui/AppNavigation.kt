package com.inkt.remotekeyboard.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inkt.remotekeyboard.ui.screens.HomeScreen
import com.inkt.remotekeyboard.ui.screens.KeyboardScreen
import com.inkt.remotekeyboard.ui.screens.SettingsScreen
import com.inkt.remotekeyboard.ui.screens.ShortcutsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToKeyboard = { navController.navigate("keyboard") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("keyboard") {
            KeyboardScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToShortcuts = { navController.navigate("shortcuts") }
            )
        }
        composable("shortcuts") {
            ShortcutsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
