package com.bl4ckswordsman.nightjar.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class TiltGravityTrackerTest {

    @Test
    fun `initial state is zero tilt and zero gravity angle`() {
        val tracker = TiltGravityTracker()
        assertEquals(0f, tracker.tilt, 0.001f)
        assertEquals(0f, tracker.gravityAngleRad, 0.001f)
    }

    @Test
    fun `repeated lateral acceleration saturates normalized tilt within bounds`() {
        val tracker = TiltGravityTracker()
        repeat(40) { tracker.update(x = 9.8f, y = 0f) }
        assertEquals(1f, tracker.tilt, 0.001f)

        repeat(80) { tracker.update(x = -9.8f, y = 0f) }
        assertEquals(-1f, tracker.tilt, 0.001f)
    }

    @Test
    fun `upside down orientation converges near PI radians`() {
        val tracker = TiltGravityTracker()
        repeat(80) { tracker.update(x = 0.1f, y = -9.8f) }
        assertTrue(kotlin.math.abs(tracker.gravityAngleRad) > (PI.toFloat() * 0.9f))
    }

    @Test
    fun `step acceleration produces high-passed linearAccel that decays to zero`() {
        val tracker = TiltGravityTracker()
        tracker.update(x = 10f, y = 9.8f)
        val initialLinearX = tracker.linearAccelX
        assertTrue("Initial step should yield positive linearAccelX", initialLinearX > 5f)

        repeat(80) { tracker.update(x = 10f, y = 9.8f) }
        assertEquals(0f, tracker.linearAccelX, 0.05f)
        assertEquals(0f, tracker.linearAccelY, 0.05f)
    }

    @Test
    fun `rawAccel tracks input and clamps extreme sensor spikes`() {
        val tracker = TiltGravityTracker()
        tracker.update(x = 12f, y = -8f)
        assertEquals(12f, tracker.rawAccelX, 0.001f)
        assertEquals(-8f, tracker.rawAccelY, 0.001f)

        tracker.update(x = 100f, y = -100f)
        assertEquals(35f, tracker.rawAccelX, 0.001f)
        assertEquals(-35f, tracker.rawAccelY, 0.001f)
    }
}
