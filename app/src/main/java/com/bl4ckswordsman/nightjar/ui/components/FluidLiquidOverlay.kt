package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.ui.components.liquid.LiquidSurfaceSimulation
import com.bl4ckswordsman.nightjar.ui.components.liquid.rememberLiquidShaderHolder
import com.bl4ckswordsman.nightjar.ui.effects.EffectsQuality
import com.bl4ckswordsman.nightjar.ui.effects.EffectsQualityController
import com.bl4ckswordsman.nightjar.ui.effects.rememberEffectsQualityController
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Central tuning parameters for [FluidLiquidOverlay].
 */
private object LiquidTuning {
    const val MAX_BUBBLES_FULL = 22
    const val MAX_BUBBLES_REDUCED = 10

    const val AMBIENT_AMP1_DP = 9f
    const val AMBIENT_AMP2_DP = 3.6f
    const val PATH_STEP_DP = 10f

    const val GRAVITY_SMOOTH_RATE = 10f
    const val WATCHDOG_FRAME_MS = 25f
    const val WATCHDOG_SUSTAINED_SEC = 1.5f

    const val BUBBLE_POP_IMPULSE_DP = 8f
    const val BUBBLE_FLOW_SCALE = 0.0022f
}

/**
 * Pre-allocated struct-of-arrays bubble pool so per-frame updates allocate zero objects.
 */
private class BubblePool(val capacity: Int) {
    val relX = FloatArray(capacity)
    val depth = FloatArray(capacity)
    val speed = FloatArray(capacity)
    val radiusDp = FloatArray(capacity)
    val wobbleFreq = FloatArray(capacity)
    val wobblePhase = FloatArray(capacity)
    val alpha = FloatArray(capacity)

    private val rng = Random(42)

    init {
        for (i in 0 until capacity) {
            relX[i] = (rng.nextFloat() * 2f - 1f) * 0.85f
            depth[i] = rng.nextFloat()
            speed[i] = 0.10f + rng.nextFloat() * 0.16f
            radiusDp[i] = 2.5f + rng.nextFloat() * 4.0f
            wobbleFreq[i] = 2.5f + rng.nextFloat() * 2f
            wobblePhase[i] = rng.nextFloat() * (2f * PI.toFloat())
            alpha[i] = 0.45f + rng.nextFloat() * 0.40f
        }
    }

    fun step(
        count: Int,
        dtSec: Float,
        lateralFlow: Float,
        onSurfacePop: (relX: Float) -> Unit,
    ) {
        val drift = lateralFlow * LiquidTuning.BUBBLE_FLOW_SCALE * dtSec
        for (i in 0 until count.coerceAtMost(capacity)) {
            depth[i] -= speed[i] * dtSec
            var x = relX[i] + drift
            if (x > 0.88f) x = -0.88f else if (x < -0.88f) x = 0.88f
            relX[i] = x

            if (depth[i] <= 0f) {
                onSurfacePop(x)
                depth[i] = 1f
                relX[i] = (rng.nextFloat() * 2f - 1f) * 0.85f
            }
        }
    }
}

/**
 * Calculates surface Y at [x] combining subtle trochoidal ambient waves,
 * interactive 1D wave simulation heights across the visible span [halfVisWidth],
 * and container-wall meniscus lift.
 */
private fun fluidSurfaceY(
    x: Float,
    baseLineY: Float,
    halfVisWidth: Float,
    amp1: Float,
    freq1: Float,
    phase1: Float,
    amp2: Float,
    freq2: Float,
    phase2: Float,
    sim: LiquidSurfaceSimulation,
    simScalePx: Float,
    isMinimal: Boolean,
): Float {
    if (isMinimal) return baseLineY

    val theta1 = x * freq1 + phase1
    val theta2 = x * freq2 + phase2
    val wave1 = (sin(theta1) - 0.22f * cos(2f * theta1)) * amp1
    val wave2 = (cos(theta2) + 0.15f * sin(2f * theta2)) * amp2

    val relX = (x / halfVisWidth.coerceAtLeast(1f)).coerceIn(-1f, 1f)
    val simHeightPx = sim.heightAt(relX) * simScalePx

    val distToEdge = (halfVisWidth - abs(x)).coerceAtLeast(0f)
    val meniscus = 6f * exp(-distToEdge / 52f)

    // Positive simHeightPx pushes the surface upward (-Y in canvas coordinates)
    return baseLineY + wave1 + wave2 - simHeightPx - meniscus
}

private fun buildFluidWavePath(
    path: Path,
    xStart: Float,
    xEnd: Float,
    bottomLimit: Float,
    step: Float,
    waveY: (Float) -> Float,
) {
    path.reset()
    path.moveTo(xStart, bottomLimit)
    path.lineTo(xStart, waveY(xStart))
    buildCubicWaveCurve(path = path, xStart = xStart, xEnd = xEnd, step = step, waveY = waveY)
    path.lineTo(xEnd, bottomLimit)
    path.close()
}

private fun buildFluidCrestLinePath(
    path: Path,
    xStart: Float,
    xEnd: Float,
    step: Float,
    waveY: (Float) -> Float,
) {
    path.reset()
    path.moveTo(xStart, waveY(xStart))
    buildCubicWaveCurve(path = path, xStart = xStart, xEnd = xEnd, step = step, waveY = waveY)
}

/**
 * Physically reactive fluid liquid sunset overlay that tracks 360° gravity orientation,
 * simulates sloshing waves and wall rebounds from device motion, renders AGSL caustic
 * depth shading on capable devices, and adapts visual complexity via [effectsController].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FluidLiquidOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    gravityAngleRad: () -> Float,
    modifier: Modifier = Modifier,
    linearAccelX: () -> Float = { 0f },
    linearAccelY: () -> Float = { 0f },
    startedAtMillis: Long? = null,
    effectsController: EffectsQualityController = rememberEffectsQualityController(),
) {
    val quality = effectsController.quality
    val isMinimal = quality == EffectsQuality.MINIMAL
    val isFull = quality == EffectsQuality.FULL

    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

    val sim = remember { LiquidSurfaceSimulation(columns = 48) }
    val bubbles = remember { BubblePool(LiquidTuning.MAX_BUBBLES_FULL) }

    val smoothedAngleState = remember { mutableFloatStateOf(gravityAngleRad()) }
    val phase1State = remember { mutableFloatStateOf(0f) }
    val phase2State = remember { mutableFloatStateOf(0f) }
    val frameTick = remember { mutableLongStateOf(0L) }

    // Single unified vsync loop driving gravity smoothing, wave physics, ambient phases,
    // bubble advection, and the frame-budget watchdog with zero per-frame recompositions.
    LaunchedEffect(quality) {
        if (isMinimal) {
            sim.reset()
        }
        var lastNanos = 0L
        var emaFrameMs = 16.6f
        var overrunSec = 0f

        while (true) {
            withFrameNanos { frameNanos ->
                val dtSec = if (lastNanos == 0L) {
                    1f / 60f
                } else {
                    ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                }
                lastNanos = frameNanos

                // Unwrap and smoothly track gravity angle
                val rawAngle = gravityAngleRad()
                val prevAngle = smoothedAngleState.floatValue
                var diff = (rawAngle - prevAngle) % (2f * PI.toFloat())
                if (diff > PI.toFloat()) diff -= (2f * PI.toFloat())
                if (diff < -PI.toFloat()) diff += (2f * PI.toFloat())
                val alpha = 1f - exp(-dtSec * LiquidTuning.GRAVITY_SMOOTH_RATE)
                val nextAngle = prevAngle + diff * alpha
                smoothedAngleState.floatValue = nextAngle

                if (!isMinimal) {
                    val omega = (nextAngle - prevAngle) / dtSec
                    val ax = linearAccelX()
                    val ay = linearAccelY()
                    val lateralAccel = ax * cos(nextAngle) + ay * sin(nextAngle)

                    if (!sim.isSettled() || abs(lateralAccel) > 0.05f || abs(omega) > 0.02f) {
                        sim.step(dtSec = dtSec, lateralAccel = lateralAccel, angularVelocity = omega)
                    }

                    val twoPi = (2 * PI).toFloat()
                    phase1State.floatValue = (phase1State.floatValue + dtSec * twoPi / 2.6f) % twoPi
                    phase2State.floatValue = (phase2State.floatValue - dtSec * twoPi / 3.4f) % twoPi

                    val bubbleCount = if (isFull) LiquidTuning.MAX_BUBBLES_FULL else LiquidTuning.MAX_BUBBLES_REDUCED
                    bubbles.step(
                        count = bubbleCount,
                        dtSec = dtSec,
                        lateralFlow = sim.lateralFlow(),
                        onSurfacePop = { relX ->
                            if (isFull) {
                                sim.impulse(relX, LiquidTuning.BUBBLE_POP_IMPULSE_DP)
                            }
                        },
                    )
                }

                // One-way frame-budget watchdog
                if (isFull) {
                    val dtMs = dtSec * 1000f
                    emaFrameMs = emaFrameMs * 0.9f + dtMs * 0.1f
                    if (emaFrameMs > LiquidTuning.WATCHDOG_FRAME_MS) {
                        overrunSec += dtSec
                        if (overrunSec >= LiquidTuning.WATCHDOG_SUSTAINED_SEC) {
                            effectsController.reportFrameBudgetExceeded()
                        }
                    } else {
                        overrunSec = 0f
                    }
                }

                frameTick.longValue++
            }
        }
    }

    // Recompose only when crossing 90° cardinal orientation quadrants
    val targetTextAngle by remember {
        derivedStateOf {
            val deg = (Math.toDegrees(smoothedAngleState.floatValue.toDouble()).toFloat()) % 360f
            val normalizedDeg = if (deg > 180f) deg - 360f else if (deg < -180f) deg + 360f else deg
            when {
                normalizedDeg > 45f && normalizedDeg <= 135f -> -90f
                normalizedDeg > 135f || normalizedDeg <= -135f -> 180f
                normalizedDeg >= -135f && normalizedDeg < -45f -> 90f
                else -> 0f
            }
        }
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
            stiffness = Spring.StiffnessLow,
        ),
        label = "text_orientation",
    )

    val motionScheme = MaterialTheme.motionScheme
    val textFastSpatial = motionScheme.fastSpatialSpec<Float>()
    val textDefaultSpatial = motionScheme.defaultSpatialSpec<Float>()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val shaderHolder = rememberLiquidShaderHolder(enabled = isFull)

    val backWavePath = remember { Path() }
    val frontWavePath = remember { Path() }
    val crestPath = remember { Path() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subscribe draw phase to unified vsync tick without triggering recomposition
            frameTick.longValue

            val animatedProgress = countdown.progress.floatValue
            drawRect(Color.Black.copy(alpha = 0.45f * animatedProgress))

            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            val diagonal = hypot(width, height)
            val r = diagonal * 0.75f

            val angle = smoothedAngleState.floatValue
            val absSin = abs(sin(angle))
            val absCos = abs(cos(angle))
            val hProj = width * absSin + height * absCos
            val wProj = width * absCos + height * absSin
            val halfVisWidth = wProj * 0.5f
            val halfVisHeight = hProj * 0.5f

            val bottomExtent = halfVisHeight + 28.dp.toPx()
            val topExtent = -halfVisHeight - 28.dp.toPx()
            val baseLineY = bottomExtent - animatedProgress * (bottomExtent - topExtent)

            val rotationDegrees = -Math.toDegrees(angle.toDouble()).toFloat()
            val dpPx = density
            val stepPx = LiquidTuning.PATH_STEP_DP.dp.toPx()
            val amp1 = LiquidTuning.AMBIENT_AMP1_DP.dp.toPx()
            val amp2 = LiquidTuning.AMBIENT_AMP2_DP.dp.toPx()
            val freq1 = 0.0055f
            val freq2 = 0.012f
            val phase1 = phase1State.floatValue
            val phase2 = phase2State.floatValue

            withTransform({
                translate(left = centerX, top = centerY)
                rotate(degrees = rotationDegrees, pivot = Offset.Zero)
            }) {
                // 1. Back fluid wave (omitted in MINIMAL)
                if (!isMinimal) {
                    buildFluidWavePath(
                        path = backWavePath,
                        xStart = -r,
                        xEnd = r,
                        bottomLimit = r,
                        step = stepPx,
                        waveY = { x ->
                            fluidSurfaceY(
                                x = x,
                                baseLineY = baseLineY,
                                halfVisWidth = halfVisWidth,
                                amp1 = amp1 * 1.15f,
                                freq1 = freq1 * 0.88f,
                                phase1 = phase2,
                                amp2 = amp2 * 1.25f,
                                freq2 = freq2 * 0.9f,
                                phase2 = phase1,
                                sim = sim,
                                simScalePx = dpPx * 0.7f,
                                isMinimal = false,
                            )
                        },
                    )
                    drawPath(
                        path = backWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.60f),
                                secondaryColor.copy(alpha = 0.80f),
                                Color(0xFF020617).copy(alpha = 0.94f),
                            ),
                            startY = baseLineY - amp1 - amp2,
                            endY = r,
                        ),
                    )
                }

                // 2. Front fluid wave
                fun frontWaveY(x: Float): Float = fluidSurfaceY(
                    x = x,
                    baseLineY = baseLineY,
                    halfVisWidth = halfVisWidth,
                    amp1 = amp1,
                    freq1 = freq1,
                    phase1 = phase1,
                    amp2 = amp2,
                    freq2 = freq2,
                    phase2 = phase2,
                    sim = sim,
                    simScalePx = dpPx,
                    isMinimal = isMinimal,
                )

                buildFluidWavePath(
                    path = frontWavePath,
                    xStart = -r,
                    xEnd = r,
                    bottomLimit = r,
                    step = stepPx,
                    waveY = ::frontWaveY,
                )

                val frontBrush = if (isFull && shaderHolder != null) {
                    val fluidSpan = (halfVisHeight - baseLineY).coerceAtLeast(120.dp.toPx())
                    shaderHolder.update(
                        baseY = baseLineY - amp1,
                        timeSec = countdown.elapsedSeconds.floatValue,
                        depthScale = 2.2f / fluidSpan,
                        top = primaryColor,
                        mid = secondaryColor,
                        deep = Color(0xFF020617),
                    )
                    shaderHolder.brush
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.88f),
                            secondaryColor.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.97f),
                        ),
                        startY = baseLineY - amp1 - amp2,
                        endY = r,
                    )
                }
                drawPath(path = frontWavePath, brush = frontBrush)

                // 3. Specular crest highlight
                if (!isMinimal) {
                    buildFluidCrestLinePath(
                        path = crestPath,
                        xStart = -r,
                        xEnd = r,
                        step = stepPx,
                        waveY = ::frontWaveY,
                    )
                    if (isFull) {
                        drawPath(
                            path = crestPath,
                            color = primaryColor.copy(alpha = 0.35f),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    drawPath(
                        path = crestPath,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // 4. Visible flow-coupled rising bubbles
                if (!isMinimal) {
                    val bubbleCount = if (isFull) LiquidTuning.MAX_BUBBLES_FULL else LiquidTuning.MAX_BUBBLES_REDUCED
                    val visibleBottom = halfVisHeight
                    val fluidDepth = (visibleBottom - baseLineY).coerceAtLeast(10f)
                    val timeSec = countdown.elapsedSeconds.floatValue
                    val wobblePx = 4.dp.toPx()
                    val surfaceMarginPx = 10.dp.toPx()

                    for (i in 0 until bubbleCount) {
                        val currentY = baseLineY + bubbles.depth[i] * fluidDepth
                        if (currentY > baseLineY + surfaceMarginPx && currentY < visibleBottom) {
                            val currentX = bubbles.relX[i] * halfVisWidth +
                                sin(timeSec * bubbles.wobbleFreq[i] + bubbles.wobblePhase[i]) * wobblePx
                            val bubbleRadiusPx = bubbles.radiusDp[i].dp.toPx()
                            val bubbleAlpha = bubbles.alpha[i]

                            drawCircle(
                                color = Color.White.copy(alpha = bubbleAlpha),
                                radius = bubbleRadiusPx,
                                center = Offset(currentX, currentY),
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = bubbleAlpha * 0.8f),
                                radius = bubbleRadiusPx * 0.5f,
                                center = Offset(
                                    currentX - bubbleRadiusPx * 0.3f,
                                    currentY - bubbleRadiusPx * 0.3f,
                                ),
                            )
                        }
                    }
                }
            }
        }

        SunsetCountdownOverlayText(
            shownRemaining = shownRemaining,
            outlineColor = Color.Black.copy(alpha = 0.85f),
            rotationDegrees = { animatedTextRotation },
            expandSpec = textFastSpatial,
            settleSpec = textDefaultSpatial,
        )
    }
}
