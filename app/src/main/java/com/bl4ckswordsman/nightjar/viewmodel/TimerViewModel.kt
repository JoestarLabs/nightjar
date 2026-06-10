package com.bl4ckswordsman.nightjar.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.service.LockTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the timer UI.
 *
 * Exposes [timerState] from [TimerRepository] and provides start/stop commands
 * that delegate to [LockTimerService] via explicit intents.
 *
 * The ViewModel does NOT hold timer logic itself — it is owned by the Service.
 * This ensures the timer survives configuration changes and activity destruction.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: TimerRepository,
) : ViewModel() {

    /** Live timer state — collect in composables with [androidx.lifecycle.compose.collectAsStateWithLifecycle]. */
    val timerState: StateFlow<TimerState> = repository.timerState

    // ── Selected duration (dial / chips) ──────────────────────────────────────

    private var _selectedSeconds: Long = 300L  // default 5 min
    val selectedSeconds: Long get() = _selectedSeconds

    fun setSelectedSeconds(seconds: Long) {
        _selectedSeconds = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    fun startTimer() {
        if (_selectedSeconds <= 0) return
        ContextCompat.startForegroundService(
            context,
            LockTimerService.startIntent(context, _selectedSeconds)
        )
        viewModelScope.launch {
            repository.preferencesDataSource.saveLastDuration(_selectedSeconds)
        }
    }

    fun stopTimer() {
        context.startService(LockTimerService.stopIntent(context))
    }

    fun toggleTimer() {
        when (repository.currentState) {
            is TimerState.Running -> stopTimer()
            else                  -> startTimer()
        }
    }

    // ── Init: restore last-used duration ─────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.preferencesDataSource.preferences.collect { prefs ->
                // Only update selected seconds if timer is not running
                if (repository.currentState is TimerState.Idle) {
                    _selectedSeconds = prefs.lastDurationSeconds
                }
            }
        }
    }

    companion object {
        const val MIN_SECONDS = 5L          // 5 seconds minimum
        const val MAX_SECONDS = 7_200L      // 2 hours maximum
    }
}
