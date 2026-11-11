package com.bl4ckswordsman.nightjar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerPreferencesTest {

    @Test
    fun testTimerPreferencesDefaultValues() {
        val preferences = TimerPreferences()

        // Asserting default values as per definition in TimerPreferences.kt
        assertEquals(300L, preferences.lastDurationSeconds)
        assertEquals(0L, preferences.startedAtMillis)
        assertFalse(preferences.commitmentMode)
        assertEquals(listOf(300L, 900L, 1800L, 3600L), preferences.customPresets)
        assertTrue(preferences.sunsetModeEnabled)
        assertEquals(30L, preferences.sunsetDurationSeconds)
        assertTrue(preferences.sunsetAudioEnabled)
        assertEquals(SunsetAnimation.FLUID_LIQUID, preferences.sunsetAnimation)
    }

    @Test
    fun testSunsetAnimationFromId() {
        assertEquals(SunsetAnimation.OFF, SunsetAnimation.fromId("off"))
        assertEquals(SunsetAnimation.FLUID_LIQUID, SunsetAnimation.fromId("fluid_liquid"))
        assertEquals(SunsetAnimation.CLASSIC_WAVE, SunsetAnimation.fromId("classic_wave"))
        assertEquals(SunsetAnimation.MATERIAL_SHAPES, SunsetAnimation.fromId("material_shapes"))
        assertEquals(SunsetAnimation.SNOWFLAKES, SunsetAnimation.fromId("snowflakes"))
        assertEquals(SunsetAnimation.FLUID_LIQUID, SunsetAnimation.fromId("unknown_value"))
        assertEquals(SunsetAnimation.FLUID_LIQUID, SunsetAnimation.fromId(null))
    }
}
