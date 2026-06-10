package com.bl4ckswordsman.nightjar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.bl4ckswordsman.nightjar.MainActivity
import com.bl4ckswordsman.nightjar.NightjarApp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs the countdown timer.
 *
 * Android 14+ foreground service type: [specialUse].
 * Declared use case: "Countdown timer that locks the device screen when it expires."
 *
 * The service uses a coroutine-based countdown loop (no AlarmManager / WorkManager),
 * which is precise for durations up to ~2 hours and survives Doze mode because
 * foreground services are exempt from Doze's idle maintenance windows.
 *
 * Communication back to the UI is via [TimerRepository.timerState] (StateFlow shared
 * with [com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel] through Hilt).
 */
@AndroidEntryPoint
class LockTimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private val nm by lazy { getSystemService<NotificationManager>()!! }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                if (durationSeconds > 0) startTimer(durationSeconds)
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private fun startTimer(durationSeconds: Long) {
        timerJob?.cancel()
        val startedAt = System.currentTimeMillis()

        // Persist so BootReceiver can resume if the device restarts
        serviceScope.launch {
            timerRepository.preferencesDataSource.saveTimerStarted(durationSeconds, startedAt)
        }

        // Post initial foreground notification so the service is promoted immediately
        startForeground(NOTIFICATION_ID, buildNotification(durationSeconds, startedAt + durationSeconds * 1_000))

        timerJob = serviceScope.launch {
            var remaining = durationSeconds
            timerRepository.updateState(
                TimerState.Running(
                    totalSeconds = durationSeconds,
                    remainingSeconds = remaining,
                    startedAtMillis = startedAt
                )
            )
            while (remaining > 0 && isActive) {
                delay(1_000)
                remaining--
                timerRepository.updateState(
                    TimerState.Running(
                        totalSeconds = durationSeconds,
                        remainingSeconds = remaining,
                        startedAtMillis = startedAt
                    )
                )
            }
            if (isActive) onTimerExpired()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        serviceScope.launch {
            timerRepository.preferencesDataSource.clearActiveTimer()
        }
        timerRepository.updateState(TimerState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerExpired() {
        timerRepository.updateState(TimerState.Finished)

        // Update notification to "Locking screen now" briefly
        nm.notify(NOTIFICATION_ID, buildFinishedNotification())

        val locked = LockAccessibilityService.requestLock()

        serviceScope.launch {
            timerRepository.preferencesDataSource.clearActiveTimer()
            // Small grace period to ensure lock action is dispatched
            delay(800)
            timerRepository.updateState(TimerState.Idle)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ── Notification builders ──────────────────────────────────────────────────

    /**
     * Builds the countdown notification using the system Chronometer API.
     * [countdownEndEpochMs] = the wall-clock time when the timer reaches zero.
     * The OS renders the countdown in the notification without any Gradle-side polling.
     */
    private fun buildNotification(durationSeconds: Long, countdownEndEpochMs: Long): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LockTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NightjarApp.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setWhen(countdownEndEpochMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setContentIntent(tapIntent)
            .addAction(
                R.drawable.ic_cancel,
                getString(R.string.notification_action_cancel),
                cancelIntent
            )
            .build()
    }

    private fun buildFinishedNotification(): Notification =
        NotificationCompat.Builder(this, NightjarApp.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(getString(R.string.notification_finished))
            .setOngoing(false)
            .build()

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        const val ACTION_START = "com.bl4ckswordsman.nightjar.action.START_TIMER"
        const val ACTION_STOP  = "com.bl4ckswordsman.nightjar.action.STOP_TIMER"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context, durationSeconds: Long): Intent =
            Intent(context, LockTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, LockTimerService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
