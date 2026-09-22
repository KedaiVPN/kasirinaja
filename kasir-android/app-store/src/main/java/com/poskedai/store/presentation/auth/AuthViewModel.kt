package com.poskedai.store.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.poskedai.core.utils.ApiErrorParser
import com.poskedai.store.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class OtpSent(val message: String, val email: String) : AuthState()
    data class ForgotOtpSent(val message: String, val role: String, val identifier: String) : AuthState()
    data class ForgotOtpVerified(val message: String, val role: String, val identifier: String) : AuthState()
    data class PasswordResetSuccess(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                _authState.value = AuthState.Success("Login successful")
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal masuk. Periksa email dan password Anda.")
            }
        }
    }

    fun registerStore(fullName: String, email: String, phone: String, passwordHash: String, storeName: String, address: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerStore(fullName, email, phone, passwordHash, storeName, address)
            if (result.isSuccess) {
                _authState.value = AuthState.OtpSent(result.getOrDefault("OTP Sent"), email)
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal mendaftarkan toko.")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.verifyOtp(email, otp)
            if (result.isSuccess) {
                _authState.value = AuthState.Success("Verification successful")
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal memverifikasi OTP.")
            }
        }
    }

    fun resendOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.resendOtp(email)
            if (result.isSuccess) {
                _authState.value = AuthState.Success("OTP resent successfully")
                _authState.value = AuthState.Idle
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal mengirim ulang OTP.")
            }
        }
    }

    fun forgotPassword(role: String, identifier: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.forgotPassword(role, identifier)
            if (result.isSuccess) {
                _authState.value = AuthState.ForgotOtpSent(
                    message = result.getOrDefault("OTP dikirim"),
                    role = role,
                    identifier = identifier
                )
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal mengirim OTP")
            }
        }
    }

    fun verifyForgotOtp(role: String, identifier: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.verifyForgotOtp(role, identifier, otp)
            if (result.isSuccess) {
                _authState.value = AuthState.ForgotOtpVerified(
                    message = result.getOrDefault("OTP berhasil diverifikasi"),
                    role = role,
                    identifier = identifier
                )
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Kode OTP tidak valid")
            }
        }
    }

    fun resetPassword(role: String, identifier: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.resetPassword(role, identifier, newPassword, confirmPassword)
            if (result.isSuccess) {
                _authState.value = AuthState.PasswordResetSuccess(result.getOrDefault("Password berhasil diperbarui"))
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal mengubah password")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
