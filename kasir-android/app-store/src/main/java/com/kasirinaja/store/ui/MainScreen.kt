package com.kasirinaja.store.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kasirinaja.store.ui.navigation.Screen
import com.kasirinaja.store.ui.screens.DashboardScreen
import com.kasirinaja.store.ui.screens.MasterScreen
import com.kasirinaja.store.ui.screens.ScanScreen
import com.kasirinaja.store.ui.screens.SettingsScreen
import com.kasirinaja.store.ui.screens.StockScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val screens = listOf(
        Screen.Dashboard,
        Screen.Stock,
        Screen.Scan,
        Screen.Master,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Stock.route) { StockScreen() }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Master.route) { MasterScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
