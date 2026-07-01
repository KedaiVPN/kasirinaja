package com.kasirinaja.store.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionItems(items: List<LocalTransactionItemEntity>)

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): LocalTransactionEntity?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getTransactionByInvoiceNumber(invoiceNumber: String): LocalTransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteTransactionItemsByTransactionId(transactionId: String)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<LocalTransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'pending'")
    suspend fun getPendingTransactions(): List<LocalTransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'pending' ORDER BY transactionTime DESC")
    fun getPendingTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transactionTime DESC")
    fun getAllTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<LocalTransactionEntity>>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :transactionId")
    suspend fun updateTransactionSyncStatus(transactionId: String, status: String)

    @Query("SELECT SUM(totalAmount) FROM transactions")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT COUNT(id) FROM transactions")
    suspend fun getTotalTransactions(): Int?

    @Query("SELECT SUM(subtotal - (buyPrice * quantity)) FROM transaction_items")
    suspend fun getNetProfit(): Double?

    @Query("SELECT SUM(totalAmount) FROM transactions")
    fun getTotalRevenueFlow(): kotlinx.coroutines.flow.Flow<Double?>

    @Query("SELECT COUNT(id) FROM transactions")
    fun getTotalTransactionsFlow(): kotlinx.coroutines.flow.Flow<Int?>

    @Query("SELECT SUM(subtotal - (buyPrice * quantity)) FROM transaction_items")
    fun getNetProfitFlow(): kotlinx.coroutines.flow.Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY transactionTime DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): kotlinx.coroutines.flow.Flow<List<LocalTransactionEntity>>
}
