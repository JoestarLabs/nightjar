package com.bl4ckswordsman.nightjar.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}
