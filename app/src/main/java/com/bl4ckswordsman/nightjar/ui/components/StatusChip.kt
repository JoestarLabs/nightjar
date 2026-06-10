package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Status indicator chip displayed below the timer dial.
 *
 * Shows:
 *  - Idle: outlined circle icon, "Ready" label
 *  - Running: pulsing filled dot + "Timer running" label
 *  - Finished: "Locking screen…"
 */
@Composable
fun StatusChip(
    isRunning: Boolean,
    isFinishing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isFinishing -> stringResource(R.string.status_finished)
        isRunning   -> stringResource(R.string.status_running)
        else        -> stringResource(R.string.status_idle)
    }

    // Pulse animation for running state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_pulse"
    )

    val chipColors = AssistChipDefaults.assistChipColors(
        containerColor = when {
            isFinishing -> MaterialTheme.colorScheme.errorContainer
            isRunning   -> MaterialTheme.colorScheme.primaryContainer
            else        -> MaterialTheme.colorScheme.surfaceVariant
        },
        labelColor = when {
            isFinishing -> MaterialTheme.colorScheme.onErrorContainer
            isRunning   -> MaterialTheme.colorScheme.onPrimaryContainer
            else        -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        leadingIconContentColor = when {
            isRunning -> MaterialTheme.colorScheme.primary
            else      -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

    AssistChip(
        onClick = { /* informational only */ },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.Circle
                              else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier
                    .size(10.dp)
                    .alpha(if (isRunning) pulseAlpha else 1f),
            )
        },
        colors = chipColors,
        border = null,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StatusChipIdlePreview() {
    NightjarTheme { StatusChip(isRunning = false) }
}

@Preview
@Composable
private fun StatusChipRunningPreview() {
    NightjarTheme { StatusChip(isRunning = true) }
}
