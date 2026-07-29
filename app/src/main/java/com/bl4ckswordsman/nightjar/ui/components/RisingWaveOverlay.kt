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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
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
    /** Epoch-ms when the timer started. When non-null, progress is computed from the
     *  wall clock every frame so the wave height is always perfectly in sync. */
    startedAtMillis: Long? = null,
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

    // ── Wall-clock progress (updated every display frame) ────────────────────
    // When startedAtMillis is available we compute progress directly from the wall
    // clock at vsync rate. This eliminates the 0.5–1 s lag that a spring/tween
    // chasing 1-second ticks would introduce. Fall back to a simple calculation
    // if the caller doesn't provide the start time.
    val continuousProgress = remember { mutableFloatStateOf(
        if (totalSeconds > 0L && startedAtMillis != null) {
            val elapsed = (System.currentTimeMillis() - startedAtMillis) / 1_000f
            (elapsed / totalSeconds).coerceIn(0f, 1f)
        } else if (totalSeconds > 0L) {
            (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
        } else 1f
    ) }
    val displayRemaining = remember { mutableLongStateOf(remainingSeconds) }

    LaunchedEffect(startedAtMillis, totalSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) return@LaunchedEffect
        while (true) {
            // withFrameMillis fires each vsync frame but its timestamp is from
            // System.nanoTime() — a monotonic clock with an arbitrary epoch that CANNOT
            // be subtracted from the wall-clock startedAtMillis. Use it only as a
            // vsync synchronisation point and read System.currentTimeMillis() instead.
            withFrameMillis {
                val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
                continuousProgress.floatValue =
                    (elapsed.toFloat() / (totalSeconds * 1_000f)).coerceIn(0f, 1f)
                // displayRemaining must use the SAME formula as the Chronometer:
                // floor((endTimeMs - now) / 1000) = floor((totalSeconds*1000 - elapsed) / 1000).
                // Using totalSeconds - elapsed/1000 is NOT equivalent: floor(A-B) ≠ A - floor(B).
                displayRemaining.longValue = ((totalSeconds * 1_000L - elapsed) / 1_000L).coerceAtLeast(0L)
            }
            if (continuousProgress.floatValue >= 1f) break
        }
    }

    val animatedProgress = continuousProgress.floatValue
    // Wall-clock remaining to display — matches the notification Chronometer exactly.
    // Falls back to the service-pushed remainingSeconds if startedAtMillis isn't available.
    val shownRemaining = if (startedAtMillis != null) displayRemaining.longValue else remainingSeconds

    // Smoothly animate tilt changes
    val animatedTilt by animateFloatAsState(
        targetValue = tilt,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wave_tilt"
    )

    // Pulse animation for the countdown text — keyed on shownRemaining so it fires at the
    // exact wall-clock second boundary, matching the notification Chronometer.
    val textScale = remember { Animatable(1f) }
    LaunchedEffect(shownRemaining) {
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

    val backPath = remember { Path() }
    val frontPath = remember { Path() }

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
                    backPath.reset()
                    backPath.moveTo(0f, bottomExtension)
                    backPath.lineTo(0f, baseLineY + tiltOffset)

                    val step = 10f
                    var x = 0f
                    while (x <= width) {
                        val currentLineY = baseLineY + (1f - 2f * x / width) * tiltOffset
                        val angle = x * waveFrequency + backWavePhase
                        val y = currentLineY + kotlin.math.sin(angle) * (waveAmplitude * 0.8f)
                        backPath.lineTo(x, y)
                        x += step
                    }
                    backPath.lineTo(width, bottomExtension)
                    backPath.close()

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
            frontPath.reset()
            frontPath.moveTo(0f, bottomExtension)
            frontPath.lineTo(0f, baseLineY + tiltOffset)

            x = 0f
            while (x <= width) {
                val currentLineY = baseLineY + (1f - 2f * x / width) * tiltOffset
                val angle = x * waveFrequency + wavePhase
                val y =
                    currentLineY + kotlin.math.cos(angle) * waveAmplitude
                frontPath.lineTo(x, y)
                x += step
            }
            frontPath.lineTo(width, bottomExtension)
            frontPath.close()

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
                    text = shownRemaining.toString(),
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
