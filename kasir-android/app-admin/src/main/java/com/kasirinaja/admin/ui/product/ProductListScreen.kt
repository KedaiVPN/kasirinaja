package com.kasirinaja.admin.ui.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.JsonObject

@Composable
fun ProductListScreen(viewModel: ProductListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "List Produk Approved",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        when (uiState) {
            is ProductListState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProductListState.Success -> {
                val products = (uiState as ProductListState.Success).products
                if (products.size() == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada produk approved.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products.size()) { index ->
                            val product = products[index].asJsonObject
                            ProductItem(product)
                        }
                    }
                }
            }
            is ProductListState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as ProductListState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProducts() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: JsonObject) {
    val name = product.get("name")?.asString ?: "Unknown"
    val barcode = product.get("barcode")?.asString ?: "-"
    val photoUrl = if (product.has("photo_url") && !product.get("photo_url").isJsonNull) {
        val urlObj = product.getAsJsonObject("photo_url")
        if (urlObj.has("String")) urlObj.get("String").asString else ""
    } else ""

    val baseUrl = "https://api-go-v1.free-account.my.id"
    val fullImageUrl = if (photoUrl.startsWith("/")) "\$baseUrl\$photoUrl" else photoUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
    }
}
