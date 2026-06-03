package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.PendingProductRequest
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.store.data.local.ProductDao
import com.kasirinaja.store.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProductRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun addProductLocalAndSync(
        name: String,
        buyPrice: String,
        sellPrice: String,
        stock: Int,
        category: String,
        description: String,
        barcode: String,
        imageUrl: String
    ): Result<Unit> {
        val localId = UUID.randomUUID().toString()
        val entity = ProductEntity(
            id = localId,
            name = name,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            stock = stock,
            category = category,
            description = description,
            barcode = barcode,
            imageUrl = imageUrl
        )

        // 1. Save to local Room Database first (Offline-First approach)
        productDao.insertProduct(entity)

        // 2. Attempt to sync to backend immediately
        return try {
            val request = PendingProductRequest(
                name = name,
                buy_price = buyPrice,
                sell_price = sellPrice,
                stock = stock,
                category = category,
                description = description,
                barcode = barcode,
                image_url = imageUrl
            )
            val response = RetrofitClient.productApi.submitPendingProduct(request)
            if (response.isSuccessful) {
                productDao.markAsSynced(localId)
                Result.success(Unit)
            } else {
                // Return success anyway since it's saved locally, but we could log the sync error
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Network failed, but it's safe in Room DB
            Result.success(Unit)
        }
    }

    // Placeholder functions to satisfy existing CatalogViewModel temporarily
    suspend fun getCatalogProducts(): Result<List<Map<String, Any>>> {
        return try {
            val response = RetrofitClient.catalogApi.getProducts()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStoreProduct(masterProductId: String, buyPrice: Double, sellPrice: Double, initialStock: Int, minStock: Int): Result<Map<String, Any>> {
        return try {
            val requestMap = mapOf(
                "master_product_id" to masterProductId,
                "buy_price" to buyPrice.toString(),
                "sell_price" to sellPrice.toString(),
                "stock" to initialStock,
                "min_stock" to minStock
            )
            val response = RetrofitClient.storeProductApi.addStoreProduct(requestMap)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
