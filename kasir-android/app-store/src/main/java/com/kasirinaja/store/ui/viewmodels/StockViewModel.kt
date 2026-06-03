package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasirinaja.store.data.local.ProductEntity
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class StockViewModel(repository: ProductRepository) : ViewModel() {
    val products: Flow<List<ProductEntity>> = repository.allProducts
}

class StockViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
