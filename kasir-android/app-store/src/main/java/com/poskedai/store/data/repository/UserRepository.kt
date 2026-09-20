package com.poskedai.store.data.repository

import com.poskedai.core.network.UserApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userApi: UserApi) {
    suspend fun getStoreUsers(): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApi.getStoreUsers()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addStoreEmployee(name: String, username: String, phone: String, role: String, password: String): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "full_name" to name,
                    "email" to username,
                    "phone" to phone,
                    "role" to role,
                    "password" to password
                )
                val response = userApi.addStoreEmployee(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteStoreEmployee(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApi.deleteStoreEmployee(id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal menghapus karyawan"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendFeedback(senderEmail: String?, message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestMap = mutableMapOf("message" to message)
                if (!senderEmail.isNullOrBlank()) {
                    requestMap["sender_email"] = senderEmail
                }
                val response = userApi.sendFeedback(requestMap)
                if (response.isSuccessful) {
                    val resBody = response.body()
                    val msg = resBody?.get("message") as? String ?: "Kritik & saran berhasil dikirim"
                    Result.success(msg)
                } else {
                    val errorBodyStr = response.errorBody()?.string() ?: "Gagal mengirim kritik & saran"
                    Result.failure(Exception(errorBodyStr))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
