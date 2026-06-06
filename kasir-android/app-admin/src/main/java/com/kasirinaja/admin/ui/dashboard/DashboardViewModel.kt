package com.kasirinaja.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.JsonObject

data class DashboardStats(
    val approvedCount: Int = 0,
    val pendingCount: Int = 0
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
                    val approved = body?.get("approved_count")?.asInt ?: 0
                    val pending = body?.get("pending_count")?.asInt ?: 0
                    _uiState.value = DashboardState.Success(DashboardStats(approved, pending))
                } else {
                    _uiState.value = DashboardState.Error("HTTP Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to load stats")
            }
        }
    }
}
