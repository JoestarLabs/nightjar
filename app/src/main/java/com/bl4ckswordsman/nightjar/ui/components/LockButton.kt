package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Primary action button for starting / stopping the lock timer.
 *
 * Uses M3 [ExtendedFloatingActionButton] with:
 * - Animated icon transition (lock → stop) via [AnimatedContent]
 * - Spring-eased scale on press for tactile feedback
 * - Subtle scale spring when transitioning between running/idle states
 * - [isLocked] = true when commitment mode is active: button shows a locked state
 *   and is visually muted to signal that cancellation is disabled.
 */
@Composable
fun LockButton(
    isRunning: Boolean,
    isFinishing: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isFinishing) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "lock_btn_scale"
    )

    val containerColor = when {
        isLocked  -> MaterialTheme.colorScheme.surfaceVariant
        isRunning -> MaterialTheme.colorScheme.errorContainer
        else      -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isLocked  -> MaterialTheme.colorScheme.onSurfaceVariant
        isRunning -> MaterialTheme.colorScheme.onErrorContainer
        else      -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor   = contentColor,
        modifier = modifier.scale(scale),
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
                    locked  -> Icons.Rounded.Lock
                    running -> Icons.Rounded.Stop
                    else    -> Icons.Rounded.LockOpen
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
                            locked  -> R.string.btn_commitment_locked
                            running -> R.string.btn_stop_timer
                            else    -> R.string.btn_start_timer
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
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

