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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Builds a multi-harmonic fluid liquid surface path across [xStart] to [xEnd].
 */
private fun buildFluidWavePath(
    path: Path,
    xStart: Float,
    xEnd: Float,
    baseLineY: Float,
    bottomLimit: Float,
    amp1: Float,
    freq1: Float,
    phase1: Float,
    amp2: Float,
    freq2: Float,
    phase2: Float,
    step: Float = 40f
) {
    path.reset()
    path.moveTo(xStart, bottomLimit)

    fun waveY(x: Float): Float {
        return baseLineY + sin(x * freq1 + phase1) * amp1 + cos(x * freq2 + phase2) * amp2
    }

    path.lineTo(xStart, waveY(xStart))

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
        currentX = nextX
    }

    path.lineTo(xEnd, bottomLimit)
    path.close()
}

/**
 * Builds only the surface line path for drawing a specular highlight crest.
 */
private fun buildFluidCrestLinePath(
    path: Path,
    xStart: Float,
    xEnd: Float,
    baseLineY: Float,
    amp1: Float,
    freq1: Float,
    phase1: Float,
    amp2: Float,
    freq2: Float,
    phase2: Float,
    step: Float = 40f
) {
    path.reset()
    fun waveY(x: Float): Float =
        baseLineY + sin(x * freq1 + phase1) * amp1 + cos(x * freq2 + phase2) * amp2

    path.moveTo(xStart, waveY(xStart))
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
        currentX = nextX
    }
}

/**
 * Fluid liquid sunset overlay that rotates a full 360° to track physical gravity,
 * even when held upside down. The countdown text also adapts its cardinal orientation
 * so it stays readable in any orientation.
 */
@Composable
fun FluidLiquidOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    gravityAngleRad: Float,
    startedAtMillis: Long? = null,
    modifier: Modifier = Modifier
) {
    // ── Phase oscillators for organic fluid sloshing ──
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_waves")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    // ── Wall-clock continuous progress ──
    val continuousProgress = remember {
        mutableFloatStateOf(
            if (totalSeconds > 0L && startedAtMillis != null) {
                val elapsed = (System.currentTimeMillis() - startedAtMillis) / 1_000f
                (elapsed / totalSeconds).coerceIn(0f, 1f)
            } else if (totalSeconds > 0L) {
                (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
            } else 1f
        )
    }
    val displayRemaining = remember { mutableLongStateOf(remainingSeconds) }

    LaunchedEffect(startedAtMillis, totalSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
                continuousProgress.floatValue =
                    (elapsed.toFloat() / (totalSeconds * 1_000f)).coerceIn(0f, 1f)
                displayRemaining.longValue =
                    ((totalSeconds * 1_000L - elapsed) / 1_000L).coerceAtLeast(0L)
            }
            if (continuousProgress.floatValue >= 1f) break
        }
    }

    val animatedProgress = continuousProgress.floatValue
    val shownRemaining = if (startedAtMillis != null) displayRemaining.longValue else remainingSeconds

    // ── Smooth 360° gravity angle tracking without wrapping glitches ──
    var unwrappedGravityAngle by remember { mutableFloatStateOf(gravityAngleRad) }
    LaunchedEffect(gravityAngleRad) {
        var diff = (gravityAngleRad - unwrappedGravityAngle) % (2f * PI.toFloat())
        if (diff > PI.toFloat()) diff -= (2f * PI.toFloat())
        if (diff < -PI.toFloat()) diff += (2f * PI.toFloat())
        unwrappedGravityAngle += diff
    }

    val animatedGravityAngle by animateFloatAsState(
        targetValue = unwrappedGravityAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fluid_gravity_angle"
    )

    // ── Orientation tracking for countdown text ──
    val deg = (Math.toDegrees(animatedGravityAngle.toDouble()).toFloat()) % 360f
    val normalizedDeg = if (deg > 180f) deg - 360f else if (deg < -180f) deg + 360f else deg

    val targetTextAngle = when {
        normalizedDeg > 45f && normalizedDeg <= 135f -> 90f
        normalizedDeg > 135f || normalizedDeg <= -135f -> 180f
        normalizedDeg >= -135f && normalizedDeg < -45f -> -90f
        else -> 0f
    }

    var unwrappedTextAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetTextAngle) {
        var diff = (targetTextAngle - unwrappedTextAngle) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        unwrappedTextAngle += diff
    }

    val animatedTextRotation by animateFloatAsState(
        targetValue = unwrappedTextAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "text_orientation"
    )

    // ── Pulse animation for text ──
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

    val backWavePath = remember { Path() }
    val frontWavePath = remember { Path() }
    val crestPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            val diagonal = hypot(width, height)
            val r = diagonal * 0.75f

            // Extent of screen along gravity direction at the current angle
            val absSin = abs(sin(animatedGravityAngle))
            val absCos = abs(cos(animatedGravityAngle))
            val hProj = width * absSin + height * absCos

            // Base Y in the rotated coordinate space (where +Y points along gravity)
            val bottomExtent = hProj / 2f + 24.dp.toPx()
            val topExtent = -hProj / 2f - 24.dp.toPx()
            val baseLineY = bottomExtent - animatedProgress * (bottomExtent - topExtent)

            val rotationDegrees = Math.toDegrees(animatedGravityAngle.toDouble()).toFloat()

            withTransform({
                translate(left = centerX, top = centerY)
                rotate(degrees = rotationDegrees, pivot = androidx.compose.ui.geometry.Offset.Zero)
            }) {
                val amp1 = 14.dp.toPx()
                val amp2 = 6.dp.toPx()
                val freq1 = 0.005f
                val freq2 = 0.012f

                // ── 1. Back fluid wave ──
                buildFluidWavePath(
                    path = backWavePath,
                    xStart = -r,
                    xEnd = r,
                    baseLineY = baseLineY,
                    bottomLimit = r,
                    amp1 = amp1 * 1.1f,
                    freq1 = freq1 * 0.9f,
                    phase1 = phase2,
                    amp2 = amp2 * 1.2f,
                    freq2 = freq2 * 0.85f,
                    phase2 = phase1
                )
                drawPath(
                    path = backWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.65f),
                            secondaryColor.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.92f)
                        ),
                        startY = baseLineY - amp1 - amp2,
                        endY = r
                    )
                )

                // ── 2. Front fluid wave ──
                buildFluidWavePath(
                    path = frontWavePath,
                    xStart = -r,
                    xEnd = r,
                    baseLineY = baseLineY,
                    bottomLimit = r,
                    amp1 = amp1,
                    freq1 = freq1,
                    phase1 = phase1,
                    amp2 = amp2,
                    freq2 = freq2,
                    phase2 = phase2
                )
                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.88f),
                            secondaryColor.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.96f)
                        ),
                        startY = baseLineY - amp1 - amp2,
                        endY = r
                    )
                )

                // ── 3. Specular crest highlight ──
                buildFluidCrestLinePath(
                    path = crestPath,
                    xStart = -r,
                    xEnd = r,
                    baseLineY = baseLineY,
                    amp1 = amp1,
                    freq1 = freq1,
                    phase1 = phase1,
                    amp2 = amp2,
                    freq2 = freq2,
                    phase2 = phase2
                )
                drawPath(
                    path = crestPath,
                    color = primaryColor.copy(alpha = 0.75f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Center visual indicator with animated orientation
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    rotationZ = animatedTextRotation
                }
            ) {
                OutlinedText(
                    text = shownRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 120.sp,
                        lineHeight = 120.sp
                    ),
                    outlineColor = Color.Black.copy(alpha = 0.85f),
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
                    outlineColor = Color.Black.copy(alpha = 0.85f),
                    fillColor = Color.White,
                    outlineWidth = 8f
                )
            }
        }
    }
}
