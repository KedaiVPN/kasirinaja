package com.poskedai.admin.data

import com.poskedai.core.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.authApi.adminLogin(
                mapOf(
                    "email" to email,
                    "password" to password
                )
            )
            val token = response.get("token").asString
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
