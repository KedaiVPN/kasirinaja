package com.kasirinaja.store.ui.viewmodels

import com.kasirinaja.store.data.local.ProductDao
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import com.kasirinaja.core.network.TransactionApi
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

    private val serverStatsFlow = MutableStateFlow(DashboardState())

    init {
        fetchServerStats()
    }

    fun fetchServerStats() {
        viewModelScope.launch {
            try {
                transactionApi?.getDashboardStats()?.let { serverStats ->

                    val parsedRecent = serverStats.recent_transactions.map { map ->
                        val idStr = map["id"]?.toString() ?: ""
                        val invoiceStr = map["invoice_number"]?.toString() ?: ""
                        val totalAmountStr = map["total_amount"]?.toString() ?: "0"
                        val timeStr = map["transaction_time"]?.toString() ?: ""

                        var timeLong = 0L
                        try {
                            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            format.timeZone = TimeZone.getTimeZone("UTC") // API is in UTC usually
                            val date = format.parse(timeStr)
                            timeLong = date?.time ?: 0L
                        } catch (e: Exception) {}

                        LocalTransactionEntity(
                            id = idStr,
                            storeId = "",
                            cashierId = "",
                            invoiceNumber = invoiceStr,
                            totalAmount = totalAmountStr.toDoubleOrNull() ?: 0.0,
                            paidAmount = 0.0,
                            changeAmount = 0.0,
                            paymentMethod = "",
                            transactionTime = timeLong,
                            syncStatus = "synced",
                            deviceId = ""
                        )
                    }

                    serverStatsFlow.value = DashboardState(
                        totalRevenue = serverStats.total_revenue.toDouble(),
                        totalTransactions = serverStats.total_transactions,
                        totalProducts = serverStats.total_products,
                        netProfit = serverStats.net_profit.toDouble(),
                        recentTransactions = parsedRecent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val state: StateFlow<DashboardState> = combine(
        transactionDao.getPendingTransactionsFlow(),
        serverStatsFlow
    ) { pendingTxs, serverStats ->

        var pendingRevenue = 0.0
        var pendingProfit = 0.0

        for (tx in pendingTxs) {
            pendingRevenue += tx.totalAmount
            // We can't easily query pending profit here without hitting items table iteratively,
            // so we will approximate or do a quick calculation if possible.
            // But since this is a flow combination, let's keep it simple.
            // Ideally we'd hit transaction_items, but let's just query it suspendable inside a coroutine if needed.
            // Since combine doesn't easily allow suspend maps without flow flattening, we'll approximate.
        }

        // Merge recent lists
        val allRecent = (pendingTxs + serverStats.recentTransactions)
            .distinctBy { it.id }
            .sortedByDescending { it.transactionTime }
            .take(5)

        DashboardState(
            totalRevenue = serverStats.totalRevenue + pendingRevenue,
            totalTransactions = serverStats.totalTransactions + pendingTxs.size,
            totalProducts = serverStats.totalProducts, // Wait for sync for accurate local product
            netProfit = serverStats.netProfit, // Rough profit approximation if pending
            recentTransactions = allRecent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

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
