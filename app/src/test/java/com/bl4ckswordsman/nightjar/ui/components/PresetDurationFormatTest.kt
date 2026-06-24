package com.bl4ckswordsman.nightjar.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetDurationFormatTest {

    @Test
    fun testMinutesUnderOneHour() {
        // Less than 1 minute formats to 0 minutes
        assertEquals(PresetDurationFormat.Minutes(0L), getPresetDurationFormat(0L))
        assertEquals(PresetDurationFormat.Minutes(0L), getPresetDurationFormat(30L))
        assertEquals(PresetDurationFormat.Minutes(0L), getPresetDurationFormat(59L))

        // Standard minutes
        assertEquals(PresetDurationFormat.Minutes(1L), getPresetDurationFormat(60L))
        assertEquals(PresetDurationFormat.Minutes(5L), getPresetDurationFormat(300L))
        assertEquals(PresetDurationFormat.Minutes(59L), getPresetDurationFormat(3540L))
    }

    @Test
    fun testExactHours() {
        // 1 hour exactly
        assertEquals(PresetDurationFormat.HourOne, getPresetDurationFormat(3600L))

        // Multiple hours exactly
        assertEquals(PresetDurationFormat.HourMany(2L), getPresetDurationFormat(7200L))
        assertEquals(PresetDurationFormat.HourMany(5L), getPresetDurationFormat(18000L))
    }

    @Test
    fun testHoursAndMinutes() {
        // 1 hour and some minutes
        assertEquals(PresetDurationFormat.HourMin(1L, 30L), getPresetDurationFormat(5400L)) // 90 min
        assertEquals(PresetDurationFormat.HourMin(1L, 1L), getPresetDurationFormat(3660L)) // 61 min

        // Multiple hours and some minutes
        assertEquals(PresetDurationFormat.HourMin(2L, 5L), getPresetDurationFormat(7500L)) // 125 min
        assertEquals(PresetDurationFormat.HourMin(3L, 45L), getPresetDurationFormat(13500L)) // 225 min
    }
}
