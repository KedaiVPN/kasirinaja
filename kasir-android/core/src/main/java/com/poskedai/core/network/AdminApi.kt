package com.poskedai.core.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class AdminDashboardStatsDto(
    val approved_count: Int = 0,
    val pending_count: Int = 0,
    val total_stores: Int = 0,
    val pro_stores: Int = 0,
    val non_pro_stores: Int = 0
)

data class AdminStoreListItemDto(
    val id: String = "",
    val store_name: String = "",
    val owner_name: String = ""
)

data class AdminStoreDetailDto(
    val id: String = "",
    val store_name: String = "",
    val owner_name: String = "",
    val email: String = "",
    val phone: String = "",
    val cashier_count: Int = 0,
    val is_pro: Boolean = false,
    val pro_expires_at: String? = null
)

data class UpdateProRequestDto(
    val days: Int
)

interface AdminApi {
    @GET("admin/dashboard")
    suspend fun getDashboardStats(): Response<AdminDashboardStatsDto>

    @POST("admin/products/{id}/approve")
    suspend fun approveProduct(@Path("id") id: String): Response<JsonObject>

    @POST("admin/products/{id}/reject")
    suspend fun rejectProduct(@Path("id") id: String): Response<JsonObject>

    @GET("admin/stores")
    suspend fun getStores(): Response<List<AdminStoreListItemDto>>

    @GET("admin/stores/{id}")
    suspend fun getStoreDetail(@Path("id") id: String): Response<AdminStoreDetailDto>

    @PUT("admin/stores/{id}/pro")
    suspend fun updateStoreProStatus(
        @Path("id") id: String,
        @Body request: UpdateProRequestDto
    ): Response<JsonObject>

    @DELETE("admin/stores/{id}")
    suspend fun deleteStore(@Path("id") id: String): Response<JsonObject>
}
