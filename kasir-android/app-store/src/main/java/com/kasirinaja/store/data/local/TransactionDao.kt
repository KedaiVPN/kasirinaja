package com.kasirinaja.store.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<LocalTransactionItemEntity>)

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): LocalTransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<LocalTransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'pending'")
    suspend fun getPendingTransactions(): List<LocalTransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :transactionId")
    suspend fun updateTransactionSyncStatus(transactionId: String, status: String)
}
