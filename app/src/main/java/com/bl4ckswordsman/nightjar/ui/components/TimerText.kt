@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Formats [seconds] as HH:MM:SS or MM:SS and displays it as a large
 * serif time string. Used in the centre of [ZenTimerDial].
 *
 * System font scaling is applied directly through sp units, while base font sizing
 * gracefully adapts to prevent clipping at high accessibility font scales (up to 2.0x).
 *
 * @param seconds Remaining or selected time in seconds.
 * @param isRunning True if the countdown timer is currently active.
 * @param modifier Modifier applied to the text component.
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

    // ── Urgency calculation (final 60 seconds) ────────────────────────────────
    val urgencyProgress = if (isRunning && seconds in 1L..60L) {
        (60L - seconds).toFloat() / 60f
    } else if (isRunning && seconds == 0L) {
        1.0f
    } else {
        0.0f
    }

    // ── Width axis tick bounce & urgency widening ─────────────────────────────
    // Baseline condensed master (78f) naturally widens to 94f as urgency swells
    val targetBaseWidth = 78f + (16f * urgencyProgress)
    val tickWidth = remember { Animatable(78f) }
    LaunchedEffect(seconds) {
        if (isRunning && seconds > 0) {
            val pulseSnap = targetBaseWidth + 30f // Noticeable pulse bounce on every second
            tickWidth.snapTo(pulseSnap)
            tickWidth.animateTo(
                targetValue = targetBaseWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            tickWidth.snapTo(targetBaseWidth)
        }
    }

    // ── Weight axis urgency bolding ───────────────────────────────────────────
    // Solid SemiBold baseline (600f) ensures text is bold & crisp while leaving
    // significant dynamic headroom to ramp to Ultra Black (1000f) at countdown end
    val targetWeight = when {
        !isRunning -> 600f
        else -> 600f + (1000f - 600f) * urgencyProgress
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
    // Natural tall display: native condensed axis, opsz=72 display sizing, subtle 10% softening
    val timerFontFamily = remember(tickWidth.value, animatedWeight) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.width(tickWidth.value),
                    FontVariation.weight(animatedWeight.toInt()),
                    FontVariation.Setting("opsz", 72f),
                    FontVariation.Setting("ROND", 10f),
                )
            )
        )
    }

    // ── Dynamic font size enlargement during countdown ───────────────────────
    val density = LocalDensity.current
    val currentFontScale = density.fontScale

    // Base font sizing smoothly expands during the final 60 seconds (urgencyProgress).
    // When accessibility font scaling is enabled by the system (up to 2.0x), base font sizing
    // gracefully scales so the countdown remains highly legible while avoiding clipping against the dial edges.
    val fontScalingCompensator = if (currentFontScale > 1.0f) {
        (1f + 0.35f * (currentFontScale - 1f)) / currentFontScale
    } else {
        1.0f
    }
    val targetBaseFontSize = if (h > 0) {
        (34f + (4f * urgencyProgress)) * fontScalingCompensator
    } else {
        (48f + (6f * urgencyProgress)) * fontScalingCompensator
    }
    val animatedFontSize by animateFloatAsState(
        targetValue = targetBaseFontSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timer_font_size"
    )
    val scaledFontSize = animatedFontSize.sp

    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = scaledFontSize,
            fontFamily = timerFontFamily,
            fontFeatureSettings = "tnum",
            color = if (isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 8.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111210)
@Composable
private fun TimerTextPreview() {
    NightjarTheme {
        TimerText(
            seconds = 1500L,
            isRunning = true,
        )
    }
}

