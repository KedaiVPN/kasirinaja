package com.kasirinaja.store.ui.viewmodels

import com.kasirinaja.core.network.TransactionApi
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.flow
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.combine
import com.kasirinaja.store.data.local.ProductDao
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.TransactionDao



data class DashboardState(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalProducts: Int = 0,
    val netProfit: Double = 0.0,
    val recentTransactions: List<LocalTransactionEntity> = emptyList()
)


class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val transactionApi: TransactionApi? = null
) : ViewModel() {

    // Poll the basic stats periodically or just load them once.
    // Flow of recent transactions will drive updates if there are new ones.
    private val statsFlow = flow {
        emit(getStats())

        // Option C: Fetch from server in background to sync
        try {
            transactionApi?.getDashboardStats()?.let { serverStats ->
                // Note: Option C usually updates local DB, but for dashboard aggregates,
                // we might just emit the server response directly if we trust it more,
                // or we trigger a sync. Here we just update the flow with server data.
                val serverState = DashboardState(
                    totalRevenue = serverStats.total_revenue.toDouble(),
                    totalTransactions = serverStats.total_transactions,
                    totalProducts = serverStats.total_products,
                    netProfit = serverStats.net_profit.toDouble()
                )
                // Emit server data
                emit(serverState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val state: StateFlow<DashboardState> = combine(
        transactionDao.getRecentTransactionsFlow(5),
        statsFlow
    ) { recent, _ -> // we recalculate stats every time recent changes to keep it fresh
        val newStats = getStats()
        DashboardState(
            totalRevenue = newStats.totalRevenue,
            totalTransactions = newStats.totalTransactions,
            totalProducts = newStats.totalProducts,
            netProfit = newStats.netProfit,
            recentTransactions = recent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    private suspend fun getStats(): DashboardState {
        return DashboardState(
            totalRevenue = transactionDao.getTotalRevenue() ?: 0.0,
            totalTransactions = transactionDao.getTotalTransactions() ?: 0,
            netProfit = transactionDao.getNetProfit() ?: 0.0,
            totalProducts = productDao.getTotalProducts() ?: 0
        )
    }

    class Factory(
        private val transactionDao: TransactionDao,
        private val productDao: ProductDao,
        private val transactionApi: TransactionApi? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(transactionDao, productDao, transactionApi) as T
        }
    }
}
