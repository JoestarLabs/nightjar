package com.bl4ckswordsman.nightjar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.bl4ckswordsman.nightjar.data.TimerPreferenceKeys
import com.bl4ckswordsman.nightjar.service.LockTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore as appDataStore

private val Context.dataStore: DataStore<Preferences>
    by appDataStore(name = "nightjar_timer_prefs")

/**
 * Receives [Intent.ACTION_BOOT_COMPLETED] and [Intent.ACTION_MY_PACKAGE_REPLACED].
 *
 * If DataStore shows a timer was active before the device restarted
 * (startedAtMillis != 0 and remaining time > 0), we restart the
 * foreground service so the user's lock timer continues after a reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
        if (intent.action !in relevantActions) return

        // Use goAsync so we can safely read DataStore in a coroutine
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                val startedAt = prefs[TimerPreferenceKeys.STARTED_AT_MILLIS] ?: 0L
                val duration  = prefs[TimerPreferenceKeys.LAST_DURATION_SECONDS] ?: 0L

                if (startedAt > 0L && duration > 0L) {
                    val elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1_000
                    val remaining = duration - elapsedSeconds
                    if (remaining > 5) {
                        // Resume timer with remaining seconds
                        ContextCompat.startForegroundService(
                            context,
                            LockTimerService.startIntent(context, remaining)
                        )
                    }
                    // If ≤ 5 s remain after reboot, skip — not worth resuming.
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
