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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.ui.components.liquid.FluidParticleSimulation
import com.bl4ckswordsman.nightjar.ui.components.liquid.createMetaballSpriteBitmap
import com.bl4ckswordsman.nightjar.ui.components.liquid.rememberLiquidMetaballShaderHolder
import com.bl4ckswordsman.nightjar.ui.effects.EffectsQuality
import com.bl4ckswordsman.nightjar.ui.effects.EffectsQualityController
import com.bl4ckswordsman.nightjar.ui.effects.rememberEffectsQualityController
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

private object FluidTuning {
    const val PARTICLES_FULL = 220
    const val PARTICLES_REDUCED = 130

    const val BACKGROUND_DIM_MAX_ALPHA = 0.25f
    const val METABALL_THRESHOLD = 0.34f
    const val NORMAL_STEP_DP = 2.0f
    const val SPRITE_RADIUS_SCALE = 1.05f

    const val GRAVITY_SMOOTH_RATE = 10f
    const val WATCHDOG_FRAME_MS = 25f
    const val WATCHDOG_SUSTAINED_SEC = 1.5f
}

/**
 * Physically reactive 2D flowing fluid sunset overlay powered by a zero-allocation
 * Double-Density Relaxation particle solver and an AGSL metaball shader with translucent
 * depth shading, 360° gravity pouring, wall splashing, and adaptive [effectsController].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FluidLiquidOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    gravityAngleRad: () -> Float,
    modifier: Modifier = Modifier,
    rawAccelX: () -> Float = { 0f },
    rawAccelY: () -> Float = { 9.8f },
    startedAtMillis: Long? = null,
    effectsController: EffectsQualityController = rememberEffectsQualityController(),
) {
    val quality = effectsController.quality
    val isMinimal = quality == EffectsQuality.MINIMAL
    val isFull = quality == EffectsQuality.FULL

    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

    val maxParticles = if (isFull) FluidTuning.PARTICLES_FULL else FluidTuning.PARTICLES_REDUCED
    val sim = remember(maxParticles) { FluidParticleSimulation(maxParticles = maxParticles) }
    val metaballSprite = remember { createMetaballSpriteBitmap(64) }

    val smoothedAngleState = remember { mutableFloatStateOf(gravityAngleRad()) }
    val frameTick = remember { mutableLongStateOf(0L) }

    // Single unified vsync loop driving 2D particle fluid physics
    // and the frame-budget watchdog with zero per-frame recompositions.
    LaunchedEffect(quality, sim) {
        if (isMinimal) {
            sim.reset()
        }
        var lastNanos = 0L
        var lastProgress = countdown.progress.floatValue
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

                val rawAngle = gravityAngleRad()
                val prevAngle = smoothedAngleState.floatValue
                var diff = (rawAngle - prevAngle) % (2f * PI.toFloat())
                if (diff > PI.toFloat()) diff -= (2f * PI.toFloat())
                if (diff < -PI.toFloat()) diff += (2f * PI.toFloat())
                val alpha = 1f - exp(-dtSec * FluidTuning.GRAVITY_SMOOTH_RATE)
                smoothedAngleState.floatValue = prevAngle + diff * alpha

                if (!isMinimal) {
                    val progress = countdown.progress.floatValue
                    if (progress + 0.1f < lastProgress) {
                        // Preview loop restarted from 1 -> 0
                        sim.reset()
                    }
                    lastProgress = progress

                    val ax = rawAccelX()
                    val ay = rawAccelY()
                    val targetCount = (progress * sim.maxParticles).roundToInt()
                        .coerceIn(if (progress > 0.01f) 4 else 0, sim.maxParticles)
                    sim.setActiveCount(targetCount, gravityX = -ax, gravityY = ay)
                    sim.step(dtSec = dtSec, accelX = ax, accelY = ay)
                }

                if (isFull) {
                    val dtMs = dtSec * 1000f
                    emaFrameMs = emaFrameMs * 0.9f + dtMs * 0.1f
                    if (emaFrameMs > FluidTuning.WATCHDOG_FRAME_MS) {
                        overrunSec += dtSec
                        if (overrunSec >= FluidTuning.WATCHDOG_SUSTAINED_SEC) {
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
    val deepColor = Color(0xFF020617)

    val shaderHolder = rememberLiquidMetaballShaderHolder(enabled = !isMinimal)

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle background dim preserving underlying content visibility
        Canvas(modifier = Modifier.fillMaxSize()) {
            frameTick.longValue
            val progress = countdown.progress.floatValue
            drawRect(Color.Black.copy(alpha = FluidTuning.BACKGROUND_DIM_MAX_ALPHA * progress))

            if (isMinimal) {
                val width = size.width
                val height = size.height
                val angle = smoothedAngleState.floatValue
                val hProj = width * abs(sin(angle)) + height * abs(cos(angle))
                val wProj = width * abs(cos(angle)) + height * abs(sin(angle))
                val bottomExtent = hProj * 0.5f
                val topExtent = -hProj * 0.5f
                val baseLineY = bottomExtent - progress * (bottomExtent - topExtent)
                val rotationDegrees = -Math.toDegrees(angle.toDouble()).toFloat()

                withTransform({
                    translate(left = width * 0.5f, top = height * 0.5f)
                    rotate(degrees = rotationDegrees, pivot = Offset.Zero)
                }) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.62f),
                                secondaryColor.copy(alpha = 0.74f),
                                deepColor.copy(alpha = 0.82f),
                            ),
                            startY = baseLineY,
                            endY = bottomExtent,
                        ),
                        topLeft = Offset(-wProj, baseLineY),
                        size = Size(wProj * 2f, (bottomExtent - baseLineY).coerceAtLeast(0f)),
                    )
                }
            }
        }

        if (!isMinimal) {
            // Offscreen metaball density pass + single AGSL liquid RenderEffect
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (shaderHolder != null) {
                            compositingStrategy = CompositingStrategy.Offscreen
                            renderEffect = shaderHolder.buildRenderEffect(
                                threshold = FluidTuning.METABALL_THRESHOLD,
                                normalStepPx = FluidTuning.NORMAL_STEP_DP.dp.toPx(),
                                shadingEnabled = isFull,
                                top = primaryColor,
                                mid = secondaryColor,
                                deep = deepColor,
                            )
                        }
                    },
            ) {
                frameTick.longValue
                sim.resize(size.width, size.height)

                val count = sim.activeCount
                if (count > 0) {
                    val radiusPx = sim.interactionRadiusPx * FluidTuning.SPRITE_RADIUS_SCALE
                    if (shaderHolder != null) {
                        val diameterPx = (radiusPx * 2f).roundToInt().coerceAtLeast(4)
                        val halfDiameter = diameterPx / 2
                        val spriteSize = IntSize(diameterPx, diameterPx)
                        for (i in 0 until count) {
                            val px = sim.x[i].roundToInt() - halfDiameter
                            val py = sim.y[i].roundToInt() - halfDiameter
                            drawImage(
                                image = metaballSprite,
                                dstOffset = IntOffset(px, py),
                                dstSize = spriteSize,
                                blendMode = BlendMode.Plus,
                            )
                        }
                    } else {
                        // Graceful fallback if AGSL shader is unavailable
                        val fallbackRadius = sim.interactionRadiusPx * 0.75f
                        for (i in 0 until count) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.45f),
                                radius = fallbackRadius,
                                center = Offset(sim.x[i], sim.y[i]),
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
