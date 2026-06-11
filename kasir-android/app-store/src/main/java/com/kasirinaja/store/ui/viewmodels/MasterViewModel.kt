package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MasterViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _masterProducts = MutableStateFlow<List<JsonObject>>(emptyList())
    val masterProducts: StateFlow<List<JsonObject>> = _masterProducts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchMasterProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val products = repository.getMasterProducts()
                _masterProducts.value = products
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Terjadi kesalahan"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStoreProduct(
        storeId: String,
        masterProductId: String,
        name: String,
        category: String,
        buyPrice: String,
        sellPrice: String,
        stock: Int,
        barcode: String,
        imageUrl: String,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.addStoreProductFromMaster(
                    storeId = storeId,
                    masterProductId = masterProductId,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    stock = stock,
                    minStock = 0,
                    localName = name,
                    localCategory = category,
                    barcode = barcode,
                    imageUrl = imageUrl,
                    description = description
                )
                // Optionally refresh or show success message
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal menambahkan produk"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MasterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MasterViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
