package com.kasirinaja.store.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VerifyOtpScreen(
    viewModel: AuthViewModel,
    email: String,
    onNavigateToLogin: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(300) } // 5 minutes in seconds
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            onNavigateToLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Verifikasi OTP", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Masukkan kode 6 digit yang dikirimkan ke email $email")

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it },
            label = { Text("Kode OTP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { viewModel.verifyOtp(email, otp) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading && otp.length == 6
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Kirim OTP")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        if (timeLeft > 0) {
            Text(text = "Kirim ulang OTP dalam $timeString")
        } else {
            TextButton(
                onClick = {
                    viewModel.resendOtp(email)
                    timeLeft = 300 // Reset timer
                },
                enabled = authState !is AuthState.Loading
            ) {
                Text("Kirim Ulang OTP")
            }
        }
    }
}
