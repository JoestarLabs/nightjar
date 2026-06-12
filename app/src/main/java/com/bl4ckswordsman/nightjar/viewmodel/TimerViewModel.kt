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
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _selectedSeconds = mutableStateOf(300L)
    val selectedSeconds: Long get() = _selectedSeconds.value

    private val _commitmentMode = MutableStateFlow(false)
    /** Whether commitment mode (no-cancel) is currently enabled. */
    val commitmentMode: StateFlow<Boolean> = _commitmentMode.asStateFlow()

    private val _presets = MutableStateFlow<List<Long>>(listOf(300L, 900L, 1800L, 3600L))
    /** List of custom presets (in seconds). */
    val presets: StateFlow<List<Long>> = _presets.asStateFlow()

    fun setSelectedSeconds(seconds: Long) {
        _selectedSeconds.value = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
    }

    fun setCommitmentMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesDataSource.saveCommitmentMode(enabled)
        }
    }

    fun saveCustomPresets(presetsMinutes: List<Long>) {
        viewModelScope.launch {
            repository.preferencesDataSource.saveCustomPresets(presetsMinutes)
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    fun startTimer() {
        if (selectedSeconds <= 0) return
        ContextCompat.startForegroundService(
            context,
            LockTimerService.startIntent(context, selectedSeconds, _commitmentMode.value)
        )
        viewModelScope.launch {
            repository.preferencesDataSource.saveLastDuration(selectedSeconds)
        }
    }

    fun stopTimer() {
        // No-op when commitment mode is active and the timer is running
        if (_commitmentMode.value && repository.currentState is TimerState.Running) return
        context.startService(LockTimerService.stopIntent(context))
    }

    fun toggleTimer() {
        when (repository.currentState) {
            is TimerState.Running -> stopTimer()
            else                  -> startTimer()
        }
    }

    // ── Init: restore last-used duration and commitment mode ──────────────────

    init {
        viewModelScope.launch {
            repository.preferencesDataSource.preferences.collect { prefs ->
                // Restore commitment mode from DataStore
                _commitmentMode.value = prefs.commitmentMode
                // Restore custom presets
                _presets.value = prefs.customPresets
                // Only update selected seconds if timer is not running
                if (repository.currentState is TimerState.Idle) {
                    setSelectedSeconds(prefs.lastDurationSeconds)
                }
            }
        }
    }

    companion object {
        const val MIN_SECONDS = 5L          // 5 seconds minimum
        const val MAX_SECONDS = 7_200L      // 2 hours maximum
    }
}
