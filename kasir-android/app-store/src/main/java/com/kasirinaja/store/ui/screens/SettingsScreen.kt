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
    onLogout: () -> Unit = {},
    onSwitchSuccess: () -> Unit = {}
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
    var showPasswordDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showConfirmSwitchDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
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
                                .fillMaxWidth()
                                .clickable {
                                    if (role == "owner" && currentRole == "kasir") {
                                        showPasswordDialog = user
                                    } else if (currentRole != role) {
                                        showConfirmSwitchDialog = user
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Default Profile",
                                                modifier = Modifier.size(32.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = role.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
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
            onAdd = { name, phone, role, password ->
                viewModel.addEmployee(name, phone, role, password)
                showAddEmployeeDialog = false
            }
        )
    }

    if (showConfirmSwitchDialog != null) {
        val targetUser = showConfirmSwitchDialog!!
        val targetName = targetUser["full_name"]?.toString() ?: ""
        val targetId = targetUser["id"]?.toString() ?: ""

        AlertDialog(
            onDismissRequest = { showConfirmSwitchDialog = null },
            title = { Text("Konfirmasi Perpindahan") },
            text = { Text("Apakah Anda yakin ingin beralih ke akun $targetName?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSwitchDialog = null
                        viewModel.switchUser(
                            targetUserId = targetId,
                            password = null,
                            onSuccess = {
                                viewModel.clearMessages()
                                android.widget.Toast.makeText(context, "Berhasil beralih ke $targetName", android.widget.Toast.LENGTH_SHORT).show()
                                onSwitchSuccess()
                            },
                            onFailure = { errorMsg ->
                                android.widget.Toast.makeText(context, "Gagal beralih: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Beralih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSwitchDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showPasswordDialog != null) {
        val user = showPasswordDialog!!
        val targetName = user["full_name"]?.toString() ?: ""
        PasswordDialog(
            userName = targetName,
            onDismiss = { showPasswordDialog = null },
            onSubmit = { password ->
                viewModel.switchUser(
                    targetUserId = user["id"]?.toString() ?: "",
                    password = password,
                    onSuccess = {
                        viewModel.clearMessages()
                        android.widget.Toast.makeText(context, "Berhasil beralih ke $targetName", android.widget.Toast.LENGTH_SHORT).show()
                        showPasswordDialog = null
                        onSwitchSuccess()
                    },
                    onFailure = { errorMsg ->
                        android.widget.Toast.makeText(context, "Gagal beralih: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
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
                    label = { Text("Password") },
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
                    if (name.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {
                        onAdd(name, phone, role, password)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDialog(
    userName: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masukkan Password") },
        text = {
            Column {
                Text("Beralih ke akun Owner ($userName) membutuhkan password.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
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
                    if (password.isNotEmpty()) {
                        onSubmit(password)
                    }
                }
            ) {
                Text("Beralih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
