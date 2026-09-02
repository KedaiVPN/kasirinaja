package com.kasirinaja.store.ui.viewmodels

import com.kasirinaja.store.data.local.ProductDao
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import com.kasirinaja.store.data.repository.TransactionRepository
import java.util.TimeZone
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.TopProductItem
import com.kasirinaja.store.data.local.TransactionDao
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
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

enum class TimeFilter {
    TODAY,
    THIS_MONTH,
    THIS_YEAR
}

data class DashboardState(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalProducts: Int = 0,
    val netProfit: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayProductsSold: Int = 0,
    val storeName: String = "Nama Toko",
    val storeAddress: String = "Alamat Toko",
    val logoUrl: String? = null,
    val role: String = "Owner",
    val userPhotoUrl: String? = null,
    val topProducts: List<TopProductItem> = emptyList(),
    val topProductsFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val triggerRefresh: Long = 0L // Helper to force UI refresh
)

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val transactionRepository: TransactionRepository? = null,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0L)
    private val filterState = MutableStateFlow(TimeFilter.THIS_MONTH)

    fun updateTopProductsFilter(filter: TimeFilter) {
        filterState.value = filter
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

    private fun getStartOfMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getStartOfYear(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfYear(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 31)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardState> = combine(refreshTrigger, filterState) { trigger, filter -> Pair(trigger, filter) }.flatMapLatest { (refreshTriggerVal, currentFilter) ->
        val role = tokenManager?.getRole() ?: "owner"
        val cashierIdFilter = if (role == "kasir") tokenManager?.getUserId() else null

        val (topStart, topEnd) = when (currentFilter) {
            TimeFilter.TODAY -> Pair(getStartOfDay(), getEndOfDay())
            TimeFilter.THIS_MONTH -> Pair(getStartOfMonth(), getEndOfMonth())
            TimeFilter.THIS_YEAR -> Pair(getStartOfYear(), getEndOfYear())
        }

        combine(
            combine(
                transactionDao.getTodayTotalRevenueFlow(getStartOfDay(), getEndOfDay()),
                transactionDao.getTodayTotalTransactionsFlow(getStartOfDay(), getEndOfDay()),
                productDao.getTotalProductsFlow(),
                transactionDao.getTodayNetProfitFlow(getStartOfDay(), getEndOfDay()),
                transactionDao.getTopSellingProductsFlow(topStart, topEnd, 10, cashierIdFilter),
                transactionDao.getTodayTotalProductsSoldFlow(getStartOfDay(), getEndOfDay())
            ) { arr ->
                @Suppress("UNCHECKED_CAST")
                val revenue = arr[0] as Double?
                val txCount = arr[1] as Int?
                val productCount = arr[2] as Int?
                val profit = arr[3] as Double?
                val topProds = arr[4] as List<TopProductItem>
                val productsSold = arr[5] as Int?

                DashboardState(
                    totalRevenue = revenue ?: 0.0,
                    totalTransactions = txCount ?: 0,
                    totalProducts = productCount ?: 0,
                    netProfit = profit ?: 0.0,
                    todayRevenue = revenue ?: 0.0,
                    todayProfit = profit ?: 0.0,
                    topProducts = topProds,
                    topProductsFilter = currentFilter,
                    todayProductsSold = productsSold ?: 0
                )
            },
            flow { emit(refreshTriggerVal) }
        ) { baseState, _ ->
            baseState.copy(
                storeName = tokenManager?.getStoreName() ?: "Nama Toko",
                storeAddress = tokenManager?.getStoreAddress() ?: "Alamat Toko",
                logoUrl = tokenManager?.getStoreLogoUrl(),
                role = tokenManager?.getRole() ?: "Owner",
                triggerRefresh = refreshTriggerVal,
                userPhotoUrl = tokenManager?.getPhotoUrl()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(
            storeName = tokenManager?.getStoreName() ?: "Nama Toko",
            storeAddress = tokenManager?.getStoreAddress() ?: "Alamat Toko",
            logoUrl = tokenManager?.getStoreLogoUrl(),
            role = tokenManager?.getRole() ?: "Owner",
            userPhotoUrl = tokenManager?.getPhotoUrl()
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