package com.bl4ckswordsman.nightjar.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunsetOverlayCommonTest {

    @Test
    fun `computeRelativeElapsedSeconds preserves sub-second precision for modern epoch timestamps`() {
        val anchor = 1_795_000_000_000L
        val t1 = computeRelativeElapsedSeconds(nowMs = anchor + 16L, anchorMs = anchor)
        val t2 = computeRelativeElapsedSeconds(nowMs = anchor + 33L, anchorMs = anchor)
        assertTrue("16ms and 33ms deltas must remain distinct in Float32", t2 > t1)
        assertEquals(0.016f, t1, 0.0001f)
        assertEquals(0.033f, t2, 0.0001f)
    }

    @Test
    fun `computeFallbackOverlayProgress handles normal and zero totals`() {
        assertEquals(0.5f, computeFallbackOverlayProgress(15L, 30L), 0.001f)
        assertEquals(1.0f, computeFallbackOverlayProgress(0L, 0L), 0.001f)
    }
}
