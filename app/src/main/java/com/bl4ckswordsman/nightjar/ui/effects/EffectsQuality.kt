package com.bl4ckswordsman.nightjar.ui.effects

import android.os.PowerManager

/**
 * Adaptive visual effects quality tier used to scale or disable expensive visual layers
 * based on power, thermal, accessibility, hardware, and runtime frame-budget signals.
 */
enum class EffectsQuality {
    FULL,
    REDUCED,
    MINIMAL,
}

/**
 * Snapshot of system and runtime signals used by [resolveEffectsQuality].
 */
data class EffectsSignals(
    val animationsDisabled: Boolean = false,
    val powerSaveMode: Boolean = false,
    val thermalStatus: Int = PowerManager.THERMAL_STATUS_NONE,
    val isLowRamDevice: Boolean = false,
    val frameBudgetExceeded: Boolean = false,
)

/**
 * Pure policy resolver mapping [EffectsSignals] to an [EffectsQuality] tier.
 * Accessibility ("Remove animations") and severe+ thermal throttling always take precedence.
 */
fun resolveEffectsQuality(
    signals: EffectsSignals,
    override: EffectsQuality? = null,
): EffectsQuality {
    if (signals.animationsDisabled || signals.thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
        return EffectsQuality.MINIMAL
    }
    if (override != null) {
        return override
    }
    return when {
        signals.powerSaveMode ||
            signals.isLowRamDevice ||
            signals.frameBudgetExceeded ||
            signals.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> EffectsQuality.REDUCED
        else -> EffectsQuality.FULL
    }
}
