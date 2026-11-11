package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WavyProgressRingTest {

    @Test
    fun `remaining fraction calculates reverse countdown accurately`() {
        val totalSeconds = 120L

        // Timer just started: 100% remaining
        assertEquals(1.0f, calculateRemainingFraction(120L, totalSeconds), 0.001f)

        // Timer halfway: 50% remaining
        assertEquals(0.5f, calculateRemainingFraction(60L, totalSeconds), 0.001f)

        // Timer near end: 5% remaining
        assertEquals(0.05f, calculateRemainingFraction(6L, totalSeconds), 0.001f)

        // Timer finished: 0% remaining
        assertEquals(0.0f, calculateRemainingFraction(0L, totalSeconds), 0.001f)

        // Edge case: zero or negative total returns 0f safely
        assertEquals(0.0f, calculateRemainingFraction(10L, 0L), 0.001f)
        assertEquals(0.0f, calculateRemainingFraction(0L, 0L), 0.001f)
        assertEquals(0.0f, calculateRemainingFraction(10L, -5L), 0.001f)

        // Edge case: remaining time greater than total is clamped to 1.0f
        assertEquals(1.0f, calculateRemainingFraction(150L, totalSeconds), 0.001f)
    }

    @Test
    fun `ring sizing formula maintains exact dial center radius`() {
        val dialSize = 280.dp
        val dialInset = 40.dp // 20.dp on each side
        val trackRadius = (dialSize - dialInset) / 2 // 120.dp

        val strokeWidth = 16.dp
        val ringSize = calculateDialRingSize(dialSize, strokeWidth, dialInset)

        // CircularWavyProgressIndicator computes: r = ringSize / 2 - strokeWidth / 2
        val computedRadius = ringSize / 2 - strokeWidth / 2
        assertEquals(trackRadius, computedRadius)
    }

    @Test
    fun `amplitude defaults to full native wave`() {
        assertEquals(1.0f, DefaultAmplitude, 0.001f)
    }
}
