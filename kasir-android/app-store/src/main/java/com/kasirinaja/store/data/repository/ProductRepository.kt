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
        existingId: String?,
        name: String,
        buyPrice: String,
        sellPrice: String,
        stock: Int,
        category: String,
        description: String,
        barcode: String,
        imageUrl: String
    ): Result<Unit> {
        val localId = existingId ?: UUID.randomUUID().toString()
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
            val finalImageUrl = uploadImageIfLocal(imageUrl) ?: imageUrl

            val request = PendingProductRequest(
                name = name,
                buy_price = buyPrice,
                sell_price = sellPrice,
                stock = stock,
                category = category,
                description = description,
                barcode = barcode,
                image_url = finalImageUrl
            )

            val response = if (existingId != null) {
                // Assume pending for MVP edit to backend
                RetrofitClient.productApi.updateProduct(existingId, "pending", request)
            } else {
                RetrofitClient.productApi.submitPendingProduct(request)
            }

            if (response.isSuccessful) {
                productDao.markAsSynced(localId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Network failed, but it's safe in Room DB
            Result.success(Unit)
        }
    }

    private suspend fun uploadImageIfLocal(uriString: String): String? {
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString // Already a remote URL or empty
        }

        // MVP: This requires context/contentResolver to actually read the file into MultipartBody
        // Skipping implementation details here as Android Uri reading requires Context access.
        // We will just return null to not break the build.
        return null
    }

    suspend fun getProductById(id: String): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun deleteProduct(id: String, isSynced: Boolean) {
        // Delete locally first
        productDao.deleteProduct(id)

        // Then delete from server
        try {
            val status = if (isSynced) "approved" else "pending"
            RetrofitClient.productApi.deleteProduct(id, status)
        } catch (e: Exception) {
            // If network fails, the local delete stands.
            // In a true offline-first robust app, you would queue this delete in WorkManager.
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
