package com.kasirinaja.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardStats(
    val approvedCount: Int = 0,
    val pendingCount: Int = 0,
    val totalStores: Int = 0,
    val proStores: Int = 0,
    val nonProStores: Int = 0
)

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val stats: DashboardStats) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = DashboardState.Loading
            try {
                val response = RetrofitClient.adminApi.getDashboardStats()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.value = DashboardState.Success(
                            DashboardStats(
                                approvedCount = body.approved_count,
                                pendingCount = body.pending_count,
                                totalStores = body.total_stores,
                                proStores = body.pro_stores,
                                nonProStores = body.non_pro_stores
                            )
                        )
                    } else {
                        _uiState.value = DashboardState.Error("Empty response body")
                    }
                } else {
                    _uiState.value = DashboardState.Error("HTTP Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to load stats")
            }
        }
    }
}
