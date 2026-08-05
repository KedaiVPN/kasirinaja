package com.kasirinaja.store.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Stock : Screen("stock", "Produk & Stok", Icons.Filled.Inventory)
    object History : Screen("history", "Riwayat", Icons.Filled.History)
    object Scan : Screen("scan", "Transaksi", Icons.Filled.PointOfSale)
    object Master : Screen("master", "Master", Icons.Filled.List)
    object Settings : Screen("settings", "Karyawan", Icons.Filled.People)
    object Reports : Screen("reports", "Laporan", Icons.Filled.BarChart)
    object More : Screen("more", "Lainnya", Icons.Filled.Menu)
    object AddProduct : Screen("add_product", "Tambah Produk", Icons.Filled.Add)
    object CameraCapture : Screen("camera_capture", "Kamera", Icons.Filled.Add) // Not in bottom bar
    object BarcodeScannerForm : Screen("barcode_scanner_form", "Scan Barcode", Icons.Filled.Add) // Not in bottom bar
    object Login : Screen("login", "Login", Icons.Filled.Home) // Not in bottom bar
    object Register : Screen("register", "Register", Icons.Filled.Home) // Not in bottom bar
    object VerifyOtp : Screen("verify_otp", "Verify OTP", Icons.Filled.Home) // Not in bottom bar
    object Payment : Screen("payment", "Pembayaran", Icons.Filled.Add) // Not in bottom bar
    object Receipt : Screen("receipt", "Struk", Icons.Filled.Add) // Not in bottom bar
}
