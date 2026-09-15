package com.kasirinaja.store.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kasirinaja.core.network.TripayInstructionDto
import com.kasirinaja.core.utils.FormatUtils
import com.kasirinaja.store.ui.viewmodels.StoreSubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    reference: String,
    viewModel: StoreSubscriptionViewModel,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val transaction by viewModel.transactionDetail.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val checkoutResult by viewModel.checkoutResult.collectAsState()

    LaunchedEffect(reference) {
        viewModel.fetchTransactionDetail(reference)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pembayaran Pro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
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
            if (transaction == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val trx = transaction!!
                val isPaid = trx.status == "PAID"

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isPaid) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Pembayaran Berhasil!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        "Fitur Pro toko Anda telah aktif.",
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                } else {
                                    Text(
                                        "Status: ${trx.status}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Silakan lakukan pembayaran sebelum batas waktu berakhir.",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Payment Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Ringkasan Tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Divider(modifier = Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ref Transaksi", color = Color.Gray, fontSize = 14.sp)
                                    Text(trx.reference, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Metode Pembayaran", color = Color.Gray, fontSize = 14.sp)
                                    Text(trx.paymentName.ifEmpty { trx.paymentMethod }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Tagihan", color = Color.Gray, fontSize = 14.sp)
                                    Text(
                                        FormatUtils.formatCurrency(trx.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (!isPaid) {
                                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Display Code / QR
                                    val payCode = trx.payCode
                                    if (!payCode.isNullOrEmpty()) {
                                        Text("Kode Bayar / Virtual Account:", fontSize = 13.sp, color = Color.Gray)
                                        Text(
                                            payCode,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    if (!trx.qrUrl.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Scan QRIS Berikut:", fontSize = 13.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(trx.qrUrl),
                                                contentDescription = "QR Code",
                                                modifier = Modifier.size(220.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isPaid) {
                        item {
                            Button(
                                onClick = { viewModel.fetchTransactionDetail(reference) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cek Status Pembayaran", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onBackToDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Kembali Ke Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
