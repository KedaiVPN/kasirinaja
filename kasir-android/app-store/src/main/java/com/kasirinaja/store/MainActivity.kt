package com.kasirinaja.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kasirinaja.store.ui.MainScreen
import com.kasirinaja.store.ui.theme.KasirTheme

import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(this)
        RetrofitClient.initialize { tokenManager.getToken() }
        setContent {
            KasirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val route = intent.getStringExtra("route")
                    val transactionId = intent.getStringExtra("transaction_id")

                    var finalRoute = route
                    if (route == "receipt" && transactionId != null) {
                        finalRoute = "receipt/$transactionId"
                    }

                    MainScreen(initialRoute = finalRoute)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra("route")
        val transactionId = intent.getStringExtra("transaction_id")

        var finalRoute = route
        if (route == "receipt" && transactionId != null) {
            finalRoute = "receipt/$transactionId"
        }

        // When onNewIntent is called, recreating the view will read the new intent.
        val tokenManager = TokenManager(this)
        setContent {
            KasirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(initialRoute = finalRoute)
                }
            }
        }
    }
}
