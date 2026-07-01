package com.kasirinaja.store.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenPrimaryLight,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = GreenPrimaryDark,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = LightBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black
)

@Composable
fun KasirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LightColorScheme // Force light scheme for simplicity based on requirements, or define DarkColorScheme if needed
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
