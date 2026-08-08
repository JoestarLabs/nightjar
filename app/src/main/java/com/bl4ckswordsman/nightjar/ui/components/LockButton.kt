package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Primary action button for starting / stopping the lock timer.
 *
 * Uses M3 [ExtendedFloatingActionButton] with:
 * - Animated icon transition (lock → stop) via [AnimatedContent]
 * - Spring-eased scale and squircle shape morphing on press for tactile feedback
 * - 5-second hold gesture in commitment mode to reveal emergency unlock sheet.
 */
@Composable
fun LockButton(
    isRunning: Boolean,
    isFinishing: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onRevealSlider: (() -> Unit)? = null,
    onLockedShortTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFinishing) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "lock_btn_scale"
    )

    val cornerPercent by animateIntAsState(
        targetValue = when {
            isPressed -> 18     // Morph to squircle on press (matches settings button)
            isLocked -> 24      // M3 Expressive shield squircle (24% corner radius)
            isRunning -> 32     // M3 Expressive warning squircle (32% corner radius)
            else -> 50          // M3 Expressive pill (50% corner radius)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "btn_shape_corners"
    )

    val containerColor = when {
        isLocked -> MaterialTheme.colorScheme.surfaceVariant
        isRunning -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
        isRunning -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val pointerModifier = if (isLocked && onRevealSlider != null) {
        Modifier.pointerInput(isLocked) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val startTime = System.currentTimeMillis()
                val totalDurationMs = 5000L
                var revealed = false

                val job = scope.launch {
                    delay(totalDurationMs)
                    if (isActive) {
                        revealed = true
                        onRevealSlider()
                    }
                }

                val up = waitForUpOrCancellation()
                val elapsed = System.currentTimeMillis() - startTime
                job.cancel()

                if (!revealed && up != null && elapsed < 500L) {
                    onLockedShortTap?.invoke()
                }
            }
        }
    } else Modifier

    ExtendedFloatingActionButton(
        onClick = {
            if (!isLocked) onClick()
        },
        interactionSource = interactionSource,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(percent = cornerPercent),
        modifier = modifier
            .then(pointerModifier)
            .scale(scale),
        icon = {
            AnimatedContent(
                targetState = Triple(isRunning, isLocked, isFinishing),
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                            (scaleOut(spring(Spring.DampingRatioMediumBouncy)) + fadeOut())
                },
                label = "lock_icon_anim"
            ) { (running, locked, _) ->
                val icon: ImageVector = when {
                    locked -> Icons.Rounded.Lock
                    running -> Icons.Rounded.Stop
                    else -> Icons.Rounded.LockOpen
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.cd_lock_icon),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            AnimatedContent(
                targetState = Triple(isRunning, isLocked, isFinishing),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "lock_text_anim"
            ) { (running, locked, _) ->
                Text(
                    text = stringResource(
                        when {
                            locked -> R.string.btn_commitment_locked
                            running -> R.string.btn_stop_timer
                            else -> R.string.btn_start_timer
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    )
}

@Preview
@Composable
private fun LockButtonIdlePreview() {
    NightjarTheme { LockButton(isRunning = false, onClick = {}) }
}

@Preview
@Composable
private fun LockButtonRunningPreview() {
    NightjarTheme { LockButton(isRunning = true, onClick = {}) }
}

@Preview
@Composable
private fun LockButtonLockedPreview() {
    NightjarTheme { LockButton(isRunning = true, isLocked = true, onClick = {}) }
}






