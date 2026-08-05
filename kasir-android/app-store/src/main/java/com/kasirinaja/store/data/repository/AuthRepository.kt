package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.AuthApi
import com.kasirinaja.core.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("email" to email, "password" to password)
                val response = authApi.login(request)
                val token = response["token"]?.toString()
                val user = response["user"] as? Map<*, *>
                val storeId = user?.get("store_id")?.toString()
                val storeName = user?.get("store_name")?.toString()
                val storeAddress = user?.get("store_address")?.toString()
                val role = user?.get("role")?.toString()
                val logoUrl = user?.get("logo_url")?.toString()
                val fullName = user?.get("full_name")?.toString()
                val photoUrl = user?.get("photo_url")?.toString()
                val userId = user?.get("id")?.toString()
                val emailValue = user?.get("email")?.toString()
                if (token != null) {
                    tokenManager.saveToken(token)
                    if (userId != null) {
                        tokenManager.saveUserId(userId)
                    }
                    if (emailValue != null) {
                        tokenManager.saveEmail(emailValue)
                    }
                    if (storeId != null) {
                        tokenManager.saveStoreId(storeId)
                    }
                    if (storeName != null) {
                        tokenManager.saveStoreName(storeName)
                    }
                    if (storeAddress != null) {
                        tokenManager.saveStoreAddress(storeAddress)
                    }
                    if (role != null) {
                        tokenManager.saveRole(role)
                    }
                    if (logoUrl != null) {
                        tokenManager.saveStoreLogoUrl(logoUrl)
                    }
                    tokenManager.saveUserProfile(fullName, photoUrl)
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
                val token = response["token"]?.toString()
                val user = response["user"] as? Map<*, *>
                val storeId = user?.get("store_id")?.toString()
                val storeName = user?.get("store_name")?.toString()
                val storeAddress = user?.get("store_address")?.toString()
                val role = user?.get("role")?.toString()
                val logoUrl = user?.get("logo_url")?.toString()
                val userId = user?.get("id")?.toString()
                val emailValue = user?.get("email")?.toString()
                if (token != null) {
                    tokenManager.saveToken(token)
                    if (userId != null) {
                        tokenManager.saveUserId(userId)
                    }
                    if (emailValue != null) {
                        tokenManager.saveEmail(emailValue)
                    }
                    if (storeId != null) {
                        tokenManager.saveStoreId(storeId)
                    }
                    if (storeName != null) {
                        tokenManager.saveStoreName(storeName)
                    }
                    if (storeAddress != null) {
                        tokenManager.saveStoreAddress(storeAddress)
                    }
                    if (role != null) {
                        tokenManager.saveRole(role)
                    }
                    if (logoUrl != null) {
                        tokenManager.saveStoreLogoUrl(logoUrl)
                    }
                }
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

    suspend fun switchUser(targetUserId: String, password: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mutableMapOf("target_user_id" to targetUserId)
                if (password != null) {
                    request["password"] = password
                }
                val response = authApi.switchUser(request)
                val token = response["token"]?.toString()
                val user = response["user"] as? Map<*, *>
                val storeId = user?.get("store_id")?.toString()
                val storeName = user?.get("store_name")?.toString()
                val storeAddress = user?.get("store_address")?.toString()
                val role = user?.get("role")?.toString()
                val logoUrl = user?.get("logo_url")?.toString()
                val fullName = user?.get("full_name")?.toString()
                val photoUrl = user?.get("photo_url")?.toString()
                val emailValue = user?.get("email")?.toString()
                if (token != null) {
                    tokenManager.saveToken(token)
                    if (emailValue != null) {
                        tokenManager.saveEmail(emailValue)
                    }
                    if (storeId != null) {
                        tokenManager.saveStoreId(storeId)
                    }
                    if (storeName != null) {
                        tokenManager.saveStoreName(storeName)
                    }
                    if (storeAddress != null) {
                        tokenManager.saveStoreAddress(storeAddress)
                    }
                    if (role != null) {
                        tokenManager.saveRole(role)
                    }
                    if (logoUrl != null) {
                        tokenManager.saveStoreLogoUrl(logoUrl)
                    }
                    tokenManager.saveUserProfile(fullName, photoUrl)
                    Result.success(token)
                } else {
                    Result.failure(Exception("Token not found in response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }
}
