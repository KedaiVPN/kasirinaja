package com.poskedai.store.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.TopAppBar
import com.poskedai.store.ui.components.GlobalTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.poskedai.core.utils.FormatUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poskedai.store.data.local.AppDatabase
import com.poskedai.store.data.repository.ProductRepository
import com.poskedai.store.ui.viewmodels.StockViewModel
import com.poskedai.store.ui.viewmodels.StockViewModelFactory
import coil.compose.AsyncImage
import com.poskedai.store.utils.FileUtil
import java.io.File
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface








import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio


import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current

    val repository = remember {
        val database = AppDatabase.getDatabase(context)
        ProductRepository(database.productDao(), database.transactionDao(), context.applicationContext)
    }

    val viewModel: StockViewModel = viewModel(factory = StockViewModelFactory(repository))
    val products by viewModel.products.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = listOf("Semua"))
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val currentRole = remember { com.poskedai.core.network.TokenManager(context).getRole() ?: "owner" }
    val actionState by viewModel.actionState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<com.poskedai.store.data.local.ProductEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }





    LaunchedEffect(actionState) {
        actionState?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearActionState()
        }
    }

    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                productToDelete = null
            },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus produk ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToDelete?.let {
                            viewModel.deleteProduct(it.id, it.isSynced)
                        }
                        showDeleteDialog = false
                        productToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        productToDelete = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Produk & Stok",
                onNavigateToEditProfile = onNavigateToEditProfile,
                onLogout = onLogout,
                onOpenDrawer = onOpenDrawer
            )
        },

        bottomBar = {
            if (currentRole != "kasir") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { onNavigateToAddProduct() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Tambah Produk",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tambah Produk",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = androidx.compose.ui.graphics.Color.White,
                shadowElevation = 4.dp
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari produk...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }

            // Compact Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Filter Chip
                var categoryExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (selectedCategory == "Semua") "Kategori" else selectedCategory,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Kategori",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.onCategoryChange(cat)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sort Filter Chip
                var sortExpanded by remember { mutableStateOf(false) }
                val sortOptions = listOf("Nama (A-Z)", "Terbaru ditambahkan")
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth().clickable { sortExpanded = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = sortOption,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Urutkan",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortOptions.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.onSortOptionChange(sort)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Daftar Produk Kosong")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductGridCard(
                            product = product,
                            currentRole = currentRole,
                            onEdit = onNavigateToEditProduct,
                            onDelete = {
                                productToDelete = it
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGridCard(
    product: com.poskedai.store.data.local.ProductEntity,
    currentRole: String,
    onEdit: (String) -> Unit,
    onDelete: (com.poskedai.store.data.local.ProductEntity) -> Unit
) {
    val context = LocalContext.current
    val imageModel = remember(product.imageUrl) {
        val url = product.imageUrl
        if (url.isNullOrEmpty()) null
        else if (url.startsWith("content://") || url.startsWith("file://") || url.startsWith("http")) url
        else {
            val fileName = FileUtil.extractFileNameFromUrl(url)
            if (FileUtil.isImageExistsLocally(context, fileName)) {
                FileUtil.getLocalImagePath(context, fileName)
            } else {
                "${com.poskedai.core.network.RetrofitClient.IMAGE_BASE_URL}${if (url.startsWith("/")) url else "/$url"}"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (imageModel == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "No Image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(150)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Kategori: ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = FormatUtils.formatCurrency(product.sellPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (product.stock == -1) "Stok: Unlimited" else "Stok: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (!product.barcode.isNullOrEmpty()) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (product.pendingSync) {
                    Text(
                        text = "Menunggu Sinkronisasi...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (currentRole != "kasir") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(product.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Edit",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Delete Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDelete(product) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
