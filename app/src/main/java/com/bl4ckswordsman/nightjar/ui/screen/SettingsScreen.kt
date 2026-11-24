package com.bl4ckswordsman.nightjar.ui.screen

import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bl4ckswordsman.nightjar.BuildConfig
import com.bl4ckswordsman.nightjar.DependenciesActivity
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.SunsetAnimation
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.receiver.LockDeviceAdminReceiver
import com.bl4ckswordsman.nightjar.ui.components.SunsetAnimationPreviewDialog
import com.bl4ckswordsman.nightjar.ui.components.labelRes
import com.bl4ckswordsman.nightjar.util.DebugInfoFormatter
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings and customization screen for Nightjar.
 *
 * Provides controls for commitment mode, custom duration presets, sunset warning options,
 * language preferences, and open source attribution.
 *
 * @param onNavigateBack Callback invoked when navigating back from the settings screen.
 * @param modifier Modifier applied to the root layout.
 * @param timerViewModel [TimerViewModel] instance providing timer configuration state.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val sunsetModeEnabled by timerViewModel.sunsetModeEnabled.collectAsStateWithLifecycle()
    val sunsetAnimation by timerViewModel.sunsetAnimation.collectAsStateWithLifecycle()
    val sunsetDurationSeconds by timerViewModel.sunsetDurationSeconds.collectAsStateWithLifecycle()
    val sunsetAudioEnabled by timerViewModel.sunsetAudioEnabled.collectAsStateWithLifecycle()

    var showPresetsDialog by rememberSaveable { mutableStateOf(false) }
    var showOverlayDialog by rememberSaveable { mutableStateOf(false) }
    var showAnimationPreview by rememberSaveable { mutableStateOf(false) }

    val onSelectSunsetAnimation: (SunsetAnimation) -> Unit = { anim ->
        if (anim != SunsetAnimation.OFF) {
            if (!Settings.canDrawOverlays(context)) {
                showOverlayDialog = true
            } else {
                timerViewModel.setSunsetAnimation(anim)
            }
        } else {
            timerViewModel.setSunsetAnimation(SunsetAnimation.OFF)
        }
    }

    // Trigger state to force recomposition when returning to settings screen (on resume)
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val selectedLocaleTag = remember(refreshTrigger) {
        localeManager.applicationLocales.toLanguageTags().takeIf { it.isNotEmpty() } ?: ""
    }
    val adminEnabled = remember(refreshTrigger) { LockDeviceAdminReceiver.isEnabled(context) }
    val notificationEnabled = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    val overlayEnabled = remember(refreshTrigger) { Settings.canDrawOverlays(context) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.settings_about_debug_info_copied)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                subtitle = { Text(text = stringResource(R.string.settings_subtitle)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        shapes = IconButtonDefaults.shapes(
                            shape = CircleShape,
                            pressedShape = RoundedCornerShape(percent = 18)
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Language ──────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_language))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                val currentLanguageLabel = when {
                    selectedLocaleTag.startsWith("en") -> stringResource(R.string.settings_language_en)
                    selectedLocaleTag.startsWith("sv") -> stringResource(R.string.settings_language_sv)
                    else -> stringResource(R.string.settings_language_system)
                }

                SettingsActionRow(
                    title = stringResource(R.string.settings_section_language),
                    description = currentLanguageLabel,
                    icon = Icons.Rounded.Language,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        startActivitySafely(context, intent)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Permissions ───────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_permissions))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsPermissionRow(
                    title = stringResource(R.string.settings_perm_admin),
                    description = stringResource(R.string.settings_perm_admin_desc),
                    statusText = stringResource(
                        if (adminEnabled) R.string.settings_perm_admin_enabled
                        else R.string.settings_perm_admin_disabled
                    ),
                    icon = Icons.Rounded.Lock,
                    granted = adminEnabled,
                    clickableWhenGranted = false,
                    onClick = {
                        startActivitySafely(context, LockDeviceAdminReceiver.getRegisterIntent(context))
                    }
                )

                SettingsPermissionRow(
                    title = stringResource(R.string.settings_perm_notification),
                    description = stringResource(R.string.settings_perm_notification_desc),
                    statusText = stringResource(
                        if (notificationEnabled) R.string.settings_perm_enabled
                        else R.string.settings_perm_disabled
                    ),
                    icon = Icons.Rounded.Notifications,
                    granted = notificationEnabled,
                    clickableWhenGranted = true,
                    onClick = {
                        startActivitySafely(
                            context,
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                )

                SettingsPermissionRow(
                    title = stringResource(R.string.settings_perm_overlay),
                    description = stringResource(R.string.settings_perm_overlay_desc),
                    statusText = stringResource(
                        if (overlayEnabled) R.string.settings_perm_enabled
                        else R.string.settings_perm_disabled
                    ),
                    icon = Icons.Rounded.Warning,
                    granted = overlayEnabled,
                    clickableWhenGranted = true,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        startActivitySafely(context, intent)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Timer behaviour ───────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_timer_behaviour))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_commitment_mode_title),
                    description = stringResource(R.string.settings_commitment_mode_desc),
                    icon = Icons.Rounded.Lock,
                    checked = commitmentMode,
                    enabled = !timerIsRunning,
                    onCheckedChange = { timerViewModel.setCommitmentMode(it) }
                )

                SettingsActionRow(
                    title = stringResource(R.string.settings_custom_presets_title),
                    description = stringResource(R.string.settings_custom_presets_desc),
                    icon = Icons.Rounded.Edit,
                    onClick = { showPresetsDialog = true }
                )

                SettingsToggleRow(
                    title = stringResource(R.string.settings_sunset_mode_title),
                    description = stringResource(R.string.settings_sunset_mode_desc),
                    icon = Icons.Rounded.Warning,
                    checked = sunsetModeEnabled,
                    enabled = !timerIsRunning,
                    onCheckedChange = { isChecked ->
                        if (isChecked && !Settings.canDrawOverlays(context)) {
                            showOverlayDialog = true
                        } else {
                            timerViewModel.setSunsetModeEnabled(isChecked)
                        }
                    }
                )

                if (sunsetModeEnabled) {
                    var isAnimationMenuExpanded by remember { mutableStateOf(false) }
                    val animationLabel = stringResource(sunsetAnimation.labelRes())

                    SettingsListItem(
                        title = stringResource(R.string.settings_sunset_animation_title),
                        leadingIcon = Icons.Rounded.AutoAwesome,
                        supportingContent = {
                            Box(modifier = Modifier.padding(top = 6.dp)) {
                                SettingsDropdownPill(
                                    label = animationLabel,
                                    expanded = isAnimationMenuExpanded,
                                    enabled = !timerIsRunning,
                                    onExpandedChange = { isAnimationMenuExpanded = it }
                                ) {
                                    listOf(
                                        SunsetAnimation.FLUID_LIQUID,
                                        SunsetAnimation.MATERIAL_SHAPES,
                                        SunsetAnimation.SNOWFLAKES,
                                        SunsetAnimation.CLASSIC_WAVE,
                                    ).forEach { anim ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(anim.labelRes())) },
                                            onClick = {
                                                isAnimationMenuExpanded = false
                                                onSelectSunsetAnimation(anim)
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { showAnimationPreview = true },
                                enabled = !timerIsRunning,
                                shapes = IconButtonDefaults.shapes(
                                    shape = CircleShape,
                                    pressedShape = RoundedCornerShape(percent = 18)
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    contentDescription = stringResource(R.string.settings_sunset_animation_preview_cd),
                                    tint = if (!timerIsRunning) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = !timerIsRunning) {
                            isAnimationMenuExpanded = true
                        }
                    )

                    var isDurationMenuExpanded by remember { mutableStateOf(false) }
                    val durationLabel = stringResource(
                        R.string.settings_sunset_duration_format,
                        sunsetDurationSeconds
                    )

                    SettingsListItem(
                        title = stringResource(R.string.settings_sunset_duration_title),
                        leadingIcon = Icons.Rounded.Edit,
                        supportingContent = { Text(durationLabel) },
                        trailingContent = {
                            SettingsDropdownPill(
                                label = durationLabel,
                                expanded = isDurationMenuExpanded,
                                enabled = !timerIsRunning,
                                onExpandedChange = { isDurationMenuExpanded = it }
                            ) {
                                listOf(15L, 30L, 45L, 60L).forEach { duration ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    R.string.settings_sunset_duration_format,
                                                    duration
                                                )
                                            )
                                        },
                                        onClick = {
                                            isDurationMenuExpanded = false
                                            timerViewModel.setSunsetDurationSeconds(duration)
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable(enabled = !timerIsRunning) {
                            isDurationMenuExpanded = true
                        }
                    )

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_sunset_audio_title),
                        description = stringResource(R.string.settings_sunset_audio_desc),
                        icon = Icons.Rounded.Notifications,
                        checked = sunsetAudioEnabled,
                        enabled = !timerIsRunning,
                        onCheckedChange = { timerViewModel.setSunsetAudioEnabled(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsActionRow(
                    title = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    description = stringResource(R.string.settings_about_version_desc),
                    icon = Icons.Rounded.Info,
                )

                SettingsActionRow(
                    title = stringResource(R.string.settings_about_github),
                    description = stringResource(R.string.settings_about_github_desc),
                    icon = Icons.Rounded.Code,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/JoestarLabs".toUri()
                        )
                        startActivitySafely(context, intent)
                    }
                )

                SettingsActionRow(
                    title = stringResource(R.string.settings_about_dependencies),
                    description = stringResource(R.string.settings_about_dependencies_desc),
                    icon = Icons.Rounded.Extension,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        startActivitySafely(
                            context,
                            Intent(context, DependenciesActivity::class.java)
                        )
                    }
                )

                SettingsActionRow(
                    title = stringResource(R.string.settings_about_debug_info),
                    description = stringResource(R.string.settings_about_debug_info_desc),
                    icon = Icons.Rounded.BugReport,
                    trailingIcon = Icons.Rounded.ContentCopy,
                    onClick = {
                        coroutineScope.launch {
                            val formatted = withContext(Dispatchers.IO) {
                                DebugInfoFormatter.format(DebugInfoFormatter.collect(context))
                            }
                            val clipboard =
                                context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText("Nightjar debug info", formatted)
                            )
                            snackbarHostState.showSnackbar(copiedMessage)
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPresetsDialog) {
        CustomPresetsDialog(
            currentPresets = presetsState,
            onSave = { presets ->
                timerViewModel.saveCustomPresets(presets)
                showPresetsDialog = false
            },
            onDismiss = { showPresetsDialog = false }
        )
    }

    if (showAnimationPreview) {
        SunsetAnimationPreviewDialog(
            initialAnimation = sunsetAnimation,
            onApplyAnimation = { anim ->
                if (anim != SunsetAnimation.OFF && !Settings.canDrawOverlays(context)) {
                    showOverlayDialog = true
                } else {
                    timerViewModel.setSunsetAnimation(anim)
                    showAnimationPreview = false
                }
            },
            onDismiss = { showAnimationPreview = false }
        )
    }

    if (showOverlayDialog) {
        OverlayPermissionDialog(
            onOpenSettings = {
                showOverlayDialog = false
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                startActivitySafely(context, intent)
            },
            onDismiss = { showOverlayDialog = false }
        )
    }
}

private fun startActivitySafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("SettingsScreen", "No activity found for intent: ${intent.action}", e)
    } catch (e: SecurityException) {
        Log.w("SettingsScreen", "Unable to launch intent: ${intent.action}", e)
    }
}
