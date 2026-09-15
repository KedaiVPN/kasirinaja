package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProSubscriptionState {
    object Loading : ProSubscriptionState()
    data class Success(
        val plans: List<SubscriptionPlanDto>,
        val isPro: Boolean,
        val proExpiresAt: String?
    ) : ProSubscriptionState()
    data class Error(val message: String) : ProSubscriptionState()
}

class StoreSubscriptionViewModel : ViewModel() {
    private val api = RetrofitClient.subscriptionApi

    private val _proState = MutableStateFlow<ProSubscriptionState>(ProSubscriptionState.Loading)
    val proState: StateFlow<ProSubscriptionState> = _proState.asStateFlow()

    private val _channelsState = MutableStateFlow<List<PaymentChannelDto>>(emptyList())
    val channelsState: StateFlow<List<PaymentChannelDto>> = _channelsState.asStateFlow()

    private val _selectedPlan = MutableStateFlow<SubscriptionPlanDto?>(null)
    val selectedPlan: StateFlow<SubscriptionPlanDto?> = _selectedPlan.asStateFlow()

    private val _checkoutResult = MutableStateFlow<CheckoutResponse?>(null)
    val checkoutResult: StateFlow<CheckoutResponse?> = _checkoutResult.asStateFlow()

    private val _transactionDetail = MutableStateFlow<SubscriptionTransactionDto?>(null)
    val transactionDetail: StateFlow<SubscriptionTransactionDto?> = _transactionDetail.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProStatusAndPlans()
    }

    fun loadProStatusAndPlans() {
        viewModelScope.launch {
            _proState.value = ProSubscriptionState.Loading
            try {
                val resp = api.getPlans()
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _proState.value = ProSubscriptionState.Success(
                        plans = body.plans,
                        isPro = body.isPro,
                        proExpiresAt = body.proExpiresAt
                    )
                } else {
                    _proState.value = ProSubscriptionState.Error("Gagal memuat paket langganan")
                }
            } catch (e: Exception) {
                _proState.value = ProSubscriptionState.Error(e.message ?: "Terjadi kesalahan koneksi")
            }
        }
    }

    fun selectPlan(plan: SubscriptionPlanDto) {
        _selectedPlan.value = plan
        loadPaymentChannels()
    }

    fun loadPaymentChannels() {
        viewModelScope.launch {
            try {
                val resp = api.getPaymentChannels()
                if (resp.isSuccessful && resp.body() != null) {
                    _channelsState.value = resp.body()!!.channels.filter { it.active }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat kanal pembayaran: ${e.message}"
            }
        }
    }

    fun checkout(paymentMethodCode: String, onCheckoutSuccess: (reference: String) -> Unit) {
        val plan = _selectedPlan.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val resp = api.checkout(CheckoutRequestDto(planId = plan.id, paymentMethod = paymentMethodCode))
                if (resp.isSuccessful && resp.body() != null) {
                    _checkoutResult.value = resp.body()
                    val ref = resp.body()!!.transaction.reference
                    onCheckoutSuccess(ref)
                } else {
                    _errorMessage.value = "Gagal memproses transaksi"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun fetchTransactionDetail(reference: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val resp = api.getTransactionByRef(reference)
                if (resp.isSuccessful && resp.body() != null) {
                    _transactionDetail.value = resp.body()!!.transaction
                    if (resp.body()!!.transaction.status == "PAID") {
                        loadProStatusAndPlans()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
