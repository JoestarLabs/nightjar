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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Micro-bubble drifting upward inside the liquid body.
 */
private data class LiquidBubble(
    val relX: Float,          // -1f to 1f across fluid width
    var currentDepth: Float,  // 0f (surface) to 1f (deep bottom)
    val speed: Float,         // depth decrement per second
    val radiusDp: Float,
    val wobbleFrequency: Float,
    val wobblePhase: Float,
    val alpha: Float
)

/**
 * Calculates trochoidal fluid surface height at [x] with realistic wave steepness and meniscus.
 */
private fun fluidSurfaceY(
    x: Float,
    baseLineY: Float,
    xStart: Float,
    xEnd: Float,
    amp1: Float,
    freq1: Float,
    phase1: Float,
    amp2: Float,
    freq2: Float,
    phase2: Float,
    sloshTilt: Float
): Float {
    val theta1 = x * freq1 + phase1
    val theta2 = x * freq2 + phase2

    // Trochoidal wave: sharper crests, broader troughs
    val wave1 = (sin(theta1) - 0.22f * cos(2f * theta1)) * amp1
    val wave2 = (cos(theta2) + 0.15f * sin(2f * theta2)) * amp2

    // Dynamic tilt slosh across the width
    val sloshOffset = (x / (xEnd - xStart).coerceAtLeast(1f)) * sloshTilt

    // Surface tension meniscus near the container walls
    val distToEdge = (x - xStart).coerceAtMost(xEnd - x).coerceAtLeast(0f)
    val meniscus = 8f * exp(-distToEdge / 60f)

    return baseLineY + wave1 + wave2 + sloshOffset - meniscus
}

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
    sloshTilt: Float,
    step: Float = 35f
) {
    path.reset()
    path.moveTo(xStart, bottomLimit)

    fun waveY(x: Float): Float = fluidSurfaceY(
        x, baseLineY, xStart, xEnd, amp1, freq1, phase1, amp2, freq2, phase2, sloshTilt
    )

    path.lineTo(xStart, waveY(xStart))
    buildCubicWaveCurve(path = path, xStart = xStart, xEnd = xEnd, step = step, waveY = ::waveY)
    path.lineTo(xEnd, bottomLimit)
    path.close()
}

/**
 * Builds the surface crest path for drawing specular water highlights.
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
    sloshTilt: Float,
    step: Float = 35f
) {
    path.reset()
    fun waveY(x: Float): Float = fluidSurfaceY(
        x, baseLineY, xStart, xEnd, amp1, freq1, phase1, amp2, freq2, phase2, sloshTilt
    )

    path.moveTo(xStart, waveY(xStart))
    buildCubicWaveCurve(path = path, xStart = xStart, xEnd = xEnd, step = step, waveY = ::waveY)
}

/**
 * Realistic fluid liquid sunset overlay that tracks physical gravity in a 360° range,
 * keeping the liquid surface level with the real world horizon, even when held upside down.
 * Features realistic trochoidal wave harmonics, specular meniscus crest highlights,
 * dynamic inertial slosh response, rising micro-bubbles, and cardinal orientation tracking
 * for the countdown timer.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FluidLiquidOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    gravityAngleRad: Float,
    modifier: Modifier = Modifier,
    startedAtMillis: Long? = null,
) {
    // ── Multi-frequency wave phase oscillators ──
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_waves")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    // ── Wall-clock continuous progress ──
    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

    // ── Smooth 360° gravity angle tracking without wrapping glitches ──
    var unwrappedGravityAngle by remember { mutableFloatStateOf(gravityAngleRad) }
    LaunchedEffect(gravityAngleRad) {
        var diff = (gravityAngleRad - unwrappedGravityAngle) % (2f * PI.toFloat())
        if (diff > PI.toFloat()) diff -= (2f * PI.toFloat())
        if (diff < -PI.toFloat()) diff += (2f * PI.toFloat())
        unwrappedGravityAngle += diff
    }

    // Angular spring for the liquid surface with natural fluid inertia
    val animatedGravityAngle by animateFloatAsState(
        targetValue = unwrappedGravityAngle,
        animationSpec = spring(
            dampingRatio = 0.65f, // slight liquid bounce/overshoot
            stiffness = Spring.StiffnessLow
        ),
        label = "fluid_gravity_angle"
    )

    // Dynamic slosh tilt computed from angular lag (rotational inertia)
    val density = LocalDensity.current
    val angularLag = unwrappedGravityAngle - animatedGravityAngle
    val sloshTiltPx = with(density) {
        (angularLag * 35.dp.toPx()).coerceIn(-40.dp.toPx(), 40.dp.toPx())
    }

    // ── Orientation tracking for countdown text ──
    // To counteract phone rotation and remain upright, text rotates in opposite direction (-deg)
    val deg = (Math.toDegrees(animatedGravityAngle.toDouble()).toFloat()) % 360f
    val normalizedDeg = if (deg > 180f) deg - 360f else if (deg < -180f) deg + 360f else deg

    val targetTextAngle = when {
        normalizedDeg > 45f && normalizedDeg <= 135f -> -90f
        normalizedDeg > 135f || normalizedDeg <= -135f -> 180f
        normalizedDeg >= -135f && normalizedDeg < -45f -> 90f
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

    // Native Material 3 Expressive Motion System for snappy text pulse
    val motionScheme = MaterialTheme.motionScheme
    val textFastSpatial = motionScheme.fastSpatialSpec<Float>()
    val textDefaultSpatial = motionScheme.defaultSpatialSpec<Float>()

    // ── Micro-bubbles pool ──
    val bubbles = remember {
        val rng = Random(42)
        List(22) {
            LiquidBubble(
                relX = (rng.nextFloat() * 2f - 1f) * 0.75f,
                currentDepth = rng.nextFloat(),
                speed = 0.08f + rng.nextFloat() * 0.14f,
                radiusDp = 2.5f + rng.nextFloat() * 4.0f,
                wobbleFrequency = 2.5f + rng.nextFloat() * 2f,
                wobblePhase = rng.nextFloat() * (2f * PI.toFloat()),
                alpha = 0.45f + rng.nextFloat() * 0.40f
            )
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val backWavePath = remember { Path() }
    val frontWavePath = remember { Path() }
    val crestPath = remember { Path() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val animatedProgress = countdown.progress.floatValue
            drawRect(Color.Black.copy(alpha = 0.45f * animatedProgress))

            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            val diagonal = hypot(width, height)
            val r = diagonal * 0.75f

            // Extent of screen along gravity direction at current angle
            val absSin = abs(sin(animatedGravityAngle))
            val absCos = abs(cos(animatedGravityAngle))
            val hProj = width * absSin + height * absCos

            // Base Y in rotated coordinate space (+Y points along gravity)
            val bottomExtent = hProj / 2f + 28.dp.toPx()
            val topExtent = -hProj / 2f - 28.dp.toPx()
            val baseLineY = bottomExtent - animatedProgress * (bottomExtent - topExtent)

            // Canvas rotation is negated (-rotationDegrees) so the fluid stays level with the real world
            val rotationDegrees = -Math.toDegrees(animatedGravityAngle.toDouble()).toFloat()

            withTransform({
                translate(left = centerX, top = centerY)
                rotate(degrees = rotationDegrees, pivot = Offset.Zero)
            }) {
                val amp1 = 15.dp.toPx()
                val amp2 = 6.dp.toPx()
                val freq1 = 0.0055f
                val freq2 = 0.012f

                // ── 1. Back fluid wave (3D depth volume) ──
                buildFluidWavePath(
                    path = backWavePath,
                    xStart = -r,
                    xEnd = r,
                    baseLineY = baseLineY,
                    bottomLimit = r,
                    amp1 = amp1 * 1.15f,
                    freq1 = freq1 * 0.88f,
                    phase1 = phase2,
                    amp2 = amp2 * 1.25f,
                    freq2 = freq2 * 0.9f,
                    phase2 = phase1,
                    sloshTilt = sloshTiltPx * 0.8f
                )
                drawPath(
                    path = backWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.60f),
                            secondaryColor.copy(alpha = 0.80f),
                            Color(0xFF020617).copy(alpha = 0.94f)
                        ),
                        startY = baseLineY - amp1 - amp2,
                        endY = r
                    )
                )

                // ── 2. Front fluid wave (Rich liquid body) ──
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
                    phase2 = phase2,
                    sloshTilt = sloshTiltPx
                )
                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.88f),
                            secondaryColor.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.97f)
                        ),
                        startY = baseLineY - amp1 - amp2,
                        endY = r
                    )
                )

                // ── 3. Specular crest highlight (Meniscus edge reflection) ──
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
                    phase2 = phase2,
                    sloshTilt = sloshTiltPx
                )
                // Soft caustic glow behind crest
                drawPath(
                    path = crestPath,
                    color = primaryColor.copy(alpha = 0.35f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Sharp specular water line
                drawPath(
                    path = crestPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // ── 4. Rising buoyant micro-bubbles ──
                val fluidDepth = (r - baseLineY).coerceAtLeast(10f)
                val timeSec = countdown.elapsedSeconds.floatValue
                val wobblePx = 4.dp.toPx()
                for (b in bubbles) {
                    val currentY = baseLineY + (1f - ((b.currentDepth + b.speed * timeSec) % 1f)) * fluidDepth
                    val currentX = b.relX * (r * 0.7f) + sin(timeSec * b.wobbleFrequency + b.wobblePhase) * wobblePx
                    val bubbleRadiusPx = b.radiusDp.dp.toPx()

                    // Only draw if fully submerged
                    if (currentY > baseLineY + 12.dp.toPx() && currentY < r) {
                        drawCircle(
                            color = Color.White.copy(alpha = b.alpha),
                            radius = bubbleRadiusPx,
                            center = Offset(currentX, currentY)
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = b.alpha * 0.8f),
                            radius = bubbleRadiusPx * 0.5f,
                            center = Offset(currentX - bubbleRadiusPx * 0.3f, currentY - bubbleRadiusPx * 0.3f)
                        )
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
