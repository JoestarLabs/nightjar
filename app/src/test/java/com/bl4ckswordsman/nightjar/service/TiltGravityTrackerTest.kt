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
}
