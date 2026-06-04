package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductFormState(
    val name: String = "",
    val buyPrice: String = "",
    val sellPrice: String = "",
    val category: String = "",
    val description: String = "",
    val barcode: String = "",
    val stockCount: Int = 0,
    val hasStock: Boolean = false,
    val imageUri: String? = null
)

class AddProductViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    private var hasLoadedInitialData = false

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

    fun updateFormState(update: (ProductFormState) -> ProductFormState) {
        _formState.value = update(_formState.value)
    }

    fun loadProduct(productId: String) {
        if (hasLoadedInitialData) return

        viewModelScope.launch {
            val product = repository.getProductById(productId)
            if (product != null) {
                _formState.value = ProductFormState(
                    name = product.name,
                    buyPrice = product.buyPrice,
                    sellPrice = product.sellPrice,
                    category = product.category,
                    description = product.description ?: "",
                    barcode = product.barcode ?: "",
                    stockCount = if (product.stock == -1) 0 else product.stock,
                    hasStock = product.stock != -1,
                    imageUri = product.imageUrl
                )
            }
            hasLoadedInitialData = true
        }
    }

    fun submitProduct(productId: String?) {
        val state = _formState.value
        if (state.name.isBlank() || state.buyPrice.isBlank() || state.sellPrice.isBlank() || state.category.isBlank()) {
            _errorMessage.value = "Nama, Harga, dan Kategori harus diisi"
            return
        }

        val finalStock = if (state.hasStock) state.stockCount else -1 // -1 signifies unlimited stock per Go schema

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // To properly handle editing, ProductRepository should have an update method.
            // For MVP simplicity and because `insertProduct` in ProductDao uses OnConflictStrategy.REPLACE,
            // we can pass the productId to `addProductLocalAndSync` to overwrite the existing record.
            val result = repository.addProductLocalAndSync(
                existingId = productId,
                name = state.name,
                buyPrice = state.buyPrice,
                sellPrice = state.sellPrice,
                stock = finalStock,
                category = state.category,
                description = state.description,
                barcode = state.barcode,
                imageUrl = state.imageUri ?: ""
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
