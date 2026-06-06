package com.kasirinaja.admin.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.JsonArray

sealed class ProductListState {
    object Loading : ProductListState()
    data class Success(val products: JsonArray) : ProductListState()
    data class Error(val message: String) : ProductListState()
}

class ProductListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProductListState>(ProductListState.Loading)
    val uiState: StateFlow<ProductListState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = ProductListState.Loading
            try {
                val response = RetrofitClient.productApi.getMasterProducts()
                _uiState.value = ProductListState.Success(response)
            } catch (e: Exception) {
                _uiState.value = ProductListState.Error(e.message ?: "Failed to load products")
            }
        }
    }
}
