<<<<<<< SEARCH
    @POST("admin/products/{id}/approve")
    suspend fun approveProduct(@Path("id") id: String): JsonObject

    @POST("admin/products/{id}/reject")
    suspend fun rejectProduct(@Path("id") id: String): JsonObject
=======
    @POST("admin/products/{id}/approve")
    suspend fun approveProduct(@Path("id") id: String): Response<JsonObject>

    @POST("admin/products/{id}/reject")
    suspend fun rejectProduct(@Path("id") id: String): Response<JsonObject>
>>>>>>> REPLACE
