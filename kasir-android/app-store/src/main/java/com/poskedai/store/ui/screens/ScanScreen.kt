package com.poskedai.store.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import com.poskedai.store.ui.components.GlobalTopAppBar
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.poskedai.store.ui.viewmodels.ScanViewModel
import com.poskedai.core.utils.FormatUtils
import com.poskedai.store.utils.FileUtil
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateToPayment: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState(initial = emptyList())
    val totalAmount = viewModel.getTotalAmount()

    var showCamera by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOutOfStockDialog by remember { mutableStateOf(false) }
    var outOfStockMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch {
            viewModel.toastMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        launch {
            viewModel.outOfStockEvent.collect { message ->
                outOfStockMessage = message
                showOutOfStockDialog = true
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }


    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (hasCameraPermission) {
        val filteredProducts = remember(searchQuery, products) {
            if (searchQuery.isEmpty()) products else {
                products.filter { product ->
                    product.name.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        BottomSheetScaffold(
            topBar = {
                GlobalTopAppBar(
                    title = "Transaksi",
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onLogout = onLogout,
                onOpenDrawer = onOpenDrawer
            )
            },
            sheetContent = {
                if (cartItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(max = 400.dp)
                    ) {
                        Text(
                            text = "Keranjang Belanja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(cartItems, key = { it.product.id }) { item ->
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
                                            val isIncrementDisabled = item.product.stock != -1 && item.quantity >= item.product.stock
                                            IconButton(
                                                onClick = { viewModel.incrementQuantity(item.product) },
                                                enabled = !isIncrementDisabled
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Tambah",
                                                    tint = if (isIncrementDisabled) Color.Gray else LocalContentColor.current
                                                )
                                            }
                                            IconButton(onClick = { viewModel.removeProduct(item.product) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                                            }
                                        }
                                    }
                                    Divider()
                                }
                            }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Harga")
                                Text(
                                    text = FormatUtils.formatCurrency(totalAmount.toLong()),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Button(onClick = onNavigateToPayment) {
                                Text("Bayar")
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(0.dp))
                }
            },
            sheetPeekHeight = if (cartItems.isNotEmpty()) 200.dp else 0.dp,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (!showCamera) {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                                imageVector = Icons.Filled.Search,
                                                contentDescription = "Search",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Cari nama produk...",
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
                                        Icons.Filled.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        val cartQuantities = remember(cartItems) {
                            cartItems.associate { it.product.id to it.quantity }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val quantityInCart = cartQuantities[product.id] ?: 0
                                ScanProductRow(
                                    product = product,
                                    quantityInCart = quantityInCart,
                                    onAddToCart = { viewModel.addProductToCart(product) }
                                )
                            }
                        }

                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Scan Barcode") },
                            navigationIcon = {
                                IconButton(onClick = { showCamera = false }) {
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
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalyzer = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        imageAnalyzer.setAnalyzer(cameraExecutor, ContinuousBarcodeAnalyzer { barcodeValue ->
                                            viewModel.onBarcodeScanned(barcodeValue)
                                        })

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalyzer
                                            )
                                        } catch (exc: Exception) {
                                            Log.e("ScanScreen", "Use case binding failed", exc)
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
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Izin Kamera diperlukan untuk menggunakan fitur Scan")
        }
    }
    if (showOutOfStockDialog) {
        AlertDialog(
            onDismissRequest = { showOutOfStockDialog = false },
            title = { Text(text = "Stok Habis") },
            text = { Text(text = outOfStockMessage) },
            confirmButton = {
                Button(
                    onClick = { showOutOfStockDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Tutup", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        )
    }
}

private class ContinuousBarcodeAnalyzer(private val onBarcodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
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

@Composable
private fun ScanProductRow(
    product: com.poskedai.store.data.local.ProductEntity,
    quantityInCart: Int,
    onAddToCart: () -> Unit
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

    val isStockDepleted = product.stock == 0
    val isStockMaxedOut = product.stock != -1 && quantityInCart >= product.stock
    val isBuyDisabled = isStockDepleted || isStockMaxedOut

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
            if (imageModel == null) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "No Image",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(150)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Kategori: ${product.category}", style = MaterialTheme.typography.bodySmall)
                val stockText = remember(product.stock, product.minStock) {
                    androidx.compose.ui.text.buildAnnotatedString {
                        append("Stok: ")
                        if (product.stock == -1) {
                            append("Unlimited")
                        } else if (product.stock == 0) {
                            append("Habis")
                        } else {
                            append(product.stock.toString())
                        }
                    }
                }
                val stockColor = when {
                    product.stock == 0 -> Color.Red
                    product.stock != -1 && product.stock <= product.minStock -> Color(0xFFFFA500)
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = stockText,
                    style = MaterialTheme.typography.bodySmall.copy(color = stockColor)
                )
                Text(
                    text = "Harga: ${FormatUtils.formatCurrency(product.sellPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onAddToCart,
                enabled = !isBuyDisabled,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text("Beli")
            }
        }
    }
}
