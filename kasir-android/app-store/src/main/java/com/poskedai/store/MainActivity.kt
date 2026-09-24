package com.poskedai.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.poskedai.store.ui.MainScreen
import com.poskedai.store.ui.theme.KasirTheme
import androidx.compose.runtime.mutableStateOf

import com.poskedai.core.network.RetrofitClient
import com.poskedai.core.network.TokenManager

class MainActivity : ComponentActivity() {
    private val currentRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(this)
        RetrofitClient.initialize { tokenManager.getToken() }

        handleIntent(intent)

        setContent {
            KasirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(initialRoute = currentRoute.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val route = intent.getStringExtra("route")
        val transactionId = intent.getStringExtra("transaction_id")
        val storeBlockedAlert = intent.getBooleanExtra("store_blocked_alert", false)
        val storeBlockedTitle = intent.getStringExtra("store_blocked_title")
        val storeBlockedMessage = intent.getStringExtra("store_blocked_message")

        var finalRoute = route
        if (route == "receipt" && transactionId != null) {
            finalRoute = "receipt/$transactionId"
        }

        currentRoute.value = finalRoute

        if (storeBlockedAlert && storeBlockedMessage != null) {
            Toast.makeText(this, storeBlockedMessage, Toast.LENGTH_LONG).show()
        }
    }
}