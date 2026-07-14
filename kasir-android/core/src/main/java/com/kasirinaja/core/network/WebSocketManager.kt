package com.kasirinaja.core.network

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketManager(private val onSyncProduct: () -> Unit) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect(storeId: String) {
        val url = RetrofitClient.BASE_URL.replace("http", "ws").replace("/api/", "/api/ws?store_id=$storeId")
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected for store: $storeId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "SYNC_PRODUCT") {
                        onSyncProduct()
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Message Parse Error: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Disconnected: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
                // Attempt to reconnect later in a production app
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User logged out")
        webSocket = null
    }
}
