package com.poskedai.store.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poskedai.core.network.SubscriptionPlanDto
import com.poskedai.core.utils.FormatUtils
import com.poskedai.store.ui.viewmodels.ProSubscriptionState
import com.poskedai.store.ui.viewmodels.StoreSubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPackagesScreen(
    viewModel: StoreSubscriptionViewModel,
    onBack: () -> Unit,
    onNavigateToChannels: () -> Unit
) {
    val proState by viewModel.proState.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProStatusAndPlans()
    }

    val benefits = listOf(
        "Kelola akun Karyawan / Kasir tanpa batas",
        "Akses Laporan Lengkap (Laporan Stok & Riwayat Shift)",
        "Akses Master Produk Global & pengajuan produk baru",
        "Export & Download Laporan Statistik Penjualan (Excel/PDF)",
        "Dukungan prioritas dari tim POS Kedai"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Langganan Fitur Pro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = proState) {
                is ProSubscriptionState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProSubscriptionState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadProStatusAndPlans() }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is ProSubscriptionState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Pro Header Status Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D5040))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (state.isPro) "Status Toko: PRO AKTIF" else "Status Toko: REGULER (NON-PRO)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    if (state.isPro && state.proExpiresAt != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Masa aktif s/d: ${state.proExpiresAt.take(10)}",
                                            color = Color(0xFFE8F5E9),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Manfaat Fitur Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    benefits.forEach { benefit ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(benefit, fontSize = 14.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Pilih Paket Langganan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(state.plans) { plan ->
                            val isSelected = selectedPlan?.id == plan.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPlan(plan) },
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectPlan(plan) }
                                        )
                                    }
                                    Text(
                                        FormatUtils.formatCurrency(plan.price),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Durasi: ${plan.durationDays} Hari", fontSize = 14.sp, color = Color.Gray)
                                    if (plan.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(plan.description, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = onNavigateToChannels,
                                enabled = selectedPlan != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Lanjutkan Ke Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
