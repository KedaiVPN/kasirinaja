package com.kasirinaja.store.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    reference: String,
    viewModel: StoreSubscriptionViewModel,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val transaction by viewModel.transactionDetail.collectAsState()
    val instructions by viewModel.paymentInstructions.collectAsState()
    val feeCalculatorData by viewModel.feeCalculatorData.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var remainingTimeText by remember { mutableStateOf("24:00:00") }

    LaunchedEffect(reference) {
        viewModel.fetchTransactionDetail(reference)
    }

    LaunchedEffect(transaction) {
        val trx = transaction
        if (trx != null && trx.status != "PAID") {
            var targetEpoch = parseToEpochMillis(trx.expiresAt)
            if (targetEpoch == null || targetEpoch <= System.currentTimeMillis()) {
                val createdEpoch = parseToEpochMillis(trx.createdAt)
                if (createdEpoch != null) {
                    targetEpoch = createdEpoch + (24 * 3600 * 1000L)
                }
            }
            if (targetEpoch == null || targetEpoch <= System.currentTimeMillis()) {
                targetEpoch = System.currentTimeMillis() + (24 * 3600 * 1000L)
            }

            while (isActive) {
                val now = System.currentTimeMillis()
                val diff = targetEpoch - now
                if (diff <= 0) {
                    remainingTimeText = "Waktu Habis"
                    break
                } else {
                    val hours = diff / (1000 * 3600)
                    val minutes = (diff % (1000 * 3600)) / (1000 * 60)
                    val seconds = (diff % (1000 * 60)) / 1000
                    remainingTimeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
                delay(1000L)
            }
        }
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
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFFE0B2),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Batas Waktu: $remainingTimeText",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFD84315),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ref Transaksi", color = Color.Gray, fontSize = 14.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(trx.reference))
                                            Toast.makeText(context, "Ref transaksi berhasil disalin", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(trx.reference, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Salin Ref Transaksi",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Metode Pembayaran", color = Color.Gray, fontSize = 14.sp)
                                    Text(trx.paymentName.ifEmpty { trx.paymentMethod }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val flatFee = feeCalculatorData?.fee?.flat ?: 0L
                                val percentDouble = when (val p = feeCalculatorData?.fee?.percent) {
                                    is Number -> p.toDouble()
                                    is String -> p.replace("%", "").toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val percentStr = if (percentDouble % 1.0 == 0.0) {
                                    percentDouble.toLong().toString()
                                } else {
                                    percentDouble.toString()
                                }
                                val hasFlat = flatFee > 0
                                val hasPercent = percentDouble > 0.0
                                val feeText = when {
                                    hasFlat && hasPercent -> "${FormatUtils.formatCurrency(flatFee)} + $percentStr%"
                                    hasFlat -> FormatUtils.formatCurrency(flatFee)
                                    hasPercent -> "$percentStr%"
                                    else -> FormatUtils.formatCurrency(0)
                                }

                                val totalFee = (feeCalculatorData?.totalFee?.customer ?: 0L) + (feeCalculatorData?.totalFee?.merchant ?: 0L)
                                val nominalPokok = if (totalFee > 0 && trx.amount > totalFee) trx.amount - totalFee else trx.amount

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Nominal Pokok", color = Color.Gray, fontSize = 14.sp)
                                    Text(FormatUtils.formatCurrency(nominalPokok), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (feeCalculatorData != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Fee", color = Color.Gray, fontSize = 14.sp)
                                        Text(feeText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (totalFee > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Total Fee", color = Color.Gray, fontSize = 14.sp)
                                            Text(FormatUtils.formatCurrency(totalFee), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Tagihan", color = Color.Gray, fontSize = 14.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(trx.amount.toString()))
                                            Toast.makeText(context, "Nominal tagihan berhasil disalin", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            FormatUtils.formatCurrency(trx.amount),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Salin Total Tagihan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (!isPaid) {
                                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Display Code / QR
                                    val payCode = trx.payCode
                                    if (!payCode.isNullOrEmpty()) {
                                        Text("Kode Bayar / Virtual Account:", fontSize = 13.sp, color = Color.Gray)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                payCode,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 22.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(payCode))
                                                    Toast.makeText(context, "Kode bayar berhasil disalin", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Salin Kode Bayar",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
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

                    if (!isPaid && instructions.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Instruksi Pembayaran",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    instructions.forEach { instruction ->
                                        InstructionAccordionItem(instruction = instruction)
                                        Spacer(modifier = Modifier.height(8.dp))
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

private fun parseToEpochMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrEmpty()) return null
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            sdf.parse(dateStr)?.time
        }
    } catch (e: Exception) {
        try {
            dateStr.toLong()
        } catch (e2: Exception) {
            null
        }
    }
}

@Composable
fun InstructionAccordionItem(instruction: TripayInstructionDto) {
    var isExpanded by remember { mutableStateOf(false) }
    val titleText = when (val title = instruction.title) {
        is String -> title
        is Map<*, *> -> (title["id"] ?: title["en"] ?: title.toString()).toString()
        else -> instruction.title?.toString() ?: "Instruksi Pembayaran"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Tutup" else "Buka"
                )
            }

            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                instruction.steps.forEachIndexed { index, stepHtml ->
                    val cleanStep = android.text.Html.fromHtml(stepHtml, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = cleanStep,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
