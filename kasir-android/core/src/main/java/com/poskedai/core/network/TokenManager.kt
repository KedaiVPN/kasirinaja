package com.poskedai.core.network

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

    fun saveProStatus(isPro: Boolean, expiresAt: String?) {
        prefs.edit()
            .putBoolean("is_pro", isPro)
            .putString("pro_expires_at", expiresAt)
            .apply()
    }

    fun getIsPro(): Boolean {
        val isPro = prefs.getBoolean("is_pro", false)
        val expiresAtStr = prefs.getString("pro_expires_at", null)
        if (!isPro) return false
        if (expiresAtStr.isNullOrEmpty()) return isPro

        return try {
            val cleanDateStr = expiresAtStr.take(19)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val expiryDate = format.parse(cleanDateStr)
            if (expiryDate != null) {
                expiryDate.after(java.util.Date())
            } else {
                isPro
            }
        } catch (e: Exception) {
            isPro
        }
    }

    fun getProExpiresAt(): String? {
        return prefs.getString("pro_expires_at", null)
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
        prefs.edit().remove("is_pro").apply()
        prefs.edit().remove("pro_expires_at").apply()
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
