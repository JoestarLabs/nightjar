package com.bl4ckswordsman.nightjar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.service.LockTimerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives [Intent.ACTION_BOOT_COMPLETED] and [Intent.ACTION_MY_PACKAGE_REPLACED].
 *
 * If DataStore shows a timer was active before the device restarted
 * (startedAtMillis != 0 and remaining time > 0), we restart the
 * foreground service so the user's lock timer continues after a reboot.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun preferencesDataSource(): TimerPreferencesDataSource
    }

    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
        if (intent.action !in relevantActions) return

        // Resolve dependencies via EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val preferencesDataSource = entryPoint.preferencesDataSource()

        // Use goAsync so we can safely read DataStore in a coroutine
        val pendingResult = goAsync()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.w(TAG, "Unhandled error during boot recovery", throwable)
        }
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            try {
                handleBoot(context, preferencesDataSource)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume timer after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handleBoot(
        context: Context,
        preferencesDataSource: TimerPreferencesDataSource,
        currentTimeMillis: Long = System.currentTimeMillis(),
        startService: (Context, Intent) -> Unit = { ctx, startIntent ->
            ContextCompat.startForegroundService(ctx, startIntent)
        }
    ) {
        val prefs = preferencesDataSource.preferences.first()
        val startedAt = prefs.startedAtMillis
        val duration = prefs.lastDurationSeconds
        val commitmentMode = prefs.commitmentMode

        if (startedAt > 0L && duration > 0L) {
            val elapsedSeconds = (currentTimeMillis - startedAt) / 1_000
            if (elapsedSeconds < 0L) {
                // Clock skewed backwards across reboot; clear stale timer rather than inflating duration
                preferencesDataSource.clearActiveTimerIfMatches(startedAt)
                return
            }
            val remaining = duration - elapsedSeconds
            if (remaining > 5) {
                try {
                    // Resume timer with remaining seconds and preserved commitment mode
                    startService(
                        context,
                        LockTimerService.startIntent(context, remaining, commitmentMode)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to start foreground service on boot", e)
                }
            } else {
                // If ≤ 5 s remain after reboot, clear persisted timer and skip.
                preferencesDataSource.clearActiveTimerIfMatches(startedAt)
            }
        } else if (startedAt > 0L) {
            preferencesDataSource.clearActiveTimerIfMatches(startedAt)
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}

