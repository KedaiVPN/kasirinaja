package com.kasirinaja.store.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.kasirinaja.core.network.PaymentChannelDto
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.utils.FormatUtils
import com.kasirinaja.store.ui.viewmodels.ChannelsUiState
import com.kasirinaja.store.ui.viewmodels.StoreSubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionChannelsScreen(
    viewModel: StoreSubscriptionViewModel,
    onBack: () -> Unit,
    onCheckoutSuccess: (reference: String) -> Unit
) {
    val channelsUiState by viewModel.channelsUiState.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    var selectedChannelCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (channelsUiState is ChannelsUiState.Loading) {
            viewModel.loadPaymentChannels()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Metode Pembayaran", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Selected Plan Summary Header
            selectedPlan?.let { plan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Paket Dipilih", fontSize = 12.sp, color = Color.Gray)
                            Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            FormatUtils.formatCurrency(plan.price),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            when (val state = channelsUiState) {
                is ChannelsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChannelsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Text(state.message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadPaymentChannels() }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is ChannelsUiState.Success -> {
                    val channels = state.channels
                    val groupedChannels = channels.groupBy { it.group }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedChannels.forEach { (groupName, channelList) ->
                            item {
                                Text(
                                    groupName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            items(channelList) { channel ->
                                val isSelected = selectedChannelCode == channel.code
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedChannelCode = channel.code },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (!channel.iconUrl.isNullOrEmpty()) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(channel.iconUrl),
                                                    contentDescription = channel.name,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            Column {
                                                Text(channel.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                                Text(channel.type, fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedChannelCode = channel.code }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    if (selectedChannelCode.isNotEmpty()) {
                                        viewModel.checkout(selectedChannelCode, onCheckoutSuccess)
                                    }
                                },
                                enabled = selectedChannelCode.isNotEmpty() && !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Bayar Sekarang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
