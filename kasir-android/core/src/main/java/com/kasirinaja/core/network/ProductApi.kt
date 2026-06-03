package com.kasirinaja.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class PendingProductRequest(
    val name: String,
    val buy_price: String,
    val sell_price: String,
    val stock: Int,
    val category: String,
    val description: String,
    val barcode: String,
    val image_url: String
)

interface ProductApi {
    @POST("products/pending")
    suspend fun submitPendingProduct(@Body request: PendingProductRequest): Response<Map<String, Any>>
}
