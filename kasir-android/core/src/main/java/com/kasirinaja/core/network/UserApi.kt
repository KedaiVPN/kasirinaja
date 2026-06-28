package com.kasirinaja.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApi {
    @GET("users/store")
    suspend fun getStoreUsers(): List<Map<String, Any>>

    @POST("users/store/add-employee")
    suspend fun addStoreEmployee(@Body request: Map<String, String>): Map<String, Any>
}
