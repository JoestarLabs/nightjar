package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.graphics.shapes.toPath
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
 * Features ALL 35 official native [MaterialShapes] from the Material 3 Expressive API,
 * powered by the native Material 3 Expressive Motion System ([MaterialTheme.motionScheme]).
 *
 * Each shape falls smoothly under gravity from the top of the screen and lands directly into
 * its non-overlapping resting slot with an expressive squash-and-stretch impact, building up
 * the pile row-by-row from bottom to top at a steady pace that finishes cleanly ~0.5s before
 * the timer expires.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialShapesOverlay(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
    startedAtMillis: Long? = null,
) {
    // ── Wall-clock continuous progress ──
    val countdown = rememberSunsetCountdownState(remainingSeconds, totalSeconds, startedAtMillis)
    val shownRemaining = if (startedAtMillis != null) countdown.displayRemaining.longValue else remainingSeconds

    // Native Material 3 Expressive Motion Scheme for snappy spatial pulse
    val motionScheme = MaterialTheme.motionScheme
    val fastSpatial = motionScheme.fastSpatialSpec<Float>()
    val defaultSpatial = motionScheme.defaultSpatialSpec<Float>()

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
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.primaryContainer,
            colorScheme.secondary,
            colorScheme.secondaryContainer,
            colorScheme.tertiary,
            colorScheme.tertiaryContainer,
            colorScheme.surfaceVariant,
            colorScheme.errorContainer
        )
    }

    // Grid layout: 6 columns x 13 rows = 78 shapes.
    // Calibrated so the final row finishes landing exactly ~0.5s before timer expiry.
    val physicalShapes = remember(totalSeconds) {
        val rng = Random(999)
        val list = mutableListOf<PhysicalM3Shape>()
        val cols = 6
        val rows = 13
        val totalSecF = totalSeconds.toFloat().coerceAtLeast(5f)

        // 0.5s pause before the screen locks
        val endBufferProgress = (0.5f / totalSecF).coerceIn(0.008f, 0.05f)
        val fallDurationProgress = (1.2f / totalSecF).coerceIn(0.02f, 0.08f)
        val startLandP = 0.04f
        val maxLandP = (1.0f - endBufferProgress - 0.015f).coerceAtLeast(startLandP + 0.1f)
        var shapeSequence = 0

        // Build from bottom row (row 12) to top row (row 0)
        for (r in 0 until rows) {
            val rowFromBottom = (rows - 1) - r
            val baseLandP = startLandP + (rowFromBottom.toFloat() / (rows - 1)) * (maxLandP - startLandP)

            for (c in 0 until cols) {
                val relX = (c + 0.5f) / cols
                val relY = (r + 0.5f) / rows

                val colStagger = rng.nextFloat() * 0.015f
                val landP = (baseLandP + colStagger).coerceAtMost(maxLandP + 0.01f)
                val dropP = (landP - fallDurationProgress).coerceAtLeast(0f)

                val shapeIdx = (shapeSequence++) % nativeShapePaths.size
                // Snug sizing so shapes sit tightly adjacent to each other with minimal gaps
                val scale = 1.03f + rng.nextFloat() * 0.04f
                val restingRot = (rng.nextFloat() * 10f) - 5f
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
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val animatedProgress = countdown.progress.floatValue
            drawRect(Color.Black.copy(alpha = 0.55f * animatedProgress))

            val width = size.width
            val height = size.height

            val cols = 6f
            val rows = 13f
            val cellWidth = width / cols
            val cellHeight = height / rows
            val maxShapeSize = min(cellWidth, cellHeight)
            val spawnY = -maxShapeSize * 1.5f

            for (shape in physicalShapes) {
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

                    val impactT = ((animatedProgress - shape.landProgress) / 0.022f).coerceIn(0f, 1f)
                    if (impactT < 1f) {
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

        SunsetCountdownOverlayText(
            shownRemaining = shownRemaining,
            outlineColor = Color.Black.copy(alpha = 0.85f),
            expandSpec = fastSpatial,
            settleSpec = defaultSpatial,
        )
    }
}
