package com.kasirinaja.store.ui.viewmodels

import com.kasirinaja.store.data.local.ProductDao
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import com.kasirinaja.store.data.repository.TransactionRepository
import java.util.TimeZone
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.TransactionDao
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import androidx.lifecycle.ViewModelProvider
import com.kasirinaja.core.network.TokenManager

data class DashboardState(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalProducts: Int = 0,
    val netProfit: Double = 0.0,
    val todayProductsSold: Int = 0,
    val storeName: String = "Nama Toko",
    val storeAddress: String = "Alamat Toko",
    val logoUrl: String? = null,
    val role: String = "Owner",
    val recentTransactions: List<LocalTransactionEntity> = emptyList(),
    val triggerRefresh: Long = 0L // Helper to force UI refresh
)

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val transactionRepository: TransactionRepository? = null,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0L)

    init {
        fetchServerStats()
    }

    fun fetchServerStats() {
        viewModelScope.launch {
            transactionRepository?.fetchAndSaveAllTransactions()
        }
    }

    fun refreshStoreInfo() {
        refreshTrigger.update { it + 1 }
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    val state: StateFlow<DashboardState> = combine(
        combine(
            transactionDao.getTodayTotalRevenueFlow(getStartOfDay(), getEndOfDay()),
            transactionDao.getTodayTotalTransactionsFlow(getStartOfDay(), getEndOfDay()),
            productDao.getTotalProductsFlow(),
            transactionDao.getTodayNetProfitFlow(getStartOfDay(), getEndOfDay()),
            transactionDao.getRecentTransactionsFlow(5),
            transactionDao.getTodayTotalProductsSoldFlow(getStartOfDay(), getEndOfDay())
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            val revenue = arr[0] as Double?
            val txCount = arr[1] as Int?
            val productCount = arr[2] as Int?
            val profit = arr[3] as Double?
            val recentTxs = arr[4] as List<LocalTransactionEntity>
            val productsSold = arr[5] as Int?

            DashboardState(
                totalRevenue = revenue ?: 0.0,
                totalTransactions = txCount ?: 0,
                totalProducts = productCount ?: 0,
                netProfit = profit ?: 0.0,
                recentTransactions = recentTxs,
                todayProductsSold = productsSold ?: 0
            )
        },
        refreshTrigger
    ) { baseState, refreshTriggerVal ->
        baseState.copy(
            storeName = tokenManager?.getStoreName() ?: "Nama Toko",
            storeAddress = tokenManager?.getStoreAddress() ?: "Alamat Toko",
            logoUrl = tokenManager?.getStoreLogoUrl(),
            role = tokenManager?.getRole() ?: "Owner",
            triggerRefresh = refreshTriggerVal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(
            storeName = tokenManager?.getStoreName() ?: "Nama Toko",
            storeAddress = tokenManager?.getStoreAddress() ?: "Alamat Toko",
            logoUrl = tokenManager?.getStoreLogoUrl(),
            role = tokenManager?.getRole() ?: "Owner"
        )
    )

    class Factory(
        private val transactionDao: TransactionDao,
        private val productDao: ProductDao,
        private val transactionRepository: TransactionRepository? = null,
        private val tokenManager: TokenManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(transactionDao, productDao, transactionRepository, tokenManager) as T
        }
    }
}