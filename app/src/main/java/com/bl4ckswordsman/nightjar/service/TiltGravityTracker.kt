package com.bl4ckswordsman.nightjar.service

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Low-pass accelerometer filter producing normalized lateral tilt and 360° gravity angle.
 */
class TiltGravityTracker {
    private var filteredGx = 0f
    private var filteredGy = 9.8f

    var tilt: Float = 0f
        private set

    var gravityAngleRad: Float = 0f
        private set

    /**
     * Incorporates an accelerometer `(x, y)` sample in m/s² and updates [tilt] and [gravityAngleRad].
     */
    fun update(x: Float, y: Float) {
        filteredGx = filteredGx * FILTER_PREVIOUS_WEIGHT + x * FILTER_SAMPLE_WEIGHT
        filteredGy = filteredGy * FILTER_PREVIOUS_WEIGHT + y * FILTER_SAMPLE_WEIGHT

        tilt = (filteredGx / TILT_NORMALIZATION_DIVISOR).coerceIn(-1f, 1f)

        val xyMagnitude = hypot(filteredGx, filteredGy)
        if (xyMagnitude > MIN_GRAVITY_MAGNITUDE) {
            gravityAngleRad = atan2(-filteredGx, filteredGy)
        }
    }

    private companion object {
        const val FILTER_PREVIOUS_WEIGHT = 0.85f
        const val FILTER_SAMPLE_WEIGHT = 0.15f
        const val TILT_NORMALIZATION_DIVISOR = 5.5f
        const val MIN_GRAVITY_MAGNITUDE = 1.2f
    }
}
