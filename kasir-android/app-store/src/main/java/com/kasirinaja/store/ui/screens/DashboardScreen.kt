package com.kasirinaja.store.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Kasir", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Pendapatan",
                        value = "Rp 1.500.000",
                        icon = Icons.Filled.TrendingUp,
                        iconColor = Color(0xFF4CAF50)
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Transaksi",
                        value = "24",
                        icon = Icons.Filled.Receipt,
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Produk",
                        value = "120",
                        icon = Icons.Filled.Inventory,
                        iconColor = Color(0xFFFF9800)
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Laba Bersih",
                        value = "Rp 450.000",
                        icon = Icons.Filled.AttachMoney,
                        iconColor = Color(0xFF2196F3)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(5) { index ->
                TransactionItem(
                    id = "TRX-100${5 - index}",
                    amount = "Rp ${(index + 1) * 25}.000",
                    time = "1${index}:00"
                )
            }
        }
    }
}

@Composable
fun DashboardCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier.height(110.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
