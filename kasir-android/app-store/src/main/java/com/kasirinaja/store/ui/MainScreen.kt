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
import com.kasirinaja.store.ui.screens.CameraCaptureScreen
import com.kasirinaja.store.ui.screens.BarcodeScannerFormScreen
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
                    },
                    onNavigateToEditProduct = { productId ->
                        navController.navigate("${Screen.AddProduct.route}?productId=$productId")
                    }
                )
            }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Master.route) { MasterScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(
                route = "${Screen.AddProduct.route}?productId={productId}",
                arguments = listOf(androidx.navigation.navArgument("productId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")

                // Read results from Camera/Scanner screens
                val savedStateHandle = backStackEntry.savedStateHandle
                val capturedImageUri = savedStateHandle.get<String>("captured_image_uri")
                val scannedBarcode = savedStateHandle.get<String>("scanned_barcode")

                // Clear state handles after reading to prevent re-triggering
                if (capturedImageUri != null) savedStateHandle.remove<String>("captured_image_uri")
                if (scannedBarcode != null) savedStateHandle.remove<String>("scanned_barcode")

                AddProductScreen(
                    productId = productId,
                    capturedImageUri = capturedImageUri,
                    scannedBarcode = scannedBarcode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCamera = { navController.navigate(Screen.CameraCapture.route) },
                    onNavigateToScanner = { navController.navigate(Screen.BarcodeScannerForm.route) }
                )
            }

            composable(Screen.CameraCapture.route) {
                CameraCaptureScreen(
                    onImageCaptured = { uriString ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_uri", uriString)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.BarcodeScannerForm.route) {
                BarcodeScannerFormScreen(
                    onBarcodeScanned = { barcode ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_barcode", barcode)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
