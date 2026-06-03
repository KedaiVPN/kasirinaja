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
import com.kasirinaja.store.ui.screens.AddProductScreen
import com.kasirinaja.store.ui.screens.DashboardScreen
import com.kasirinaja.store.ui.screens.MasterScreen
import com.kasirinaja.store.ui.screens.ScanScreen
import com.kasirinaja.store.ui.screens.SettingsScreen
import com.kasirinaja.store.ui.screens.StockScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val bottomBarScreens = listOf(
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
                val currentRoute = currentDestination?.route

                // Only show bottom bar on main screens
                if (bottomBarScreens.any { it.route == currentRoute }) {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
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
            composable(Screen.Stock.route) {
                StockScreen(
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddProduct.route)
                    }
                )
            }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Master.route) { MasterScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.AddProduct.route) {
                AddProductScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
