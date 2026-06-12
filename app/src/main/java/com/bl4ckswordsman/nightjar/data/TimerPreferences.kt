package com.bl4ckswordsman.nightjar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore singleton extension ─────────────────────────────────────────────
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "nightjar_timer_prefs")

// ── Keys ──────────────────────────────────────────────────────────────────────
object TimerPreferenceKeys {
    val LAST_DURATION_SECONDS = longPreferencesKey("last_duration_seconds")
    val STARTED_AT_MILLIS = longPreferencesKey("started_at_millis")
    val COMMITMENT_MODE = booleanPreferencesKey("commitment_mode")
    val CUSTOM_PRESETS = stringPreferencesKey("custom_presets_string")
    val SUNSET_MODE_ENABLED = booleanPreferencesKey("sunset_mode_enabled")
    val SUNSET_DURATION_SECONDS = longPreferencesKey("sunset_duration_seconds")
}

/**
 * Data access object for timer preferences stored in DataStore.
 * Provides a [Flow] of the persisted [TimerPreferences] data class.
 */
data class TimerPreferences(
    val lastDurationSeconds: Long = 300L,  // default: 5 minutes
    val startedAtMillis: Long = 0L,        // 0 means no active timer was persisted
    val commitmentMode: Boolean = false,   // when true, timer cannot be cancelled once started
    val customPresets: List<Long> = listOf(300L, 900L, 1800L, 3600L), // custom presets (in seconds)
    val sunsetModeEnabled: Boolean = true,  // default: enabled
    val sunsetDurationSeconds: Long = 30L   // default: 30 seconds
)

@Singleton
class TimerPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val preferences: Flow<TimerPreferences> = context.dataStore.data.map { prefs ->
        val presetsStr = prefs[TimerPreferenceKeys.CUSTOM_PRESETS] ?: "5,15,30,60"
        val presetsList = try {
            presetsStr.split(",").map { it.trim().toLong() * 60L }
        } catch (e: Exception) {
            listOf(300L, 900L, 1800L, 3600L)
        }
        TimerPreferences(
            lastDurationSeconds = prefs[TimerPreferenceKeys.LAST_DURATION_SECONDS] ?: 300L,
            startedAtMillis = prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] ?: 0L,
            commitmentMode = prefs[TimerPreferenceKeys.COMMITMENT_MODE] ?: false,
            customPresets = presetsList,
            sunsetModeEnabled = prefs[TimerPreferenceKeys.SUNSET_MODE_ENABLED] ?: true,
            sunsetDurationSeconds = prefs[TimerPreferenceKeys.SUNSET_DURATION_SECONDS] ?: 30L,
        )
    }

    suspend fun saveLastDuration(durationSeconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.LAST_DURATION_SECONDS] = durationSeconds
        }
    }

    /** Persist that a timer started at [startedAtMillis] with [durationSeconds] duration. */
    suspend fun saveTimerStarted(durationSeconds: Long, startedAtMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.LAST_DURATION_SECONDS] = durationSeconds
            prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] = startedAtMillis
        }
    }

    /** Clear the persisted active timer (called when timer finishes or is cancelled). */
    suspend fun clearActiveTimer() {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] = 0L
        }
    }

    /** Persist the commitment mode setting. */
    suspend fun saveCommitmentMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.COMMITMENT_MODE] = enabled
        }
    }

    /** Persist custom preset durations (in minutes). */
    suspend fun saveCustomPresets(presetsMinutes: List<Long>) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.CUSTOM_PRESETS] = presetsMinutes.joinToString(",")
        }
    }

    /** Persist the sunset mode enabled setting. */
    suspend fun saveSunsetMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.SUNSET_MODE_ENABLED] = enabled
        }
    }

    /** Persist the sunset mode warning duration. */
    suspend fun saveSunsetDuration(seconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.SUNSET_DURATION_SECONDS] = seconds
        }
    }
}
