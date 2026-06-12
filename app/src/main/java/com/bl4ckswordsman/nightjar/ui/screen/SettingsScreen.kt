package com.bl4ckswordsman.nightjar.ui.screen

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bl4ckswordsman.nightjar.BuildConfig
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.service.LockAccessibilityService
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel

data class LanguageOption(val tag: String, val labelRes: Int)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.settings_language_system),
    LanguageOption("en", R.string.settings_language_en),
    LanguageOption("sv", R.string.settings_language_sv),
)

@Composable
private fun RoundedCardContainer(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 2.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    timerViewModel: TimerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    val commitmentMode by timerViewModel.commitmentMode.collectAsStateWithLifecycle()
    val timerState by timerViewModel.timerState.collectAsStateWithLifecycle()
    val timerIsRunning = timerState is TimerState.Running
    val presetsState by timerViewModel.presets.collectAsStateWithLifecycle()

    var showPresetsDialog by remember { mutableStateOf(false) }

    // Read current locale from LocaleManager
    var selectedLocaleTag by remember {
        mutableStateOf(
            localeManager.applicationLocales.toLanguageTags().takeIf { it.isNotEmpty() } ?: ""
        )
    }

    val accessibilityEnabled = LockAccessibilityService.isEnabled()
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Language ──────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_language))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                Box {
                    val currentLanguageLabel = when {
                        selectedLocaleTag.startsWith("en") -> stringResource(R.string.settings_language_en)
                        selectedLocaleTag.startsWith("sv") -> stringResource(R.string.settings_language_sv)
                        else -> stringResource(R.string.settings_language_system)
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_section_language)) },
                        supportingContent = { Text(currentLanguageLabel) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            androidx.compose.material3.Surface(
                                onClick = { isMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = currentLanguageLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMenuExpanded = true }
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        LANGUAGE_OPTIONS.forEach { option ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    isMenuExpanded = false
                                    selectedLocaleTag = option.tag
                                    localeManager.applicationLocales =
                                        LocaleList.forLanguageTags(option.tag)
                                    // Recreate activity to force language rebinding immediately
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Permissions ───────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_permissions))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                // Accessibility
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_perm_accessibility))
                    },
                    supportingContent = {
                        Text(
                            text = if (accessibilityEnabled)
                                stringResource(R.string.settings_perm_accessibility_enabled)
                            else
                                stringResource(R.string.settings_perm_accessibility_disabled),
                            color = if (accessibilityEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (accessibilityEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (accessibilityEnabled) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (accessibilityEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !accessibilityEnabled) {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                )

                // Notifications
                val notificationEnabled =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_perm_notification))
                    },
                    supportingContent = {
                        Text(
                            text = if (notificationEnabled)
                                stringResource(R.string.settings_perm_accessibility_enabled)
                            else
                                stringResource(R.string.settings_perm_accessibility_disabled),
                            color = if (notificationEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = if (notificationEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (notificationEnabled) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (notificationEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Timer behaviour ───────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_timer_behaviour))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_commitment_mode_title))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_commitment_mode_desc))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = commitmentMode,
                            onCheckedChange = { timerViewModel.setCommitmentMode(it) },
                            enabled = !timerIsRunning,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_custom_presets_title))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_custom_presets_desc))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetsDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.settings_about_version,
                                BuildConfig.VERSION_NAME
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPresetsDialog) {
        val currentPresets = presetsState
        var preset1 by remember { mutableStateOf(if (currentPresets.isNotEmpty()) (currentPresets[0] / 60).toString() else "5") }
        var preset2 by remember { mutableStateOf(if (currentPresets.size > 1) (currentPresets[1] / 60).toString() else "15") }
        var preset3 by remember { mutableStateOf(if (currentPresets.size > 2) (currentPresets[2] / 60).toString() else "30") }
        var preset4 by remember { mutableStateOf(if (currentPresets.size > 3) (currentPresets[3] / 60).toString() else "60") }

        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
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

                    OutlinedTextField(
                        value = preset1,
                        onValueChange = {
                            preset1 = it
                            errorMessage = null
                        },
                        label = { Text("#1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = preset2,
                        onValueChange = {
                            preset2 = it
                            errorMessage = null
                        },
                        label = { Text("#2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = preset3,
                        onValueChange = {
                            preset3 = it
                            errorMessage = null
                        },
                        label = { Text("#3") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = preset4,
                        onValueChange = {
                            preset4 = it
                            errorMessage = null
                        },
                        label = { Text("#4") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

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
                        val val1 = preset1.trim().toLongOrNull()
                        val val2 = preset2.trim().toLongOrNull()
                        val val3 = preset3.trim().toLongOrNull()
                        val val4 = preset4.trim().toLongOrNull()

                        if (val1 != null && val1 in 1..120 &&
                            val2 != null && val2 in 1..120 &&
                            val3 != null && val3 in 1..120 &&
                            val4 != null && val4 in 1..120
                        ) {
                            timerViewModel.saveCustomPresets(listOf(val1, val2, val3, val4))
                            showPresetsDialog = false
                        } else {
                            errorMessage = context.getString(R.string.dialog_invalid_input)
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_btn_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPresetsDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
