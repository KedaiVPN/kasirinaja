package com.kasirinaja.store.ui.screens

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

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Laporan Transaksi",
                onLogout = onLogout,
                onNavigateToEditProfile = onNavigateToEditProfile,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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
                                    val keys = state.transactionData.keys.sorted()
                                    val index = value.toInt()
                                    if (index >= 0 && index < keys.size) {
                                        keys[index].takeLast(2)
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
