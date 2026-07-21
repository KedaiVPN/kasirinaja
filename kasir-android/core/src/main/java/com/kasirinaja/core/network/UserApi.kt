package com.kasirinaja.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

interface UserApi {
    @GET("users/store")
    suspend fun getStoreUsers(): List<Map<String, Any>>

    @POST("users/store/add-employee")
    suspend fun addStoreEmployee(@Body request: Map<String, String>): Map<String, Any>

    @DELETE("users/store/employees/{id}")
    suspend fun deleteStoreEmployee(@Path("id") id: String): Response<Map<String, Any>>

    @Multipart
    @PUT("users/store/profile")
    suspend fun updateProfile(
        @Part("full_name") fullName: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<Map<String, Any>>
}
