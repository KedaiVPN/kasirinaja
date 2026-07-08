package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.StoreApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

class StoreRepository(private val storeApi: StoreApi) {
    suspend fun updateStore(storeName: String, address: String, phone: String = ""): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "store_name" to storeName,
                    "address" to address,
                    "phone" to phone
                )
                val response = storeApi.updateStore(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadStoreLogo(logoPart: MultipartBody.Part): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = storeApi.uploadStoreLogo(logoPart)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
