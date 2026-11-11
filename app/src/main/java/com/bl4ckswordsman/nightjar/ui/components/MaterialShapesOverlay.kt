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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.toPath
import com.bl4ckswordsman.nightjar.R
import kotlin.math.min
import kotlin.random.Random

private data class SettledM3Shape(
    val relX: Float,
    val relY: Float,
    val shapeIndex: Int,
    val scaleFactor: Float,
    val rotation: Float,
    val colorIndex: Int
)

private data class FallingM3Shape(
    val relX: Float,
    val initialRelY: Float,
    val speedPxPerSec: Float,
    val shapeIndex: Int,
    val scaleFactor: Float,
    val rotationSpeed: Float,
    val colorIndex: Int
)

/**
 * Material 3 Expressive sunset overlay.
 * Features ALL 35 official native [MaterialShapes] from the Material 3 Expressive API:
 * (Arch, Arrow, Boom, SoftBoom, Bun, Burst, SoftBurst, Circle, ClamShell, Clover4Leaf,
 * Clover8Leaf, Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided,
 * Diamond, Fan, Flower, Gem, Ghostish, Heart, Oval, Pentagon, Pill, PixelCircle,
 * PixelTriangle, Puffy, PuffyDiamond, SemiCircle, Slanted, Square, Sunny, Triangle, VerySunny).
 *
 * Shapes pile up neatly without overlapping into each other, letting each distinct shape
 * remain fully visible as the screen fills up from the bottom.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // ALL 35 official native Material 3 Expressive shapes paths (converted once to Compose Path)
    val nativeShapePaths = remember {
        listOf(
            MaterialShapes.Arch,
            MaterialShapes.Arrow,
            MaterialShapes.Boom,
            MaterialShapes.SoftBoom,
            MaterialShapes.Bun,
            MaterialShapes.Burst,
            MaterialShapes.SoftBurst,
            MaterialShapes.Circle,
            MaterialShapes.ClamShell,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Clover8Leaf,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie7Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Diamond,
            MaterialShapes.Fan,
            MaterialShapes.Flower,
            MaterialShapes.Gem,
            MaterialShapes.Ghostish,
            MaterialShapes.Heart,
            MaterialShapes.Oval,
            MaterialShapes.Pentagon,
            MaterialShapes.Pill,
            MaterialShapes.PixelCircle,
            MaterialShapes.PixelTriangle,
            MaterialShapes.Puffy,
            MaterialShapes.PuffyDiamond,
            MaterialShapes.SemiCircle,
            MaterialShapes.Slanted,
            MaterialShapes.Square,
            MaterialShapes.Sunny,
            MaterialShapes.Triangle,
            MaterialShapes.VerySunny
        ).map { polygon ->
            polygon.toPath().asComposePath()
        }
    }

    // Material 3 Expressive Palette
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.errorContainer
    )

    // Grid layout with non-overlapping cells: 6 columns x 13 rows
    val settledShapes = remember {
        val rng = Random(777)
        val list = mutableListOf<SettledM3Shape>()
        val cols = 6
        val rows = 13
        var shapeSequence = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Exact cell center with minimal jitter (keeps shapes neatly separated without overlapping)
                val relX = (c + 0.5f + (rng.nextFloat() * 0.12f - 0.06f)) / cols
                val relY = (r + 0.5f + (rng.nextFloat() * 0.12f - 0.06f)) / rows

                // Cycle through all shapes so every single M3 shape appears prominently
                val shapeIdx = (shapeSequence++) % nativeShapePaths.size
                // Scale factor between 0.86 and 0.94 to guarantee no overlap with neighbors
                val scale = 0.86f + rng.nextFloat() * 0.08f
                val rot = (rng.nextFloat() * 16f) - 8f
                val colorIdx = rng.nextInt(colors.size)
                list.add(SettledM3Shape(relX, relY, shapeIdx, scale, rot, colorIdx))
            }
        }
        list
    }

    // Shower of falling shapes tumbling down towards the fill line
    val fallingShapes = remember {
        val rng = Random(333)
        List(25) {
            FallingM3Shape(
                relX = (it + 0.5f + (rng.nextFloat() * 0.4f - 0.2f)) / 25f,
                initialRelY = rng.nextFloat(),
                speedPxPerSec = 220f + rng.nextFloat() * 180f,
                shapeIndex = rng.nextInt(nativeShapePaths.size),
                scaleFactor = 0.82f + rng.nextFloat() * 0.12f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 140f,
                colorIndex = rng.nextInt(colors.size)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // The fill line rises from bottom (height) to top (0)
            val fillLineY = height * (1f - animatedProgress)
            val cols = 6f
            val rows = 13f
            val cellWidth = width / cols
            val cellHeight = height / rows
            // Sized to fit cleanly inside cell bounds without overlapping neighbors
            val maxShapeSize = min(cellWidth, cellHeight)

            // ── 1. Draw settled M3 expressive shapes (mosaic stacking from bottom up without overlap) ──
            for (shape in settledShapes) {
                val shapeX = shape.relX * width
                val shapeY = shape.relY * height

                // If the fill line has reached this shape's position
                if (shapeY >= fillLineY - cellHeight * 0.5f) {
                    val popProgress = ((shapeY - fillLineY + cellHeight * 0.5f) / cellHeight).coerceIn(0f, 1f)
                    // Joyful M3 spring overshoot scaling into its spot
                    val springScale = popProgress * (1f + 0.14f * (1f - popProgress))
                    val drawSize = maxShapeSize * shape.scaleFactor * springScale
                    val color = colors[shape.colorIndex % colors.size]
                    val path = nativeShapePaths[shape.shapeIndex % nativeShapePaths.size]

                    withTransform({
                        translate(shapeX, shapeY)
                        rotate(shape.rotation, Offset.Zero)
                        scale(drawSize, drawSize, Offset.Zero)
                        translate(-0.5f, -0.5f) // Center the normalized [0,1] polygon at (0,0)
                    }) {
                        drawPath(path, color)
                    }
                }
            }

            // ── 2. Shower of falling native M3 shapes above the fill line ──
            val elapsedSec = (frameTimeMillis.longValue - (startedAtMillis ?: frameTimeMillis.longValue)) / 1000f
            for (falling in fallingShapes) {
                val totalDistance = (falling.initialRelY * height + falling.speedPxPerSec * elapsedSec)
                val currentY = if (fillLineY > 0f) totalDistance % (fillLineY + cellHeight) else -100f

                if (currentY < fillLineY && currentY > -50f) {
                    val currentX = (falling.relX * width).coerceIn(0f, width)
                    val rotation = falling.rotationSpeed * elapsedSec
                    val color = colors[falling.colorIndex % colors.size].copy(alpha = 0.92f)
                    val path = nativeShapePaths[falling.shapeIndex % nativeShapePaths.size]
                    val drawSize = maxShapeSize * falling.scaleFactor

                    withTransform({
                        translate(currentX, currentY)
                        rotate(rotation, Offset.Zero)
                        scale(drawSize, drawSize, Offset.Zero)
                        translate(-0.5f, -0.5f)
                    }) {
                        drawPath(path, color)
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
