package com.bl4ckswordsman.nightjar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
    val STARTED_AT_MILLIS     = longPreferencesKey("started_at_millis")
}

/**
 * Data access object for timer preferences stored in DataStore.
 * Provides a [Flow] of the persisted [TimerPreferences] data class.
 */
data class TimerPreferences(
    val lastDurationSeconds: Long = 300L,  // default: 5 minutes
    val startedAtMillis: Long = 0L,        // 0 means no active timer was persisted
)

@Singleton
class TimerPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val preferences: Flow<TimerPreferences> = context.dataStore.data.map { prefs ->
        TimerPreferences(
            lastDurationSeconds = prefs[TimerPreferenceKeys.LAST_DURATION_SECONDS] ?: 300L,
            startedAtMillis     = prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] ?: 0L,
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
            prefs[TimerPreferenceKeys.STARTED_AT_MILLIS]     = startedAtMillis
        }
    }

    /** Clear the persisted active timer (called when timer finishes or is cancelled). */
    suspend fun clearActiveTimer() {
        context.dataStore.edit { prefs ->
            prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] = 0L
        }
    }
}
