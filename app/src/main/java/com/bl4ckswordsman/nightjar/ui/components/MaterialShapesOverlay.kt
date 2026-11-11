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
    val sizePx: Float,
    val rotationSpeed: Float,
    val colorIndex: Int
)

/**
 * Material 3 Expressive sunset overlay.
 * Uses official native [MaterialShapes] (4-sided cookie, 6-sided cookie, bun, heart,
 * ghostish, fan, very sunny, slanted, arch, flower, clamshell, boom, clover, pill, sunny)
 * that tumble from the sky and stack up to fill the screen without any separate rectangular layer.
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

    // Official native Material 3 Expressive shapes paths (converted once to Compose Path)
    val nativeShapePaths = remember {
        listOf(
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Bun,
            MaterialShapes.Heart,
            MaterialShapes.Ghostish,
            MaterialShapes.Fan,
            MaterialShapes.VerySunny,
            MaterialShapes.Slanted,
            MaterialShapes.Arch,
            MaterialShapes.Flower,
            MaterialShapes.ClamShell,
            MaterialShapes.Boom,
            MaterialShapes.SoftBoom,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Pill,
            MaterialShapes.Sunny
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

    // Staggered tessellation of shapes that stack and fill the entire screen
    val settledShapes = remember {
        val rng = Random(888)
        val list = mutableListOf<SettledM3Shape>()
        val cols = 7
        val rows = 14
        for (r in 0 until rows) {
            val isStaggered = (r % 2 == 1)
            val rowCols = if (isStaggered) cols + 1 else cols
            for (c in 0 until rowCols) {
                val baseX = if (isStaggered) (c - 0.5f) / cols else c.toFloat() / cols
                val relX = (baseX + (0.5f / cols) + (rng.nextFloat() * 0.3f - 0.15f) / cols).coerceIn(0f, 1f)
                val relY = (r.toFloat() / rows + (0.5f / rows) + (rng.nextFloat() * 0.3f - 0.15f) / rows).coerceIn(0f, 1f)

                val shapeIdx = rng.nextInt(nativeShapePaths.size)
                val scale = 1.35f + rng.nextFloat() * 0.25f // overlap cells to pack seamlessly
                val rot = (rng.nextFloat() * 60f) - 30f
                val colorIdx = rng.nextInt(colors.size)
                list.add(SettledM3Shape(relX, relY, shapeIdx, scale, rot, colorIdx))
            }
        }
        list
    }

    // Shower of falling shapes tumbling down towards the fill line
    val fallingShapes = remember {
        val rng = Random(555)
        List(30) {
            FallingM3Shape(
                relX = rng.nextFloat(),
                initialRelY = rng.nextFloat(),
                speedPxPerSec = 220f + rng.nextFloat() * 200f,
                shapeIndex = rng.nextInt(nativeShapePaths.size),
                sizePx = 42f + rng.nextFloat() * 22f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 180f,
                colorIndex = rng.nextInt(colors.size)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // The fill line rises from bottom (height) to top (0)
            val fillLineY = height * (1f - animatedProgress)
            val cellBaseSize = width / 6.5f

            // ── 1. Draw settled M3 expressive shapes (mosaic stacking from bottom up) ──
            for (shape in settledShapes) {
                val shapeX = shape.relX * width
                val shapeY = shape.relY * height

                // If the fill line has reached this shape's position
                if (shapeY >= fillLineY - 30f) {
                    val popProgress = ((shapeY - fillLineY + 30f) / (height / 10f)).coerceIn(0f, 1f)
                    // Bouncy overshoot spring scale as each shape appears into place
                    val springScale = popProgress * (1f + 0.18f * (1f - popProgress))
                    val drawSize = cellBaseSize * shape.scaleFactor * springScale
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
                val currentY = if (fillLineY > 0f) totalDistance % (fillLineY + 40f) else -100f

                if (currentY < fillLineY && currentY > -50f) {
                    val currentX = falling.relX * width
                    val rotation = falling.rotationSpeed * elapsedSec
                    val color = colors[falling.colorIndex % colors.size].copy(alpha = 0.92f)
                    val path = nativeShapePaths[falling.shapeIndex % nativeShapePaths.size]

                    withTransform({
                        translate(currentX, currentY)
                        rotate(rotation, Offset.Zero)
                        scale(falling.sizePx, falling.sizePx, Offset.Zero)
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
