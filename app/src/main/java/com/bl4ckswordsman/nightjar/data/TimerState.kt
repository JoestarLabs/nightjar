package com.bl4ckswordsman.nightjar.data

/**
 * Represents the state of the countdown timer.
 *
 * The state machine is:
 *   Idle ──start──► Running ──cancel──► Idle
 *   Running ──expires──► Finished ──(auto)──► Idle
 */
sealed class TimerState {

    /** No active timer. */
    data object Idle : TimerState()

    /**
     * Timer is counting down.
     *
     * @param totalSeconds     The full duration the user selected.
     * @param remainingSeconds Seconds still remaining.
     * @param startedAtMillis  Epoch-ms when the timer was started (for persistence across process death).
     */
    data class Running(
        val totalSeconds: Long,
        val remainingSeconds: Long,
        val startedAtMillis: Long,
    ) : TimerState() {
        val progressFraction: Float
            get() = if (totalSeconds == 0L) 0f
            else 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
    }

    /** Timer reached zero; the lock action is being triggered. */
    data object Finished : TimerState()
}
