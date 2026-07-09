package com.kasirinaja.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasirinaja.store.ui.viewmodels.EditStoreViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.webkit.MimeTypeMap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kasirinaja.core.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStoreScreen(
    viewModel: EditStoreViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Copy and compress to temp file in background
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (originalBitmap != null) {
                        // Downscale if needed
                        val maxDim = 512f
                        val ratio = Math.min(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
                        val scaledBitmap = if (ratio < 1.0f) {
                            Bitmap.createScaledBitmap(
                                originalBitmap,
                                (originalBitmap.width * ratio).toInt(),
                                (originalBitmap.height * ratio).toInt(),
                                true
                            )
                        } else {
                            originalBitmap
                        }

                        val file = File(context.cacheDir, "temp_logo_compressed.jpg")
                        val outputStream = FileOutputStream(file)
                        // Compress to JPEG to save space
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        outputStream.close()
                        tempImageFile = file
                    }
                }
            }
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Toko") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile/Logo Editor
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "New Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!state.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${RetrofitClient.IMAGE_BASE_URL}${state.logoUrl}")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Current Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Upload Logo",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ubah Logo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.storeName,
                onValueChange = viewModel::onStoreNameChange,
                label = { Text("Nama Toko") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.storeAddress,
                onValueChange = viewModel::onStoreAddressChange,
                label = { Text("Alamat Toko") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.errorMessage != null) {
                Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.updateStore(tempImageFile) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Simpan")
                }
            }
        }
    }
}
