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
 * Tidak menggeser konten utama — muncul melayang di atas.
 * Icon cloud-sync (awan + panah circular berputar) dengan background putih + teks.
 */
@Composable
fun SyncBanner(
    isVisible: Boolean,
    message: String = "Sinkronisasi dimulai...",
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
                CloudSyncIcon(
                    size = 34.dp,
                    cloudColor = GreenPrimary,
                    arrowColor = GreenPrimaryLight
                )
                Spacer(modifier = Modifier.width(14.dp))
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

/**
 * Icon awan + panah circular yang berputar.
 * Konversi dari CSS cloud-sync loader (Uiverse.io by andrew-manzyk).
 */
@Composable
fun CloudSyncIcon(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    cloudColor: Color,
    arrowColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_sync")

    // Rotasi panah circular — 1 putaran penuh per 1.2 detik
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_rotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cloudCenterY = h * 0.38f

        // --- Awan: 3 lingkaran bertumpuk + dasar rounded rect ---
        val rBig = w * 0.22f
        val rSmall = w * 0.15f

        // Lingkaran kiri
        drawCircle(
            cloudColor,
            radius = rSmall,
            center = Offset(cx - w * 0.22f, cloudCenterY - rSmall * 0.2f)
        )
        // Lingkaran tengah (paling besar)
        drawCircle(
            cloudColor,
            radius = rBig,
            center = Offset(cx, cloudCenterY - rBig * 0.55f)
        )
        // Lingkaran kanan
        drawCircle(
            cloudColor,
            radius = rSmall,
            center = Offset(cx + w * 0.22f, cloudCenterY - rSmall * 0.2f)
        )
        // Badan awan (rounded rect)
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(cx - w * 0.32f, cloudCenterY - rSmall * 0.2f),
            size = Size(w * 0.64f, rBig * 0.85f),
            cornerRadius = CornerRadius(rBig * 0.4f, rBig * 0.4f)
        )

        // --- Panah circular berputar di bawah awan ---
        val arrowCenterY = h * 0.72f
        val arrowRadius = w * 0.28f
        val strokeW = w * 0.075f

        rotate(rotation, pivot = Offset(cx, arrowCenterY)) {
            // Busur (arc) — hampir penuh (300 derajat)
            drawArc(
                color = arrowColor,
                startAngle = -40f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(cx - arrowRadius, arrowCenterY - arrowRadius),
                size = Size(arrowRadius * 2f, arrowRadius * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Kepala panah di ujung busur
            // Sudut akhir: -40 + 300 = 260 derajat
            val endAngleRad = Math.toRadians(260.0)
            val endX = cx + arrowRadius * cos(endAngleRad).toFloat()
            val endY = arrowCenterY + arrowRadius * sin(endAngleRad).toFloat()

            // Arah tangen (searah jarum jam pada koordinat layar)
            val tx = -sin(endAngleRad).toFloat()
            val ty = cos(endAngleRad).toFloat()

            val headLen = w * 0.10f
            val headWidth = w * 0.07f
            val tipX = endX + tx * headLen
            val tipY = endY + ty * headLen
            val px = -ty * headWidth
            val py = tx * headWidth

            val head = Path().apply {
                moveTo(tipX, tipY)
                lineTo(endX + px, endY + py)
                lineTo(endX - px, endY - py)
                close()
            }
            drawPath(head, arrowColor)
        }
    }
}