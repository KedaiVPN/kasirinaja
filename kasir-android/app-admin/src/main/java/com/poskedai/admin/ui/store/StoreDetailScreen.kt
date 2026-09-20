package com.poskedai.admin.ui.store

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poskedai.core.network.AdminStoreDetailDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    onBackClick: () -> Unit,
    viewModel: StoreDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val context = LocalContext.current

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(storeId) {
        viewModel.loadStoreDetail(storeId)
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Toko", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StoreDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is StoreDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadStoreDetail(storeId) }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is StoreDetailUiState.Success -> {
                    val detail = state.detail
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = detail.store_name.ifEmpty { "Tanpa Nama Toko" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ID: ${detail.id}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (detail.is_pro) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = if (detail.is_pro) "PRO" else "NON-PRO",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = if (detail.is_pro) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Detail Information
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Informasi Toko",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Divider()

                                InfoRow(icon = Icons.Default.Person, label = "Nama Owner", value = detail.owner_name)
                                InfoRow(icon = Icons.Default.Email, label = "Email", value = detail.email)
                                InfoRow(icon = Icons.Default.Phone, label = "No. Telepon", value = detail.phone)
                                InfoRow(icon = Icons.Default.Group, label = "Jumlah User Kasir", value = "${detail.cashier_count} Kasir")
                                InfoRow(
                                    icon = Icons.Default.Star,
                                    label = "Status Pro",
                                    value = if (detail.is_pro) "Aktif (Pro)" else "Non-Pro (Gratis)"
                                )
                                InfoRow(
                                    icon = Icons.Default.Badge,
                                    label = "Kadaluarsa Pro",
                                    value = formatProExpiresDate(detail.pro_expires_at)
                                )
                            }
                        }

                        // Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showUpgradeDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upgrade Pro / Tambah Durasi", fontWeight = FontWeight.Bold)
                            }

                            if (detail.is_pro) {
                                OutlinedButton(
                                    onClick = { showDeactivateDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Nonaktifkan Pro", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpgradeDialog) {
        ProUpgradeDialog(
            onDismiss = { showUpgradeDialog = false },
            onConfirm = { days ->
                viewModel.updateProStatus(storeId, days)
                showUpgradeDialog = false
            }
        )
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Nonaktifkan Pro?") },
            text = { Text("Apakah Anda yakin ingin menonaktifkan status Pro toko ini? Status akan langsung menjadi Non-Pro.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProStatus(storeId, 0)
                        showDeactivateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value.ifEmpty { "-" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var daysText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade Status Pro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Masukkan durasi Pro yang ingin diberikan (dalam hari):")
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter { char -> char.isDigit() } },
                    label = { Text("Durasi (Hari)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Preset Pilihan Quick:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60, 90, 365).forEach { days ->
                        FilterChip(
                            selected = daysText == days.toString(),
                            onClick = { daysText = days.toString() },
                            label = { Text("$days H") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toIntOrNull() ?: 0
                    if (days > 0) {
                        onConfirm(days)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

fun formatProExpiresDate(rawDate: String?): String {
    if (rawDate.isNullOrEmpty()) return "Belum/Tidak Pro"
    return try {
        val parts = rawDate.split("T")
        if (parts.isNotEmpty()) {
            val datePart = parts[0]
            val timePart = if (parts.size > 1) parts[1].take(5) else ""
            if (timePart.isNotEmpty()) "$datePart $timePart" else datePart
        } else {
            rawDate
        }
    } catch (e: Exception) {
        rawDate
    }
}
