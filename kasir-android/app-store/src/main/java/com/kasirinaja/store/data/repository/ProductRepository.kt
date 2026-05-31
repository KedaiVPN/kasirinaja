package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.CatalogApi
import com.kasirinaja.core.network.StoreProductApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val catalogApi: CatalogApi,
    private val storeProductApi: StoreProductApi
) {
    suspend fun getCatalogProducts(): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val products = catalogApi.getProducts()
                Result.success(products)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStoreProducts(): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val products = storeProductApi.getStoreProducts()
                Result.success(products)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addStoreProduct(
        masterProductId: String,
        buyPrice: Double,
        sellPrice: Double,
        initialStock: Int,
        minStock: Int
    ): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "masterProductId" to masterProductId,
                    "buyPrice" to buyPrice,
                    "sellPrice" to sellPrice,
                    "initialStock" to initialStock,
                    "minStock" to minStock
                )
                val response = storeProductApi.addStoreProduct(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
