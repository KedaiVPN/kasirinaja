package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var showPasswordDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
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
                title = "Pengaturan Users",
                onNavigateToEditProfile = onNavigateToEditProfile,
                onLogout = onLogout
            )
        },
        floatingActionButton = {
            if (currentRole == "owner") {
                FloatingActionButton(onClick = { showAddEmployeeDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Karyawan")
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
                                    } else if (currentRole != role || role != currentRole) {
                                        viewModel.switchUser(id, null) {
                                            viewModel.clearMessages()
                                            showAddEmployeeDialog = false
                                            viewModel.fetchUsers()
                                            // Optional: Show success, or reload context
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
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

    if (showPasswordDialog != null) {
        val user = showPasswordDialog!!
        PasswordDialog(
            userName = user["full_name"]?.toString() ?: "",
            onDismiss = { showPasswordDialog = null },
            onSubmit = { password ->
                viewModel.switchUser(user["id"]?.toString() ?: "", password) {
                    viewModel.clearMessages()
                    viewModel.fetchUsers()
                    showPasswordDialog = null
                }
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
