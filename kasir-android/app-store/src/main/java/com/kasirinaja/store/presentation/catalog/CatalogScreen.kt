package com.kasirinaja.store.presentation.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel
) {
    val catalogState by viewModel.catalogState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Central Catalog", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        when (catalogState) {
            is CatalogState.Loading -> {
                CircularProgressIndicator()
            }
            is CatalogState.Error -> {
                Text(text = (catalogState as CatalogState.Error).message, color = MaterialTheme.colorScheme.error)
            }
            is CatalogState.Success -> {
                val products = (catalogState as CatalogState.Success).products
                LazyColumn {
                    items(products) { product ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = product["name"]?.toString() ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Barcode: " + product["barcode"])
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    val masterProductId = product["id"]?.toString() ?: return@Button
                                    viewModel.addProductToStore(masterProductId, 0.0, 0.0, 0, 0)
                                }) {
                                    Text("Add to Store")
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
