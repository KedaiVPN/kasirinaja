package com.poskedai.admin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.poskedai.admin.MainActivity
import com.poskedai.admin.R
import com.poskedai.core.network.RetrofitClient
import com.poskedai.core.network.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AdminFCMService", "Refreshed token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.authApi

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!tokenManager.getToken().isNullOrEmpty()) {
                    val request = mapOf("fcm_token" to token)
                    authApi.updateFcmToken(request)
                    Log.d("AdminFCMService", "FCM token updated to backend")
                }
            } catch (e: Exception) {
                Log.e("AdminFCMService", "Failed to update FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("AdminFCMService", "Message received from: ${remoteMessage.from}")

        // Handle force‑logout push (sent when the store is deleted)
        val forceLogout = remoteMessage.data["type"] == "force_logout"
        if (forceLogout) {
            Log.d("AdminFCMService", "Force‑logout command received – clearing local auth data")
            TokenManager(applicationContext).clearToken()
            // Optional: open Login screen (MainActivity will redirect based on auth state)
            val loginIntent = android.content.Intent(this, com.poskedai.admin.MainActivity::class.java)
            loginIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(loginIntent)
            return
        }

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Pemberitahuan Admin"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        if (body.isNotEmpty() || remoteMessage.notification != null) {
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val uniqueId = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, uniqueId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "admin_pro_alerts"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Admin Pro Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(uniqueId, notificationBuilder.build())
    }
}
