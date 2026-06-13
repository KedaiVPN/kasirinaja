package com.kasirinaja.admin.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.JsonObject

sealed class RequestProductState {
    object Loading : RequestProductState()
    data class Success(val products: List<JsonObject>) : RequestProductState()
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
                if (response.isSuccessful) {
                    val products = response.body() ?: emptyList()
                    _uiState.value = RequestProductState.Success(products)
                } else {
                    _uiState.value = RequestProductState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = RequestProductState.Error(e.message ?: "Failed to load pending products")
            }
        }
    }

    fun approveProduct(id: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.adminApi.approveProduct(id)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil di-approve"
                    loadPendingProducts()
                } else {
                    _actionState.value = "Gagal approve produk: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionState.value = "Gagal approve produk: \${e.message}"
            }
        }
    }

    fun rejectProduct(id: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.adminApi.rejectProduct(id)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil di-reject"
                    loadPendingProducts()
                } else {
                    _actionState.value = "Gagal reject produk: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionState.value = "Gagal reject produk: \${e.message}"
            }
        }
    }

    fun updateProduct(id: String, name: String, category: String, barcode: String, buyPrice: String, sellPrice: String, stock: Int, description: String, imageUrl: String) {
        viewModelScope.launch {
            try {
                val request = com.kasirinaja.core.network.PendingProductRequest(
                    name = name,
                    category = category,
                    barcode = barcode,
                    buy_price = buyPrice.toLongOrNull() ?: 0L,
                    sell_price = sellPrice.toLongOrNull() ?: 0L,
                    stock = stock,
                    description = description,
                    image_url = imageUrl
                )
                val response = RetrofitClient.productApi.updateProduct(id, "pending", request)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil diupdate"
                    loadPendingProducts()
                } else {
                    _actionState.value = "Gagal update produk: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionState.value = "Gagal update produk: ${e.message}"
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}
