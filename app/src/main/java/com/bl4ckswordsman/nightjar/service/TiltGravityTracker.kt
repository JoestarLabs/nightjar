package com.bl4ckswordsman.nightjar.service

import android.view.Surface
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Low-pass accelerometer filter producing normalized lateral tilt and 360° gravity angle
 * in the active display's coordinate frame.
 */
class TiltGravityTracker {
    private var filteredGx = 0f
    private var filteredGy = 9.8f

    var tilt: Float = 0f
        private set

    var gravityAngleRad: Float = 0f
        private set

    var linearAccelX: Float = 0f
        private set

    var linearAccelY: Float = 0f
        private set

    var rawAccelX: Float = 0f
        private set

    var rawAccelY: Float = 9.8f
        private set

    /**
     * Resets filter and output states to an upright display baseline `(0f, 9.8f)`.
     */
    fun reset() {
        filteredGx = 0f
        filteredGy = 9.8f
        tilt = 0f
        gravityAngleRad = 0f
        linearAccelX = 0f
        linearAccelY = 0f
        rawAccelX = 0f
        rawAccelY = 9.8f
    }

    /**
     * Incorporates an accelerometer `(x, y)` sample in m/s², remaps it into the active
     * [displayRotation] coordinate frame, and updates [tilt], [gravityAngleRad],
     * high-passed [linearAccelX]/[linearAccelY], and clamped [rawAccelX]/[rawAccelY].
     */
    fun update(x: Float, y: Float, displayRotation: Int = Surface.ROTATION_0) {
        val (displayX, displayY) = when (displayRotation) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }

        rawAccelX = displayX.coerceIn(-MAX_RAW_ACCEL, MAX_RAW_ACCEL)
        rawAccelY = displayY.coerceIn(-MAX_RAW_ACCEL, MAX_RAW_ACCEL)

        filteredGx = filteredGx * FILTER_PREVIOUS_WEIGHT + displayX * FILTER_SAMPLE_WEIGHT
        filteredGy = filteredGy * FILTER_PREVIOUS_WEIGHT + displayY * FILTER_SAMPLE_WEIGHT

        linearAccelX = displayX - filteredGx
        linearAccelY = displayY - filteredGy

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
        const val MAX_RAW_ACCEL = 35f
    }
}
