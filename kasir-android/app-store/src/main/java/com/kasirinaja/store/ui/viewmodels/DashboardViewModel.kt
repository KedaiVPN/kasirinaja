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
    val storeName: String = "Nama Toko",
    val storeAddress: String = "Alamat Toko",
    val role: String = "Owner",
    val recentTransactions: List<LocalTransactionEntity> = emptyList()
)



class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val transactionRepository: TransactionRepository? = null,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    init {
        fetchServerStats()
    }

    fun fetchServerStats() {
        viewModelScope.launch {
            transactionRepository?.fetchAndSaveAllTransactions()
        }
    }

    val state: StateFlow<DashboardState> = combine(
        transactionDao.getTotalRevenueFlow(),
        transactionDao.getTotalTransactionsFlow(),
        productDao.getTotalProductsFlow(),
        transactionDao.getNetProfitFlow(),
        transactionDao.getRecentTransactionsFlow(5)
    ) { revenue, txCount, productCount, profit, recentTxs ->

        DashboardState(
            totalRevenue = revenue ?: 0.0,
            totalTransactions = txCount ?: 0,
            totalProducts = productCount ?: 0,
            netProfit = profit ?: 0.0,
            storeName = tokenManager?.getStoreName() ?: "Nama Toko",
            storeAddress = tokenManager?.getStoreAddress() ?: "Alamat Toko",
            role = tokenManager?.getRole() ?: "Owner",
            recentTransactions = recentTxs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
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
