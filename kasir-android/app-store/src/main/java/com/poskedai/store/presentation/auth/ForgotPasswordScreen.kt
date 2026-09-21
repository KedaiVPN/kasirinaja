package com.poskedai.store.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poskedai.store.R
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToResetPassword: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Role selection: "owner" or "kasir". Default is "owner".
    var isOwnerSelected by remember { mutableStateOf(true) }
    var identifier by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSentStep by remember { mutableStateOf(false) }

    // Countdown resend timer stages: 1m (60s), 2m (120s), 5m (300s)
    val resendIntervals = listOf(60, 120, 300)
    var intervalIndex by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60) }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(isOtpSentStep, timeLeft) {
        if (isOtpSentStep && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.ForgotOtpSent) {
            val state = authState as AuthState.ForgotOtpSent
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            isOtpSentStep = true
            viewModel.resetState()
        } else if (authState is AuthState.ForgotOtpVerified) {
            val state = authState as AuthState.ForgotOtpVerified
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            onNavigateToResetPassword(state.role, state.identifier)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_round),
                    contentDescription = "Logo Aplikasi",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Lupa Password",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Pilih peran dan masukkan akun Anda untuk menerima OTP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Toggle (Kiri: Kasir/Karyawan, Kanan: Owner, Default: Owner)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (!isOwnerSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable(enabled = !isOtpSentStep) {
                            isOwnerSelected = false
                            identifier = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kasir / Karyawan",
                        fontSize = 14.sp,
                        fontWeight = if (!isOwnerSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isOwnerSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isOwnerSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable(enabled = !isOtpSentStep) {
                            isOwnerSelected = true
                            identifier = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Owner Toko",
                        fontSize = 14.sp,
                        fontWeight = if (isOwnerSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOwnerSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isOtpSentStep) {
                        val labelText = if (isOwnerSelected) "Email Owner Toko" else "Username Kasir / Karyawan"
                        val icon = if (isOwnerSelected) Icons.Filled.Email else Icons.Filled.Person

                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text(labelText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val hintText = if (isOwnerSelected) {
                            "Kode OTP akan dikirimkan ke email Anda."
                        } else {
                            "Kode OTP akan dikirimkan ke email Owner toko Anda."
                        }

                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // OTP Step
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = { Text("Kode OTP (6 digit)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val infoMsg = if (isOwnerSelected) {
                            "Kode OTP telah dikirimkan ke email: $identifier"
                        } else {
                            "Silahkan hubungi owner untuk meminta OTP"
                        }

                        Text(
                            text = infoMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val selectedRole = if (isOwnerSelected) "owner" else "kasir"
                            if (!isOtpSentStep) {
                                if (identifier.isBlank()) {
                                    Toast.makeText(context, "Harap isi kolom akun terlebih dahulu", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.forgotPassword(selectedRole, identifier.trim())
                                }
                            } else {
                                if (otpCode.length != 6) {
                                    Toast.makeText(context, "Harap masukkan 6 digit kode OTP", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.verifyForgotOtp(selectedRole, identifier.trim(), otpCode.trim())
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (!isOtpSentStep) "Kirim OTP" else "Verifikasi OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isOtpSentStep) {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (timeLeft > 0) {
                            val minutes = timeLeft / 60
                            val seconds = timeLeft % 60
                            val timeString = String.format("%02d:%02d", minutes, seconds)
                            Text(
                                text = "Kirim ulang OTP dalam $timeString",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    val selectedRole = if (isOwnerSelected) "owner" else "kasir"
                                    viewModel.forgotPassword(selectedRole, identifier.trim())
                                    if (intervalIndex < resendIntervals.lastIndex) {
                                        intervalIndex++
                                    }
                                    timeLeft = resendIntervals[intervalIndex]
                                },
                                enabled = authState !is AuthState.Loading
                            ) {
                                Text(
                                    text = "Kirim Ulang OTP",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Kembali ke Halaman Login",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
