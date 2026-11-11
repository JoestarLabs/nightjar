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
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class PhysicalM3Shape(
    val col: Int,
    val row: Int,
    val relX: Float,
    val relY: Float,
    val shapeIndex: Int,
    val colorIndex: Int,
    val scaleFactor: Float,
    val restingRotation: Float,
    val tumbleSpin: Float,
    val dropProgress: Float,
    val landProgress: Float
)

/**
 * Material 3 Expressive sunset overlay.
 * Features ALL 35 official native [MaterialShapes] from the Material 3 Expressive API.
 *
 * Uses a single unified physics system: each shape falls smoothly under gravity from the top
 * of the screen and lands directly into its non-overlapping resting slot with an expressive
 * squash-and-stretch impact, building up the pile row-by-row from bottom to top at a steady,
 * constant pace throughout the countdown.
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

    LaunchedEffect(startedAtMillis, totalSeconds) {
        if (startedAtMillis == null || totalSeconds <= 0L) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                val now = System.currentTimeMillis()
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

    // Grid layout: 6 columns x 13 rows = 78 shapes.
    // Each shape has an assigned target slot, drop time, and land time.
    val physicalShapes = remember {
        val rng = Random(999)
        val list = mutableListOf<PhysicalM3Shape>()
        val cols = 6
        val rows = 13
        val fallDurationProgress = 0.045f // Constant fall duration across all rows
        var shapeSequence = 0

        // Build from bottom row (row 12) to top row (row 0)
        for (r in 0 until rows) {
            // Rows pile up from bottom to top
            val rowFromBottom = (rows - 1) - r // 0 for bottom row, 12 for top row
            val baseLandP = 0.06f + (rowFromBottom.toFloat() / (rows - 1)) * 0.84f

            for (c in 0 until cols) {
                val relX = (c + 0.5f) / cols
                val relY = (r + 0.5f) / rows

                // Slight stagger within each row so shapes don't hit at the exact same millisecond
                val colStagger = (rng.nextFloat() * 0.025f)
                val landP = (baseLandP + colStagger).coerceAtMost(0.98f)
                val dropP = (landP - fallDurationProgress).coerceAtLeast(0f)

                val shapeIdx = (shapeSequence++) % nativeShapePaths.size
                val scale = 0.88f + rng.nextFloat() * 0.06f
                val restingRot = (rng.nextFloat() * 16f) - 8f
                val spin = (rng.nextFloat() - 0.5f) * 120f
                val colorIdx = rng.nextInt(colors.size)

                list.add(
                    PhysicalM3Shape(
                        col = c,
                        row = r,
                        relX = relX,
                        relY = relY,
                        shapeIndex = shapeIdx,
                        colorIndex = colorIdx,
                        scaleFactor = scale,
                        restingRotation = restingRot,
                        tumbleSpin = spin,
                        dropProgress = dropP,
                        landProgress = landP
                    )
                )
            }
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * animatedProgress))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val cols = 6f
            val rows = 13f
            val cellWidth = width / cols
            val cellHeight = height / rows
            val maxShapeSize = min(cellWidth, cellHeight)
            val spawnY = -maxShapeSize * 1.5f

            for (shape in physicalShapes) {
                // If shape hasn't started its drop yet, do not draw
                if (animatedProgress < shape.dropProgress) continue

                val targetX = shape.relX * width
                val targetY = shape.relY * height
                val baseDrawSize = maxShapeSize * shape.scaleFactor
                val color = colors[shape.colorIndex % colors.size]
                val path = nativeShapePaths[shape.shapeIndex % nativeShapePaths.size]

                val currentX: Float
                val currentY: Float
                val currentRot: Float
                val scaleX: Float
                val scaleY: Float

                if (animatedProgress < shape.landProgress) {
                    // ── In flight: Smooth gravitational acceleration from top of screen ──
                    val fallProgress = ((animatedProgress - shape.dropProgress) /
                            (shape.landProgress - shape.dropProgress)).coerceIn(0f, 1f)
                    // Quadratic ease-in acceleration (gravity physics)
                    val gravityT = fallProgress * fallProgress

                    currentX = targetX
                    currentY = spawnY + (targetY - spawnY) * gravityT
                    currentRot = shape.restingRotation + (1f - fallProgress) * shape.tumbleSpin
                    scaleX = 1f
                    scaleY = 1f
                } else {
                    // ── Landed: Expressive squash-and-stretch impact settling into place ──
                    currentX = targetX
                    currentY = targetY
                    currentRot = shape.restingRotation

                    // Time elapsed since touchdown (lasts 0.02 progress units)
                    val impactT = ((animatedProgress - shape.landProgress) / 0.022f).coerceIn(0f, 1f)
                    if (impactT < 1f) {
                        // Dampened harmonic squash on impact: wide & short, then settles to 1.0
                        val squash = sin(impactT * PI.toFloat()) * (1f - impactT) * 0.22f
                        scaleX = 1f + squash
                        scaleY = 1f - squash
                    } else {
                        scaleX = 1f
                        scaleY = 1f
                    }
                }

                withTransform({
                    translate(currentX, currentY)
                    rotate(currentRot, Offset.Zero)
                    scale(baseDrawSize * scaleX, baseDrawSize * scaleY, Offset.Zero)
                    translate(-0.5f, -0.5f) // Center unit [0,1] polygon at (0,0)
                }) {
                    drawPath(path, color)
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
