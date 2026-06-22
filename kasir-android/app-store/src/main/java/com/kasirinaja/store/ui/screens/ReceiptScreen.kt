package com.kasirinaja.store.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.core.content.FileProvider
import com.kasirinaja.core.network.TokenManager
import com.kasirinaja.core.utils.FormatUtils
import com.kasirinaja.store.ui.viewmodels.ReceiptViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: ReceiptViewModel,
    transactionId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val storeName = tokenManager.getStoreName() ?: "Nama Toko"
    val storeAddress = tokenManager.getStoreAddress() ?: "Alamat Toko"

    val transaction by viewModel.transaction.collectAsState()
    val items by viewModel.items.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // We will render a ComposeView off-screen to capture it as bitmap
    var viewToCapture by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Struk Pembayaran") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Dashboard")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            viewToCapture?.let { view ->
                                val bitmap = view.drawToBitmap()
                                shareBitmap(context, bitmap, transaction?.invoiceNumber ?: "receipt")
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)) // Light gray background for contrast
        ) {
            // This is the container we want to capture
            AndroidView(
                factory = { ctx ->
                    ComposeView(ctx).apply {
                        setContent {
                            ReceiptContent(
                                storeName = storeName,
                                storeAddress = storeAddress,
                                transaction = transaction,
                                items = items
                            )
                        }
                        post {
                            viewToCapture = this
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Tutup & Kembali ke Dashboard")
            }
        }
    }
}

@Composable
fun ReceiptContent(
    storeName: String,
    storeAddress: String,
    transaction: com.kasirinaja.store.data.local.LocalTransactionEntity?,
    items: List<com.kasirinaja.store.data.local.LocalTransactionItemEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = storeName,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = storeAddress,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (transaction != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID Transaksi:", fontSize = 12.sp, color = Color.Gray)
                Text(transaction.invoiceNumber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tanggal:", fontSize = 12.sp, color = Color.Gray)
                val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                Text(sdf.format(Date(transaction.transactionTime)), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray, thickness = 1.dp)

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        text = "${item.quantity}x @ ${FormatUtils.formatCurrency(item.sellPrice.toLong())}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = FormatUtils.formatCurrency((item.sellPrice * item.quantity).toLong()),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray, thickness = 1.dp)

        if (transaction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pesanan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = FormatUtils.formatCurrency(transaction.totalAmount.toLong()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bayar", fontSize = 14.sp)
                Text(text = FormatUtils.formatCurrency(transaction.paidAmount.toLong()), fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kembali", fontSize = 14.sp)
                val kembaliText = if (transaction.changeAmount > 0) {
                    FormatUtils.formatCurrency(transaction.changeAmount.toLong())
                } else {
                    "Uang Pas"
                }
                Text(text = kembaliText, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Terima Kasih", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        // Save to cache directory
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/$fileName.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "$fileName.png")
        val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
