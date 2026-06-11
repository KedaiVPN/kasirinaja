package com.kasirinaja.store.data.repository

import android.content.Context
import android.net.Uri
import com.kasirinaja.core.network.PendingProductRequest
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.store.utils.ImageCompressor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.kasirinaja.store.data.local.ProductDao
import com.kasirinaja.store.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import com.google.gson.JsonObject

class ProductRepository(
    private val productDao: ProductDao,
    private val context: Context
) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun getMasterProducts(): List<JsonObject> {
        val response = RetrofitClient.productApi.getMasterProducts()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Failed to fetch master products: ${response.message()}")
        }
    }

    suspend fun addStoreProductFromMaster(
        storeId: String,
        masterProductId: String,
        buyPrice: String,
        sellPrice: String,
        stock: Int,
        minStock: Int,
        localName: String,
        localCategory: String,
        barcode: String,
        imageUrl: String,
        description: String
    ) {
        val request = mapOf(
            "store_id" to storeId,
            "master_product_id" to masterProductId,
            "buy_price" to buyPrice,
            "sell_price" to sellPrice,
            "stock" to stock,
            "min_stock" to minStock,
            "local_name" to localName,
            "local_category" to localCategory
        )
        val response = RetrofitClient.productApi.addStoreProduct(request)
        if (response.isSuccessful) {
            val body = response.body()
            // Ensure ID is properly handled as string
            val newId = body?.get("id")?.toString() ?: UUID.randomUUID().toString()

            val entity = ProductEntity(
                id = newId,
                name = localName,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                category = localCategory,
                description = description,
                barcode = barcode,
                imageUrl = imageUrl,
                isSynced = true,
                pendingSync = false
            )
            productDao.insertProduct(entity)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrEmpty()) {
                try {
                    val jsonError = org.json.JSONObject(errorBody)
                    jsonError.optString("error", response.message())
                } catch (e: Exception) {
                    errorBody
                }
            } else {
                response.message()
            }
            throw Exception("Gagal menambahkan produk ke toko: $errorMessage")
        }
    }

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

        // Preserve existing sync state if editing
        val existingProduct = if (existingId != null) getProductById(existingId) else null
        val isCurrentlySynced = existingProduct?.isSynced ?: false
        val statusParam = if (isCurrentlySynced) "approved" else "pending"

        val entity = ProductEntity(
            id = localId,
            name = name,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            stock = stock,
            category = category,
            description = description,
            barcode = barcode,
            imageUrl = imageUrl,
            isSynced = isCurrentlySynced,
            pendingSync = true // Always needs sync after edit
        )

        // 1. Save to local Room Database first (Offline-First approach)
        productDao.insertProduct(entity)

        // 2. Attempt to sync to backend immediately
        return try {
            val finalImageUrl = uploadImageIfLocal(imageUrl)

            val request = PendingProductRequest(
                name = name,
                buy_price = buyPrice,
                sell_price = sellPrice,
                stock = stock,
                category = category,
                description = description,
                barcode = barcode,
                image_url = finalImageUrl,
                store_id = null // Can be populated if store context is available
            )

            val response = if (existingId != null) {
                RetrofitClient.productApi.updateProduct(existingId, statusParam, request)
            } else {
                RetrofitClient.productApi.submitPendingProduct(request)
            }

            if (response.isSuccessful) {
                productDao.markAsSynced(localId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal melakukan sinkronisasi dengan server."))
            }
        } catch (e: Exception) {
            // Return failure instead of success so UI can show error message
            Result.failure(e)
        }
    }

    private suspend fun uploadImageIfLocal(uriString: String): String {
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString // Already a remote URL or empty
        }

        val uri = Uri.parse(uriString)
        val compressedFile = ImageCompressor.compressImageFromUri(context, uri)
            ?: throw Exception("Gagal mengkompresi gambar")

        val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", compressedFile.name, requestFile)

        val response = RetrofitClient.productApi.uploadImage(body)
        if (response.isSuccessful) {
            val data = response.body()
            return data?.get("image_url") as? String ?: throw Exception("URL gambar tidak ditemukan dalam respons")
        } else {
            throw Exception("Gagal mengupload gambar: ${response.code()}")
        }
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
