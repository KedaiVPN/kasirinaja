package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import com.kasirinaja.store.ui.viewmodels.ReportsViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
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
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = dateFormat.format(Date(state.startDate))
            val endStr = dateFormat.format(Date(state.endDate))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Periode: $startStr - $endStr",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Ubah")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.transactionData.isNotEmpty()) {
                val chartEntries = state.transactionData.entries.sortedBy { it.key }.mapIndexed { index, entry ->
                    FloatEntry(x = index.toFloat(), y = entry.value.toFloat())
                }

                val chartEntryModel = entryModelOf(chartEntries)

                Text(
                    text = "Jumlah Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Chart(
                    chart = columnChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            val keys = state.transactionData.keys.sorted()
                            val index = value.toInt()
                            if (index >= 0 && index < keys.size) {
                                // Extract DD from YYYY-MM-DD
                                keys[index].takeLast(2)
                            } else {
                                ""
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            } else {
                Text("Tidak ada data transaksi di periode ini.")
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
                    Text("Pilih")
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
