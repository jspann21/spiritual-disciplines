package com.spiritualdisciplines.ui

import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spiritualdisciplines.viewmodel.MainViewModel
import com.spiritualdisciplines.update.UpdateUiState

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "Today", Icons.Outlined.Home, Icons.Filled.Home)
    object Bible : Screen("bible", "Bible", Icons.Outlined.Book, Icons.Filled.Book)
    object Prayer : Screen("prayer", "Prayer", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    object Memory : Screen("memory", "Memory", Icons.Outlined.Lightbulb, Icons.Filled.Lightbulb)
    object Journal : Screen("journal", "Journal", Icons.Outlined.Edit, Icons.Filled.Edit)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Bible,
    Screen.Prayer,
    Screen.Memory,
    Screen.Journal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val haptics = rememberExpressiveHaptics()
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    if (updateState is UpdateUiState.Available) {
        val update = (updateState as UpdateUiState.Available).update
        AlertDialog(
            onDismissRequest = viewModel::clearUpdateStatus,
            title = { Text("Update available") },
            text = { Text("Spiritual Disciplines ${update.versionName} is ready to download.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, update.releaseUrl.toUri()))
                        viewModel.clearUpdateStatus()
                    }
                ) {
                    Text("View update")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearUpdateStatus) {
                    Text("Later")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.expressiveScrollFeedback(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) screen.selectedIcon else screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            haptics.select()
                            if (screen.route == Screen.Dashboard.route) {
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel, onSettingsClick = { navController.navigate("settings") }) }
            composable(Screen.Bible.route) { BibleScreen(viewModel) }
            composable(Screen.Prayer.route) { PrayerScreen(viewModel) }
            composable(Screen.Memory.route) { MemoryScreen(viewModel) }
            composable(Screen.Journal.route) { JournalScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel, onBack = { navController.popBackStack() }) }
        }
    }
}
