package com.kasirinaja.store.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasirinaja.store.ui.components.GlobalTopAppBar
import com.kasirinaja.store.ui.viewmodels.FeedbackUiState
import com.kasirinaja.store.ui.viewmodels.FeedbackViewModel

@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val userRole = viewModel.getUserRole()
    val isOwner = userRole == "owner"

    var emailState by remember { mutableStateOf("") }
    var messageState by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FeedbackUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                messageState = ""
                if (!isOwner) {
                    emailState = ""
                }
                viewModel.resetState()
            }
            is FeedbackUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            GlobalTopAppBar(
                title = "Kritik & Saran",
                onLogout = onLogout,
                onNavigateToEditProfile = onNavigateToEditProfile,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Text(
                text = "Sampaikan Kritik & Saran Anda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Masukan Anda sangat berharga untuk pengembangan aplikasi yang lebih baik.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            if (!isOwner) {
                Text(
                    text = "Email Aktif Pengirim",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = emailState,
                    onValueChange = { emailState = it },
                    placeholder = { Text("Masukkan email aktif Anda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Kritik / Saran",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = messageState,
                onValueChange = { messageState = it },
                placeholder = { Text("Tuliskan kritik atau saran Anda di sini...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            val isLoading = uiState is FeedbackUiState.Loading

            Button(
                onClick = {
                    viewModel.sendFeedback(
                        senderEmail = if (isOwner) null else emailState,
                        message = messageState
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    Text(
                        text = "Kirim Kritik & Saran",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
