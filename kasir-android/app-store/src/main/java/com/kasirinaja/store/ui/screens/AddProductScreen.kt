package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.kasirinaja.store.utils.BarcodeUtils
import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kasirinaja.store.ui.viewmodels.AddProductViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var productName by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var barcodeValue by remember { mutableStateOf("") }

    var hasStock by remember { mutableStateOf(false) }
    var stockCount by remember { mutableStateOf(0) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var generatedBarcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            Toast.makeText(context, "Produk berhasil diajukan untuk verifikasi", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            onNavigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Produk") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Text("Foto dipilih: ${imageUri?.lastPathSegment}", color = MaterialTheme.colorScheme.primary)
                    // TODO: Replace with coil-compose AsyncImage for actual rendering
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, contentDescription = "Pilih Foto")
                        Text("Pilih Foto Produk")
                    }
                }
            }

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("Harga Beli") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sellPrice,
                    onValueChange = { sellPrice = it },
                    label = { Text("Harga Jual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori Produk") },
                modifier = Modifier.fillMaxWidth()
            )

            // Stock Toggle Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Kelola Stok", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = hasStock, onCheckedChange = { hasStock = it })
                    }

                    if (hasStock) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(onClick = { if (stockCount > 0) stockCount-- }) {
                                Text("-")
                            }
                            OutlinedTextField(
                                value = stockCount.toString(),
                                onValueChange = { stockCount = it.toIntOrNull() ?: 0 },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { stockCount++ }) {
                                Text("+")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stok tidak terbatas (unlimited)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Barcode Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Barcode Produk", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcodeValue,
                            onValueChange = { barcodeValue = it },
                            label = { Text("Kode Barcode") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                barcodeValue = UUID.randomUUID().toString().substring(0, 12).uppercase()
                                generatedBarcodeBitmap = BarcodeUtils.generateBarcode(barcodeValue, 400, 150)
                            }
                        ) {
                            Text("Generate")
                        }
                    }

                    if (generatedBarcodeBitmap != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            bitmap = generatedBarcodeBitmap!!.asImageBitmap(),
                            contentDescription = "Generated Barcode",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentScale = ContentScale.Fit
                        )
                        TextButton(
                            onClick = { /* TODO: Save bitmap to local storage */ },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Simpan Barcode ke HP")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.submitProduct(
                        name = productName,
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        stockCount = stockCount,
                        hasStock = hasStock,
                        category = category,
                        description = description,
                        barcode = barcodeValue,
                        imageUrl = imageUri?.toString() ?: "" // Temporary handling
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Simpan Produk")
                }
            }
        }
    }
}
