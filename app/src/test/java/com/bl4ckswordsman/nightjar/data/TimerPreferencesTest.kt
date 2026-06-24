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
    }
}
