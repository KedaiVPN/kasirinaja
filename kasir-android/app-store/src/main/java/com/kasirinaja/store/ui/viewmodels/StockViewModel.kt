package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasirinaja.store.data.local.ProductEntity
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


class StockViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _sortOption = MutableStateFlow("Nama (A-Z)")
    val sortOption: StateFlow<String> = _sortOption

    private val _actionState = MutableStateFlow<String?>(null)
    val actionState: StateFlow<String?> = _actionState

    val categories: Flow<List<String>> = repository.allProducts.map { products ->
        val cats = products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Semua") + cats
    }

    val products: Flow<List<ProductEntity>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategory,
        _sortOption
    ) { products, query, category, sort ->
        var result = products

        // Filter Search
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                (it.barcode != null && it.barcode.contains(query, ignoreCase = true))
            }
        }

        // Filter Category
        if (category != "Semua") {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Sort
        result = when (sort) {
            "Nama (A-Z)" -> result.sortedBy { it.name.lowercase() }
            "Terbaru ditambahkan" -> result.sortedByDescending { it.createdAt }
            else -> result
        }

        result
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
    }

    fun onSortOptionChange(sort: String) {
        _sortOption.value = sort
    }

    fun clearActionState() {
        _actionState.value = null
    }

    init {
        startSyncLoop()
    }

    private fun startSyncLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    repository.syncStoreProducts()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(60_000)
                }
            }
        }
    }

    fun deleteProduct(id: String, isSynced: Boolean) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(id, isSynced)
                _actionState.value = "Produk berhasil dihapus"
            } catch (e: Exception) {
                _actionState.value = e.message ?: "Gagal menghapus produk"
            }
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
