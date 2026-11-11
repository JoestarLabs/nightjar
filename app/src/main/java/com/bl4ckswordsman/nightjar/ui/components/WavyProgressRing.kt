/*
 * Inspired by Morphe Manager (https://github.com/MorpheApp/morphe-manager)
 * app/src/main/java/app/morphe/manager/ui/screen/shared/WavyProgress.kt
 */

package com.bl4ckswordsman.nightjar.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default stroke width for the wavy progress ring. */
private val DefaultRingStrokeWidth = 16.dp

/** Default wavelength along the circumference of the ring. */
private val DefaultWavelength = 32.dp

/** Default wave amplitude (0f = flat circle, 1f = full wave depth). */
private const val DefaultAmplitude = 0.65f

/**
 * Expressive circular wavy progress indicator inspired by Morphe Manager.
 *
 * Sized by [modifier] to wrap the dial and host countdown statistics at the centre.
 * Supports smooth reverse progression during timer countdowns.
 * The wave undulates while running, flattening when accessibility services request reduced motion.
 *
 * @param progress Lambda returning the progress fraction (0.0f to 1.0f).
 *                 For countdown timers, this represents remaining fraction.
 *                 Pass null for indeterminate spinning animation.
 * @param modifier Modifier applied to the indicator container.
 * @param wavelength Length of one wave along the ring circumference.
 * @param amplitude Height of the wave as a fraction [0f..1f].
 * @param strokeWidth Thickness of the ring and track stroke.
 * @param color Active indicator wave color.
 * @param trackColor Color for the remaining track. Pass [Color.Transparent] to hide track.
 * @param gapSize Spacing between the active wave and inactive track.
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
) {
    val density = LocalDensity.current
    val stroke = remember(density, strokeWidth) {
        Stroke(width = with(density) { strokeWidth.toPx() }, cap = StrokeCap.Round)
    }
    val waveHeight = rememberWaveHeight(amplitude)

    if (progress != null) {
        CircularWavyProgressIndicator(
            progress = progress,
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = gapSize,
            amplitude = { (if (it < 0.03f) (it / 0.03f) else 1f) * waveHeight },
            wavelength = wavelength,
        )
    } else {
        CircularWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = gapSize,
            amplitude = waveHeight,
            wavelength = wavelength,
        )
    }
}

/**
 * Detects if accessibility services (like TalkBack or touch exploration) are active.
 * Updates reactively when the user toggles accessibility settings.
 */
@Composable
fun rememberAccessibilityEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var enabled by remember {
        mutableStateOf(manager.isEnabled && manager.isTouchExplorationEnabled)
    }
    DisposableEffect(manager) {
        val stateListener = AccessibilityManager.AccessibilityStateChangeListener {
            enabled = manager.isEnabled && manager.isTouchExplorationEnabled
        }
        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener {
            enabled = manager.isEnabled && manager.isTouchExplorationEnabled
        }
        manager.addAccessibilityStateChangeListener(stateListener)
        manager.addTouchExplorationStateChangeListener(touchListener)
        onDispose {
            manager.removeAccessibilityStateChangeListener(stateListener)
            manager.removeTouchExplorationStateChangeListener(touchListener)
        }
    }
    return enabled
}

/**
 * Returns [amplitude] when full motion is allowed, or 0f when accessibility services
 * request reduced motion.
 */
@Composable
fun rememberWaveHeight(amplitude: Float): Float =
    if (rememberAccessibilityEnabled()) 0f else amplitude
