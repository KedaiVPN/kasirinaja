package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasirinaja.core.utils.FormatUtils
import com.kasirinaja.store.ui.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kasirinaja.core.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToEditStore: () -> Unit = {},
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    Scaffold(
        topBar = {
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* TODO: Open drawer/menu */ }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color.White, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "POS KEDAI",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ShoppingBasket,
                                contentDescription = "Basket",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Laporan Hari ini",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("Total Penjualan", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                                    Text(": ${FormatUtils.formatCurrency(state.totalRevenue.toLong())}", color = Color.White, fontSize = 12.sp)
                                }
                                Row {
                                    Text("Total Profit", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                                    Text(": ${FormatUtils.formatCurrency(state.netProfit.toLong())}", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                "v1.1.7",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Bottom)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(
                            onClick = { onNavigateToSettings() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black, CircleShape)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                        }
                        IconButton(
                            onClick = { onLogout() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE53935), CircleShape)
                        ) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = state.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, end = 8.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(1.dp, Color.Black)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!state.logoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data("${RetrofitClient.IMAGE_BASE_URL}${state.logoUrl}")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Store Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("logo\ntoko", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = state.storeName,
                                    fontSize = 24.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Divider(color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = state.storeAddress,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    if (state.role.lowercase() == "owner") {
                        IconButton(
                            onClick = { onNavigateToEditStore() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 12.dp, y = (-12).dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .zIndex(1f)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.recentTransactions.size) { index ->
                val tx = state.recentTransactions[index]
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeString = formatter.format(Date(tx.transactionTime))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TransactionItem(
                        id = tx.invoiceNumber,
                        amount = FormatUtils.formatCurrency(tx.totalAmount.toLong()),
                        time = timeString
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TransactionItem(id: String, amount: String, time: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = id, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(text = amount, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 16.sp)
        }
    }
}
