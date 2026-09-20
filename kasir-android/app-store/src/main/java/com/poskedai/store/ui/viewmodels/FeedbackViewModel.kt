package com.poskedai.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.poskedai.core.network.TokenManager
import com.poskedai.store.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FeedbackUiState {
    object Idle : FeedbackUiState
    object Loading : FeedbackUiState
    data class Success(val message: String) : FeedbackUiState
    data class Error(val message: String) : FeedbackUiState
}

class FeedbackViewModel(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Idle)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun getUserRole(): String {
        return tokenManager.getRole() ?: "kasir"
    }

    fun getUserEmail(): String {
        return tokenManager.getEmail() ?: ""
    }

    fun sendFeedback(senderEmail: String?, message: String) {
        if (message.isBlank()) {
            _uiState.value = FeedbackUiState.Error("Pesan kritik & saran tidak boleh kosong")
            return
        }

        val isKasir = getUserRole() != "owner"
        if (isKasir) {
            if (senderEmail.isNullOrBlank()) {
                _uiState.value = FeedbackUiState.Error("Email aktif pengirim harus diisi")
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches()) {
                _uiState.value = FeedbackUiState.Error("Format email tidak valid")
                return
            }
        }

        _uiState.value = FeedbackUiState.Loading

        viewModelScope.launch {
            val result = userRepository.sendFeedback(senderEmail, message)
            result.onSuccess { msg ->
                _uiState.value = FeedbackUiState.Success(msg)
            }.onFailure { err ->
                _uiState.value = FeedbackUiState.Error(err.message ?: "Gagal mengirim kritik & saran")
            }
        }
    }

    fun resetState() {
        _uiState.value = FeedbackUiState.Idle
    }

    class Factory(
        private val userRepository: UserRepository,
        private val tokenManager: TokenManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
                return FeedbackViewModel(userRepository, tokenManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
