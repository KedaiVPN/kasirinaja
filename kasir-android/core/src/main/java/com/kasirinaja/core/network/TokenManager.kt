package com.kasirinaja.core.network

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun saveStoreId(storeId: String) {
        prefs.edit().putString("store_id", storeId).apply()
    }

    fun getStoreId(): String? {
        return prefs.getString("store_id", null)
    }

    fun saveStoreName(storeName: String) {
        prefs.edit().putString("store_name", storeName).apply()
    }

    fun getStoreName(): String? {
        return prefs.getString("store_name", null)
    }

    fun saveStoreAddress(storeAddress: String) {
        prefs.edit().putString("store_address", storeAddress).apply()
    }

    fun getStoreAddress(): String? {
        return prefs.getString("store_address", null)
    }

    fun saveRole(role: String) {
        prefs.edit().putString("role", role).apply()
    }

    fun getRole(): String? {
        return prefs.getString("role", null)
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun clearToken() {
        prefs.edit().remove("jwt_token").apply()
        prefs.edit().remove("role").apply()
    }
}
