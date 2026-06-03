package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddProductViewModel(private val repository: ProductRepository) : ViewModel() {
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

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.addProductLocalAndSync(
                name = name,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = finalStock,
                category = category,
                description = description,
                barcode = barcode,
                imageUrl = imageUrl
            )

            if (result.isSuccess) {
                _isSuccess.value = true
            } else {
                _errorMessage.value = "Gagal menyimpan produk."
            }
            _isLoading.value = false
        }
    }
}

class AddProductViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
