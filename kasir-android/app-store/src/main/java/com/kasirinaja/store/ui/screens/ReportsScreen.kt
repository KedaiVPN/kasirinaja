package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import java.text.NumberFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Divider
import com.kasirinaja.core.network.RetrofitClient
import android.util.Log
import com.kasirinaja.core.network.CashierReportDto
import java.util.TimeZone

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.kasirinaja.store.utils.ReportExportUtil
import com.kasirinaja.store.data.local.AppDatabase
import com.kasirinaja.store.data.local.ReportItem


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import com.kasirinaja.store.ui.viewmodels.ReportsViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.kasirinaja.store.ui.components.rememberMarker
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.compose.component.textComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = state.startDate,
        initialSelectedEndDateMillis = state.endDate
    )


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isExporting by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("XLS") }
    val formatOptions = listOf("PDF", "XLS")
    var formatExpanded by remember { mutableStateOf(false) }

    val transactionDao = remember { AppDatabase.getDatabase(context).transactionDao() }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Grafik Penjualan", "Riwayat Shift")
    var cashierReports by remember { mutableStateOf<List<CashierReportDto>>(emptyList()) }
    var isLoadingReports by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(state.startDate))
            val endStr = dateFormat.format(Date(state.endDate))
            val totalTransactions = state.transactionData.values.sum()

            // Header Section: Date Filter Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true },
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Periode Laporan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$startStr - $endStr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Pilih Tanggal",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Export Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format Selector Chip
                Box(modifier = Modifier.weight(0.4f)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { formatExpanded = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = selectedFormat,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Pilih Format",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        formatOptions.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedFormat = format
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                // Download Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(0.6f)
                        .height(44.dp)
                        .clickable(enabled = !isExporting) {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val items = transactionDao.getReportItemsBetweenDates(state.startDate, state.endDate)

                                        val success = if (selectedFormat == "PDF") {
                                            ReportExportUtil.exportToPdf(context, items, state.startDate, state.endDate, state.totalRevenue, state.netProfit)
                                        } else {
                                            ReportExportUtil.exportToXlsx(context, items, state.startDate, state.endDate, state.totalRevenue, state.netProfit)
                                        }

                                        if (success) {
                                            snackbarHostState.showSnackbar("Berhasil disimpan ke Download/pos kedai")
                                        } else {
                                            snackbarHostState.showSnackbar("Gagal menyimpan laporan.")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal menyimpan laporan: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "Unduh Laporan",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Unduh Laporan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Section
            val currencyFormatter = remember {
                NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pendapatan Kotor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormatter.format(state.totalRevenue).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pendapatan Bersih",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormatter.format(state.netProfit).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grafik Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "Total: $totalTransactions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.transactionData.isNotEmpty()) {
                        val sortedKeys = remember(state.transactionData) { state.transactionData.keys.sorted() }
                        val chartEntries = state.transactionData.entries.sortedBy { it.key }.mapIndexed { index, entry ->
                            FloatEntry(x = index.toFloat(), y = entry.value.toFloat())
                        }

                        val chartEntryModel = entryModelOf(chartEntries)
                        val marker = rememberMarker()
                        val primaryColor = MaterialTheme.colorScheme.primary

                        val axisLabelColor = Color(0xFF495057) // Dark gray for visibility
                        val axisLabelStyle = textComponent(
                            color = axisLabelColor,
                            textSize = 10.sp,
                            typeface = android.graphics.Typeface.DEFAULT
                        )

                        val axisLineStyle = lineComponent(
                            color = Color(0xFFDEE2E6), // Light gray for grid
                            thickness = 1.dp
                        )

                        Chart(
                            chart = columnChart(
                                columns = listOf(
                                    LineComponent(
                                        color = primaryColor.toArgb(),
                                        thicknessDp = 6f, // Thinner bar to fit more in view
                                        shape = Shapes.roundedCornerShape(allPercent = 25)
                                    )
                                ),
                                spacing = 4.dp // Closer bars to fit 30 days
                            ),
                            model = chartEntryModel,
                            marker = marker,
                            startAxis = rememberStartAxis(
                                label = axisLabelStyle,
                                axis = null,
                                tick = null,
                                guideline = axisLineStyle,
                                valueFormatter = { value, _ -> value.toInt().toString() },
                                itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
                            ),
                            bottomAxis = rememberBottomAxis(
                                label = axisLabelStyle,
                                axis = axisLineStyle,
                                tick = null,
                                guideline = null, // No vertical grid lines
                                valueFormatter = { value, _ ->
                                    val index = value.toInt()
                                    if (index >= 0 && index < sortedKeys.size) {
                                        sortedKeys[index].takeLast(2)
                                    } else {
                                        ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "← Geser grafik →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data transaksi di periode ini.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            } // End of Column for Tab 0
            } else {
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
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cashierReports) { report ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isExporting) {
                                            isExporting = true
                                            coroutineScope.launch {
                                                try {
                                                    // Need to convert date format to long
                                                    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                                    val sTime = df.parse(report.start_time.take(19))?.time ?: 0L
                                                    val eTime = df.parse(report.end_time.take(19))?.time ?: System.currentTimeMillis()

                                                    // Fetch items for that range
                                                    val items = transactionDao.getReportItemsBetweenDates(sTime, eTime)

                                                    val success = ReportExportUtil.exportToPdf(
                                                        context,
                                                        items,
                                                        sTime,
                                                        eTime,
                                                        report.total_revenue.toDouble(),
                                                        report.total_profit.toDouble()
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
                                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Download PDF", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.updateDateRange(start, end)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
