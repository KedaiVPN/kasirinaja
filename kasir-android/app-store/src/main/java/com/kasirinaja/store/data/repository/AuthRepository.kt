package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.AuthApi
import com.kasirinaja.core.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, passwordHash: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("email" to email, "passwordHash" to passwordHash)
                val response = authApi.login(request)
                val token = response["token"]
                if (token != null) {
                    tokenManager.saveToken(token)
                    Result.success(token)
                } else {
                    Result.failure(Exception("Token not found in response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registerStore(
        fullName: String, email: String, phone: String,
        password: String, storeName: String, address: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "password" to password,
                    "storeName" to storeName,
                    "address" to address
                )
                val response = authApi.registerStore(request)
                val message = response["message"] ?: "Success"
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("email" to email, "otp" to otp)
                val response = authApi.verifyOtp(request)
                val message = response["message"]?.toString() ?: "Success"
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resendOtp(email: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("email" to email)
                val response = authApi.resendOtp(request)
                val message = response["message"]?.toString() ?: "Success"
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }
}
