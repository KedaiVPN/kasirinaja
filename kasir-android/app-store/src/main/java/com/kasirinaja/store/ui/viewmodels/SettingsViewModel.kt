package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.repository.AuthRepository
import com.kasirinaja.store.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val users: StateFlow<List<Map<String, Any>>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.getStoreUsers()
            result.onSuccess {
                _users.value = it
            }.onFailure {
                _error.value = it.message ?: "Failed to fetch users"
            }
            _isLoading.value = false
        }
    }

    fun addEmployee(name: String, phone: String, role: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.addStoreEmployee(name, phone, role, password)
            result.onSuccess {
                _successMessage.value = "Karyawan berhasil ditambahkan"
                fetchUsers()
            }.onFailure {
                _error.value = it.message ?: "Gagal menambahkan karyawan"
            }
            _isLoading.value = false
        }
    }

    fun switchUser(targetUserId: String, password: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.switchUser(targetUserId, password)
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _error.value = it.message ?: "Gagal beralih akun"
            }
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(authRepository, userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
