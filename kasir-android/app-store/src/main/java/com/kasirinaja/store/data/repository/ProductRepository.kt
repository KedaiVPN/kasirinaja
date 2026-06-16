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
import com.kasirinaja.core.network.TokenManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.Constraints
import com.kasirinaja.store.worker.ImageDownloadWorker

class ProductRepository(
    private val productDao: ProductDao,
    private val context: Context
) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun syncStoreProducts() {
        val storeId = TokenManager(context).getStoreId() ?: return
        val response = RetrofitClient.productApi.getStoreProducts(storeId)
        if (response.isSuccessful) {
            val remoteProducts = response.body() ?: emptyList()

            // Delete previously synced products before inserting new ones
            // This prevents duplicates from lingering.
            productDao.deleteSyncedProducts()

            remoteProducts.forEach { product ->
                val id = product.get("id")?.asString ?: return@forEach
                val name = product.get("local_name")?.asString ?: ""

                // If a pending product with the same name exists locally (and just got approved), remove it
                if (name.isNotEmpty()) {
                    productDao.deletePendingProductByName(name)
                }
                val buyPrice = product.get("buy_price")?.asLong?.toString() ?: "0"
                val sellPrice = product.get("sell_price")?.asLong?.toString() ?: "0"
                val stock = product.get("stock")?.asInt ?: 0
                val localCategory = if (product.get("local_category") != null && !product.get("local_category").isJsonNull) product.get("local_category").asString else ""
                val categoryName = if (product.get("category_name") != null && !product.get("category_name").isJsonNull) product.get("category_name").asString else ""
                val category = if (localCategory.isNotEmpty()) localCategory else categoryName

                val barcode = if (product.get("barcode") != null && !product.get("barcode").isJsonNull) product.get("barcode").asString else ""
                val imageUrl = if (product.get("image_url") != null && !product.get("image_url").isJsonNull && product.get("image_url").asString.isNotEmpty()) {
                    product.get("image_url").asString
                } else if (product.get("photo_url") != null && !product.get("photo_url").isJsonNull && product.get("photo_url").asString.isNotEmpty()) {
                    product.get("photo_url").asString
                } else {
                    ""
                }
                val description = if (product.get("description") != null && !product.get("description").isJsonNull) product.get("description").asString else ""

                val entity = ProductEntity(
                    id = id,
                    name = name,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    stock = stock,
                    category = category,
                    description = description,
                    barcode = barcode,
                    imageUrl = imageUrl,
                    isSynced = true,
                    pendingSync = false
                )
                productDao.insertProduct(entity)
            }

            // Queue image downloads using WorkManager
            // Instead of passing array which might hit 10KB limit, the worker will query Room database for images
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        } else {
            throw Exception("Gagal sync data: ${response.message()}")
        }
    }

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
        val request = mapOf<String, Any>(
            "store_id" to storeId,
            "master_product_id" to masterProductId,
            "buy_price" to (buyPrice.toLongOrNull() ?: 0L),
            "sell_price" to (sellPrice.toLongOrNull() ?: 0L),
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
        buyPrice: Long,
        sellPrice: Long,
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
            buyPrice = buyPrice.toString(),
            sellPrice = sellPrice.toString(),
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

            val storeId = TokenManager(context).getStoreId()

            val request = PendingProductRequest(
                name = name,
                buy_price = buyPrice,
                sell_price = sellPrice,
                stock = stock,
                category = category,
                description = description,
                barcode = barcode,
                image_url = finalImageUrl,
                store_id = storeId
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
            RetrofitClient.productApi.deleteStoreProductSpecific(id)
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
                "buy_price" to buyPrice.toLong(),
                "sell_price" to sellPrice.toLong(),
                "stock" to initialStock,
                "min_stock" to minStock
            )
            val response = RetrofitClient.productApi.addStoreProduct(requestMap)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyMap())
            } else {
                Result.failure(Exception("Gagal menambahkan produk ke toko"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
