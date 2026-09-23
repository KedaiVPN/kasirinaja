package com.poskedai.store.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poskedai.store.ui.theme.GreenPrimary
import com.poskedai.store.ui.theme.GreenPrimaryDark
import com.poskedai.store.ui.theme.GreenPrimaryLight
import kotlin.math.cos
import kotlin.math.sin

/**
 * Overlay banner sinkronisasi melayang di bawah header.
 * Cloud-sync icon: awan bergaris diagonal + panah circular berputar.
 * Konversi dari CSS Uiverse.io by andrew-manzyk.
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "Sinkronisasi dimulai",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CloudSyncIcon(size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimaryDark,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

@Composable
fun CloudSyncIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val transition = rememberInfiniteTransition(label = "cloud_sync")

    // Animasi 1: panah berputar 0->360 dalam 1s
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animasi 2: garis diagonal bergerak vertikal -10 -> 8 dalam ~750ms (1/1.33)
    val lineOffset by transition.animateFloat(
        initialValue = -10f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lines"
    )

    // Animasi 3: titik bergerak (cloud keyframe) 0->1 dalam 2s
    val cloudT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud"
    )

    Canvas(modifier = modifier.size(size)) {
        val sw = size.toPx() / 100f

        // === 1. BENTUK AWAN (clipped) ===
        val cloudPath = Path().apply {
            // Dasar awan — rounded rect di tengah bawah
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 22f * sw,
                    top = 40f * sw,
                    right = 78f * sw,
                    bottom = 62f * sw,
                    cornerRadius = CornerRadius(10f * sw, 10f * sw)
                )
            )
            // Bundar kiri
            addOval(androidx.compose.ui.geometry.Rect(
                left = 20f * sw, top = 35f * sw,
                right = 45f * sw, bottom = 60f * sw
            ))
            // Bundar tengah (paling besar)
            addOval(androidx.compose.ui.geometry.Rect(
                left = 35f * sw, top = 25f * sw,
                right = 65f * sw, bottom = 55f * sw
            ))
            // Bundar kanan
            addOval(androidx.compose.ui.geometry.Rect(
                left = 55f * sw, top = 35f * sw,
                right = 80f * sw, bottom = 60f * sw
            ))
        }

        // Draw cloud body (fill solid)
        drawPath(cloudPath, GreenPrimary)

        // === 2. GARIS DIAGONAL bergerak (rotate -65deg, translateY) ===
        rotate(degrees = -65f, pivot = Offset(50f * sw, 50f * sw)) {
            val yBase = 50f * sw + lineOffset * sw
            for (i in -2..3) {
                val x = (50f + i * 14f) * sw
                drawLine(
                    color = GreenPrimaryLight.copy(alpha = 0.85f),
                    start = Offset(x, yBase - 20f * sw),
                    end = Offset(x, yBase + 20f * sw),
                    strokeWidth = 5f * sw,
                    cap = StrokeCap.Round
                )
            }
        }

        // === 3. PANAH CIRCULAR berputar di bawah awan ===
        val arrowCY = 70f * sw
        val arrowR = 18f * sw
        val strokeW = 3.5f * sw

        rotate(degrees = rotation, pivot = Offset(50f * sw, arrowCY)) {
            // Busur 300 derajat
            drawArc(
                color = GreenPrimaryLight,
                startAngle = -40f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset((50f - arrowR) * sw, (arrowCY / sw - arrowR) * sw),
                size = Size(arrowR * 2f * sw, arrowR * 2f * sw),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Kepala panah
            val endRad = Math.toRadians(220.0)
            val ex = (50f + arrowR * cos(endRad)).toFloat() * sw
            val ey = (arrowCY / sw + arrowR * sin(endRad)).toFloat() * sw
            val tx = (-sin(endRad)).toFloat()
            val ty = (cos(endRad)).toFloat()

            val headLen = 7f * sw
            val tipX = ex + tx * headLen
            val tipY = ey + ty * headLen
            val px = -ty * 4f * sw
            val py = tx * 4f * sw

            val head = Path().apply {
                moveTo(tipX, tipY)
                lineTo(ex + px, ey + py)
                lineTo(ex - px, ey - py)
                close()
            }
            drawPath(head, GreenPrimaryLight)
        }
    }
}
