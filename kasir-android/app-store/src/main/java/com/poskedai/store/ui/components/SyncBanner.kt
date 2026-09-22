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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.poskedai.store.ui.theme.GreenPrimary
import com.poskedai.store.ui.theme.GreenPrimaryDark
import com.poskedai.store.ui.theme.GreenPrimaryLight

/**
 * Banner sinkronisasi yang tampil tepat di bawah header (GlobalTopAppBar).
 * Menggunakan warna tema POS Kedai (GreenPrimary) dan animasi 3 dot yang jelas terlihat.
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "Sedang menyinkronkan data transaksi...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(250)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F5E9)) // Light green background aksen
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PulsatingDotsLoader()
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = GreenPrimaryDark,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Animasi 3 dot pulsating – adaptasi CSS ke Jetpack Compose.
 * Dot lebih besar (18dp) agar jelas terlihat, dengan warna tema aplikasi.
 */
@Composable
fun PulsatingDotsLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_loader")

    // Warna dari tema POS Kedai
    val dotColors = listOf(
        GreenPrimaryLight, // Dot 1
        GreenPrimary,      // Dot 2
        GreenPrimaryDark   // Dot 3
    )

    // Delay per dot: 0ms, 600ms, 1200ms untuk ritme yang pas
    val delays = listOf(0, 600, 1200)

    val scales = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1800
                    1f   at 0
                    0.35f at 600
                    1f   at 1200
                    1f   at 1800
                },
                initialStartOffset = StartOffset(delay)
            ),
            label = "scale_dot_$delay"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(dotColors[index])
            )
        }
    }
}
