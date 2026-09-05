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
        Log.d("FCMService", "Message received from: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FCMService", "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Peringatan", it.body ?: "")
        }
    }

    private fun showNotification(title: String, messageBody: String) {
        val intent = android.content.Intent(this, com.kasirinaja.store.MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "stock_alerts"
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.kasirinaja.store.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Since Android Oreo, notification channel is needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Stock Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
