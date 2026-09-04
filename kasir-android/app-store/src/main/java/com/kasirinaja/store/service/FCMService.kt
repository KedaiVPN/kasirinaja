package com.kasirinaja.store.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kasirinaja.core.network.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kasirinaja.core.network.RetrofitClient

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")

        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.authApi

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (tokenManager.getToken() != null) {
                    val request = mapOf("fcm_token" to token)
                    authApi.updateFcmToken(request)
                    Log.d("FCMService", "FCM token updated to backend")
                }
            } catch (e: Exception) {
                Log.e("FCMService", "Failed to update FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Firebase automatically shows notification when app is in background.
        // For foreground, we could show a local notification here if needed.
        Log.d("FCMService", "Message received from: ${remoteMessage.from}")

        if (remoteMessage.notification != null) {
            Log.d("FCMService", "Message Notification Body: ${remoteMessage.notification?.body}")
        }
    }
}
