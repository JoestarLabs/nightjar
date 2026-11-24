package com.bl4ckswordsman.nightjar.ui.components.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiquidSurfaceSimulationTest {

    @Test
    fun `simulation settles to rest without forcing`() {
        val sim = LiquidSurfaceSimulation(columns = 48)
        sim.impulse(relX = 0f, strengthDp = 120f)
        assertFalse(sim.isSettled())

        repeat(600) {
            sim.step(dtSec = 1f / 60f, lateralAccel = 0f, angularVelocity = 0f)
        }
        assertTrue("Expected simulation to settle after damping", sim.isSettled())
    }

    @Test
    fun `positive lateral acceleration raises the left side and lowers the right`() {
        val sim = LiquidSurfaceSimulation(columns = 48)
        repeat(15) {
            sim.step(dtSec = 1f / 60f, lateralAccel = 8f, angularVelocity = 0f)
        }
        assertTrue("Left side should rise", sim.heightAt(-0.9f) > 0.5f)
        assertTrue("Right side should drop", sim.heightAt(0.9f) < -0.5f)
    }

    @Test
    fun `mean surface height remains near zero to conserve volume`() {
        val sim = LiquidSurfaceSimulation(columns = 48)
        sim.impulse(relX = -0.4f, strengthDp = 160f)
        repeat(45) {
            sim.step(dtSec = 1f / 60f, lateralAccel = 6f, angularVelocity = 1.5f)
        }

        var sum = 0f
        val samples = sim.columns
        for (i in 0 until samples) {
            val relX = i / (samples - 1f) * 2f - 1f
            sum += sim.heightAt(relX)
        }
        assertEquals(0f, sum / samples, 0.25f)
    }

    @Test
    fun `huge frame delta remains finite and bounded`() {
        val sim = LiquidSurfaceSimulation(columns = 48)
        sim.impulse(relX = 0.2f, strengthDp = 500f)
        repeat(10) {
            sim.step(dtSec = 2f, lateralAccel = 50f, angularVelocity = 20f)
        }

        for (i in 0 until sim.columns) {
            val relX = i / (sim.columns - 1f) * 2f - 1f
            val h = sim.heightAt(relX)
            assertTrue("Height must be finite", h.isFinite())
            assertTrue(
                "Height $h exceeded MAX_HEIGHT_DP",
                abs(h) <= LiquidSurfaceSimulation.MAX_HEIGHT_DP + 0.001f,
            )
        }
    }

    @Test
    fun `edge impulse stays bounded and reflects cleanly`() {
        val sim = LiquidSurfaceSimulation(columns = 48)
        sim.impulse(relX = -1f, strengthDp = 150f)
        repeat(20) {
            sim.step(dtSec = 1f / 60f, lateralAccel = 0f, angularVelocity = 0f)
        }
        val edgeH = sim.heightAt(-1f)
        assertTrue(edgeH.isFinite())
        assertTrue(abs(edgeH) <= LiquidSurfaceSimulation.MAX_HEIGHT_DP)
    }

    @Test
    fun `heightAt interpolates between column samples`() {
        val sim = LiquidSurfaceSimulation(columns = 5)
        sim.impulse(relX = 0f, strengthDp = 100f)
        sim.step(dtSec = 1f / 60f, lateralAccel = 0f, angularVelocity = 0f)

        val left = sim.heightAt(-0.5f)
        val center = sim.heightAt(0f)
        val mid = sim.heightAt(-0.25f)
        assertEquals((left + center) / 2f, mid, 0.001f)
    }
}
