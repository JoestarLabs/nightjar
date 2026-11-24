package com.bl4ckswordsman.nightjar.ui.components.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FluidParticleSimulationTest {

    @Test
    fun `normal downward gravity pools particles at the bottom within screen bounds`() {
        val sim = FluidParticleSimulation(maxParticles = 60)
        sim.resize(width = 1080f, height = 2400f)
        sim.setActiveCount(targetCount = 40, gravityX = 0f, gravityY = 9.8f)

        repeat(90) {
            sim.step(dtSec = 1f / 60f, accelX = 0f, accelY = 9.8f)
        }

        var sumY = 0f
        for (i in 0 until sim.activeCount) {
            assertTrue(sim.x[i] in 0f..1080f)
            assertTrue(sim.y[i] in 0f..2400f)
            sumY += sim.y[i]
        }
        val meanY = sumY / sim.activeCount
        assertTrue("Expected particles pooled near bottom, got meanY=$meanY", meanY > 1400f)
    }

    @Test
    fun `tilting phone right flows particles to the right wall`() {
        val sim = FluidParticleSimulation(maxParticles = 60)
        sim.resize(width = 1080f, height = 2400f)
        sim.setActiveCount(targetCount = 40, gravityX = 0f, gravityY = 9.8f)

        // ax = -9.8 -> fx = -ax = +9.8 (rightward gravity)
        repeat(90) {
            sim.step(dtSec = 1f / 60f, accelX = -9.8f, accelY = 0f)
        }

        var sumX = 0f
        for (i in 0 until sim.activeCount) {
            sumX += sim.x[i]
        }
        val meanX = sumX / sim.activeCount
        assertTrue("Expected particles flowed to right wall, got meanX=$meanX", meanX > 650f)
    }

    @Test
    fun `flipping upside down pours particles to the top`() {
        val sim = FluidParticleSimulation(maxParticles = 60)
        sim.resize(width = 1080f, height = 2400f)
        sim.setActiveCount(targetCount = 40, gravityX = 0f, gravityY = 9.8f)

        repeat(90) {
            sim.step(dtSec = 1f / 60f, accelX = 0f, accelY = -9.8f)
        }

        var sumY = 0f
        for (i in 0 until sim.activeCount) {
            sumY += sim.y[i]
        }
        val meanY = sumY / sim.activeCount
        assertTrue("Expected particles poured to top, got meanY=$meanY", meanY < 960f)
    }

    @Test
    fun `violent shake impulse and huge dt stay finite and strictly within bounds`() {
        val sim = FluidParticleSimulation(maxParticles = 80)
        sim.resize(width = 1080f, height = 2400f)
        sim.setActiveCount(targetCount = 80, gravityX = 0f, gravityY = 9.8f)

        repeat(20) { idx ->
            val shakeX = if (idx % 2 == 0) 120f else -120f
            sim.step(dtSec = 2f, accelX = shakeX, accelY = -80f)
        }

        for (i in 0 until sim.activeCount) {
            assertTrue("x[$i] must be finite", sim.x[i].isFinite())
            assertTrue("y[$i] must be finite", sim.y[i].isFinite())
            assertTrue("x[$i] out of bounds: ${sim.x[i]}", sim.x[i] in 0f..1080f)
            assertTrue("y[$i] out of bounds: ${sim.y[i]}", sim.y[i] in 0f..2400f)
        }
    }

    @Test
    fun `overlapping particles separate due to incompressibility`() {
        val sim = FluidParticleSimulation(maxParticles = 10)
        sim.resize(width = 1000f, height = 1000f)
        sim.setActiveCount(targetCount = 6, gravityX = 0f, gravityY = 9.8f)
        for (i in 0 until sim.activeCount) {
            sim.x[i] = 500f
            sim.y[i] = 500f
        }

        repeat(15) {
            sim.step(dtSec = 1f / 60f, accelX = 0f, accelY = 0f)
        }

        var spread = 0f
        for (i in 1 until sim.activeCount) {
            spread += kotlin.math.hypot(sim.x[i] - sim.x[0], sim.y[i] - sim.y[0])
        }
        assertTrue("Overlapping particles should push apart, spread=$spread", spread > 10f)
    }

    @Test
    fun `reset clears activeCount and setActiveCount clamps to maxParticles`() {
        val sim = FluidParticleSimulation(maxParticles = 20)
        sim.resize(width = 1080f, height = 2400f)
        sim.setActiveCount(targetCount = 50, gravityX = 0f, gravityY = 9.8f)
        assertEquals(20, sim.activeCount)

        sim.reset()
        assertEquals(0, sim.activeCount)
    }

    @Test
    fun `full particle count fills nearly to the top of the screen under gravity`() {
        for (count in listOf(220, 130)) {
            val sim = FluidParticleSimulation(maxParticles = count)
            val width = 1080f
            val height = 2400f
            sim.resize(width = width, height = height)
            sim.setActiveCount(targetCount = count, gravityX = 0f, gravityY = 9.8f)

            repeat(180) {
                sim.step(dtSec = 1f / 60f, accelX = 0f, accelY = 9.8f)
            }

            var minY = height
            for (i in 0 until sim.activeCount) {
                if (sim.y[i] < minY) minY = sim.y[i]
            }
            val topWithMetaballReach = (minY - sim.interactionRadiusPx).coerceAtLeast(0f)
            assertTrue(
                "Expected full count ($count) to fill near top, got minY=$minY, reach=$topWithMetaballReach",
                topWithMetaballReach < height * 0.15f,
            )
        }
    }
}
