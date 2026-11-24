package com.bl4ckswordsman.nightjar.ui.effects

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Holds the live [EffectsQuality] state and allows a render loop's frame-time watchdog
 * to latch a one-way downgrade when sustained frame-budget overruns occur.
 */
@Stable
class EffectsQualityController internal constructor(
    qualityState: State<EffectsQuality>,
    private val onFrameBudgetExceeded: () -> Unit,
) {
    val quality: EffectsQuality by qualityState

    /** Signals that the render loop could not sustain the target frame budget. */
    fun reportFrameBudgetExceeded() {
        onFrameBudgetExceeded()
    }
}

/**
 * Observes battery saver, thermal status, animator scale, and low-RAM status for the lifetime
 * of the composition, returning an [EffectsQualityController].
 */
@Composable
fun rememberEffectsQualityController(): EffectsQualityController {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    val activityManager = remember(context) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    val animationsDisabled = remember(context) {
        readAnimationsDisabled(context)
    }
    val isLowRamDevice = remember(activityManager) {
        activityManager?.isLowRamDevice == true
    }

    var signals by remember(animationsDisabled, isLowRamDevice) {
        mutableStateOf(
            EffectsSignals(
                animationsDisabled = animationsDisabled,
                powerSaveMode = powerManager?.isPowerSaveMode == true,
                thermalStatus = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE,
                isLowRamDevice = isLowRamDevice,
                frameBudgetExceeded = false,
            )
        )
    }

    DisposableEffect(context, powerManager) {
        val powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    val isPowerSave = powerManager?.isPowerSaveMode == true
                    if (signals.powerSaveMode != isPowerSave) {
                        signals = signals.copy(powerSaveMode = isPowerSave)
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            powerSaveReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val currentPowerSave = powerManager?.isPowerSaveMode == true
        if (signals.powerSaveMode != currentPowerSave) {
            signals = signals.copy(powerSaveMode = currentPowerSave)
        }

        val animatorScaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val disabled = readAnimationsDisabled(context)
                if (signals.animationsDisabled != disabled) {
                    signals = signals.copy(animationsDisabled = disabled)
                }
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                animatorScaleObserver,
            )
        }
        val currentAnimationsDisabled = readAnimationsDisabled(context)
        if (signals.animationsDisabled != currentAnimationsDisabled) {
            signals = signals.copy(animationsDisabled = currentAnimationsDisabled)
        }

        val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
            if (signals.thermalStatus != status) {
                signals = signals.copy(thermalStatus = status)
            }
        }
        powerManager?.addThermalStatusListener(context.mainExecutor, thermalListener)

        onDispose {
            runCatching { context.unregisterReceiver(powerSaveReceiver) }
            runCatching { context.contentResolver.unregisterContentObserver(animatorScaleObserver) }
            powerManager?.removeThermalStatusListener(thermalListener)
        }
    }

    val qualityState = remember {
        derivedStateOf { resolveEffectsQuality(signals) }
    }

    return remember {
        EffectsQualityController(
            qualityState = qualityState,
            onFrameBudgetExceeded = {
                if (!signals.frameBudgetExceeded) {
                    signals = signals.copy(frameBudgetExceeded = true)
                }
            },
        )
    }
}

/**
 * Convenience wrapper returning a read-only [State] of the current [EffectsQuality].
 */
@Composable
fun rememberEffectsQuality(): State<EffectsQuality> {
    val controller = rememberEffectsQualityController()
    return remember(controller) { derivedStateOf { controller.quality } }
}

private fun readAnimationsDisabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
}
