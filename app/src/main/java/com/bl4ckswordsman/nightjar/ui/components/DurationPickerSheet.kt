@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Playful, Material 3 Expressive Modal Bottom Sheet that allows parents to easily
 * and accurately input custom hours, minutes, and seconds for the lock timer.
 */
@Composable
fun DurationPickerSheet(
    initialSeconds: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    maxSeconds: Long = 7200L // 2 hours
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hours by remember { mutableIntStateOf((initialSeconds / 3600).toInt().coerceIn(0, 2)) }
    var minutes by remember { mutableIntStateOf(((initialSeconds % 3600) / 60).toInt().coerceIn(0, 59)) }
    var seconds by remember { mutableIntStateOf((initialSeconds % 60).toInt().coerceIn(0, 59)) }

    fun addSeconds(amount: Long) {
        val currentTotal = hours * 3600L + minutes * 60L + seconds
        val newTotal = (currentTotal + amount).coerceIn(0L, maxSeconds)
        hours = (newTotal / 3600).toInt()
        minutes = ((newTotal % 3600) / 60).toInt()
        seconds = (newTotal % 60).toInt()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Set Custom Duration",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Hour : Minute : Second selectors in a card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    UnitColumn(
                        value = hours,
                        label = "Hours",
                        onValueChange = {
                            hours = it
                            // Enforce max constraint
                            val total = hours * 3600L + minutes * 60L + seconds
                            if (total > maxSeconds) {
                                hours = (maxSeconds / 3600).toInt()
                                minutes = ((maxSeconds % 3600) / 60).toInt()
                                seconds = (maxSeconds % 60).toInt()
                            }
                        },
                        max = 2
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 24.dp)
                    )

                    UnitColumn(
                        value = minutes,
                        label = "Minutes",
                        onValueChange = {
                            minutes = it
                            val total = hours * 3600L + minutes * 60L + seconds
                            if (total > maxSeconds) {
                                hours = (maxSeconds / 3600).toInt()
                                minutes = ((maxSeconds % 3600) / 60).toInt()
                                seconds = (maxSeconds % 60).toInt()
                            }
                        },
                        max = 59
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 24.dp)
                    )

                    UnitColumn(
                        value = seconds,
                        label = "Seconds",
                        onValueChange = {
                            seconds = it
                            val total = hours * 3600L + minutes * 60L + seconds
                            if (total > maxSeconds) {
                                hours = (maxSeconds / 3600).toInt()
                                minutes = ((maxSeconds % 3600) / 60).toInt()
                                seconds = (maxSeconds % 60).toInt()
                            }
                        },
                        max = 59
                    )
                }
            }

            // Playful Quick add shortcuts
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                PlayfulShortcutButton(
                    text = "+1m",
                    onClick = { addSeconds(60L) },
                    modifier = Modifier.weight(1f)
                )
                PlayfulShortcutButton(
                    text = "+5m",
                    onClick = { addSeconds(300L) },
                    modifier = Modifier.weight(1f)
                )
                PlayfulShortcutButton(
                    text = "+15m",
                    onClick = { addSeconds(900L) },
                    modifier = Modifier.weight(1f)
                )
                PlayfulShortcutButton(
                    text = "+30m",
                    onClick = { addSeconds(1800L) },
                    modifier = Modifier.weight(1f)
                )
                PlayfulShortcutButton(
                    text = "Clear",
                    onClick = {
                        hours = 0
                        minutes = 0
                        seconds = 0
                    },
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Confirm & Cancel Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Cancel", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = {
                        val totalSeconds = hours * 3600L + minutes * 60L + seconds
                        onConfirm(totalSeconds.coerceIn(0L, maxSeconds))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(text = "Set Timer", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun UnitColumn(
    value: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    max: Int
) {
    var textValue by remember { mutableStateOf("%02d".format(value)) }
    var isFocused by remember { mutableStateOf(false) }

    // Sync textValue with value when value changes externally (e.g. arrow keys, shortcuts)
    LaunchedEffect(value) {
        val parsed = textValue.toIntOrNull() ?: 0
        if (parsed != value) {
            textValue = "%02d".format(value)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        IconButton(
            onClick = { onValueChange((value + 1).coerceAtMost(max)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Increment $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }.take(2)
                textValue = filtered
                if (filtered.isNotEmpty()) {
                    val parsed = filtered.toInt()
                    val constrained = parsed.coerceIn(0, max)
                    onValueChange(constrained)
                } else {
                    onValueChange(0)
                }
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Force the inner text field to fill the width of the decoration box,
                    // allowing it to align the text using TextAlign.Center instead of
                    // wrapping it, which avoids layout alignment and horizontal scrolling issues.
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        propagateMinConstraints = true
                    ) {
                        innerTextField()
                    }
                }
            },
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        textValue = "%02d".format(value)
                    }
                }
                .border(
                    width = 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        IconButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Decrement $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PlayfulShortcutButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btn_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
