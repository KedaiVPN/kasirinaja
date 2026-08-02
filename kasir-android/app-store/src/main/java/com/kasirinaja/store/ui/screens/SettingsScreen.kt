package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.TokenManager
import com.kasirinaja.store.data.repository.AuthRepository
import com.kasirinaja.store.data.repository.UserRepository
import com.kasirinaja.store.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val authRepository = remember { AuthRepository(RetrofitClient.authApi, tokenManager) }
    val userRepository = remember { UserRepository(RetrofitClient.userApi) }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(authRepository, userRepository))

    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    val currentRole = tokenManager.getRole() ?: "owner"

    LaunchedEffect(Unit) {
        viewModel.fetchUsers()
    }

    LaunchedEffect(error, successMessage) {
        if (error != null || successMessage != null) {
            // Can show snackbar here if scaffold is available, or just rely on dialog/text
        }
    }

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Daftar Karyawan",
                onNavigateToEditProfile = onNavigateToEditProfile,
                onLogout = onLogout
            )
        },
        bottomBar = {
            if (currentRole == "owner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { showAddEmployeeDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Tambah Karyawan",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tambah Karyawan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {


            if (isLoading && users.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users) { user ->
                        val role = user["role"]?.toString() ?: ""
                        val name = user["full_name"]?.toString() ?: ""
                        val id = user["id"]?.toString() ?: ""

                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val photoUrl = user["photo_url"]?.toString()
                                    if (!photoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data("${RetrofitClient.IMAGE_BASE_URL}$photoUrl")
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Foto Karyawan",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "Default Profile",
                                                    modifier = Modifier.size(36.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Role Chip Kustom yang indah
                                        val (chipBg, chipText, roleLabel) = if (role == "owner") {
                                            Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), "Owner")
                                        } else {
                                            Triple(Color(0xFFE3F2FD), Color(0xFF0D47A1), "Kasir")
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = chipBg,
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipText,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tombol Delete (hanya muncul untuk role "kasir" dan jika yang login adalah owner)
                                    if (role == "kasir" && currentRole == "owner") {
                                        var showDeleteConfirm by remember { mutableStateOf(false) }

                                        if (showDeleteConfirm) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteConfirm = false },
                                                title = { Text("Hapus Karyawan") },
                                                text = { Text("Apakah Anda yakin ingin menghapus karyawan $name?") },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            showDeleteConfirm = false
                                                            viewModel.deleteEmployee(
                                                                id = id,
                                                                onSuccess = {
                                                                    android.widget.Toast.makeText(context, "Karyawan berhasil dihapus", android.widget.Toast.LENGTH_SHORT).show()
                                                                },
                                                                onFailure = { errorMsg ->
                                                                    android.widget.Toast.makeText(context, "Gagal menghapus: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                                    ) {
                                                        Text("Hapus", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                                        Text("Batal")
                                                    }
                                                }
                                            )
                                        }

                                        Button(
                                            onClick = { showDeleteConfirm = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Text("Hapus", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
            if (successMessage != null) {
                Text(text = successMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }

    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onAdd = { name, username, phone, role, password ->
                viewModel.addEmployee(name, username, phone, role, password)
                showAddEmployeeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("kasir") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Karyawan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Role dropdown (only kasir for now)
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    label = { Text("Role") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Karyawan") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && username.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {
                        onAdd(name, username, phone, role, password)
                    }
                }
            ) {
                Text("Daftarkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

