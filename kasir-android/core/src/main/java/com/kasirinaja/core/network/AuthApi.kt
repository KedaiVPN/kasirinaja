package com.kasirinaja.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Map<String, Any>

    @POST("login")
    suspend fun adminLogin(@Body request: Map<String, String>): com.google.gson.JsonObject

    @POST("auth/register-store")
    suspend fun registerStore(@Body request: Map<String, String>): Map<String, String>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: Map<String, String>): Map<String, Any>

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body request: Map<String, String>): Map<String, Any>
}
