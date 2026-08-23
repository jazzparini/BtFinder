package com.example.btfinder.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.btfinder.domain.Proximity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val RadarBackground = Color(0xFF041B12)
private val RadarGrid = Color(0xFF2E7D5B)
private val RadarSweep = Color(0xFF35F27A)
private val RadarBlip = Color(0xFF35F27A)

/**
 * Vista tipo radar/sonar para ubicar el audífono cuando se pierde de vista.
 * BLE no da orientación real, así que el radio del "blip" representa la
 * proximidad estimada (RF-05) y el ángulo es estable por dispositivo
 * seleccionado: solo da sensación de barrido, no una dirección real.
 */
@Composable
fun RadarView(
    proximity: Proximity,
    deviceKey: String?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "sweep-angle"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blip-pulse"
    )

    val blipAngleDeg = remember(deviceKey) { Random.nextFloat() * 360f }

    val targetRadiusFraction = when (proximity) {
        Proximity.CONNECTED, Proximity.VERY_CLOSE -> 0.14f
        Proximity.CLOSE -> 0.4f
        Proximity.FAR -> 0.65f
        Proximity.WEAK -> 0.88f
        Proximity.NOT_FOUND -> 1.15f
    }

    val radiusFraction by animateFloatAsState(
        targetValue = targetRadiusFraction,
        animationSpec = tween(durationMillis = 600),
        label = "blip-radius"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = RadarBackground, radius = radius, center = center)

        val rings = 4
        for (i in 1..rings) {
            drawCircle(
                color = RadarGrid.copy(alpha = 0.55f),
                radius = radius * i / rings,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        drawLine(
            color = RadarGrid.copy(alpha = 0.45f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = RadarGrid.copy(alpha = 0.45f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.5f
        )

        rotate(degrees = sweepAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.82f to Color.Transparent,
                    1f to RadarSweep.copy(alpha = 0.4f),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        drawCircle(color = RadarSweep, radius = 6f, center = center)

        if (proximity != Proximity.NOT_FOUND) {
            val blipRadiusPx = radius * radiusFraction
            val angleRad = Math.toRadians(blipAngleDeg.toDouble())
            val blipCenter = Offset(
                x = center.x + (blipRadiusPx * cos(angleRad)).toFloat(),
                y = center.y + (blipRadiusPx * sin(angleRad)).toFloat()
            )

            drawCircle(
                color = RadarBlip.copy(alpha = 0.25f),
                radius = 18f * pulse,
                center = blipCenter
            )
            drawCircle(
                color = RadarBlip,
                radius = 9f,
                center = blipCenter
            )
        }
    }
}
