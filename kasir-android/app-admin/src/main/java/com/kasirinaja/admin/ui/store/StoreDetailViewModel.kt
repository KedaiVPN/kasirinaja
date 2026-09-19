package com.kasirinaja.admin.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.AdminStoreDetailDto
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.UpdateProRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoreDetailUiState {
    object Loading : StoreDetailUiState()
    data class Success(val detail: AdminStoreDetailDto) : StoreDetailUiState()
    data class Error(val message: String) : StoreDetailUiState()
}

class StoreDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<StoreDetailUiState>(StoreDetailUiState.Loading)
    val uiState: StateFlow<StoreDetailUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun loadStoreDetail(storeId: String) {
        viewModelScope.launch {
            _uiState.value = StoreDetailUiState.Loading
            try {
                val response = RetrofitClient.adminApi.getStoreDetail(storeId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = StoreDetailUiState.Success(response.body()!!)
                } else {
                    _uiState.value = StoreDetailUiState.Error("Gagal memuat detail toko (Code: ${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = StoreDetailUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun updateProStatus(storeId: String, days: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.adminApi.updateStoreProStatus(storeId, UpdateProRequestDto(days))
                if (response.isSuccessful) {
                    _actionMessage.value = if (days > 0) "Status Pro berhasil diperbarui ($days Hari)" else "Status Pro berhasil dinonaktifkan"
                    loadStoreDetail(storeId)
                } else {
                    _actionMessage.value = "Gagal memperbarui status Pro (Code: ${response.code()})"
                }
            } catch (e: Exception) {
                _actionMessage.value = e.message ?: "Gagal memperbarui status Pro"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
