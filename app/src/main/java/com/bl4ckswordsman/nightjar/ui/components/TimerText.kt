@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import com.bl4ckswordsman.nightjar.R

/**
 * Formats [seconds] as HH:MM:SS or MM:SS and displays it as a large
 * serif time string. Used in the centre of [ZenTimerDial].
 */
@Composable
fun TimerText(
    seconds: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)

    // ── Width axis tick bounce ────────────────────────────────────────────────
    val tickWidth = remember { Animatable(100f) }
    LaunchedEffect(seconds) {
        if (isRunning && seconds > 0) {
            tickWidth.snapTo(145f) // Snap to a stretched width
            tickWidth.animateTo(
                targetValue = 100f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            tickWidth.snapTo(100f)
        }
    }

    // ── Weight axis urgency bolding ───────────────────────────────────────────
    val targetWeight = when {
        !isRunning -> 300f  // Light font weight equivalent
        seconds <= 60L -> {
            // Transition from W400 (Normal) to W900 (Black) in the final 60 seconds
            val progress = (60L - seconds).toFloat() / 60f
            400f + (900f - 400f) * progress
        }

        else -> 400f        // Normal font weight equivalent
    }

    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timer_text_weight"
    )

    // ── Create custom variable font family from animated values ──────────────
    val timerFontFamily = remember(tickWidth.value, animatedWeight) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.width(tickWidth.value),
                    FontVariation.weight(animatedWeight.toInt())
                )
            )
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(
            fontFamily = timerFontFamily,
            color = if (isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        ),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

