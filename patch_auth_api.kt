<<<<<<< SEARCH
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Map<String, String>
=======
    @POST("login")
    suspend fun login(@Body request: Map<String, String>): com.google.gson.JsonObject
>>>>>>> REPLACE
