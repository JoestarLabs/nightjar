@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.bl4ckswordsman.nightjar.ui.screen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.service.LockAccessibilityService
import com.bl4ckswordsman.nightjar.ui.components.AccessibilityPermissionDialog
import com.bl4ckswordsman.nightjar.ui.components.AnimatedAppTitle
import com.bl4ckswordsman.nightjar.ui.components.LockButton
import com.bl4ckswordsman.nightjar.ui.components.NotificationPermissionDialog
import com.bl4ckswordsman.nightjar.ui.components.PresetChips
import com.bl4ckswordsman.nightjar.ui.components.StatusChip
import com.bl4ckswordsman.nightjar.ui.components.ZenTimerDial
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimerViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val commitmentMode by viewModel.commitmentMode.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isRunning = timerState is TimerState.Running
    val isFinished = timerState is TimerState.Finished

    val runningState = timerState as? TimerState.Running
    val runningSeconds = runningState?.remainingSeconds
    val runningTotalSeconds = runningState?.totalSeconds

    // ── Permission dialog state ───────────────────────────────────────────────
    var showNotifDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && !LockAccessibilityService.isEnabled()) {
            showAccessibilityDialog = true
        }
    }

    // ── Dial elevation animation (rises when timer starts) ───────────────────
    val dialElevation by animateDpAsState(
        targetValue = if (isRunning) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dial_elevation"
    )


    // ── Settings gear spin & shape morphing ────────────────────────────────────
    var gearRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        // Spin the gear on entry to look playful
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        ) { value, _ -> gearRotation = value }
    }


    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showNotifDialog) {
        NotificationPermissionDialog(
            onAllow = {
                showNotifDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    if (!LockAccessibilityService.isEnabled()) showAccessibilityDialog = true
                }
            },
            onDismiss = { showNotifDialog = false }
        )
    }
    if (showAccessibilityDialog) {
        AccessibilityPermissionDialog(
            onOpenSettings = {
                showAccessibilityDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedAppTitle()
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onNavigateToSettings,
                        enabled = !isRunning,
                        shapes = IconButtonDefaults.shapes(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            pressedShape = RoundedCornerShape(percent = 18)
                        ),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            ),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.5f
                            )
                        ),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings_icon),
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { rotationZ = gearRotation }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Zen timer dial ────────────────────────────────────────────────
            ZenTimerDial(
                selectedSeconds = viewModel.selectedSeconds,
                runningSeconds = runningSeconds,
                runningTotalSeconds = runningTotalSeconds,
                onSecondsChanged = { viewModel.setSelectedSeconds(it) },
                contentDesc = stringResource(R.string.cd_timer_dial),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Spacer(Modifier.height(8.dp))

            // ── Preset chips ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isRunning && !isFinished,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                PresetChips(
                    selectedSeconds = viewModel.selectedSeconds,
                    onPresetSelected = { viewModel.setSelectedSeconds(it) },
                    enabled = !isRunning,
                    presets = presets,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Status chip ───────────────────────────────────────────────────
            StatusChip(
                isRunning = isRunning,
                isFinishing = isFinished,
            )

            Spacer(Modifier.height(16.dp))

            // ── Start / Stop button ───────────────────────────────────────────
            LockButton(
                isRunning = isRunning,
                isFinishing = isFinished,
                isLocked = isRunning && commitmentMode,
                onClick = {
                    when {
                        isRunning && commitmentMode -> { /* commitment mode: ignore stop */
                        }

                        isRunning -> viewModel.stopTimer()
                        !LockAccessibilityService.isEnabled() -> showAccessibilityDialog = true
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !hasNotificationPermission(context) -> showNotifDialog = true

                        else -> viewModel.startTimer()
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}
