package com.bl4ckswordsman.nightjar.ui.components

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

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
    val currentLineY = baseLineY + (1f - 2f * x / width.coerceAtLeast(1f)) * tiltOffset
    val angle = x * frequency + phase
    return currentLineY + if (useCosine) kotlin.math.cos(angle) * amplitude
                          else           kotlin.math.sin(angle) * (amplitude * 0.8f)
}

/**
 * Fullscreen classic wave sunset warning animation overlay.
 *
 * Simulates a rising liquid wave responding dynamically to device tilt as the countdown expires.
 *
 * @param remainingSeconds Seconds remaining until the timer expires.
 * @param totalSeconds Total duration of the sunset warning window.
 * @param tilt Device tilt offset factor used for wave inclination.
 * @param startedAtMillis Epoch-ms when the timer started, enabling continuous wall-clock animation.
 * @param modifier Modifier applied to the overlay container.
 */
@Composable
fun RisingWaveOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    tilt: Float,
    modifier: Modifier = Modifier,
    /** Epoch-ms when the timer started. When non-null, progress is computed from the
     *  wall clock every frame so the wave height is always perfectly in sync. */
    startedAtMillis: Long? = null,
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
    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

    // Smoothly animate tilt changes
    val animatedTilt by animateFloatAsState(
        targetValue = tilt,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wave_tilt"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val backPath = remember { Path() }
    val frontPath = remember { Path() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val animatedProgress = countdown.progress.floatValue
            drawRect(Color.Black.copy(alpha = 0.4f * animatedProgress))

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
                path            = backPath,
                width           = width,
                baseLineY       = baseLineY,
                tiltOffset      = tiltOffset,
                bottomExtension = bottomExtension,
                amplitude       = waveAmplitude,
                frequency       = waveFrequency,
                phase           = backWavePhase,
                useCosine       = false
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

        SunsetCountdownOverlayText(
            shownRemaining = shownRemaining,
            outlineColor = Color.Black.copy(alpha = 0.8f),
        )
    }
}
