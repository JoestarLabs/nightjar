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
 */
@Composable
fun LockButton(
    isRunning: Boolean,
    isFinishing: Boolean = false,
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

    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer
                         else MaterialTheme.colorScheme.primaryContainer,
        contentColor   = if (isRunning) MaterialTheme.colorScheme.onErrorContainer
                         else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.scale(scale),
        icon = {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                    (scaleOut(spring(Spring.DampingRatioMediumBouncy)) + fadeOut())
                },
                label = "lock_icon_anim"
            ) { running ->
                Icon(
                    imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.cd_lock_icon),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "lock_text_anim"
            ) { running ->
                Text(
                    text = stringResource(
                        if (running) R.string.btn_stop_timer
                        else         R.string.btn_start_timer
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
