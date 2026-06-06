package com.kasirinaja.admin.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.JsonObject

sealed class ProductListState {
    object Loading : ProductListState()
    data class Success(val products: List<JsonObject>) : ProductListState()
    data class Error(val message: String) : ProductListState()
}

class ProductListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProductListState>(ProductListState.Loading)
    val uiState: StateFlow<ProductListState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private val _actionState = MutableStateFlow<String?>(null)
    val actionState: StateFlow<String?> = _actionState.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = ProductListState.Loading
            try {
                val response = RetrofitClient.productApi.getMasterProducts()
                if (response.isSuccessful) {
                    val products = response.body() ?: emptyList()
                    _uiState.value = ProductListState.Success(products)
                } else {
                    _uiState.value = ProductListState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ProductListState.Error(e.message ?: "Failed to load products")
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
                    buy_price = buyPrice,
                    sell_price = sellPrice,
                    stock = stock,
                    description = description,
                    image_url = imageUrl
                )
                val response = RetrofitClient.productApi.updateProduct(id, "approved", request)
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil diupdate"
                    loadProducts()
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


    fun deleteProduct(id: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.productApi.deleteProduct(id, "approved")
                if (response.isSuccessful) {
                    _actionState.value = "Produk berhasil dihapus"
                    loadProducts()
                } else {
                    _actionState.value = "Gagal hapus produk: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionState.value = "Gagal hapus produk: ${e.message}"
            }
        }
    }
}
