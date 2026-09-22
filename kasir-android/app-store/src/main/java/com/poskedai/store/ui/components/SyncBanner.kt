package com.poskedai.store.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

/**
 * Banner sinkronisasi yang tampil di bawah header.
 * Menampilkan animasi 3 dot pulsating hijau saat sinkronisasi berlangsung.
 * User tetap bisa beraktivitas karena bukan dialog/blocking UI.
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "Sedang menyinkronkan transaksi...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300, easing = EaseOutCubic)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(250, easing = EaseInCubic)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE8F5E9), // hijau sangat muda di kiri
                            Color(0xFFF1F8E9), // sedikit lebih terang di tengah
                            Color(0xFFE8F5E9)  // kembali di kanan
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PulsatingDotsLoader()
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E7D32), // hijau gelap
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Animasi 3 dot pulsating – konversi dari CSS loader.
 * Setiap dot scale dari 1.0 → 0.3 → 1.0 dengan delay berbeda.
 */
@Composable
fun PulsatingDotsLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_loader")

    // Warna hijau tema aplikasi
    val dotColors = listOf(
        Color(0xFF4CAF50), // hijau medium
        Color(0xFF388E3C), // hijau lebih gelap
        Color(0xFF2E7D32)  // hijau paling gelap
    )

    // Delay per dot: 0ms, 1000ms, 2000ms (sama seperti CSS #one, #two, #three)
    val delays = listOf(0, 1000, 2000)

    val scales = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    1f   at 0    using FastOutSlowInEasing
                    0.3f at 1000 using FastOutSlowInEasing
                    1f   at 2000 using FastOutSlowInEasing
                    1f   at 3000
                },
                initialStartOffset = StartOffset(delay)
            ),
            label = "scale_dot_$delay"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                dotColors[index],
                                dotColors[index].copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }
    }
}
