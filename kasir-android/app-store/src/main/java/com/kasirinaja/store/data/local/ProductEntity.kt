package com.kasirinaja.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_products")
data class ProductEntity(
    @PrimaryKey val id: String, // UUID string
    val name: String,
    val buyPrice: String,
    val sellPrice: String,
    val stock: Int,
    val category: String,
    val description: String?,
    val barcode: String?,
    val imageUrl: String?,
    val minStock: Int = 0,
    val isStockNotificationEnabled: Boolean = false,
    val isSynced: Boolean = false, // True if approved by admin and converted to master/store product
    val pendingSync: Boolean = true, // True if waiting to be uploaded to pending_products
    val createdAt: Long = System.currentTimeMillis()
)
