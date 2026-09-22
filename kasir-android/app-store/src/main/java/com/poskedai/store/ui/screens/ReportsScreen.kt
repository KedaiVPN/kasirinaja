package com.poskedai.store.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poskedai.core.network.CashierReportDto
import com.poskedai.core.network.RetrofitClient
import com.poskedai.store.data.local.AppDatabase
import com.poskedai.store.ui.components.GlobalTopAppBar
import com.poskedai.store.ui.viewmodels.ReportsViewModel
import com.poskedai.store.utils.ReportExportUtil
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit,
    isPro: Boolean = false,
    onProRequired: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isExporting by remember { mutableStateOf(false) }

    val transactionDao = remember { AppDatabase.getDatabase(context).transactionDao() }

    // Tab 0: Laporan Stok, Tab 1: Riwayat Shift
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Laporan Stok", "Riwayat Shift")
    val stockReports by viewModel.stockReports.collectAsState()
    val isStockLoading by viewModel.isStockLoading.collectAsState()

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            viewModel.fetchStockReport()
        }
    }
    var cashierReports by remember { mutableStateOf<List<CashierReportDto>>(emptyList()) }
    var isLoadingReports by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    val deleteStatus by viewModel.deleteReportStatus.collectAsState()

    var showAddStockDialog by remember { mutableStateOf<String?>(null) }
    var stockToAdd by remember { mutableStateOf("") }

    LaunchedEffect(deleteStatus) {
        deleteStatus?.let { result ->
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("Laporan berhasil dihapus")
                cashierReports = emptyList() // Trigger reload
                isLoadingReports = true
                try {
                    val response = RetrofitClient.reportApi.getStoreReports()
                    if (response.isSuccessful && response.body() != null) {
                        cashierReports = response.body()!!.reports
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoadingReports = false
                }
            } else {
                snackbarHostState.showSnackbar("Gagal menghapus laporan: ${result.exceptionOrNull()?.message}")
            }
            viewModel.resetDeleteReportStatus()
            showDeleteDialog = null
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && cashierReports.isEmpty()) {
            isLoadingReports = true
            try {
                val response = RetrofitClient.reportApi.getStoreReports()
                Log.d("ReportsScreen", "Response: ${response.code()} ${response.message()}")
                if (response.isSuccessful && response.body() != null) {
                    cashierReports = response.body()!!.reports
                } else {
                    snackbarHostState.showSnackbar("Gagal memuat riwayat laporan: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Terjadi kesalahan: ${e.message}")
            } finally {
                isLoadingReports = false
            }
        }
    }

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Laporan Transaksi",
                onLogout = onLogout,
                onNavigateToEditProfile = onNavigateToEditProfile,
                onOpenDrawer = onOpenDrawer
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // Tab 0: Laporan Stok
                StockReportTab(
                    isStockLoading = isStockLoading,
                    stockReports = stockReports,
                    onAddStockClick = { productId ->
                        showAddStockDialog = productId
                        stockToAdd = ""
                    }
                )
            } else if (selectedTabIndex == 1) {
                // Tab 1: Riwayat Shift
                if (isLoadingReports) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (cashierReports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat laporan shift", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
                    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cashierReports, key = { it.id }) { report ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Kasir: ${report.cashier_name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = report.created_at.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val startParsed = runCatching { dateFormatter.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(report.start_time)!!) }.getOrDefault(report.start_time)
                                    val endParsed = runCatching { dateFormatter.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(report.end_time)!!) }.getOrDefault(report.end_time)

                                    Text(text = "Periode: $startParsed - $endParsed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Transaksi: ${report.total_transactions}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Pendapatan: ${currencyFormatter.format(report.total_revenue).replace("Rp", "Rp ")}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Laba: ${currencyFormatter.format(report.total_profit).replace("Rp", "Rp ")}", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isExporting) {
                                                if (!isPro) {
                                                    onProRequired()
                                                    return@clickable
                                                }
                                                isExporting = true
                                                coroutineScope.launch {
                                                    try {
                                                        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                                        val sTime = df.parse(report.start_time.take(19))?.time ?: 0L
                                                        val eTime = df.parse(report.end_time.take(19))?.time ?: System.currentTimeMillis()

                                                        val items = transactionDao.getReportItemsBetweenDates(sTime, eTime)

                                                        val success = ReportExportUtil.exportToPdf(
                                                            context,
                                                            items,
                                                            sTime,
                                                            eTime,
                                                            report.total_revenue.toDouble(),
                                                            report.total_profit.toDouble(),
                                                            report.cashier_name
                                                        )

                                                        if (success) {
                                                            snackbarHostState.showSnackbar("PDF Berhasil disimpan ke Download/pos kedai")
                                                        } else {
                                                            snackbarHostState.showSnackbar("Gagal menyimpan PDF.")
                                                        }
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                                    } finally {
                                                        isExporting = false
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Download PDF", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            if (!isPro) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFFFE082),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "PRO",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE65100),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isExporting) { showDeleteDialog = report.id },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Red.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = Color.Red)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Hapus Laporan", color = Color.Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddStockDialog = null },
            title = { Text("Tambah Stok") },
            text = {
                Column {
                    Text("Masukkan jumlah stok yang ingin ditambahkan:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stockToAdd,
                        onValueChange = { stockToAdd = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    val amount = stockToAdd.toIntOrNull() ?: 0
                    if (amount > 0) {
                        viewModel.addStock(showAddStockDialog!!, amount) { success, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                    showAddStockDialog = null
                }) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Laporan") },
            text = { Text("Apakah Anda yakin ingin menghapus laporan ini?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReport(showDeleteDialog!!, RetrofitClient.reportApi)
                }) {
                    Text("Hapus", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
