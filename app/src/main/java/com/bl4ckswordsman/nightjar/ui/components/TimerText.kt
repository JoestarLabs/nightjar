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
    // Smoothly expands in size (50sp -> 56sp for MM:ss) in the final 60 seconds
    val targetBaseFontSize = if (h > 0) {
        36f + (4f * urgencyProgress)
    } else {
        50f + (6f * urgencyProgress)
    }
    val animatedFontSize by animateFloatAsState(
        targetValue = targetBaseFontSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timer_font_size"
    )
    val clampedScale = currentFontScale.coerceIn(1.0f, 1.25f)
    // Convert through dp to sp with current density to prevent double application of system font scaling
    val scaledFontSize = with(density) {
        (animatedFontSize * clampedScale).dp.toSp()
    }

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
        overflow = TextOverflow.Clip,
        modifier = modifier,
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

