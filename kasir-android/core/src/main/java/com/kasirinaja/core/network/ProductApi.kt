package com.kasirinaja.core.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class PendingProductRequest(
    val name: String,
    val buy_price: String,
    val sell_price: String,
    val stock: Int,
    val category: String,
    val description: String,
    val barcode: String,
    val image_url: String,
    val store_id: String? = null
)

interface ProductApi {
    @POST("products/pending")
    suspend fun submitPendingProduct(@Body request: PendingProductRequest): Response<Map<String, Any>>

    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<Map<String, String>>

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String,
        @Query("status") status: String
    ): Response<Map<String, Any>>

    @retrofit2.http.PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Query("status") status: String,
        @Body request: PendingProductRequest
    ): Response<Map<String, Any>>

    @retrofit2.http.GET("products/master")
    suspend fun getMasterProducts(): Response<List<com.google.gson.JsonObject>>

    @retrofit2.http.GET("products/pending")
    suspend fun getPendingProducts(): Response<List<com.google.gson.JsonObject>>


    @POST("products/master")
    suspend fun submitMasterProduct(@Body request: MasterProductRequest): Response<Map<String, Any>>

    @POST("products/store")
    suspend fun addStoreProduct(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>
}
