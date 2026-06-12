package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

/**
 * Formats preset durations in seconds into localized strings dynamically.
 */
@Composable
fun formatPresetDuration(seconds: Long): String {
    val minutes = seconds / 60L
    return when {
        minutes >= 60L && minutes % 60L == 0L -> {
            val hours = minutes / 60L
            if (hours == 1L) stringResource(R.string.preset_format_hour_one)
            else stringResource(R.string.preset_format_hour_many, hours)
        }

        minutes >= 60L -> {
            val hours = minutes / 60L
            val remainingMins = minutes % 60L
            stringResource(R.string.preset_format_hour_min, hours, remainingMins)
        }

        else -> {
            stringResource(R.string.preset_format_min, minutes)
        }
    }
}

/**
 * Row of quick-select preset chips.
 * Selecting a chip animates with a spring scale bounce and updates [selectedSeconds].
 */
@Composable
fun PresetChips(
    selectedSeconds: Long,
    onPresetSelected: (Long) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    presets: List<Long> = listOf(300L, 900L, 1800L, 3600L),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        val anySelected = presets.any { selectedSeconds == it }

        presets.forEach { presetSeconds ->
            val isSelected = selectedSeconds == presetSeconds

            val targetScale = when {
                isSelected -> 1.12f
                anySelected -> 0.94f
                else -> 1.0f
            }

            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "chip_scale_$presetSeconds"
            )

            val yOffset by animateDpAsState(
                targetValue = if (isSelected) (-4).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "chip_y_offset_$presetSeconds"
            )

            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onPresetSelected(presetSeconds) },
                label = {
                    Text(
                        text = formatPresetDuration(presetSeconds),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .scale(scale)
                    .offset(y = yOffset)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PresetChipsPreview() {
    NightjarTheme {
        PresetChips(selectedSeconds = 900L, onPresetSelected = {})
    }
}

