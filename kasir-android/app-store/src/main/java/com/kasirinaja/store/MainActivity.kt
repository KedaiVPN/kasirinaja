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
                    MainScreen()
                }
            }
        }
    }
}
