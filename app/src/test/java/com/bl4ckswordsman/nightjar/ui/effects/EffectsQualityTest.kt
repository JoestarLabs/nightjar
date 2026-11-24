package com.bl4ckswordsman.nightjar.ui.effects

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectsQualityTest {

    @Test
    fun `default clean signals resolve to FULL`() {
        assertEquals(EffectsQuality.FULL, resolveEffectsQuality(EffectsSignals()))
    }

    @Test
    fun `power save, low ram, frame budget overrun, and moderate thermal resolve to REDUCED`() {
        assertEquals(
            EffectsQuality.REDUCED,
            resolveEffectsQuality(EffectsSignals(powerSaveMode = true)),
        )
        assertEquals(
            EffectsQuality.REDUCED,
            resolveEffectsQuality(EffectsSignals(isLowRamDevice = true)),
        )
        assertEquals(
            EffectsQuality.REDUCED,
            resolveEffectsQuality(EffectsSignals(frameBudgetExceeded = true)),
        )
        assertEquals(
            EffectsQuality.REDUCED,
            resolveEffectsQuality(EffectsSignals(thermalStatus = PowerManager.THERMAL_STATUS_MODERATE)),
        )
    }

    @Test
    fun `severe thermal and disabled animations resolve to MINIMAL and take precedence`() {
        assertEquals(
            EffectsQuality.MINIMAL,
            resolveEffectsQuality(EffectsSignals(thermalStatus = PowerManager.THERMAL_STATUS_SEVERE)),
        )
        assertEquals(
            EffectsQuality.MINIMAL,
            resolveEffectsQuality(EffectsSignals(thermalStatus = PowerManager.THERMAL_STATUS_CRITICAL)),
        )
        assertEquals(
            EffectsQuality.MINIMAL,
            resolveEffectsQuality(
                EffectsSignals(animationsDisabled = true),
                override = EffectsQuality.FULL,
            ),
        )
    }
}
