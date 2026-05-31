package com.kasirinaja.store.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, passwordHash: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, passwordHash)
            if (result.isSuccess) {
                _authState.value = AuthState.Success("Login successful")
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun registerStore(fullName: String, email: String, phone: String, passwordHash: String, storeName: String, address: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerStore(fullName, email, phone, passwordHash, storeName, address)
            if (result.isSuccess) {
                _authState.value = AuthState.Success(result.getOrDefault("Registered successfully"))
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
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
