package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Material 3 Expressive Slide-to-Confirm Emergency Unlock Track.
 *
 * Features:
 * - Horizontal drag tracking with spring-physics snap-back if released early.
 * - Escalating haptic feedback at 25%, 50%, and 85%+ milestones.
 * - Dynamic color interpolation from surface variant to error container.
 * - Smooth container shape morphing.
 */
@Composable
fun EmergencyUnlockSlider(
    onConfirmUnlock: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val dragOffset = remember { Animatable(0f) }
    var lastHapticMilestone by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val maxPx = with(density) { (maxWidth - 56.dp).toPx() }.coerceAtLeast(1f)
        val fraction = (dragOffset.value / maxPx).coerceIn(0f, 1f)

        val containerColor by animateColorAsState(
            targetValue = androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.errorContainer,
                fraction
            ),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "slider_container_color"
        )

        val contentColor by animateColorAsState(
            targetValue = androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.onErrorContainer,
                fraction
            ),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "slider_content_color"
        )

        val thumbColor by animateColorAsState(
            targetValue = if (fraction > 0.5f) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            label = "slider_thumb_color"
        )

        fun checkHapticMilestone(currentFraction: Float) {
            when {
                currentFraction >= 0.85f && lastHapticMilestone < 3 -> {
                    lastHapticMilestone = 3
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }

                currentFraction >= 0.5f && lastHapticMilestone < 2 -> {
                    lastHapticMilestone = 2
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                currentFraction >= 0.25f && lastHapticMilestone < 1 -> {
                    lastHapticMilestone = 1
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                currentFraction < 0.2f -> {
                    lastHapticMilestone = 0
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                lastHapticMilestone = 0
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (dragOffset.value >= maxPx * 0.85f) {
                                        dragOffset.animateTo(
                                            targetValue = maxPx,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessHigh
                                            )
                                        )
                                        onConfirmUnlock()
                                    } else {
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        onCancel()
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onCancel()
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxPx)
                                scope.launch {
                                    dragOffset.snapTo(newOffset)
                                }
                                checkHapticMilestone(newOffset / maxPx)
                            }
                        )
                    }
            ) {
                // Background progress fill trailing the thumb
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(with(density) { (dragOffset.value + 56.dp.toPx()).toDp() })
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                    )
                }

                // Label Text
                val labelText = if (fraction > 0.1f) {
                    stringResource(R.string.slider_release_to_cancel)
                } else {
                    stringResource(R.string.slider_slide_to_stop)
                }

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 60.dp)
                )

                // Drag Thumb Handle
                Surface(
                    shape = CircleShape,
                    color = thumbColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(4.dp)
                        .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                        .size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (fraction > 0.5f) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EmergencyUnlockSliderPreview() {
    NightjarTheme {
        EmergencyUnlockSlider(onConfirmUnlock = {}, onCancel = {})
    }
}
