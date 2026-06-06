<<<<<<< SEARCH
    @retrofit2.http.GET("products/master")
    suspend fun getMasterProducts(): com.google.gson.JsonArray

    @retrofit2.http.GET("products/pending")
    suspend fun getPendingProducts(): com.google.gson.JsonArray
=======
    @retrofit2.http.GET("products/master")
    suspend fun getMasterProducts(): List<com.google.gson.JsonObject>

    @retrofit2.http.GET("products/pending")
    suspend fun getPendingProducts(): List<com.google.gson.JsonObject>
>>>>>>> REPLACE
