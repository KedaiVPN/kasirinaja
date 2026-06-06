<<<<<<< SEARCH
    @retrofit2.http.PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Query("status") status: String,
        @Body request: PendingProductRequest
    ): Response<Map<String, Any>>
}
=======
    @retrofit2.http.PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Query("status") status: String,
        @Body request: PendingProductRequest
    ): Response<Map<String, Any>>

    @retrofit2.http.GET("products/master")
    suspend fun getMasterProducts(): com.google.gson.JsonArray

    @retrofit2.http.GET("products/pending")
    suspend fun getPendingProducts(): com.google.gson.JsonArray
}
>>>>>>> REPLACE
