package com.kasirinaja.core.network

import retrofit2.http.GET

interface CatalogApi {
    @GET("catalog/products")
    suspend fun getProducts(): List<Map<String, Any>>
}
