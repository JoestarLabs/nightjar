package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class SnowflakeType {
    DOT_SMALL,
    DOT_MEDIUM,
    CRYSTAL
}

private data class SnowflakeParticle(
    val relX: Float,
    val initialRelY: Float,
    val speedPxPerSec: Float,
    val radiusPx: Float,
    val type: SnowflakeType,
    val windAmplitudePx: Float,
    val windFrequency: Float,
    val windPhase: Float,
    val rotationSpeed: Float,
    val alpha: Float
)

@Composable
fun SnowflakesOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    gravityAngleRad: Float = 0f,
    startedAtMillis: Long? = null,
    modifier: Modifier = Modifier
) {
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
    val frameTimeMillis = remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(startedAtMillis, totalSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                val now = System.currentTimeMillis()
                frameTimeMillis.longValue = now
                val elapsed = (now - startedAtMillis).coerceAtLeast(0L)
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

    // Pulse animation for text
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

    // ── Pool of deterministic snowflakes ──
    val snowflakes = remember {
        val rng = Random(2025)
        List(60) { index ->
            val type = when {
                index < 35 -> SnowflakeType.DOT_SMALL
                index < 50 -> SnowflakeType.DOT_MEDIUM
                else -> SnowflakeType.CRYSTAL
            }
            val radius = when (type) {
                SnowflakeType.DOT_SMALL -> 2f + rng.nextFloat() * 2.5f
                SnowflakeType.DOT_MEDIUM -> 4.5f + rng.nextFloat() * 3f
                SnowflakeType.CRYSTAL -> 8f + rng.nextFloat() * 5f
            }
            SnowflakeParticle(
                relX = rng.nextFloat(),
                initialRelY = rng.nextFloat(),
                speedPxPerSec = 70f + rng.nextFloat() * 120f,
                radiusPx = radius,
                type = type,
                windAmplitudePx = 15f + rng.nextFloat() * 25f,
                windFrequency = 1.2f + rng.nextFloat() * 1.5f,
                windPhase = rng.nextFloat() * (2f * PI.toFloat()),
                rotationSpeed = (rng.nextFloat() - 0.5f) * 80f,
                alpha = 0.65f + rng.nextFloat() * 0.35f
            )
        }
    }

    val snowbankPath = remember { Path() }
    val snowbankCrestPath = remember { Path() }
    val crystalPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.55f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // ── 1. Rising snowbank at the bottom ──
            val maxSnowHeight = height + 40.dp.toPx()
            val snowTopBaseY = height - (animatedProgress * maxSnowHeight)

            snowbankPath.reset()
            snowbankCrestPath.reset()
            snowbankPath.moveTo(0f, height + 50f)

            // Build undulating snow dune surface across width
            val duneAmp = 18.dp.toPx()
            val duneFreq = 0.005f
            val dunePhase = (frameTimeMillis.longValue / 1000f) * 0.4f

            fun duneY(x: Float): Float =
                snowTopBaseY + sin(x * duneFreq + dunePhase) * duneAmp + cos(x * 0.01f) * (duneAmp * 0.4f)

            snowbankPath.lineTo(0f, duneY(0f))
            snowbankCrestPath.moveTo(0f, duneY(0f))

            var curX = 0f
            val step = 40f
            while (curX < width) {
                val nextX = (curX + step).coerceAtMost(width)
                val midX = (curX + nextX) / 2f

                val yCur = duneY(curX)
                val yNext = duneY(nextX)
                val yMid = duneY(midX)

                val c1Y = yCur + (4f / 3f) * (yMid - (yCur + yNext) / 2f)
                val c2Y = yNext + (4f / 3f) * (yMid - (yCur + yNext) / 2f)

                val c1X = curX + (nextX - curX) / 3f
                val c2X = curX + 2f * (nextX - curX) / 3f

                snowbankPath.cubicTo(c1X, c1Y, c2X, c2Y, nextX, yNext)
                snowbankCrestPath.cubicTo(c1X, c1Y, c2X, c2Y, nextX, yNext)
                curX = nextX
            }

            snowbankPath.lineTo(width, height + 50f)
            snowbankPath.close()

            // Draw snowbank fill
            drawPath(
                path = snowbankPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC).copy(alpha = 0.95f),
                        Color(0xFFE2E8F0).copy(alpha = 0.96f),
                        Color(0xFF94A3B8).copy(alpha = 0.98f)
                    ),
                    startY = snowTopBaseY - duneAmp,
                    endY = height
                )
            )

            // Draw snowbank soft top crest highlight
            drawPath(
                path = snowbankCrestPath,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // ── 2. Falling snowflakes ──
            val elapsedSec = (frameTimeMillis.longValue - (startedAtMillis ?: frameTimeMillis.longValue)) / 1000f
            for (flake in snowflakes) {
                val totalFall = flake.initialRelY * height + flake.speedPxPerSec * elapsedSec
                val currentY = if (snowTopBaseY > 0f) totalFall % (snowTopBaseY + 30f) else -100f

                if (currentY < duneY(flake.relX * width) && currentY > -30f) {
                    val windOffset = sin(elapsedSec * flake.windFrequency + flake.windPhase) * flake.windAmplitudePx
                    val currentX = (flake.relX * width + windOffset).mod(width)
                    val color = Color.White.copy(alpha = flake.alpha)

                    when (flake.type) {
                        SnowflakeType.DOT_SMALL, SnowflakeType.DOT_MEDIUM -> {
                            drawCircle(
                                color = color,
                                radius = flake.radiusPx,
                                center = Offset(currentX, currentY)
                            )
                        }
                        SnowflakeType.CRYSTAL -> {
                            val rotation = flake.rotationSpeed * elapsedSec
                            withTransform({
                                translate(currentX, currentY)
                                rotate(rotation, Offset.Zero)
                            }) {
                                drawCrystalSnowflake(
                                    radius = flake.radiusPx,
                                    color = color,
                                    crystalPath = crystalPath
                                )
                            }
                        }
                    }
                }
            }
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
                    outlineColor = Color(0xFF0F172A).copy(alpha = 0.9f),
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
                    outlineColor = Color(0xFF0F172A).copy(alpha = 0.9f),
                    fillColor = Color.White,
                    outlineWidth = 8f
                )
            }
        }
    }
}

private fun DrawScope.drawCrystalSnowflake(
    radius: Float,
    color: Color,
    crystalPath: Path
) {
    crystalPath.reset()
    // 6-pointed snowflake
    for (i in 0 until 3) {
        val angle = i * (PI / 3f).toFloat()
        val cosA = cos(angle) * radius
        val sinA = sin(angle) * radius
        crystalPath.moveTo(-cosA, -sinA)
        crystalPath.lineTo(cosA, sinA)
    }
    drawPath(
        path = crystalPath,
        color = color,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )
}
