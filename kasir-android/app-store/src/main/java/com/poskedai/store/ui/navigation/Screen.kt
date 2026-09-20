package com.poskedai.store.ui.navigation

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Stock : Screen("stock", "Produk & Stok", Icons.Filled.Inventory)
    object History : Screen("history", "Riwayat", Icons.Filled.History)
    object Scan : Screen("scan", "Transaksi", Icons.Filled.PointOfSale)
    object Master : Screen("master", "Master Produk", Icons.Filled.List)
    object Settings : Screen("settings", "Karyawan", Icons.Filled.People)
    object Feedback : Screen("feedback", "Kritik & Saran", Icons.Filled.RateReview)
    object Support : Screen("support", "Support", Icons.Filled.SupportAgent)
    object SalesStats : Screen("sales_stats", "Statistik Penjualan", Icons.Filled.BarChart)
    object Reports : Screen("reports", "Laporan", Icons.Filled.Assignment)
    object More : Screen("more", "Lainnya", Icons.Filled.Menu)
    object AddProduct : Screen("add_product", "Tambah Produk", Icons.Filled.Add)
    object CameraCapture : Screen("camera_capture", "Kamera", Icons.Filled.Add) // Not in bottom bar
    object BarcodeScannerForm : Screen("barcode_scanner_form", "Scan Barcode", Icons.Filled.Add) // Not in bottom bar
    object Login : Screen("login", "Login", Icons.Filled.Home) // Not in bottom bar
    object Register : Screen("register", "Register", Icons.Filled.Home) // Not in bottom bar
    object VerifyOtp : Screen("verify_otp", "Verify OTP", Icons.Filled.Home) // Not in bottom bar
    object ForgotPassword : Screen("forgot_password", "Lupa Password", Icons.Filled.Home) // Not in bottom bar
    object ResetPassword : Screen("reset_password/{role}/{identifier}", "Reset Password", Icons.Filled.Home) {
        fun createRoute(role: String, identifier: String) = "reset_password/$role/$identifier"
    }
    object Payment : Screen("payment", "Pembayaran", Icons.Filled.Add) // Not in bottom bar
    object Receipt : Screen("receipt", "Struk", Icons.Filled.Add) // Not in bottom bar
    object SubscriptionPackages : Screen("subscription_packages", "Fitur Pro", Icons.Filled.Add)
    object SubscriptionChannels : Screen("subscription_channels", "Pilih Pembayaran", Icons.Filled.Add)
    object SubscriptionDetail : Screen("subscription_detail/{reference}", "Detail Pembayaran", Icons.Filled.Add) {
        fun createRoute(reference: String) = "subscription_detail/$reference"
    }
}
