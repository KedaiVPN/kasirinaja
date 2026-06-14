package com.kasirinaja.store.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM local_products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("UPDATE local_products SET pendingSync = 0 WHERE id = :productId")
    suspend fun markAsSynced(productId: String)

    @Query("DELETE FROM local_products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("SELECT * FROM local_products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("DELETE FROM local_products WHERE isSynced = 1")
    suspend fun deleteSyncedProducts()

    @Query("DELETE FROM local_products WHERE name = :name AND pendingSync = 1")
    suspend fun deletePendingProductByName(name: String)
}
