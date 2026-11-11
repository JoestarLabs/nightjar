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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R
import kotlin.random.Random

private enum class M3ShapeType {
    CIRCLE,
    PILL,
    SQUIRCLE,
    SPARKLE,
    CLOVER,
    ARCH
}

private data class SettledShape(
    val relX: Float,
    val relY: Float,
    val shapeType: M3ShapeType,
    val sizePx: Float,
    val rotation: Float,
    val colorIndex: Int
)

private data class FallingParticle(
    val relX: Float,
    val initialRelY: Float,
    val speedPxPerSec: Float,
    val shapeType: M3ShapeType,
    val sizePx: Float,
    val rotationSpeed: Float,
    val colorIndex: Int
)

@Composable
fun MaterialShapesOverlay(
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

    // Material 3 Color Palette
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )

    // Deterministic random generation of settled bed shapes and falling particles
    val settledShapes = remember {
        val rng = Random(42)
        val list = mutableListOf<SettledShape>()
        val cols = 8
        val rows = 16
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val relX = (c + 0.5f + rng.nextFloat() * 0.6f - 0.3f) / cols
                val relY = (r + 0.5f + rng.nextFloat() * 0.6f - 0.3f) / rows
                val shape = M3ShapeType.entries[rng.nextInt(M3ShapeType.entries.size)]
                val size = 26f + rng.nextFloat() * 18f
                val rot = rng.nextFloat() * 360f
                val colorIdx = rng.nextInt(colors.size)
                list.add(SettledShape(relX, relY, shape, size, rot, colorIdx))
            }
        }
        list
    }

    val fallingParticles = remember {
        val rng = Random(1337)
        List(35) {
            FallingParticle(
                relX = rng.nextFloat(),
                initialRelY = rng.nextFloat(),
                speedPxPerSec = 220f + rng.nextFloat() * 180f,
                shapeType = M3ShapeType.entries[rng.nextInt(M3ShapeType.entries.size)],
                sizePx = 20f + rng.nextFloat() * 14f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 160f,
                colorIndex = rng.nextInt(colors.size)
            )
        }
    }

    val sparklePath = remember { Path() }
    val cloverPath = remember { Path() }
    val archPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Bed rising from bottom to top
            val bedY = height * (1f - animatedProgress)

            // Gradient filling the accumulated bed below bedY
            if (bedY < height) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors[3].copy(alpha = 0.85f),
                            colors[4].copy(alpha = 0.90f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = bedY,
                        endY = height
                    ),
                    topLeft = Offset(0f, bedY),
                    size = Size(width, height - bedY)
                )
            }

            // Draw settled shapes in the rising bed
            for (shape in settledShapes) {
                val shapeX = shape.relX * width
                val shapeY = shape.relY * height

                if (shapeY >= bedY) {
                    // Pop in with scale as bed line rises above it
                    val popProgress = ((shapeY - bedY) / 70f).coerceIn(0f, 1f)
                    val scale = popProgress * (1f + 0.15f * (1f - popProgress))
                    val color = colors[shape.colorIndex % colors.size]

                    withTransform({
                        translate(shapeX, shapeY)
                        rotate(shape.rotation, Offset.Zero)
                        scale(scale, scale, Offset.Zero)
                    }) {
                        drawM3Shape(
                            shapeType = shape.shapeType,
                            sizePx = shape.sizePx,
                            color = color,
                            sparklePath = sparklePath,
                            cloverPath = cloverPath,
                            archPath = archPath
                        )
                    }
                }
            }

            // Draw falling tumbling shapes above bedY
            val elapsedSec = (frameTimeMillis.longValue - (startedAtMillis ?: frameTimeMillis.longValue)) / 1000f
            for (particle in fallingParticles) {
                val totalDistance = (particle.initialRelY * height + particle.speedPxPerSec * elapsedSec)
                val currentY = if (bedY > 0f) totalDistance % (bedY + 40f) else -100f

                if (currentY < bedY && currentY > -50f) {
                    val currentX = particle.relX * width
                    val rotation = particle.rotationSpeed * elapsedSec
                    val color = colors[particle.colorIndex % colors.size].copy(alpha = 0.9f)

                    withTransform({
                        translate(currentX, currentY)
                        rotate(rotation, Offset.Zero)
                    }) {
                        drawM3Shape(
                            shapeType = particle.shapeType,
                            sizePx = particle.sizePx,
                            color = color,
                            sparklePath = sparklePath,
                            cloverPath = cloverPath,
                            archPath = archPath
                        )
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

private fun DrawScope.drawM3Shape(
    shapeType: M3ShapeType,
    sizePx: Float,
    color: Color,
    sparklePath: Path,
    cloverPath: Path,
    archPath: Path
) {
    val half = sizePx / 2f
    when (shapeType) {
        M3ShapeType.CIRCLE -> {
            drawCircle(color = color, radius = half, center = Offset.Zero)
        }
        M3ShapeType.PILL -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(-sizePx * 0.8f, -half * 0.7f),
                size = Size(sizePx * 1.6f, sizePx * 0.7f),
                cornerRadius = CornerRadius(half * 0.35f)
            )
        }
        M3ShapeType.SQUIRCLE -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(-half, -half),
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(sizePx * 0.32f)
            )
        }
        M3ShapeType.SPARKLE -> {
            sparklePath.reset()
            sparklePath.moveTo(0f, -half)
            sparklePath.quadraticTo(0f, 0f, half, 0f)
            sparklePath.quadraticTo(0f, 0f, 0f, half)
            sparklePath.quadraticTo(0f, 0f, -half, 0f)
            sparklePath.quadraticTo(0f, 0f, 0f, -half)
            sparklePath.close()
            drawPath(sparklePath, color)
        }
        M3ShapeType.CLOVER -> {
            cloverPath.reset()
            val r = half * 0.5f
            cloverPath.addOval(androidx.compose.ui.geometry.Rect(0f, -half, half, -half + 2 * r))
            cloverPath.addOval(androidx.compose.ui.geometry.Rect(0f, 0f, half, 2 * r))
            cloverPath.addOval(androidx.compose.ui.geometry.Rect(-half, 0f, 0f, 2 * r))
            cloverPath.addOval(androidx.compose.ui.geometry.Rect(-half, -half, 0f, -half + 2 * r))
            drawPath(cloverPath, color)
        }
        M3ShapeType.ARCH -> {
            archPath.reset()
            archPath.moveTo(-half, half)
            archPath.lineTo(half, half)
            archPath.lineTo(half, 0f)
            archPath.arcTo(
                rect = androidx.compose.ui.geometry.Rect(-half, -half, half, half),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            archPath.close()
            drawPath(archPath, color)
        }
    }
}
