package com.poskedai.store.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poskedai.store.ui.theme.GreenPrimary

/**
 * Overlay sinkronisasi melayang di bawah header — moving bar loader.
 * Konversi dari CSS Uiverse.io by satyamchaudharydev:
 * track abu transparan, bar hijau tema tumbuh kiri->kanan lalu menyusut ke kanan.
 * Tanpa teks, tanpa background card.
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "", // tidak dipakai — animasi tanpa teks (param dijaga demi kompatibilitas)
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        MovingBarLoader(
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

/**
 * Moving bar loader — konversi persis dari CSS:
 * .loader            -> track 130x4dp, radius 30, rgba(0,0,0,0.2)
 * .loader::before    -> bar hijau tema
 * @keyframes moving  -> 0%: width 0 (kiri) -> 50%: width 100% -> 100%: width 0 (kanan)
 */
@Composable
fun MovingBarLoader(
    modifier: Modifier = Modifier,
    barWidth: Dp = 130.dp,
    barHeight: Dp = 4.dp,
    color: Color = GreenPrimary
) {
    val transition = rememberInfiniteTransition(label = "moving_bar")

    // CSS: animation: moving 1s ease-in-out infinite
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moving"
    )

    // Interpolasi keyframes "moving":
    // t <= 0.5 -> bar tumbuh dari kiri (width 0% -> 100%)
    // t >  0.5 -> bar menyusut menempel kanan (width 100% -> 0%)
    val alignEnd = t > 0.5f
    val widthFraction = if (t <= 0.5f) t / 0.5f else 1f - (t - 0.5f) / 0.5f

    Box(
        modifier = modifier
            .width(barWidth)
            .height(barHeight)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .align(if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(widthFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(30.dp))
                .background(color)
        )
    }
}
