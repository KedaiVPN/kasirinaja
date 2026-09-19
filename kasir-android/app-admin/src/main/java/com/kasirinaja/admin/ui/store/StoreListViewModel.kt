package com.kasirinaja.admin.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.AdminStoreListItemDto
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoreListUiState {
    object Loading : StoreListUiState()
    data class Success(val stores: List<AdminStoreListItemDto>) : StoreListUiState()
    data class Error(val message: String) : StoreListUiState()
}

class StoreListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<StoreListUiState>(StoreListUiState.Loading)
    val uiState: StateFlow<StoreListUiState> = _uiState.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = StoreListUiState.Loading
            try {
                val response = RetrofitClient.adminApi.getStores()
                if (response.isSuccessful) {
                    val stores = response.body() ?: emptyList()
                    _uiState.value = StoreListUiState.Success(stores)
                } else {
                    _uiState.value = StoreListUiState.Error("Gagal memuat daftar toko (Code: ${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = StoreListUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
