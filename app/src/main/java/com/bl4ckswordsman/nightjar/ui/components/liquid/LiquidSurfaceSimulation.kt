package com.bl4ckswordsman.nightjar.ui.components.liquid

import kotlin.math.abs
import kotlin.math.ceil

/**
 * Lightweight 1D height-field ("spring-column") wave simulation for interactive 2D fluid.
 *
 * All arrays are pre-allocated at construction so [step], [heightAt], and [lateralFlow]
 * perform zero allocations per frame.
 *
 * @param columns Number of discrete surface columns spanning `relX` in `[-1, 1]`.
 */
internal class LiquidSurfaceSimulation(val columns: Int = 48) {
    init {
        require(columns >= 3) { "Simulation requires at least 3 columns, got $columns" }
    }

    private val h = FloatArray(columns)
    private val v = FloatArray(columns)
    private val a = FloatArray(columns)

    /**
     * Advances the wave simulation by [dtSec] seconds under [lateralAccel] (m/s²)
     * along the surface tangent and [angularVelocity] (rad/s) of device rotation.
     */
    fun step(dtSec: Float, lateralAccel: Float, angularVelocity: Float) {
        if (dtSec <= 0f) return
        val clampedDt = dtSec.coerceAtMost(MAX_FRAME_DT_SEC)
        val subSteps = ceil(clampedDt / MAX_SUBSTEP_SEC).toInt().coerceAtLeast(1)
        val subDt = clampedDt / subSteps
        repeat(subSteps) {
            integrate(subDt, lateralAccel, angularVelocity)
        }
    }

    private fun integrate(dt: Float, aLat: Float, omega: Float) {
        val last = columns - 1
        val invLast = 1f / last
        val forcing = aLat * LATERAL_GAIN + omega * ROTATION_GAIN

        for (i in 0 until columns) {
            val left = h[if (i > 0) i - 1 else 0]
            val right = h[if (i < last) i + 1 else last]
            val x = i * invLast * 2f - 1f
            a[i] = K_SPREAD * (left + right - 2f * h[i]) -
                K_RESTORE * h[i] -
                DAMPING * v[i] -
                forcing * x
        }

        var sumH = 0f
        for (i in 0 until columns) {
            v[i] = (v[i] + a[i] * dt).coerceIn(-MAX_VELOCITY_DP_PER_SEC, MAX_VELOCITY_DP_PER_SEC)
            h[i] += v[i] * dt
            sumH += h[i]
        }

        // Conserve total fluid volume by removing mean height drift, then clamp extremes.
        val meanH = sumH / columns
        for (i in 0 until columns) {
            h[i] = (h[i] - meanH).coerceIn(-MAX_HEIGHT_DP, MAX_HEIGHT_DP)
        }
    }

    /**
     * Injects a localized vertical disturbance at normalized coordinate [relX] in `[-1, 1]`.
     */
    fun impulse(relX: Float, strengthDp: Float) {
        val normalized = ((relX.coerceIn(-1f, 1f) + 1f) * 0.5f) * (columns - 1)
        val center = normalized.toInt().coerceIn(0, columns - 1)
        v[center] = (v[center] + strengthDp).coerceIn(-MAX_VELOCITY_DP_PER_SEC, MAX_VELOCITY_DP_PER_SEC)
        if (center > 0) {
            v[center - 1] = (v[center - 1] + strengthDp * 0.5f)
                .coerceIn(-MAX_VELOCITY_DP_PER_SEC, MAX_VELOCITY_DP_PER_SEC)
        }
        if (center < columns - 1) {
            v[center + 1] = (v[center + 1] + strengthDp * 0.5f)
                .coerceIn(-MAX_VELOCITY_DP_PER_SEC, MAX_VELOCITY_DP_PER_SEC)
        }
    }

    /**
     * Returns linearly interpolated surface height (in dp, positive = upward) at [relX] in `[-1, 1]`.
     */
    fun heightAt(relX: Float): Float {
        val clamped = relX.coerceIn(-1f, 1f)
        val pos = ((clamped + 1f) * 0.5f) * (columns - 1)
        val idx0 = pos.toInt().coerceIn(0, columns - 2)
        val frac = pos - idx0
        return h[idx0] + (h[idx0 + 1] - h[idx0]) * frac
    }

    /**
     * Signed mean surface slope/velocity proxy used to drift submerged bubbles with the current.
     */
    fun lateralFlow(): Float {
        val half = columns / 2
        var leftSum = 0f
        var rightSum = 0f
        for (i in 0 until half) leftSum += v[i]
        for (i in half until columns) rightSum += v[i]
        return ((rightSum - leftSum) / columns).coerceIn(-MAX_FLOW_DP_PER_SEC, MAX_FLOW_DP_PER_SEC)
    }

    /** Returns true when all columns have decayed to rest. */
    fun isSettled(): Boolean {
        for (i in 0 until columns) {
            if (abs(h[i]) > SETTLE_EPSILON || abs(v[i]) > SETTLE_EPSILON) {
                return false
            }
        }
        return true
    }

    /** Clears all heights and velocities back to a flat calm surface. */
    fun reset() {
        h.fill(0f)
        v.fill(0f)
        a.fill(0f)
    }

    companion object {
        const val MAX_HEIGHT_DP = 56f
        private const val MAX_VELOCITY_DP_PER_SEC = 420f
        private const val MAX_FLOW_DP_PER_SEC = 80f
        private const val MAX_FRAME_DT_SEC = 1f / 20f
        private const val MAX_SUBSTEP_SEC = 1f / 120f

        private const val K_SPREAD = 180f
        private const val K_RESTORE = 24f
        private const val DAMPING = 2.6f
        private const val LATERAL_GAIN = 14f
        private const val ROTATION_GAIN = 38f
        private const val SETTLE_EPSILON = 0.02f
    }
}
