package com.kasirinaja.core.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminApi {
    @GET("admin/dashboard")
    suspend fun getDashboardStats(): JsonObject

    @POST("admin/products/{id}/approve")
    suspend fun approveProduct(@Path("id") id: String): JsonObject

    @POST("admin/products/{id}/reject")
    suspend fun rejectProduct(@Path("id") id: String): JsonObject
}
