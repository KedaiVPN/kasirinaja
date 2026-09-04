package com.kasirinaja.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_products")
data class LocalStoreProductEntity(
    @PrimaryKey
    val id: String,
    val storeId: String,
    val masterProductId: String,
    val barcode: String,
    val name: String,
    val photoUrl: String?,
    val localPhotoPath: String?,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val minStock: Int,
    val isActive: Boolean,
    val updatedAt: Long,
    val syncStatus: String,
    val isStockNotificationEnabled: Boolean = false
)
