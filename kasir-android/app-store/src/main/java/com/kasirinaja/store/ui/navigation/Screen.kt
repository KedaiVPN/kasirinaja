package com.kasirinaja.store.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Stock : Screen("stock", "Produk & Stok", Icons.Filled.Inventory)
    object Scan : Screen("scan", "Scan", Icons.Filled.QrCodeScanner)
    object Master : Screen("master", "Master", Icons.Filled.List)
    object Settings : Screen("settings", "Pengaturan", Icons.Filled.Settings)
    object AddProduct : Screen("add_product", "Tambah Produk", Icons.Filled.Add)
    object CameraCapture : Screen("camera_capture", "Kamera", Icons.Filled.Add) // Not in bottom bar
    object BarcodeScannerForm : Screen("barcode_scanner_form", "Scan Barcode", Icons.Filled.Add) // Not in bottom bar
}
