package com.kasirinaja.store.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CatalogState {
    object Idle : CatalogState()
    object Loading : CatalogState()
    data class Success(val products: List<Map<String, Any>>) : CatalogState()
    data class Error(val message: String) : CatalogState()
}

class CatalogViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Idle)
    val catalogState: StateFlow<CatalogState> = _catalogState

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = CatalogState.Loading
            val result = repository.getCatalogProducts()
            if (result.isSuccess) {
                _catalogState.value = CatalogState.Success(result.getOrDefault(emptyList()))
            } else {
                _catalogState.value = CatalogState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun addProductToStore(masterProductId: String, buyPrice: Double, sellPrice: Double, initialStock: Int, minStock: Int) {
        viewModelScope.launch {
            // Optimistically adding the product, handle UI logic inside Activity/Composable based on flow
            repository.addStoreProduct(masterProductId, buyPrice, sellPrice, initialStock, minStock)
            // Reload local store products later
        }
    }
}

class CatalogViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
