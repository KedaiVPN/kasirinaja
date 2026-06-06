<<<<<<< SEARCH
    @GET("admin/dashboard")
    suspend fun getDashboardStats(): JsonObject
=======
    @GET("admin/dashboard")
    suspend fun getDashboardStats(): Response<JsonObject>
>>>>>>> REPLACE
