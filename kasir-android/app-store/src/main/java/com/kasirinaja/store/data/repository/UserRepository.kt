package com.kasirinaja.store.data.repository

import com.kasirinaja.core.network.UserApi
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
}
