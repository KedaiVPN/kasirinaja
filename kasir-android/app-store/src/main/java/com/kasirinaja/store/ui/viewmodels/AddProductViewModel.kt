package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.PendingProductRequest
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddProductViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }

    fun submitProduct(
        name: String,
        buyPrice: String,
        sellPrice: String,
        stockCount: Int,
        hasStock: Boolean,
        category: String,
        description: String,
        barcode: String,
        imageUrl: String
    ) {
        if (name.isBlank() || buyPrice.isBlank() || sellPrice.isBlank() || category.isBlank()) {
            _errorMessage.value = "Nama, Harga, dan Kategori harus diisi"
            return
        }

        val finalStock = if (hasStock) stockCount else -1 // -1 signifies unlimited stock per Go schema

        val request = PendingProductRequest(
            name = name,
            buy_price = buyPrice,
            sell_price = sellPrice,
            stock = finalStock,
            category = category,
            description = description,
            barcode = barcode,
            image_url = imageUrl // TODO: Local image upload handling before this step
        )

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.productApi.submitPendingProduct(request)
                if (response.isSuccessful) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value = "Gagal menyimpan produk: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan koneksi: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
