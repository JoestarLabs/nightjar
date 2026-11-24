package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
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

/**
 * Fullscreen seasonal snowflakes sunset warning animation overlay.
 *
 * Renders drifting crystal snowflakes with wind oscillation and a rising snowbank at the bottom of the screen.
 *
 * @param remainingSeconds Seconds remaining until the timer expires.
 * @param totalSeconds Total duration of the sunset warning window.
 * @param gravityAngleRad Full 2D orientation angle in radians indicating physical downward gravity.
 * @param startedAtMillis Epoch-ms when the timer started, enabling continuous wall-clock animation.
 * @param modifier Modifier applied to the overlay container.
 */
@Composable
fun SnowflakesOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
    startedAtMillis: Long? = null,
) {
    // ── Wall-clock continuous progress ──
    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

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
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val animatedProgress = countdown.progress.floatValue
            val elapsedSec = countdown.elapsedSeconds.floatValue
            drawRect(Color(0xFF0F172A).copy(alpha = 0.55f * animatedProgress))

            // ── 1. Rising snowbank at the bottom ──
            val maxSnowHeight = height + 40.dp.toPx()
            val snowTopBaseY = height - (animatedProgress * maxSnowHeight)

            snowbankPath.reset()
            snowbankCrestPath.reset()
            snowbankPath.moveTo(0f, height + 50f)

            // Build undulating snow dune surface across width
            val duneAmp = 18.dp.toPx()
            val duneFreq = 0.005f
            val dunePhase = elapsedSec * 0.4f

            fun duneY(x: Float): Float =
                snowTopBaseY + sin(x * duneFreq + dunePhase) * duneAmp + cos(x * 0.01f) * (duneAmp * 0.4f)

            snowbankPath.lineTo(0f, duneY(0f))
            snowbankCrestPath.moveTo(0f, duneY(0f))

            buildCubicWaveCurve(
                path = snowbankPath,
                xStart = 0f,
                xEnd = width,
                step = 40f,
                secondaryPath = snowbankCrestPath,
                waveY = ::duneY,
            )

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

        SunsetCountdownOverlayText(
            shownRemaining = shownRemaining,
            outlineColor = Color(0xFF0F172A).copy(alpha = 0.9f),
        )
    }
}

/** Draws a 6-pointed crystal snowflake with branch ticks. */
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
