package com.kasirinaja.admin.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.JsonArray

sealed class RequestProductState {
    object Loading : RequestProductState()
    data class Success(val products: JsonArray) : RequestProductState()
    data class Error(val message: String) : RequestProductState()
}

class RequestProductViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<RequestProductState>(RequestProductState.Loading)
    val uiState: StateFlow<RequestProductState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<String?>(null)
    val actionState: StateFlow<String?> = _actionState.asStateFlow()

    init {
        loadPendingProducts()
    }

    fun loadPendingProducts() {
        viewModelScope.launch {
            _uiState.value = RequestProductState.Loading
            try {
                val response = RetrofitClient.productApi.getPendingProducts()
                _uiState.value = RequestProductState.Success(response)
            } catch (e: Exception) {
                _uiState.value = RequestProductState.Error(e.message ?: "Failed to load pending products")
            }
        }
    }

    fun approveProduct(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.adminApi.approveProduct(id)
                _actionState.value = "Produk berhasil di-approve"
                loadPendingProducts()
            } catch (e: Exception) {
                _actionState.value = "Gagal approve produk: \${e.message}"
            }
        }
    }

    fun rejectProduct(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.adminApi.rejectProduct(id)
                _actionState.value = "Produk berhasil di-reject"
                loadPendingProducts()
            } catch (e: Exception) {
                _actionState.value = "Gagal reject produk: \${e.message}"
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}
