package com.kasirinaja.store.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kasirinaja.store.utils.FileUtil
import java.io.File
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import com.google.gson.JsonObject
import com.kasirinaja.store.data.local.AppDatabase
import com.kasirinaja.store.data.repository.ProductRepository
import com.kasirinaja.store.ui.viewmodels.MasterViewModel
import com.kasirinaja.core.network.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterScreen(
    initialSearchQuery: String = "",
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = AppDatabase.getDatabase(context)
    val repository = remember { ProductRepository(database.productDao(), database.transactionDao(), context) }
    val viewModel: MasterViewModel = viewModel(factory = MasterViewModel.Factory(repository))

    val masterProducts by viewModel.masterProducts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var productToEdit by remember { mutableStateOf<JsonObject?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotEmpty()) {
            searchQuery = initialSearchQuery
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Derived state for filtered products
    val currentRole = remember { com.kasirinaja.core.network.TokenManager(context).getRole() ?: "owner" }
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

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Master",
                onNavigateToEditProfile = onNavigateToEditProfile,
                onLogout = onLogout
            )
        }
    ) { paddingValues ->
        if (showCamera) {
            var cameraProvider by remember { mutableStateOf<androidx.camera.lifecycle.ProcessCameraProvider?>(null) }

            DisposableEffect(lifecycleOwner) {
                onDispose {
                    cameraProvider?.unbindAll()
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Scan Barcode") },
                    navigationIcon = {
                        IconButton(onClick = {
                            cameraProvider?.unbindAll()
                            showCamera = false
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                )
                Box(modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(16.dp))) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                cameraProvider = provider
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalyzer.setAnalyzer(cameraExecutor, MasterContinuousBarcodeAnalyzer { barcodeValue ->
                                    searchQuery = barcodeValue
                                    showCamera = false
                                })

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    Log.e("MasterScreen", "Use case binding failed", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "Arahkan barcode ke area ini",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        color = Color.White
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = androidx.compose.ui.graphics.Color.White,
                shadowElevation = 4.dp
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
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
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari nama atau barcode...",
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
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { showCamera = true }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                        MasterProductItem(product = product, currentRole = currentRole, onAddClick = {
                            productToEdit = product
                        })
                    }
                }
            }
        }
        }
    } // End of Scaffold

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
fun MasterProductItem(product: JsonObject, onAddClick: () -> Unit, currentRole: String) {
    val name = product.get("name")?.asString ?: ""
    val categoryName = product.get("category_name")?.asString ?: product.get("category_id")?.asString ?: ""
    val barcode = product.get("barcode")?.asString ?: ""
    val photoUrl = product.get("photo_url")?.asString

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
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

            val imageModel = remember(fileName) {
                if (fileName.isNotEmpty() && FileUtil.isImageExistsLocally(context, fileName)) {
                    localFile
                } else if (!photoUrl.isNullOrEmpty()) {
                    "${com.kasirinaja.core.network.RetrofitClient.IMAGE_BASE_URL}${if(photoUrl.startsWith("/")) photoUrl else "/$photoUrl"}"
                } else {
                    null
                }
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
            if (currentRole != "kasir") {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                }
            }
        }
    }
}

private class MasterContinuousBarcodeAnalyzer(private val onBarcodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val displayValue = barcodes[0].rawValue
                        if (!displayValue.isNullOrEmpty()) {
                            onBarcodeScanned(displayValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
