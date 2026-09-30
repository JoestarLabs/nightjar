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

/**
 * Builds a sine-wave-shaped [Path] using cubic Bézier segments — one [Path.cubicTo] per
 * half-wavelength — instead of a per-pixel [Path.lineTo] loop.
 *
 * A sine/cosine wave of one period [2π] has an optimal Bézier approximation using 4 cubic
 * segments (one per quarter-period). Using one segment per **half**-period keeps the segment
 * count low and the visual error imperceptible at display resolution.
 *
 * @param path      Reusable [Path] instance (caller must call [Path.reset] first).
 * @param width     Canvas width in pixels.
 * @param baseLineY Y coordinate of the water surface mid-line (before tilt).
 * @param tiltOffset Left-to-right tilt delta in pixels.
 * @param bottomExtension Y coordinate to use for the bottom of the filled shape.
 * @param amplitude Peak deviation of the wave in pixels.
 * @param frequency Spatial frequency of the wave (radians per pixel).
 * @param phase     Current phase offset (0..2π) driven by the animation.
 * @param useCosine When true uses cos instead of sin (front wave), otherwise sin (back wave).
 */
private fun buildWavePath(
    path: Path,
    width: Float,
    baseLineY: Float,
    tiltOffset: Float,
    bottomExtension: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    useCosine: Boolean
) {
    // Half-wavelength in pixels: how far along x until the wave completes half a period.
    // λ/2 = π / frequency
    val halfWavelength = (Math.PI / frequency).toFloat()

    // For a sine (or cosine) wave, the optimal single cubic Bézier approximation over [0, π]
    // uses control points at x = π/3 and x = 2π/3 with a y-scaling factor of 4/3.
    // This gives max error < 0.2% of amplitude — visually perfect at display resolution.
    val controlYScale = (4f / 3f)

    path.moveTo(0f, bottomExtension)
    path.lineTo(0f, yAtX(0f, baseLineY, tiltOffset, width, amplitude, frequency, phase, useCosine))

    var segStart = 0f
    while (segStart < width) {
        val segEnd = (segStart + halfWavelength).coerceAtMost(width)
        val segLen = segEnd - segStart

        val xCtrl1 = segStart + segLen / 3f
        val xCtrl2 = segStart + 2f * segLen / 3f

        // Control point Y: the sine/cosine peak sits at the quarter-point of the half-period,
        // so ctrl1 mirrors the peak and ctrl2 mirrors the trough (opposite sign).
        val yMid = yAtX(segStart + segLen / 2f, baseLineY, tiltOffset, width, amplitude, frequency, phase, useCosine)
        // Adjust control-point Y for Bézier overshoot (scale by 4/3 relative to the midpoint)
        val yStart = yAtX(segStart, baseLineY, tiltOffset, width, amplitude, frequency, phase, useCosine)
        val yEnd   = yAtX(segEnd,   baseLineY, tiltOffset, width, amplitude, frequency, phase, useCosine)
        val yCtrl1 = yStart + controlYScale * (yMid - (yStart + yEnd) / 2f)
        val yCtrl2 = yEnd   + controlYScale * (yMid - (yStart + yEnd) / 2f)

        path.cubicTo(xCtrl1, yCtrl1, xCtrl2, yCtrl2, segEnd, yEnd)
        segStart = segEnd
    }

    path.lineTo(width, bottomExtension)
    path.close()
}

/** Returns the on-wave Y value at the given x pixel coordinate. */
private fun yAtX(
    x: Float,
    baseLineY: Float,
    tiltOffset: Float,
    width: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    useCosine: Boolean
): Float {
    val currentLineY = baseLineY + (1f - 2f * x / width) * tiltOffset
    val angle = x * frequency + phase
    return currentLineY + if (useCosine) kotlin.math.cos(angle) * amplitude
                          else           kotlin.math.sin(angle) * (amplitude * 0.8f)
}

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
                    buildWavePath(
                        path           = backPath,
                        width          = width,
                        baseLineY      = baseLineY,
                        tiltOffset     = tiltOffset,
                        bottomExtension = bottomExtension,
                        amplitude      = waveAmplitude,
                        frequency      = waveFrequency,
                        phase          = backWavePhase,
                        useCosine      = false
                    )

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
            buildWavePath(
                path           = frontPath,
                width          = width,
                baseLineY      = baseLineY,
                tiltOffset     = tiltOffset,
                bottomExtension = bottomExtension,
                amplitude      = waveAmplitude,
                frequency      = waveFrequency,
                phase          = wavePhase,
                useCosine      = true
            )

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
