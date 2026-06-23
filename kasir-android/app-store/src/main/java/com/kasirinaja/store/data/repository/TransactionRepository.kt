package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.TransactionApi
import com.kasirinaja.core.network.TransactionRequest
import com.kasirinaja.core.network.TransactionItemRequest
import com.kasirinaja.store.data.local.TransactionDao
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.LocalTransactionItemEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TransactionSyncState {
    private val _syncStatus = MutableSharedFlow<String>()
    val syncStatus = _syncStatus.asSharedFlow()

    suspend fun emit(status: String) {
        _syncStatus.emit(status)
    }
}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val transactionApi: TransactionApi
) {
    suspend fun saveTransactionLocally(
        transaction: LocalTransactionEntity,
        items: List<LocalTransactionItemEntity>
    ) {
        transactionDao.insertTransaction(transaction)
        transactionDao.insertTransactionItems(items)
    }

    suspend fun syncPendingTransactions() {
        val pendingTransactions = transactionDao.getPendingTransactions()
        if (pendingTransactions.isNotEmpty()) {
            TransactionSyncState.emit("sync_started")
        }

        var anyFailed = false
        var anySuccess = false

        for (localTx in pendingTransactions) {
            try {
                val localItems = transactionDao.getTransactionItems(localTx.id)

                val itemsRequest = localItems.map {
                    TransactionItemRequest(
                        store_product_id = it.storeProductId,
                        master_product_id = it.masterProductId,
                        product_name = it.productName,
                        barcode = it.barcode,
                        quantity = it.quantity,
                        buy_price = it.buyPrice.toLong(),
                        sell_price = it.sellPrice.toLong(),
                        subtotal = it.subtotal.toLong()
                    )
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val dateString = dateFormat.format(Date(localTx.transactionTime))

                val request = TransactionRequest(
                    store_id = localTx.storeId,
                    cashier_id = localTx.cashierId,
                    invoice_number = localTx.invoiceNumber,
                    total_amount = localTx.totalAmount.toLong(),
                    paid_amount = localTx.paidAmount.toLong(),
                    change_amount = localTx.changeAmount.toLong(),
                    payment_method = localTx.paymentMethod,
                    transaction_time = dateString,
                    sync_status = "synced",
                    device_id = localTx.deviceId,
                    items = itemsRequest
                )

                transactionApi.createTransaction(request)

                // If successful, update local status
                transactionDao.updateTransactionSyncStatus(localTx.id, "synced")
                anySuccess = true
            } catch (e: Exception) {
                // If sync fails, it will remain pending and retry later
                e.printStackTrace()
                anyFailed = true
            }
        }

        if (pendingTransactions.isNotEmpty()) {
            if (anyFailed) {
                TransactionSyncState.emit("sync_failed")
            } else if (anySuccess) {
                TransactionSyncState.emit("sync_success")
            }
        }
    }
}
