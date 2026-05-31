package com.kasirinaja.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_items")
data class LocalTransactionItemEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val storeProductId: String,
    val masterProductId: String,
    val barcode: String,
    val productName: String,
    val quantity: Int,
    val buyPrice: Double,
    val sellPrice: Double,
    val subtotal: Double
)
