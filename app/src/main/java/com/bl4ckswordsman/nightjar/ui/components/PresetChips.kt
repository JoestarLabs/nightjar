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

data class TimerPreset(val labelRes: Int, val seconds: Long)

val DefaultPresets = listOf(
    TimerPreset(R.string.preset_5m,  5  * 60),
    TimerPreset(R.string.preset_15m, 15 * 60),
    TimerPreset(R.string.preset_30m, 30 * 60),
    TimerPreset(R.string.preset_1h,  60 * 60),
)

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
    presets: List<TimerPreset> = DefaultPresets,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        val anySelected = presets.any { selectedSeconds == it.seconds }

        presets.forEach { preset ->
            val isSelected = selectedSeconds == preset.seconds
            
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
                label = "chip_scale_${preset.seconds}"
            )

            val yOffset by animateDpAsState(
                targetValue = if (isSelected) (-4).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
                label = "chip_y_offset_${preset.seconds}"
            )

            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onPresetSelected(preset.seconds) },
                label = {
                    Text(
                        text = stringResource(preset.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
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
