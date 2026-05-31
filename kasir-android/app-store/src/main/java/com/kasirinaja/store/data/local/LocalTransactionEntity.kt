package com.kasirinaja.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val cashierId: String,
    val invoiceNumber: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val changeAmount: Double,
    val paymentMethod: String,
    val transactionTime: Long,
    val syncStatus: String,
    val deviceId: String
)
