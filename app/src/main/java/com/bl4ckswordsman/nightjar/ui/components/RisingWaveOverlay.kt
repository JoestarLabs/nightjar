package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R

@Composable
fun RisingWaveOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    tilt: Float,
    modifier: Modifier = Modifier
) {
    // Phase for wave ripple animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave_oscillation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Phase for the back wave (slightly different timing)
    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "back_wave_phase"
    )

    // Progress goes from 0f (empty) to 1f (full screen cover)
    val targetProgress = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    } else {
        1f
    }

    // Smoothly animate height changes
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "wave_height_progress"
    )

    // Smoothly animate tilt changes
    val animatedTilt by animateFloatAsState(
        targetValue = tilt,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wave_tilt"
    )

    // Pulse animation for the countdown text
    val textScale = remember { Animatable(1f) }
    LaunchedEffect(remainingSeconds) {
        textScale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        textScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f * animatedProgress)) // Gradual dimming of the background
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate base Y position of the liquid wave
            val baseLineY = height - (animatedProgress * height)

            // Tilt calculations (negate animatedTilt to reverse tilt direction)
            val maxTiltOffset = 120.dp.toPx()
            val tiltOffset = -animatedTilt * maxTiltOffset
            val bottomExtension = height + maxTiltOffset

            // Wave specs
            val waveAmplitude = 16.dp.toPx()
            val waveFrequency = 0.006f // controlling width/frequency of waves

            // ─── 1. Back Wave (Slightly darker, offset) ───
            val backPath = Path().apply {
                moveTo(0f, bottomExtension)
                lineTo(0f, baseLineY + tiltOffset)

                val step = 10f
                var x = 0f
                while (x <= width) {
                    val currentLineY = baseLineY + (1f - 2f * x / width) * tiltOffset
                    val angle = x * waveFrequency + backWavePhase
                    val y = currentLineY + kotlin.math.sin(angle.toDouble())
                        .toFloat() * (waveAmplitude * 0.8f)
                    lineTo(x, y)
                    x += step
                }
                lineTo(width, bottomExtension)
                close()
            }

            drawPath(
                path = backPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.65f),
                        secondaryColor.copy(alpha = 0.80f),
                        Color.Black.copy(alpha = 0.90f)
                    ),
                    startY = baseLineY - waveAmplitude - kotlin.math.abs(tiltOffset),
                    endY = height
                )
            )

            // ─── 2. Front Wave (Primary) ───
            val frontPath = Path().apply {
                moveTo(0f, bottomExtension)
                lineTo(0f, baseLineY + tiltOffset)

                val step = 10f
                var x = 0f
                while (x <= width) {
                    val currentLineY = baseLineY + (1f - 2f * x / width) * tiltOffset
                    val angle = x * waveFrequency + wavePhase
                    val y =
                        currentLineY + kotlin.math.cos(angle.toDouble()).toFloat() * waveAmplitude
                    lineTo(x, y)
                    x += step
                }
                lineTo(width, bottomExtension)
                close()
            }

            drawPath(
                path = frontPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.90f),
                        Color.Black.copy(alpha = 0.95f)
                    ),
                    startY = baseLineY - waveAmplitude - kotlin.math.abs(tiltOffset),
                    endY = height
                )
            )
        }

        // Center visual indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedText(
                    text = remainingSeconds.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 120.sp,
                        lineHeight = 120.sp
                    ),
                    outlineColor = Color.Black.copy(alpha = 0.8f),
                    fillColor = Color.White,
                    outlineWidth = 14f,
                    modifier = Modifier.graphicsLayer {
                        scaleX = textScale.value
                        scaleY = textScale.value
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedText(
                    text = stringResource(R.string.sunset_warning_text),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    outlineColor = Color.Black.copy(alpha = 0.8f),
                    fillColor = Color.White,
                    outlineWidth = 8f
                )
            }
        }
    }
}

@Composable
private fun OutlinedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    outlineColor: Color,
    fillColor: Color,
    outlineWidth: Float,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            style = style.copy(
                color = outlineColor,
                drawStyle = Stroke(
                    width = outlineWidth,
                    join = StrokeJoin.Round
                )
            )
        )
        Text(
            text = text,
            style = style.copy(
                color = fillColor
            )
        )
    }
}
