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
import com.kasirinaja.store.presentation.auth.LoginScreen
import com.kasirinaja.store.presentation.auth.RegisterStoreScreen
import com.kasirinaja.store.presentation.auth.VerifyOtpScreen
import com.kasirinaja.store.presentation.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kasirinaja.store.presentation.auth.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = com.kasirinaja.core.network.TokenManager(context)
    val authRepository = com.kasirinaja.store.data.repository.AuthRepository(
        com.kasirinaja.core.network.RetrofitClient.authApi,
        tokenManager
    )

    // Auth ViewModel
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    var startDest by remember { mutableStateOf(Screen.Login.route) }
    var isCheckingToken by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (!token.isNullOrEmpty()) {
            startDest = Screen.Dashboard.route
        } else {
            startDest = Screen.Login.route
        }
        isCheckingToken = false
    }

    if (isCheckingToken) {
        return // Or a splash screen
    }


    val bottomBarScreens = listOf(
        Screen.Dashboard,
        Screen.Stock,
        Screen.Scan,
        Screen.Master,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            if (bottomBarScreens.any { it.route == currentRoute }) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterStoreScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToVerifyOtp = { email ->
                        navController.navigate("${Screen.VerifyOtp.route}/$email")
                    }
                )
            }
            composable(
                route = "${Screen.VerifyOtp.route}/{email}",
                arguments = listOf(androidx.navigation.navArgument("email") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                VerifyOtpScreen(
                    viewModel = authViewModel,
                    email = email,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    }
                )
            }
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
