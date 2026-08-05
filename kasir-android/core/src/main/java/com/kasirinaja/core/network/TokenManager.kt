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

    fun saveStoreLogoUrl(logoUrl: String) {
        prefs.edit().putString("store_logo_url", logoUrl).apply()
    }

    fun getStoreLogoUrl(): String? {
        return prefs.getString("store_logo_url", null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }

    fun saveEmail(email: String) {
        prefs.edit().putString("email", email).apply()
    }

    fun getEmail(): String? {
        return prefs.getString("email", null)
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
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
        prefs.edit().remove("user_id").apply()
        prefs.edit().remove("user_name").apply()
        prefs.edit().remove("email").apply()
        prefs.edit().remove("user_photo_url").apply()
        prefs.edit().remove("store_id").apply()
        prefs.edit().remove("store_name").apply()
        prefs.edit().remove("store_address").apply()
        prefs.edit().remove("store_logo_url").apply()
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }

    fun getPhotoUrl(): String? {
        return prefs.getString("user_photo_url", null)
    }

    fun saveUserProfile(name: String?, photoUrl: String?) {
        prefs.edit().apply {
            if (name != null) putString("user_name", name)
            if (!photoUrl.isNullOrBlank()) {
                putString("user_photo_url", photoUrl)
            } else {
                remove("user_photo_url")
            }
            apply()
        }
    }
}
