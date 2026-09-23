package com.poskedai.store.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

/**
 * Overlay banner sinkronisasi melayang di bawah header.
 * Tidak menggeser konten utama — muncul melayang di atas.
 * Versi minimalis: Hanya 3 dot hijau gelap dengan bayangan (tanpa background).
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "", // Teks dihilangkan sesuai permintaan
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        // Container transparan, hanya menampilkan dot-nya saja
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsatingDotsLoader()
        }
    }
}

/**
 * Animasi 3 dot pulsating — warna hijau gelap dengan bayangan.
 */
@Composable
fun PulsatingDotsLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_loader")

    val delays = listOf(0, 600, 1200)

    val scales = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1800
                    1f    at 0
                    0.35f at 600
                    1f    at 1200
                    1f    at 1800
                },
                initialStartOffset = StartOffset(delay)
            ),
            label = "scale_dot_$delay"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp), // Jarak antar dot sedikit dilebarkan
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEach { scale ->
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .shadow(
                        elevation = 8.dp, // Shadow hitam bawaan compose
                        shape = CircleShape,
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary) // Hijau tua seperti dashboard
            )
        }
    }
}
