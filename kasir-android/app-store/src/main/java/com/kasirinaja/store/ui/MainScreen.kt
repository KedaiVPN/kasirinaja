package com.kasirinaja.store.ui
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import com.kasirinaja.store.ui.viewmodels.ScanViewModel
import com.kasirinaja.store.ui.screens.BarcodeScannerFormScreen
import com.kasirinaja.store.ui.screens.StockScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.kasirinaja.store.ui.screens.CameraCaptureScreen
import androidx.compose.foundation.layout.width
import com.kasirinaja.store.data.local.AppDatabase
import com.kasirinaja.store.ui.screens.AddProductScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kasirinaja.store.ui.viewmodels.ReceiptViewModel
import com.kasirinaja.store.ui.screens.DashboardScreen
import com.kasirinaja.store.ui.screens.EditStoreScreen
import com.kasirinaja.store.ui.viewmodels.EditStoreViewModel
import com.kasirinaja.store.data.repository.StoreRepository
import com.kasirinaja.store.presentation.auth.RegisterStoreScreen
import com.kasirinaja.store.ui.screens.PaymentScreen
import com.kasirinaja.store.ui.screens.HistoryScreen
import com.kasirinaja.store.ui.viewmodels.HistoryViewModel
import com.kasirinaja.store.ui.viewmodels.HistoryViewModelFactory
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.size
import com.kasirinaja.store.presentation.auth.AuthViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

import com.kasirinaja.core.network.WebSocketManager
import kotlinx.coroutines.launch

import androidx.compose.runtime.setValue
import com.kasirinaja.store.presentation.auth.AuthViewModelFactory
import com.kasirinaja.store.ui.screens.ScanScreen
import androidx.compose.foundation.layout.padding
import com.kasirinaja.store.ui.viewmodels.ScanViewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import com.kasirinaja.store.ui.screens.MasterScreen
import androidx.compose.material3.Icon
import com.kasirinaja.store.ui.navigation.Screen
import androidx.compose.material3.NavigationBarItem
import com.kasirinaja.store.presentation.auth.VerifyOtpScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.kasirinaja.store.data.repository.ProductRepository
import androidx.compose.material3.NavigationBar
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kasirinaja.store.presentation.auth.LoginScreen
import androidx.navigation.compose.composable
import com.kasirinaja.store.ui.screens.SettingsScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.kasirinaja.store.ui.screens.ReceiptScreen
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import com.kasirinaja.store.ui.viewmodels.ReceiptViewModelFactory


@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = com.kasirinaja.core.network.TokenManager(context)
    val authRepository = com.kasirinaja.store.data.repository.AuthRepository(
        com.kasirinaja.core.network.RetrofitClient.authApi,
        tokenManager
    )

    // Auth ViewModel
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Scan ViewModel scoped to MainScreen so it persists between Scan and Payment
    val productDao = AppDatabase.getDatabase(context).productDao()
    val transactionDao = AppDatabase.getDatabase(context).transactionDao()
    val productRepository = ProductRepository(productDao, transactionDao, context)

    val webSocketManager = remember {
        WebSocketManager(
            onSyncProduct = {
                coroutineScope.launch {
                    try {
                        productRepository.syncStoreProducts()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    val isUserLoggedIn = tokenManager.getToken() != null
    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            tokenManager.getStoreId()?.let {
                webSocketManager.connect(it)
            }

            // Perform one-time initial sync on startup/login
            coroutineScope.launch {
                try {
                    productRepository.syncStoreProducts()
                    val transactionRepository = com.kasirinaja.store.data.repository.TransactionRepository(
                        com.kasirinaja.store.data.local.AppDatabase.getDatabase(context).transactionDao(),
                        com.kasirinaja.core.network.RetrofitClient.transactionApi
                    )
                    transactionRepository.fetchAndSaveAllTransactions()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            webSocketManager.disconnect()
        }
    }

    val database = com.kasirinaja.store.data.local.AppDatabase.getDatabase(context)
    val transactionRepository = com.kasirinaja.store.data.repository.TransactionRepository(
        database.transactionDao(),
        com.kasirinaja.core.network.RetrofitClient.transactionApi
    )
    val workManager = androidx.work.WorkManager.getInstance(context)

        val dashboardViewModel: com.kasirinaja.store.ui.viewmodels.DashboardViewModel = viewModel(
        factory = com.kasirinaja.store.ui.viewmodels.DashboardViewModel.Factory(
            database.transactionDao(),
            database.productDao(),
            transactionRepository,
            tokenManager
        )
    )

    var showSyncDialog by remember { mutableStateOf(false) }
    var showInitialSyncDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.kasirinaja.store.data.repository.TransactionSyncState.syncStatus.collect { status ->
            when (status) {
                "sync_started" -> {
                    showSyncDialog = true
                    android.widget.Toast.makeText(context, "Sinkronisasi dimulai...", android.widget.Toast.LENGTH_SHORT).show()
                }
                "sync_success" -> {
                    showSyncDialog = false
                    android.widget.Toast.makeText(context, "Sinkronisasi berhasil!", android.widget.Toast.LENGTH_SHORT).show()
                    dashboardViewModel.fetchServerStats()
                }
                "sync_failed" -> {
                    showSyncDialog = false
                    android.widget.Toast.makeText(context, "Sinkronisasi gagal. Akan mencoba lagi nanti.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showSyncDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Cannot dismiss */ }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.size(24.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    androidx.compose.material3.Text("Sedang menyinkronkan transaksi...")
                }
            }
        }
    }

    if (showInitialSyncDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Cannot dismiss */ }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.size(24.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    androidx.compose.material3.Text("Sedang menyinkronkan data, mohon tunggu...")
                }
            }
        }
    }

    val scanViewModel: ScanViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(productRepository, transactionRepository, workManager) as T
            }
        }
    )

    val receiptViewModel: ReceiptViewModel = viewModel(
        factory = ReceiptViewModelFactory(productRepository)
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(transactionDao, transactionRepository)
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
        Screen.History,
        Screen.Scan,
        Screen.More
    )

    var showMoreMenu by remember { mutableStateOf(false) }
    val bottomBarVisibleScreens = listOf(Screen.Dashboard.route, Screen.Stock.route, Screen.History.route, Screen.Scan.route, Screen.Master.route, Screen.Settings.route)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            if (bottomBarVisibleScreens.contains(currentRoute)) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Indicator logic
                        var indicatorOffsetX by remember { androidx.compose.runtime.mutableStateOf(0f) }
                        var indicatorWidth by remember { androidx.compose.runtime.mutableStateOf(0f) }
                        var indicatorHeight by remember { androidx.compose.runtime.mutableStateOf(0f) }
                        val density = androidx.compose.ui.platform.LocalDensity.current

                        val animatedOffsetX by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = indicatorOffsetX,
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        )
                        val animatedWidth by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = indicatorWidth,
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        )

                        // Animated Pill Background
                        if (indicatorWidth > 0f) {
                            Box(
                                modifier = Modifier
                                    .offset { androidx.compose.ui.unit.IntOffset(animatedOffsetX.toInt(), 0) }
                                    .width(with(density) { animatedWidth.toDp() })
                                    .height(with(density) { indicatorHeight.toDp() })
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                    .align(androidx.compose.ui.Alignment.CenterStart)
                            )
                        }

                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            bottomBarScreens.forEach { screen ->
                                val isSelected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route || (screen == Screen.More && (it.route == Screen.Master.route || it.route == Screen.Settings.route))
                                } == true

                                Box(modifier = Modifier.onGloballyPositioned { coordinates ->
                                    if (isSelected) {
                                        indicatorOffsetX = coordinates.positionInParent().x
                                        indicatorWidth = coordinates.size.width.toFloat()
                                        indicatorHeight = coordinates.size.height.toFloat()
                                    }
                                }) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                            .clickable {
                                                if (screen == Screen.More) {
                                                    showMoreMenu = true
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
                                            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 12.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            tint = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF2C3E50)
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = screen.title,
                                                color = androidx.compose.ui.graphics.Color.White,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (screen == Screen.More) {
                                        DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Master") },
                                                leadingIcon = { Icon(Screen.Master.icon, contentDescription = "Master") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    navController.navigate(Screen.Master.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Karyawan") },
                                                leadingIcon = { Icon(Screen.Settings.icon, contentDescription = "Karyawan") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    navController.navigate(Screen.Settings.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                        coroutineScope.launch {
                            try {
                                showInitialSyncDialog = true
                                productRepository.syncStoreProducts()
                                transactionRepository.fetchAndSaveAllTransactions()
                                dashboardViewModel.fetchServerStats()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                showInitialSyncDialog = false
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
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
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) // Clear backstack so user can't go back to auth
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToEditStore = { navController.navigate("edit_store") },
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
            composable("edit_store") {
                val storeRepository = com.kasirinaja.store.data.repository.StoreRepository(com.kasirinaja.core.network.RetrofitClient.storeApi)
                val editStoreViewModel: com.kasirinaja.store.ui.viewmodels.EditStoreViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.kasirinaja.store.ui.viewmodels.EditStoreViewModel.Factory(storeRepository, tokenManager)
                )
                com.kasirinaja.store.ui.screens.EditStoreScreen(
                    viewModel = editStoreViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Stock.route) {
                StockScreen(
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddProduct.route)
                    },
                    onNavigateToEditProduct = { productId ->
                        navController.navigate("${Screen.AddProduct.route}?productId=$productId")
                    },
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToReceipt = { transactionId ->
                        navController.navigate("${Screen.Receipt.route}/$transactionId")
                    },
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
                        composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = scanViewModel,
                    onNavigateToPayment = { navController.navigate(Screen.Payment.route) },
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Payment.route) {
                PaymentScreen(
                    viewModel = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSuccess = { transactionId ->
                        navController.navigate("${Screen.Receipt.route}/$transactionId") {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "${Screen.Receipt.route}/{transactionId}",
                arguments = listOf(androidx.navigation.navArgument("transactionId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                ReceiptScreen(
                    viewModel = receiptViewModel,
                    transactionId = transactionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Master.route) {
                MasterScreen(
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        startDest = Screen.Login.route
                        authViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
            composable("edit_profile") {
                val editProfileViewModel: com.kasirinaja.store.ui.viewmodels.EditProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.kasirinaja.store.ui.viewmodels.EditProfileViewModel.Factory(tokenManager)
                )
                com.kasirinaja.store.ui.screens.EditProfileScreen(viewModel = editProfileViewModel, onNavigateBack = { navController.popBackStack() })
            }
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
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
