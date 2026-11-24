package com.bl4ckswordsman.nightjar.ui.components.liquid

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Zero-allocation 2D particle fluid solver using Clavet et al. (2005) Double-Density Relaxation
 * with a pre-allocated spatial hash grid.
 *
 * All arrays are allocated once at construction so per-frame [step] calls allocate zero objects.
 *
 * @param maxParticles Maximum number of fluid particles (e.g. 130 at FULL, 80 at REDUCED).
 */
internal class FluidParticleSimulation(val maxParticles: Int = 130) {
    init {
        require(maxParticles >= 1) { "maxParticles must be >= 1, got $maxParticles" }
    }

    val x = FloatArray(maxParticles)
    val y = FloatArray(maxParticles)
    private val prevX = FloatArray(maxParticles)
    private val prevY = FloatArray(maxParticles)
    val vx = FloatArray(maxParticles)
    val vy = FloatArray(maxParticles)

    private val cellHead = IntArray(MAX_GRID_CELLS)
    private val nextParticle = IntArray(maxParticles)
    private val rng = Random(42)

    var widthPx: Float = 0f
        private set
    var heightPx: Float = 0f
        private set
    var interactionRadiusPx: Float = 48f
        private set
    var activeCount: Int = 0
        private set

    private var gridCols = 1
    private var gridRows = 1
    private var invCellSize = 1f / 48f

    /**
     * Updates container dimensions and recomputes interaction radius so [maxParticles]
     * fills ~105% of the screen area consistently across screen sizes and densities.
     */
    fun resize(width: Float, height: Float) {
        if (width <= 1f || height <= 1f) return
        if (width == widthPx && height == heightPx) return

        val prevW = widthPx
        val prevH = heightPx
        widthPx = width
        heightPx = height

        val areaPerParticle = (width * height * 1.05f) / maxParticles.toFloat()
        val spacing = sqrt(areaPerParticle)
        interactionRadiusPx = (spacing * 1.65f).coerceAtLeast(16f)
        invCellSize = 1f / interactionRadiusPx
        gridCols = ceil(width * invCellSize).toInt().coerceIn(1, MAX_GRID_COLS)
        gridRows = ceil(height * invCellSize).toInt().coerceIn(1, MAX_GRID_ROWS)

        if (prevW > 1f && prevH > 1f && activeCount > 0) {
            val sx = width / prevW
            val sy = height / prevH
            for (i in 0 until activeCount) {
                x[i] = (x[i] * sx).coerceIn(0f, width)
                y[i] = (y[i] * sy).coerceIn(0f, height)
                prevX[i] = x[i]
                prevY[i] = y[i]
            }
        }
    }

    /**
     * Adjusts [activeCount] smoothly as countdown progress rises, spawning new particles
     * along the current gravity-bottom edge so water pours into the screen organically.
     */
    fun setActiveCount(targetCount: Int, gravityX: Float, gravityY: Float) {
        val clamped = targetCount.coerceIn(0, maxParticles)
        if (clamped < activeCount) {
            activeCount = clamped
            return
        }
        if (widthPx <= 1f || heightPx <= 1f) {
            activeCount = clamped
            return
        }

        val gMag = hypot(gravityX, gravityY).coerceAtLeast(0.001f)
        val gx = gravityX / gMag
        val gy = gravityY / gMag
        val margin = interactionRadiusPx * 0.25f

        for (i in activeCount until clamped) {
            // Spawn along the bottom wall relative to gravity, with natural spread
            val u = rng.nextFloat() * 0.86f + 0.07f
            val spawnX: Float
            val spawnY: Float
            if (kotlin.math.abs(gy) >= kotlin.math.abs(gx)) {
                spawnX = u * widthPx
                spawnY = if (gy >= 0f) heightPx - margin - rng.nextFloat() * interactionRadiusPx * 0.4f
                else margin + rng.nextFloat() * interactionRadiusPx * 0.4f
            } else {
                spawnY = u * heightPx
                spawnX = if (gx >= 0f) widthPx - margin - rng.nextFloat() * interactionRadiusPx * 0.4f
                else margin + rng.nextFloat() * interactionRadiusPx * 0.4f
            }
            x[i] = spawnX
            y[i] = spawnY
            prevX[i] = spawnX
            prevY[i] = spawnY
            vx[i] = (rng.nextFloat() - 0.5f) * interactionRadiusPx * 0.4f
            vy[i] = (rng.nextFloat() - 0.5f) * interactionRadiusPx * 0.4f
        }
        activeCount = clamped
    }

    /**
     * Advances the 2D fluid simulation by [dtSec] seconds under raw accelerometer `(accelX, accelY)`
     * in m/s² (`fx = -accelX`, `fy = +accelY` in screen coordinates).
     */
    fun step(dtSec: Float, accelX: Float, accelY: Float) {
        if (dtSec <= 0f || activeCount == 0 || widthPx <= 1f || heightPx <= 1f) return
        val clampedDt = dtSec.coerceAtMost(MAX_FRAME_DT_SEC)
        val subDt = clampedDt / SUB_STEPS
        val fx = -accelX.coerceIn(-MAX_ACCEL, MAX_ACCEL) * interactionRadiusPx * ACCEL_SCALE_PER_H
        val fy = accelY.coerceIn(-MAX_ACCEL, MAX_ACCEL) * interactionRadiusPx * ACCEL_SCALE_PER_H

        repeat(SUB_STEPS) {
            integrateAndRelax(subDt, fx, fy)
        }
    }

    private fun integrateAndRelax(dt: Float, fx: Float, fy: Float) {
        val count = activeCount
        val h = interactionRadiusPx
        val invH = 1f / h
        val hSq = h * h
        val dtSq = dt * dt
        val margin = h * 0.24f
        val maxX = (widthPx - margin).coerceAtLeast(margin)
        val maxY = (heightPx - margin).coerceAtLeast(margin)

        // 1. Apply external force + mild damping, predict positions
        for (i in 0 until count) {
            vx[i] = vx[i] * VELOCITY_DAMPING + fx * dt
            vy[i] = vy[i] * VELOCITY_DAMPING + fy * dt
            prevX[i] = x[i]
            prevY[i] = y[i]
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
        }

        // 2. Build spatial hash grid
        val totalCells = gridCols * gridRows
        for (c in 0 until totalCells) cellHead[c] = -1
        for (i in 0 until count) {
            val cx = (x[i] * invCellSize).toInt().coerceIn(0, gridCols - 1)
            val cy = (y[i] * invCellSize).toInt().coerceIn(0, gridRows - 1)
            val cell = cy * gridCols + cx
            nextParticle[i] = cellHead[cell]
            cellHead[cell] = i
        }

        // 3. Double-Density Relaxation (Clavet et al.)
        val stiffness = STIFFNESS * h
        val nearStiffness = NEAR_STIFFNESS * h
        val minNegPressure = -stiffness * 0.35f

        for (i in 0 until count) {
            val xi = x[i]
            val yi = y[i]
            val cx = (xi * invCellSize).toInt().coerceIn(0, gridCols - 1)
            val cy = (yi * invCellSize).toInt().coerceIn(0, gridRows - 1)

            var rho = 0f
            var rhoNear = 0f

            val xMin = (cx - 1).coerceAtLeast(0)
            val xMax = (cx + 1).coerceAtMost(gridCols - 1)
            val yMin = (cy - 1).coerceAtLeast(0)
            val yMax = (cy + 1).coerceAtMost(gridRows - 1)

            for (ny in yMin..yMax) {
                val rowOffset = ny * gridCols
                for (nx in xMin..xMax) {
                    var j = cellHead[rowOffset + nx]
                    while (j != -1) {
                        if (j != i) {
                            val dx = x[j] - xi
                            val dy = y[j] - yi
                            val distSq = dx * dx + dy * dy
                            if (distSq < hSq && distSq > 1e-6f) {
                                val dist = sqrt(distSq)
                                val q = 1f - dist * invH
                                val q2 = q * q
                                rho += q2
                                rhoNear += q2 * q
                            }
                        }
                        j = nextParticle[j]
                    }
                }
            }

            val pressure = (stiffness * (rho - REST_DENSITY)).coerceAtLeast(minNegPressure)
            val nearPressure = nearStiffness * rhoNear

            var dispXi = 0f
            var dispYi = 0f

            for (ny in yMin..yMax) {
                val rowOffset = ny * gridCols
                for (nx in xMin..xMax) {
                    var j = cellHead[rowOffset + nx]
                    while (j != -1) {
                        if (j != i) {
                            var dx = x[j] - xi
                            var dy = y[j] - yi
                            val distSq = dx * dx + dy * dy
                            if (distSq < hSq) {
                                val dist: Float
                                if (distSq > 1e-6f) {
                                    dist = sqrt(distSq)
                                    dx /= dist
                                    dy /= dist
                                } else {
                                    // Break exact overlap cleanly
                                    dist = 0.01f
                                    dx = if ((i and 1) == 0) 1f else -1f
                                    dy = 0f
                                }
                                val q = 1f - dist * invH
                                val d = dtSq * (pressure * q + nearPressure * q * q) * 0.5f
                                val dX = dx * d
                                val dY = dy * d
                                x[j] += dX
                                y[j] += dY
                                dispXi -= dX
                                dispYi -= dY
                            }
                        }
                        j = nextParticle[j]
                    }
                }
            }

            x[i] += dispXi
            y[i] += dispYi
        }

        // 4. Wall constraints & velocity derivation
        val invDt = 1f / dt
        val maxSpeed = h * MAX_SPEED_PER_H
        for (i in 0 until count) {
            var px = x[i]
            var py = y[i]
            if (px < margin) px = margin else if (px > maxX) px = maxX
            if (py < margin) py = margin else if (py > maxY) py = maxY
            x[i] = px
            y[i] = py

            var nvx = (px - prevX[i]) * invDt
            var nvy = (py - prevY[i]) * invDt

            // Mild wall bounce when hitting edges fast
            if (px <= margin || px >= maxX) nvx *= -WALL_RESTITUTION
            if (py <= margin || py >= maxY) nvy *= -WALL_RESTITUTION

            val speedSq = nvx * nvx + nvy * nvy
            if (speedSq > maxSpeed * maxSpeed) {
                val scale = maxSpeed / sqrt(speedSq)
                nvx *= scale
                nvy *= scale
            }
            vx[i] = nvx
            vy[i] = nvy
        }
    }

    /** Clears all active particles (e.g. when looping the preview or entering MINIMAL quality). */
    fun reset() {
        activeCount = 0
    }

    companion object {
        private const val MAX_GRID_COLS = 20
        private const val MAX_GRID_ROWS = 40
        private const val MAX_GRID_CELLS = MAX_GRID_COLS * MAX_GRID_ROWS

        private const val MAX_FRAME_DT_SEC = 1f / 20f
        private const val SUB_STEPS = 2
        private const val MAX_ACCEL = 35f

        // Lively & splashy water tuning
        private const val ACCEL_SCALE_PER_H = 3.8f
        private const val VELOCITY_DAMPING = 0.996f
        private const val REST_DENSITY = 1.65f
        private const val STIFFNESS = 10.5f
        private const val NEAR_STIFFNESS = 18f
        private const val WALL_RESTITUTION = 0.25f
        private const val MAX_SPEED_PER_H = 18f
    }
}
