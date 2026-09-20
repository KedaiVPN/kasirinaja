package com.poskedai.admin.ui.subscription

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poskedai.core.network.SubscriptionPlanDto
import com.poskedai.core.network.SubscriptionTransactionDto
import com.poskedai.core.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubscriptionScreen(
    viewModel: AdminSubscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlanDto?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengelolaan Paket Pro") },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = {
                            editingPlan = null
                            showPlanDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Paket")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Paket Langganan") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat Pembelian") }
                )
            }

            when (val state = uiState) {
                is SubscriptionUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SubscriptionUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadData() }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is SubscriptionUiState.Success -> {
                    if (selectedTab == 0) {
                        PlanListSection(
                            plans = state.plans,
                            onEdit = {
                                editingPlan = it
                                showPlanDialog = true
                            },
                            onDelete = { viewModel.deletePlan(it.id) }
                        )
                    } else {
                        TransactionListSection(transactions = state.transactions)
                    }
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanFormDialog(
            plan = editingPlan,
            onDismiss = { showPlanDialog = false },
            onSave = { name, duration, price, desc, active ->
                if (editingPlan == null) {
                    viewModel.createPlan(name, duration, price, desc, active)
                } else {
                    viewModel.updatePlan(editingPlan!!.id, name, duration, price, desc, active)
                }
                showPlanDialog = false
            }
        )
    }
}

@Composable
fun PlanListSection(
    plans: List<SubscriptionPlanDto>,
    onEdit: (SubscriptionPlanDto) -> Unit,
    onDelete: (SubscriptionPlanDto) -> Unit
) {
    if (plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada paket langganan.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plans) { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(if (plan.isActive) "Aktif" else "Nonaktif") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (plan.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    labelColor = if (plan.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            FormatUtils.formatCurrency(plan.price),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Durasi: ${plan.durationDays} Hari", fontSize = 14.sp, color = Color.Gray)
                        if (plan.description.isNotEmpty()) {
                            Text(plan.description, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEdit(plan) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                            }
                            IconButton(onClick = { onDelete(plan) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListSection(transactions: List<SubscriptionTransactionDto>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada riwayat transaksi.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transactions) { trx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(trx.storeName ?: "Toko", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val (bgColor, textColor) = when (trx.status) {
                                "PAID" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                "UNPAID" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                            }
                            Surface(
                                color = bgColor,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    trx.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Paket: ${trx.planName ?: "-"}", fontSize = 14.sp)
                        Text("Nominal: ${FormatUtils.formatCurrency(trx.amount)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Metode: ${trx.paymentName.ifEmpty { trx.paymentMethod }}", fontSize = 12.sp, color = Color.Gray)
                        Text("Ref: ${trx.reference}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun PlanFormDialog(
    plan: SubscriptionPlanDto?,
    onDismiss: () -> Unit,
    onSave: (name: String, durationDays: Int, price: Long, description: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var durationText by remember { mutableStateOf(plan?.durationDays?.toString() ?: "30") }
    var priceText by remember { mutableStateOf(plan?.price?.toString() ?: "20000") }
    var description by remember { mutableStateOf(plan?.description ?: "") }
    var isActive by remember { mutableStateOf(plan?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "Tambah Paket Pro" else "Edit Paket Pro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Paket") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Durasi (Hari)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Harga (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan / Manfaat") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Paket Aktif")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 0
                    val price = priceText.toLongOrNull() ?: 0L
                    if (name.isNotEmpty() && duration > 0 && price > 0) {
                        onSave(name, duration, price, description, isActive)
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
