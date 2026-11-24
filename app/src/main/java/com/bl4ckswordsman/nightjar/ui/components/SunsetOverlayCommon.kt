package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R

/**
 * State holders for wall-clock sunset overlay progress and remaining seconds.
 * Reading [progress] and [frameTimeMillis] inside draw/graphicsLayer scopes avoids
 * recomposing the entire overlay tree at 60–120fps.
 */
@Stable
internal class SunsetCountdownState(
    val progress: FloatState,
    val displayRemaining: LongState,
    val elapsedSeconds: FloatState,
)

internal fun computeFallbackOverlayProgress(remainingSeconds: Long, totalSeconds: Long): Float =
    if (totalSeconds > 0L) ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    else 1f

/**
 * Computes relative elapsed seconds from [anchorMs] without losing Float32 precision on epoch timestamps.
 */
internal fun computeRelativeElapsedSeconds(nowMs: Long, anchorMs: Long): Float =
    (nowMs - anchorMs).coerceAtLeast(0L) / 1_000f

/**
 * Tracks wall-clock progress and remaining seconds at vsync rate when [startedAtMillis] is provided,
 * and falls back cleanly to [remainingSeconds]/[totalSeconds] updates otherwise.
 */
@Composable
internal fun rememberSunsetCountdownState(
    remainingSeconds: Long,
    totalSeconds: Long,
    startedAtMillis: Long?,
): SunsetCountdownState {
    val fallbackAnchorMs = remember { System.currentTimeMillis() }
    val anchorMs = startedAtMillis ?: fallbackAnchorMs

    val continuousProgress = remember {
        mutableFloatStateOf(
            if (totalSeconds > 0L && startedAtMillis != null) {
                (computeRelativeElapsedSeconds(System.currentTimeMillis(), startedAtMillis) / totalSeconds)
                    .coerceIn(0f, 1f)
            } else {
                computeFallbackOverlayProgress(remainingSeconds, totalSeconds)
            }
        )
    }
    val displayRemaining = remember { mutableLongStateOf(remainingSeconds) }
    val elapsedSeconds = remember {
        mutableFloatStateOf(computeRelativeElapsedSeconds(System.currentTimeMillis(), anchorMs))
    }

    LaunchedEffect(startedAtMillis, totalSeconds, remainingSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) {
            continuousProgress.floatValue = computeFallbackOverlayProgress(remainingSeconds, totalSeconds)
            displayRemaining.longValue = remainingSeconds.coerceAtLeast(0L)
            elapsedSeconds.floatValue = computeRelativeElapsedSeconds(System.currentTimeMillis(), anchorMs)
        }
    }

    LaunchedEffect(startedAtMillis, totalSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                val now = System.currentTimeMillis()
                val elapsedMs = (now - startedAtMillis).coerceAtLeast(0L)
                elapsedSeconds.floatValue = elapsedMs / 1_000f
                continuousProgress.floatValue =
                    (elapsedMs.toFloat() / (totalSeconds * 1_000f)).coerceIn(0f, 1f)
                displayRemaining.longValue =
                    ((totalSeconds * 1_000L - elapsedMs) / 1_000L).coerceAtLeast(0L)
            }
            if (continuousProgress.floatValue >= 1f) break
        }
    }

    return remember {
        SunsetCountdownState(
            progress = continuousProgress,
            displayRemaining = displayRemaining,
            elapsedSeconds = elapsedSeconds,
        )
    }
}

/**
 * Centered countdown number and warning label shared across all sunset warning overlays.
 */
@Composable
internal fun SunsetCountdownOverlayText(
    shownRemaining: Long,
    outlineColor: Color,
    modifier: Modifier = Modifier,
    rotationDegrees: (() -> Float)? = null,
    expandSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    ),
    settleSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    ),
) {
    val textScale = remember { Animatable(1f) }
    LaunchedEffect(shownRemaining) {
        textScale.animateTo(targetValue = 1.2f, animationSpec = expandSpec)
        textScale.animateTo(targetValue = 1.0f, animationSpec = settleSpec)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (rotationDegrees != null) {
                Modifier.graphicsLayer { rotationZ = rotationDegrees() }
            } else {
                Modifier
            }
        ) {
            OutlinedText(
                text = shownRemaining.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 120.sp,
                    lineHeight = 120.sp
                ),
                outlineColor = outlineColor,
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
                outlineColor = outlineColor,
                fillColor = Color.White,
                outlineWidth = 8f
            )
        }
    }
}

/**
 * Appends smooth cubic Bézier segments along [xStart]..[xEnd] sampling [waveY],
 * optionally mirroring the same curve onto [secondaryPath] (e.g. a crest highlight).
 */
internal inline fun buildCubicWaveCurve(
    path: Path,
    xStart: Float,
    xEnd: Float,
    step: Float,
    secondaryPath: Path? = null,
    waveY: (Float) -> Float,
) {
    var currentX = xStart
    while (currentX < xEnd) {
        val nextX = (currentX + step).coerceAtMost(xEnd)
        val midX = (currentX + nextX) / 2f

        val yCurrent = waveY(currentX)
        val yNext = waveY(nextX)
        val yMid = waveY(midX)

        val ctrl1Y = yCurrent + (4f / 3f) * (yMid - (yCurrent + yNext) / 2f)
        val ctrl2Y = yNext + (4f / 3f) * (yMid - (yCurrent + yNext) / 2f)

        val ctrl1X = currentX + (nextX - currentX) / 3f
        val ctrl2X = currentX + 2f * (nextX - currentX) / 3f

        path.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, nextX, yNext)
        secondaryPath?.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, nextX, yNext)
        currentX = nextX
    }
}
