/*
 * Inspired by Morphe Manager (https://github.com/MorpheApp/morphe-manager)
 * app/src/main/java/app/morphe/manager/ui/screen/shared/WavyProgress.kt
 */

package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default stroke width for the wavy progress ring. */
private val DefaultRingStrokeWidth = 16.dp

/** Default wavelength along the circumference of the ring. */
private val DefaultWavelength = 32.dp

/** Default wave amplitude (1f = native Material 3 Expressive wave). */
private const val DefaultAmplitude = 1f

/**
 * Expressive circular wavy progress indicator inspired by Morphe Manager.
 *
 * Sized by [modifier] to wrap the dial and host countdown statistics at the centre.
 * Supports smooth reverse progression during timer countdowns.
 * The wave undulates continuously while running via native Material 3 Expressive wave motion.
 *
 * @param progress Lambda returning the progress fraction (0.0f to 1.0f).
 *                 For countdown timers, this represents remaining fraction.
 *                 Pass null for indeterminate spinning animation.
 * @param modifier Modifier applied to the indicator container.
 * @param wavelength Length of one wave along the ring circumference.
 * @param amplitude Height of the wave as a fraction [0f..1f]. Defaults to 1f for native M3 wave.
 * @param strokeWidth Thickness of the ring and track stroke.
 * @param color Active indicator wave color.
 * @param trackColor Color for the remaining track. Pass [Color.Transparent] to hide track.
 * @param gapSize Spacing between the active wave and inactive track.
 * @param invertWaveMotion If true, inverts the ripple travel direction so waves flow clockwise
 *                         while preserving the clockwise start-to-end arc span.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyProgressRing(
    progress: (() -> Float)?,
    modifier: Modifier = Modifier,
    wavelength: Dp = DefaultWavelength,
    amplitude: Float = DefaultAmplitude,
    strokeWidth: Dp = DefaultRingStrokeWidth,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    gapSize: Dp = WavyProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
    invertWaveMotion: Boolean = true,
) {
    val density = LocalDensity.current
    val stroke = remember(density, strokeWidth) {
        Stroke(width = with(density) { strokeWidth.toPx() }, cap = StrokeCap.Round)
    }

    val motionModifier = if (invertWaveMotion && progress != null) {
        modifier.graphicsLayer {
            scaleX = -1f
            rotationZ = (progress() * 360f).coerceIn(0f, 360f)
        }
    } else {
        modifier
    }

    if (progress != null) {
        CircularWavyProgressIndicator(
            progress = progress,
            modifier = motionModifier,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = gapSize,
            amplitude = { amplitude },
            wavelength = wavelength,
        )
    } else {
        CircularWavyProgressIndicator(
            modifier = motionModifier,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = gapSize,
            amplitude = amplitude,
            wavelength = wavelength,
        )
    }
}
