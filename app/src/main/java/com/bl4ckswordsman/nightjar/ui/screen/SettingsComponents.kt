package com.bl4ckswordsman.nightjar.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bl4ckswordsman.nightjar.R

/**
 * Container layout applying rounded corners and spacing to a column of card items.
 */
@Composable
internal fun RoundedCardContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/**
 * Header label for sections in the settings list.
 */
@Composable
internal fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/**
 * Full-row toggleable setting item with synchronized switch thumb press feedback and Switch semantics.
 */
@Composable
internal fun SettingsToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ListItem(
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                interactionSource = interactionSource,
                enabled = enabled,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    ) {
        Text(title)
    }
}

/**
 * Setting row displaying permission status (active/inactive) and optional click action.
 */
@Composable
internal fun SettingsPermissionRow(
    title: String,
    description: String,
    statusText: String,
    icon: ImageVector,
    granted: Boolean,
    clickableWhenGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    ListItem(
        supportingContent = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
            )
        },
        trailingContent = {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = statusColor,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = clickableWhenGranted || !granted, onClick = onClick)
    ) {
        Text(title)
    }
}

/**
 * Expressive pill button paired with a [DropdownMenu] for settings selection rows.
 */
@Composable
internal fun SettingsDropdownPill(
    label: String,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 18 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dropdown_pill_corners"
    )

    Box {
        Surface(
            onClick = { if (enabled) onExpandedChange(true) },
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(percent = cornerPercent),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            menuContent()
        }
    }
}

/**
 * Dialog for editing the four quick-select preset durations (in minutes, 1..120).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CustomPresetsDialog(
    currentPresets: List<Long>,
    onSave: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var preset1 by rememberSaveable { mutableStateOf(if (currentPresets.isNotEmpty()) (currentPresets[0] / 60).toString() else "5") }
    var preset2 by rememberSaveable { mutableStateOf(if (currentPresets.size > 1) (currentPresets[1] / 60).toString() else "15") }
    var preset3 by rememberSaveable { mutableStateOf(if (currentPresets.size > 2) (currentPresets[2] / 60).toString() else "30") }
    var preset4 by rememberSaveable { mutableStateOf(if (currentPresets.size > 3) (currentPresets[3] / 60).toString() else "60") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val invalidInputStr = stringResource(R.string.dialog_invalid_input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_presets_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dialog_presets_help),
                    style = MaterialTheme.typography.bodyMedium
                )

                listOf(
                    Triple("#1", preset1, { v: String -> preset1 = v }),
                    Triple("#2", preset2, { v: String -> preset2 = v }),
                    Triple("#3", preset3, { v: String -> preset3 = v }),
                    Triple("#4", preset4, { v: String -> preset4 = v }),
                ).forEach { (fieldLabel, value, setter) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            setter(it)
                            errorMessage = null
                        },
                        label = { Text(fieldLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = listOf(preset1, preset2, preset3, preset4).map { it.trim().toLongOrNull() }
                    if (parsed.all { it != null && it in 1L..120L }) {
                        onSave(parsed.filterNotNull())
                    } else {
                        errorMessage = invalidInputStr
                    }
                },
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(percent = 18)
                )
            ) {
                Text(stringResource(R.string.dialog_btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(percent = 18)
                )
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Dialog explaining why Nightjar needs SYSTEM_ALERT_WINDOW and redirecting to Android Settings.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OverlayPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_overlay_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_overlay_body),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onOpenSettings,
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(percent = 18)
                )
            ) {
                Text(stringResource(R.string.dialog_btn_open_settings))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(percent = 18)
                )
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
