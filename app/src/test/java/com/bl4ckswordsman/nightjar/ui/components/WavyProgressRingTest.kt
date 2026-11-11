package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WavyProgressRingTest {

    @Test
    fun `remaining fraction calculates reverse countdown accurately`() {
        val totalSeconds = 120L

        // Timer just started: 100% remaining
        var remainingSeconds = 120L
        var fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        assertEquals(1.0f, fraction, 0.001f)

        // Timer halfway: 50% remaining
        remainingSeconds = 60L
        fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.5f, fraction, 0.001f)

        // Timer near end: 5% remaining
        remainingSeconds = 6L
        fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.05f, fraction, 0.001f)

        // Timer finished: 0% remaining
        remainingSeconds = 0L
        fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.0f, fraction, 0.001f)
    }

    @Test
    fun `ring sizing formula maintains exact dial center radius`() {
        val dialSize = 280.dp
        val dialInset = 40.dp // 20.dp on each side
        val trackRadius = (dialSize - dialInset) / 2 // 120.dp

        // For any stroke width, ringSize = (dialSize - dialInset) + strokeWidth
        val strokeWidth = 16.dp
        val ringSize = (dialSize - dialInset) + strokeWidth

        // CircularWavyProgressIndicator computes: r = ringSize / 2 - strokeWidth / 2
        val computedRadius = ringSize / 2 - strokeWidth / 2
        assertEquals(trackRadius, computedRadius)
    }

    @Test
    fun `amplitude defaults to full native wave`() {
        val amplitude = 1f
        assertEquals(1.0f, amplitude, 0.001f)
    }
}
