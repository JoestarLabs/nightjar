package com.bl4ckswordsman.nightjar.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the live [TimerState].
 *
 * Both [com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel] and
 * [com.bl4ckswordsman.nightjar.service.LockTimerService] share the same
 * Hilt-managed singleton instance, so UI state always reflects service state.
 */
@Singleton
class TimerRepository @Inject constructor(
    val preferencesDataSource: TimerPreferencesDataSource
) {
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /** Update the live timer state. Called from the service's countdown loop. */
    fun updateState(state: TimerState) {
        _timerState.value = state
    }

    /** Convenience to read the current state without collecting the flow. */
    val currentState: TimerState get() = _timerState.value

    init {
        // Eagerly rehydrate from DataStore so the UI reflects "Running" on the very
        // first frame after a process restart (e.g. user leaves and returns while the
        // foreground service is still running). Without this, the StatusChip shows
        // "Ready" for up to one full second while waiting for the service's next tick.
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val prefs = preferencesDataSource.preferences.first()
            val startedAt = prefs.startedAtMillis
            val duration = prefs.lastDurationSeconds
            if (startedAt > 0L && duration > 0L) {
                val elapsed = (System.currentTimeMillis() - startedAt) / 1_000L
                val remaining = duration - elapsed
                if (remaining > 0L) {
                    _timerState.value = TimerState.Running(
                        totalSeconds = duration,
                        remainingSeconds = remaining,
                        startedAtMillis = startedAt
                    )
                }
            }
        }
    }
}
