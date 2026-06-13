package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular timer dial with spring-physics drag interaction.
 *
 * The dial maps a full 360° rotation to [maxSeconds] of duration.
 * Dragging the handle snaps to the nearest second with a spring release animation.
 *
 * Visual layers (back to front):
 *   1. Track ring (faint, full circle)
 *   2. Filled arc showing selected duration (gradient sweep)
 *   3. Tick marks at quarter positions
 *   4. Drag handle (filled circle at arc end)
 *   5. Centre text: formatted HH:MM:SS
 */
@Composable
fun ZenTimerDial(
    selectedSeconds: Long,
    runningSeconds: Long?,          // non-null while timer is running
    onSecondsChanged: (Long) -> Unit,
    maxSeconds: Long = 7_200L,      // 2 hours
    runningTotalSeconds: Long? = null,
    onDialClicked: (() -> Unit)? = null,
    size: Dp = 280.dp,
    contentDesc: String = "",
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "dial_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Animated sweep angle (degrees, 0..360)
    val displaySeconds = runningSeconds ?: selectedSeconds
    val targetAngle = if (runningSeconds != null) {
        val total = runningTotalSeconds ?: selectedSeconds.takeIf { it > 0L } ?: maxSeconds
        if (total == 0L) 0f
        else (runningSeconds.toFloat() / total.toFloat()) * 360f
    } else {
        (selectedSeconds.toFloat() / maxSeconds.toFloat()) * 360f
    }
    val sweepAnim = remember { Animatable(targetAngle) }

    // Keep in sync with external changes (preset chips, running countdown)
    var prevRunningSeconds by remember { mutableStateOf<Long?>(null) }
    var useSpring by remember { mutableStateOf(true) }

    LaunchedEffect(targetAngle, runningSeconds) {
        val currentlyRunning = runningSeconds != null
        val startedJustNow = currentlyRunning && prevRunningSeconds == null
        
        useSpring = !currentlyRunning || startedJustNow
        prevRunningSeconds = runningSeconds

        sweepAnim.animateTo(
            targetValue = targetAngle,
            animationSpec = if (useSpring) {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            } else {
                tween(durationMillis = 1000, easing = LinearEasing)
            }
        )
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var dragCenter by remember { mutableFloatStateOf(0f) }

    // ── Drag & tactile physics animations ──────────────────────────────────────
    var isDragging by remember { mutableStateOf(false) }

    val strokeWidth by animateDpAsState(
        targetValue = if (isDragging) 26.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dial_stroke_width"
    )

    val handleRadius by animateDpAsState(
        targetValue = if (isDragging) 18.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "handle_radius"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDesc }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxSeconds, runningSeconds) {
                    // Disable drag interaction while the timer is running
                    if (runningSeconds != null) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            dragCenter = size.toPx() / 2f
                            isDragging = true
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val center = dragCenter
                            val dx = change.position.x - center
                            val dy = change.position.y - center
                            // atan2 gives angle from positive-x axis; shift so 0 = top
                            var angle =
                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            if (angle < 0f) angle += 360f

                            val newSeconds = ((angle / 360f) * maxSeconds).toLong()
                                .coerceIn(0L, maxSeconds)

                            scope.launch {
                                sweepAnim.snapTo(angle)
                            }
                            onSecondsChanged(newSeconds)
                        },
                        onDragEnd = {
                            isDragging = false
                            // Spring-snap to the nearest second boundary
                            scope.launch {
                                val snapped = sweepAnim.value
                                sweepAnim.animateTo(
                                    targetValue = snapped,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val sweep = sweepAnim.value
            drawDialTrack(surfaceVariant, strokeWidth.toPx())
            if (runningSeconds != null) {
                drawSquigglyDialArc(sweep, primary, secondary, strokeWidth.toPx(), wavePhase)
            } else {
                drawDialArc(sweep, primary, secondary, strokeWidth.toPx())
            }
            drawTickMarks(onSurface)
            drawDragHandle(sweep, primary, onSurface, handleRadius.toPx())
        }

        val dialClickableModifier = if (runningSeconds == null && onDialClicked != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDialClicked()
            }
        } else Modifier

        // Centre text: HH:MM:SS
        TimerText(
            seconds = displaySeconds,
            isRunning = runningSeconds != null,
            modifier = dialClickableModifier,
        )
    }
}

// ── Draw helpers ──────────────────────────────────────────────────────────────

private fun DrawScope.drawDialTrack(color: Color, strokeWidthPx: Float) {
    drawArc(
        color = color.copy(alpha = 0.35f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        topLeft = Offset(20.dp.toPx(), 20.dp.toPx()),
        size = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx()),
    )
}

private fun DrawScope.drawDialArc(
    sweep: Float,
    primary: Color,
    secondary: Color,
    strokeWidthPx: Float
) {
    if (sweep <= 0f) return
    val gradient = Brush.sweepGradient(
        0f to secondary.copy(alpha = 0.6f),
        0.5f to primary,
        1f to primary,
    )
    drawArc(
        brush = gradient,
        startAngle = -90f,
        sweepAngle = sweep.coerceIn(0f, 360f),
        useCenter = false,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        topLeft = Offset(20.dp.toPx(), 20.dp.toPx()),
        size = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx()),
    )
}

private fun DrawScope.drawSquigglyDialArc(
    sweep: Float,
    primary: Color,
    secondary: Color,
    strokeWidthPx: Float,
    wavePhase: Float
) {
    if (sweep <= 0f) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = (size.width - 40.dp.toPx()) / 2f

    // Wave parameters: amplitude of 3dp, 12 waves around the circle
    val waveAmplitude = 3.dp.toPx()
    val waveFrequency = 12f

    val path = Path()
    val stepDeg = 2f
    val startAngleDeg = -90f
    val endAngleDeg = -90f + sweep

    var isFirst = true
    var currentAngleDeg = startAngleDeg
    while (currentAngleDeg <= endAngleDeg) {
        val angleRad = Math.toRadians(currentAngleDeg.toDouble())
        val offset = waveAmplitude * sin(angleRad * waveFrequency - wavePhase).toFloat()
        val r = baseRadius + offset

        val x = cx + r * cos(angleRad).toFloat()
        val y = cy + r * sin(angleRad).toFloat()

        if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
        } else {
            path.lineTo(x, y)
        }

        if (currentAngleDeg == endAngleDeg) break
        currentAngleDeg = (currentAngleDeg + stepDeg).coerceAtMost(endAngleDeg)
    }

    val gradient = Brush.sweepGradient(
        0f to secondary.copy(alpha = 0.6f),
        0.5f to primary,
        1f to primary,
    )

    drawPath(
        path = path,
        brush = gradient,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawTickMarks(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.width / 2f - 20.dp.toPx()
    val tickAngles = listOf(0f, 90f, 180f, 270f)  // 12, 3, 6, 9 o'clock

    tickAngles.forEach { angleDeg ->
        val rad = Math.toRadians((angleDeg - 90.0))
        val outerX = cx + radius * cos(rad).toFloat()
        val outerY = cy + radius * sin(rad).toFloat()
        val innerX = cx + (radius - 12.dp.toPx()) * cos(rad).toFloat()
        val innerY = cy + (radius - 12.dp.toPx()) * sin(rad).toFloat()
        drawLine(
            color = color.copy(alpha = 0.25f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawDragHandle(
    sweep: Float,
    primary: Color,
    onSurface: Color,
    handleRadiusPx: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.width / 2f - 20.dp.toPx()
    val rad = Math.toRadians((sweep - 90.0))
    val hx = cx + radius * cos(rad).toFloat()
    val hy = cy + radius * sin(rad).toFloat()

    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.15f),
        radius = handleRadiusPx,
        center = Offset(hx + 1.dp.toPx(), hy + 2.dp.toPx()),
    )
    // Handle
    drawCircle(
        color = primary,
        radius = handleRadiusPx,
        center = Offset(hx, hy),
    )
    // Inner dot
    drawCircle(
        color = onSurface.copy(alpha = 0.15f),
        radius = (handleRadiusPx / 3f).coerceAtLeast(2.dp.toPx()),
        center = Offset(hx, hy),
    )
}

@Preview(showBackground = true)
@Composable
private fun ZenTimerDialPreview() {
    NightjarTheme {
        ZenTimerDial(
            selectedSeconds = 1800L,
            runningSeconds = null,
            onSecondsChanged = {},
        )
    }
}
