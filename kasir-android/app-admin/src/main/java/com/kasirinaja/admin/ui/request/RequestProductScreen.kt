package com.kasirinaja.admin.ui.request

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.JsonObject

@Composable
fun RequestProductScreen(viewModel: RequestProductViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(actionState) {
        actionState?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Request Produk (Pending)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        when (uiState) {
            is RequestProductState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RequestProductState.Success -> {
                val products = (uiState as RequestProductState.Success).products
                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada request produk saat ini.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products.size) { index ->
                            val product = products[index]
                            PendingProductItem(
                                product = product,
                                onApprove = { id -> viewModel.approveProduct(id) },
                                onReject = { id -> viewModel.rejectProduct(id) }
                            )
                        }
                    }
                }
            }
            is RequestProductState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as RequestProductState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPendingProducts() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingProductItem(
    product: JsonObject,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val id = product.get("id")?.asString ?: ""
    val name = product.get("name")?.asString ?: "Unknown"
    val barcode = product.get("barcode")?.asString ?: "-"
    val photoUrl = if (product.has("image_url") && !product.get("image_url").isJsonNull) {
        val urlObj = product.getAsJsonObject("image_url")
        if (urlObj.has("String")) urlObj.get("String").asString else ""
    } else ""

    val baseUrl = "https://api-go-v1.free-account.my.id"
    val fullImageUrl = if (photoUrl.startsWith("/")) "\$baseUrl\$photoUrl" else photoUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = fullImageUrl.ifEmpty { "https://via.placeholder.com/150" },
                    contentDescription = name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Barcode: \$barcode",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onReject(id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onApprove(id) }) {
                    Text("Approve")
                }
            }
        }
    }
}
