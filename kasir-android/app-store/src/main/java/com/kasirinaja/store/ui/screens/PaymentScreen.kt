package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import com.kasirinaja.core.utils.FormatUtils
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.kasirinaja.store.ui.viewmodels.ScanViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (String) -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val totalAmount = viewModel.getTotalAmount()

    var isExactAmount by remember { mutableStateOf(false) }
    var paidAmountText by remember { mutableStateOf("") }

    val paidAmount = if (isExactAmount) {
        totalAmount
    } else {
        paidAmountText.toDoubleOrNull() ?: 0.0
    }

    val changeAmount = if (paidAmount >= totalAmount) {
        paidAmount - totalAmount
    } else {
        0.0
    }

    val isPaymentValid = paidAmount >= totalAmount
    val context = androidx.compose.ui.platform.LocalContext.current
    val isValid = paidAmount >= totalAmount && totalAmount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pembayaran") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Daftar Pesanan",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(cartItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.product.name, fontWeight = FontWeight.SemiBold)
                            val price = item.product.sellPrice.toLongOrNull() ?: 0L
                            Text(text = FormatUtils.formatCurrency(price))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.decrementQuantity(item.product) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                            }
                            Text(text = item.quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { viewModel.incrementQuantity(item.product) }) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah")
                            }
                            IconButton(onClick = { viewModel.removeProduct(item.product) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                    Divider()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Bayar:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = FormatUtils.formatCurrency(totalAmount.toLong()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isExactAmount,
                    onCheckedChange = { isExactAmount = it }
                )
                Text("Uang Pas")
            }

            if (!isExactAmount) {
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("Jumlah Uang Diterima") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (paidAmountText.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kembalian:")
                        Text(
                            text = FormatUtils.formatCurrency(changeAmount.toLong()),
                            color = if (paidAmount < totalAmount) Color.Red else Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isValid) {
                        val tokenManager = com.kasirinaja.core.network.TokenManager(context)
                        val storeId = tokenManager.getStoreId() ?: ""

                        // Extract user ID from token (or assuming it's available). For now we'll send a valid UUID format
                        // Or we can extract it properly. For this fix, let's parse JWT payload to get cashier/user id
                        val token = tokenManager.getToken() ?: ""
                        var cashierId = ""
                        if (token.isNotEmpty()) {
                            try {
                                val parts = token.split(".")
                                if (parts.size > 1) {
                                    val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                                    val jsonObject = org.json.JSONObject(payload)
                                    cashierId = jsonObject.optString("user_id", "")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Fallback UUIDs if empty to prevent completely breaking while debugging
                        if (storeId.isEmpty()) {
                            android.widget.Toast.makeText(context, "Error: Store ID tidak ditemukan!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (cashierId.isEmpty()) {
                            android.widget.Toast.makeText(context, "Error: Cashier ID tidak ditemukan!", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        viewModel.saveTransaction(
                            paidAmount = paidAmount,
                            changeAmount = changeAmount,
                            storeId = storeId,
                            cashierId = cashierId
                        ) { transactionId ->
                            onPaymentSuccess(transactionId)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isPaymentValid
            ) {
                Text("Selesaikan Pembayaran")
            }
        }
    }
}
