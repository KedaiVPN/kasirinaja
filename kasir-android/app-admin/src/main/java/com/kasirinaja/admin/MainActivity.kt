package com.kasirinaja.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kasirinaja.admin.ui.auth.LoginScreen
import com.kasirinaja.admin.ui.main.MainScreen
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        RetrofitClient.initialize { tokenManager.getToken() }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminApp(tokenManager)
                }
            }
        }
    }
}

@Composable
fun AdminApp(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val startDestination = if (tokenManager.getToken().isNullOrEmpty()) "login" else "main"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                onLogout = {
                    tokenManager.clearToken()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
