package com.kasirinaja.core.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface StoreApi {
    @PUT("stores/update")
    suspend fun updateStore(@Body request: Map<String, String>): Map<String, Any>

    @Multipart
    @POST("stores/upload-logo")
    suspend fun uploadStoreLogo(@Part logo: MultipartBody.Part): Map<String, Any>
}
