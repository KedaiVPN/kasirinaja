package com.kasirinaja.admin.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.CreatePlanRequest
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.SubscriptionPlanDto
import com.kasirinaja.core.network.SubscriptionTransactionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    data class Success(
        val plans: List<SubscriptionPlanDto>,
        val transactions: List<SubscriptionTransactionDto>
    ) : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

class AdminSubscriptionViewModel : ViewModel() {
    private val api = RetrofitClient.subscriptionApi

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                val plansResp = api.adminListPlans()
                val trxsResp = api.adminListTransactions()

                if (plansResp.isSuccessful && trxsResp.isSuccessful) {
                    val plans = plansResp.body()?.plans ?: emptyList()
                    val trxs = trxsResp.body()?.transactions ?: emptyList()
                    _uiState.value = SubscriptionUiState.Success(plans, trxs)
                } else {
                    _uiState.value = SubscriptionUiState.Error("Gagal memuat data dari server")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun createPlan(name: String, durationDays: Int, price: Long, description: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val resp = api.adminCreatePlan(CreatePlanRequest(name, durationDays, price, description, isActive))
                if (resp.isSuccessful) {
                    _actionMessage.value = "Paket berhasil ditambahkan"
                    loadData()
                } else {
                    _actionMessage.value = "Gagal menambah paket: ${resp.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _actionMessage.value = e.message
            }
        }
    }

    fun updatePlan(id: Int, name: String, durationDays: Int, price: Long, description: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val resp = api.adminUpdatePlan(id, CreatePlanRequest(name, durationDays, price, description, isActive))
                if (resp.isSuccessful) {
                    _actionMessage.value = "Paket berhasil diperbarui"
                    loadData()
                } else {
                    _actionMessage.value = "Gagal mengbarui paket: ${resp.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _actionMessage.value = e.message
            }
        }
    }

    fun deletePlan(id: Int) {
        viewModelScope.launch {
            try {
                val resp = api.adminDeletePlan(id)
                if (resp.isSuccessful) {
                    _actionMessage.value = "Paket berhasil dihapus"
                    loadData()
                } else {
                    _actionMessage.value = "Gagal menghapus paket"
                }
            } catch (e: Exception) {
                _actionMessage.value = e.message
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
