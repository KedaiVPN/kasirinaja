package com.kasirinaja.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Map<String, String>

    @POST("auth/register-store")
    suspend fun registerStore(@Body request: Map<String, String>): Map<String, String>
}
