package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasirinaja.store.utils.FileUtil
import java.io.File
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject
import com.kasirinaja.store.data.local.AppDatabase
import com.kasirinaja.store.data.repository.ProductRepository
import com.kasirinaja.store.ui.viewmodels.MasterViewModel
import com.kasirinaja.core.network.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterScreen(
    onNavigateToScanner: () -> Unit = {},
    initialSearchQuery: String = ""
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = remember { ProductRepository(database.productDao(), context) }
    val viewModel: MasterViewModel = viewModel(factory = MasterViewModel.Factory(repository))

    val masterProducts by viewModel.masterProducts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var productToEdit by remember { mutableStateOf<JsonObject?>(null) }

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotEmpty()) {
            searchQuery = initialSearchQuery
        }
    }

    // Derived state for filtered products
    val filteredProducts = remember(searchQuery, masterProducts) {
        if (searchQuery.isEmpty()) masterProducts else {
            masterProducts.filter { product ->
                val name = product.get("name")?.asString ?: ""
                val barcode = product.get("barcode")?.asString ?: ""
                name.contains(searchQuery, ignoreCase = true) || barcode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchMasterProducts()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cari nama atau barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onNavigateToScanner) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!errorMessage.isNullOrEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    MasterProductItem(product = product, onAddClick = {
                        productToEdit = product
                    })
                }
            }
        }
    }

    productToEdit?.let { product ->
        AddStoreProductModal(
            product = product,
            onDismiss = { productToEdit = null },
            onSave = { name, category, buyPrice, sellPrice, stock ->
                val storeId = TokenManager(context).getStoreId() ?: ""

                // Set default to "0" if empty because backend expects a numeric string but requires the field
                val safeBuyPrice = buyPrice.ifEmpty { "0" }
                val safeSellPrice = sellPrice.ifEmpty { "0" }

                val masterProductId = product.get("id")?.asString ?: ""
                val barcode = product.get("barcode")?.asString ?: ""
                val photoUrl = product.get("photo_url")?.asString ?: ""
                val description = product.get("description")?.asString ?: ""
                viewModel.addStoreProduct(
                    storeId = storeId,
                    masterProductId = masterProductId,
                    name = name,
                    category = category,
                    buyPrice = safeBuyPrice,
                    sellPrice = safeSellPrice,
                    stock = stock,
                    barcode = barcode,
                    imageUrl = photoUrl,
                    description = description
                )
                productToEdit = null
            }
        )
    }
}

@Composable
fun MasterProductItem(product: JsonObject, onAddClick: () -> Unit) {
    val name = product.get("name")?.asString ?: ""
    val categoryName = product.get("category_name")?.asString ?: product.get("category_id")?.asString ?: ""
    val barcode = product.get("barcode")?.asString ?: ""
    val photoUrl = product.get("photo_url")?.asString

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
            // Photo
            val context = LocalContext.current
            val fileName = if (!photoUrl.isNullOrEmpty()) FileUtil.extractFileNameFromUrl(photoUrl) else ""
            val localFile = if (fileName.isNotEmpty()) FileUtil.getLocalImagePath(context, fileName) else null

            var isLocalExists by remember(fileName) { mutableStateOf(false) }

            LaunchedEffect(fileName) {
                if (fileName.isNotEmpty()) {
                    isLocalExists = withContext(Dispatchers.IO) {
                        FileUtil.isImageExistsLocally(context, fileName)
                    }
                }
            }

            val imageModel = if (isLocalExists) {
                localFile
            } else if (!photoUrl.isNullOrEmpty()) {
                "${com.kasirinaja.core.network.RetrofitClient.IMAGE_BASE_URL}${if(photoUrl.startsWith("/")) photoUrl else "/$photoUrl"}"
            } else {
                null
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 16.dp)
            )

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Kategori: $categoryName", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Barcode: $barcode", style = MaterialTheme.typography.bodySmall)
            }

            // Add Button
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    }
}
