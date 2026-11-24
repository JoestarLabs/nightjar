package com.bl4ckswordsman.nightjar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.getSystemService
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.SunsetAnimation
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.receiver.LockDeviceAdminReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var sunsetAudioPlayer: SunsetAudioPlayer

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var activeRunId = 0L
    private var activeStartedAtMillis = 0L
    private val nm by lazy { getSystemService<NotificationManager>()!! }

    /** Whether cancellation is locked for the current timer run. Set from the start intent. */
    private var commitmentMode = false

    private var overlayManager: ComposeOverlayManager? = null
    private val notificationFactory by lazy { TimerNotificationFactory(this) }


    // Sensor-based tilt detection
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var accelerometer: Sensor? = null
    private val tiltTracker = TiltGravityTracker()

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            tiltTracker.update(event.values[0], event.values[1])
            if (overlayManager?.isShowing == true) {
                overlayManager?.updateSensors(
                    tilt = tiltTracker.tilt,
                    gravityAngleRad = tiltTracker.gravityAngleRad,
                    linearAccelX = tiltTracker.linearAccelX,
                    linearAccelY = tiltTracker.linearAccelY,
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Registers the accelerometer sensor listener for tilt tracking. */
    private fun registerSensor() {
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /** Unregisters the accelerometer sensor listener. */
    private fun unregisterSensor() {
        sensorManager.unregisterListener(sensorListener)
    }

    /** Returns whether the application has granted SYSTEM_ALERT_WINDOW permission. */
    private fun hasOverlayPermission(): Boolean {
        return android.provider.Settings.canDrawOverlays(this)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        overlayManager = ComposeOverlayManager(this)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                commitmentMode = intent.getBooleanExtra(EXTRA_COMMITMENT_MODE, false)
                if (durationSeconds > 0) {
                    startTimer(durationSeconds)
                } else {
                    startForeground(NOTIFICATION_ID, notificationFactory.buildFinishedNotification())
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                val force = intent.getBooleanExtra(EXTRA_FORCE_STOP, false)
                if (!commitmentMode || force) stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        unregisterSensor()
        overlayManager?.dismiss()
        overlayManager = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    /** Starts the timer countdown for [durationSeconds] and posts the foreground notification. */
    private fun startTimer(durationSeconds: Long) {
        timerJob?.cancel()
        unregisterSensor()
        overlayManager?.dismiss()
        val startedAt = System.currentTimeMillis()
        activeRunId++
        activeStartedAtMillis = startedAt

        persistTimerStart(durationSeconds, startedAt)

        // Post initial foreground notification so the service is promoted immediately
        startForeground(
            NOTIFICATION_ID,
            buildNotification(durationSeconds, startedAt + durationSeconds * 1_000, durationSeconds)
        )

        timerJob = serviceScope.launch {
            runCountdown(durationSeconds, startedAt)
        }
    }

    /** Persists timer start parameters to DataStore for reboot recovery. */
    private fun persistTimerStart(durationSeconds: Long, startedAt: Long) {
        // Persist so BootReceiver can resume if the device restarts
        serviceScope.launch {
            timerRepository.preferencesDataSource.saveTimerStarted(durationSeconds, startedAt)
        }
    }

    /** Executes the coroutine countdown loop and updates notification/overlay state. */
    private suspend fun CoroutineScope.runCountdown(durationSeconds: Long, startedAt: Long) {
        val prefs = timerRepository.preferencesDataSource.preferences.first()
        val sunsetAnimation = prefs.sunsetAnimation
        val sunsetEnabled = prefs.sunsetModeEnabled && sunsetAnimation != SunsetAnimation.OFF
        val sunsetDuration = prefs.sunsetDurationSeconds
        val sunsetAudioEnabled = prefs.sunsetAudioEnabled

        // Wall-clock end time — the single source of truth for all remaining-time calculations.
        val endTimeMs = startedAt + durationSeconds * 1_000L

        var remaining = computeCeilingRemainingSeconds(endTimeMs, System.currentTimeMillis())
        var alertFired = false
        var sunsetChimePlayed = false

        timerRepository.updateState(
            TimerState.Running(
                totalSeconds = durationSeconds,
                remainingSeconds = remaining,
                startedAtMillis = startedAt
            )
        )

        // Trigger overlay immediately if within warning window at start
        if (sunsetEnabled && remaining <= sunsetDuration) {
            if (sunsetAudioEnabled && !sunsetChimePlayed) {
                sunsetChimePlayed = true
                sunsetAudioPlayer.playChime()
            }
            if (hasOverlayPermission()) {
                launch(Dispatchers.Main) {
                    registerSensor()
                    // sunsetWindowStartMs: the wall-clock moment when progress = 0 for the overlay
                    // (i.e. when remaining == sunsetDuration). Using this keeps the wave filling
                    // correctly from 0→1 over exactly sunsetDuration seconds.
                    val sunsetWindowStartMs = endTimeMs - sunsetDuration * 1_000L
                    overlayManager?.show(
                        remainingSeconds = remaining,
                        totalSeconds = sunsetDuration,
                        tilt = tiltTracker.tilt,
                        gravityAngleRad = tiltTracker.gravityAngleRad,
                        animation = sunsetAnimation,
                        startedAtMillis = sunsetWindowStartMs
                    )
                }
            }
        }

        while (remaining > 0 && isActive) {
            // Sleep until the exact wall-clock second boundary for (remaining - 1).
            val sleepMs = computeNextTickDelayMs(endTimeMs, remaining, System.currentTimeMillis())
            delay(sleepMs)

            remaining--
            val wallRem = computeCeilingRemainingSeconds(endTimeMs, System.currentTimeMillis())
            if (wallRem < remaining - 1) remaining = wallRem  // drift guard: >1 s behind

            timerRepository.updateState(
                TimerState.Running(
                    totalSeconds = durationSeconds,
                    remainingSeconds = remaining,
                    startedAtMillis = startedAt
                )
            )

            if (sunsetEnabled && remaining <= sunsetDuration) {
                if (sunsetAudioEnabled && !sunsetChimePlayed) {
                    sunsetChimePlayed = true
                    sunsetAudioPlayer.playChime()
                }
                if (hasOverlayPermission()) {
                    launch(Dispatchers.Main) {
                        if (overlayManager?.isShowing == true) {
                            overlayManager?.updateRemainingTime(remaining)
                        } else {
                            registerSensor()
                            val sunsetWindowStartMs = endTimeMs - sunsetDuration * 1_000L
                            overlayManager?.show(
                                remainingSeconds = remaining,
                                totalSeconds = sunsetDuration,
                                tilt = tiltTracker.tilt,
                                gravityAngleRad = tiltTracker.gravityAngleRad,
                                animation = sunsetAnimation,
                                startedAtMillis = sunsetWindowStartMs
                            )
                        }
                    }
                }
            }

            // ── 1-minute remaining alert (fires exactly once) ──────────
            // Use <= instead of == so the alert is never skipped if a tick lands
            // slightly past the 60-second boundary (e.g. remaining jumps 62→59).
            val isAlertTick = remaining <= ONE_MINUTE_SECONDS &&
                !alertFired &&
                durationSeconds > ONE_MINUTE_SECONDS
            if (isAlertTick) {
                alertFired = true
                postOneMinuteAlert()
            }

            nm.notify(
                NOTIFICATION_ID,
                buildNotification(
                    durationSeconds = durationSeconds,
                    countdownEndEpochMs = endTimeMs,
                    remainingSeconds = remaining,
                    alertOnce = !isAlertTick,
                    isUrgent = isAlertTick || isUrgent(remaining)
                )
            )
        }
        if (isActive) {
            onTimerExpired()
        }
    }

    /** Stops the active countdown, cleans up overlay windows, and terminates the service. */
    private fun stopTimer() {
        val runId = activeRunId
        val startedAt = activeStartedAtMillis
        timerJob?.cancel()
        timerJob = null
        unregisterSensor()
        overlayManager?.dismiss()
        nm.cancel(NOTIFICATION_ALERT_ID)
        timerRepository.updateState(TimerState.Idle)
        serviceScope.launch {
            withContext(NonCancellable) {
                try {
                    if (startedAt > 0L) {
                        timerRepository.preferencesDataSource.clearActiveTimerIfMatches(startedAt)
                    } else {
                        timerRepository.preferencesDataSource.clearActiveTimer()
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to clear active timer on stop", e)
                }
            }
            if (activeRunId != runId) return@launch
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Invoked upon countdown expiry to lock device screen and update completion state. */
    private fun onTimerExpired() {
        val runId = activeRunId
        val startedAt = activeStartedAtMillis
        timerRepository.updateState(TimerState.Finished)

        serviceScope.launch(Dispatchers.Main) {
            unregisterSensor()
            overlayManager?.dismiss()
        }

        nm.cancel(NOTIFICATION_ALERT_ID)
        // Update notification to "Locking screen now" briefly
        nm.notify(NOTIFICATION_ID, notificationFactory.buildFinishedNotification())

        val locked = LockDeviceAdminReceiver.requestLock(this)
        if (!locked) {
            Log.w(TAG, "Failed to lock screen: Device Admin is inactive or lockNow threw")
        }

        serviceScope.launch {
            withContext(NonCancellable) {
                try {
                    if (startedAt > 0L) {
                        timerRepository.preferencesDataSource.clearActiveTimerIfMatches(startedAt)
                    } else {
                        timerRepository.preferencesDataSource.clearActiveTimer()
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to clear active timer on expiry", e)
                }
            }
            // Small grace period to ensure lock action is dispatched
            delay(800)
            if (activeRunId != runId) return@launch
            timerRepository.updateState(TimerState.Idle)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ── 1-minute alert ────────────────────────────────────────────────────────

    /**
     * Fires the 1-minute remaining alert:
     *
     * - On **all API levels**: posts a short-lived heads-up notification on the high-importance
     *   alert channel (`CHANNEL_ALERT_ID`). This is the most reliable way to visually alert
     *   the user regardless of OS version.
     *
     * - On **Android 16+ (API 36)** additionally: updates the existing Live Update chip with
     *   an amber segment colour as an in-chip visual cue. The chip's expand/pulse animation
     *   itself is entirely system-controlled and cannot be triggered programmatically.
     */
    private fun postOneMinuteAlert() {
        // ── Heads-up alert (all API levels) ───────────────────────────────────
        nm.notify(NOTIFICATION_ALERT_ID, notificationFactory.buildOneMinuteAlertNotification())

        // Auto-cancel after 5 s so the alert doesn't linger
        serviceScope.launch {
            delay(5_000)
            nm.cancel(NOTIFICATION_ALERT_ID)
        }
    }

    /** Constructs the ongoing countdown foreground notification with Live Update chip styling. */
    internal fun buildNotification(
        durationSeconds: Long,
        countdownEndEpochMs: Long,
        remainingSeconds: Long,
        alertOnce: Boolean = true,
        isUrgent: Boolean = isUrgent(remainingSeconds),
    ): Notification = notificationFactory.buildCountdownNotification(
        durationSeconds = durationSeconds,
        countdownEndEpochMs = countdownEndEpochMs,
        remainingSeconds = remainingSeconds,
        commitmentMode = commitmentMode,
        alertOnce = alertOnce,
        isUrgent = isUrgent,
    )

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "LockTimerService"
        const val ACTION_START = "com.bl4ckswordsman.nightjar.action.START_TIMER"
        const val ACTION_STOP = "com.bl4ckswordsman.nightjar.action.STOP_TIMER"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_COMMITMENT_MODE = "extra_commitment_mode"
        const val EXTRA_FORCE_STOP = "extra_force_stop"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ALERT_ID = 1002
        private const val ONE_MINUTE_SECONDS = 60L

        /**
         * Resolves the small icon resource ID for the notification and Live Update chip.
         * Returns [R.drawable.outline_error_24] when [isUrgent] is true,
         * or [R.drawable.ic_lock_notification] when false.
         */
        @DrawableRes
        fun resolveSmallIconRes(isUrgent: Boolean): Int =
            if (isUrgent) R.drawable.outline_error_24 else R.drawable.ic_lock_notification

        /**
         * Resolves the segment color resource ID for the Live Update chip.
         * Returns [R.color.notification_alert_color] when [isUrgent] is true,
         * or [R.color.bamboo_green_40] when false.
         */
        @ColorRes
        fun resolveSegmentColorRes(isUrgent: Boolean): Int =
            if (isUrgent) R.color.notification_alert_color else R.color.bamboo_green_40

        /**
         * Returns true if the countdown has entered the 1-minute alert window (remaining <= 60s).
         */
        fun isUrgent(remainingSeconds: Long): Boolean =
            remainingSeconds <= ONE_MINUTE_SECONDS

        /**
         * Returns ceiling-rounded remaining seconds until [endTimeMs], so `t = 0..999ms` after
         * start still reports `duration` and ticks align with 1-second wall-clock boundaries.
         */
        internal fun computeCeilingRemainingSeconds(endTimeMs: Long, nowMs: Long): Long =
            ((endTimeMs - nowMs + 999L).coerceAtLeast(0L)) / 1_000L

        /**
         * Computes the delay in ms until the next 1-second boundary (`endTimeMs - (remaining - 1) * 1000`).
         */
        internal fun computeNextTickDelayMs(
            endTimeMs: Long,
            currentRemainingSeconds: Long,
            nowMs: Long
        ): Long {
            val nextTickMs = endTimeMs - (currentRemainingSeconds - 1L).coerceAtLeast(0L) * 1_000L
            return (nextTickMs - nowMs).coerceAtLeast(0L)
        }

        /** Creates an [Intent] to start the timer countdown service. */
        fun startIntent(
            context: Context,
            durationSeconds: Long,
            commitmentMode: Boolean = false
        ): Intent =
            Intent(context, LockTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_COMMITMENT_MODE, commitmentMode)
            }

        /** Creates an [Intent] to stop the active timer countdown service. */
        fun stopIntent(context: Context, force: Boolean = false): Intent =
            Intent(context, LockTimerService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_FORCE_STOP, force)
            }
    }
}
