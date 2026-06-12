package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasirinaja.store.data.local.ProductEntity
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class StockViewModel(private val repository: ProductRepository) : ViewModel() {
    val products: Flow<List<ProductEntity>> = repository.allProducts

    init {
        startSyncLoop()
    }

    private fun startSyncLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    repository.syncStoreProducts()
                    // If sync succeeds, exit loop
                    break
                } catch (e: Exception) {
                    // Sync failed, log error and retry after 1 minute
                    e.printStackTrace()
                    delay(60_000)
                }
            }
        }
    }

    fun deleteProduct(id: String, isSynced: Boolean) {
        viewModelScope.launch {
            repository.deleteProduct(id, isSynced)
        }
    }
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
