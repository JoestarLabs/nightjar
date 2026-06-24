package com.bl4ckswordsman.nightjar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.bl4ckswordsman.nightjar.MainActivity
import com.bl4ckswordsman.nightjar.NightjarApp
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.receiver.LockDeviceAdminReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var timerRepository: TimerRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private val nm by lazy { getSystemService<NotificationManager>()!! }

    /** Whether cancellation is locked for the current timer run. Set from the start intent. */
    private var commitmentMode = false

    private var overlayManager: ComposeOverlayManager? = null
    private var cachedLockBitmap: android.graphics.Bitmap? = null


    // Sensor-based tilt detection
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var accelerometer: Sensor? = null
    private var currentTilt = 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            // X-axis value measures left/right tilt.
            val x = event.values[0]

            // Apply a low-pass filter to smooth the tilt changes:
            currentTilt = currentTilt * 0.85f + x * 0.15f

            if (overlayManager?.isShowing == true) {
                // Map accelerometer X to a normalized tilt float (-1f to 1f)
                val normalizedTilt = (currentTilt / 5.5f).coerceIn(-1f, 1f)
                overlayManager?.updateTilt(normalizedTilt)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerSensor() {
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensor() {
        sensorManager.unregisterListener(sensorListener)
    }

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
                if (durationSeconds > 0) startTimer(durationSeconds)
            }

            ACTION_STOP -> if (!commitmentMode) stopTimer()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        unregisterSensor()
        overlayManager?.dismiss()
        overlayManager = null
        super.onDestroy()
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private fun startTimer(durationSeconds: Long) {
        timerJob?.cancel()
        val startedAt = System.currentTimeMillis()

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

    private fun persistTimerStart(durationSeconds: Long, startedAt: Long) {
        // Persist so BootReceiver can resume if the device restarts
        serviceScope.launch {
            timerRepository.preferencesDataSource.saveTimerStarted(durationSeconds, startedAt)
        }
    }

    private suspend fun CoroutineScope.runCountdown(durationSeconds: Long, startedAt: Long) {
        val prefs = timerRepository.preferencesDataSource.preferences.first()
        val sunsetEnabled = prefs.sunsetModeEnabled
        val sunsetDuration = prefs.sunsetDurationSeconds

        var remaining = durationSeconds
        var alertFired = false

        timerRepository.updateState(
            TimerState.Running(
                totalSeconds = durationSeconds,
                remainingSeconds = remaining,
                startedAtMillis = startedAt
            )
        )

        // Trigger overlay immediately if within warning window at start
        if (sunsetEnabled && remaining <= sunsetDuration && hasOverlayPermission()) {
            launch(Dispatchers.Main) {
                registerSensor()
                overlayManager?.show(remaining, sunsetDuration, currentTilt)
            }
        }

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

            if (sunsetEnabled && remaining <= sunsetDuration && hasOverlayPermission()) {
                launch(Dispatchers.Main) {
                    if (overlayManager?.isShowing == true) {
                        overlayManager?.updateRemainingTime(remaining)
                    } else {
                        registerSensor()
                        overlayManager?.show(remaining, sunsetDuration, currentTilt)
                    }
                }
            }

            nm.notify(
                NOTIFICATION_ID,
                buildNotification(
                    durationSeconds,
                    startedAt + durationSeconds * 1_000,
                    remaining
                )
            )

            // ── 1-minute remaining alert (fires exactly once) ──────────
            if (remaining == ONE_MINUTE_SECONDS && !alertFired && durationSeconds > ONE_MINUTE_SECONDS) {
                alertFired = true
                val countdownEndEpochMs = startedAt + durationSeconds * 1_000
                postOneMinuteAlert(durationSeconds, countdownEndEpochMs, remaining)
            }
        }
        if (isActive) {
            launch(Dispatchers.Main) {
                unregisterSensor()
                overlayManager?.dismiss()
            }
            onTimerExpired()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        unregisterSensor()
        overlayManager?.dismiss()
        serviceScope.launch {
            timerRepository.preferencesDataSource.clearActiveTimer()
        }
        timerRepository.updateState(TimerState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerExpired() {
        timerRepository.updateState(TimerState.Finished)

        serviceScope.launch(Dispatchers.Main) {
            unregisterSensor()
            overlayManager?.dismiss()
        }

        // Update notification to "Locking screen now" briefly
        nm.notify(NOTIFICATION_ID, buildFinishedNotification())

        val locked = LockDeviceAdminReceiver.requestLock(this)

        serviceScope.launch {
            timerRepository.preferencesDataSource.clearActiveTimer()
            // Small grace period to ensure lock action is dispatched
            delay(800)
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
    private fun postOneMinuteAlert(
        durationSeconds: Long,
        countdownEndEpochMs: Long,
        remainingSeconds: Long
    ) {
        // ── Heads-up alert (all API levels) ───────────────────────────────────
        val localizedContext = getLocalizedContext()
        val tapIntent = PendingIntent.getActivity(
            this, 2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alert = NotificationCompat.Builder(this, NightjarApp.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(localizedContext.getString(R.string.notification_alert_title))
            .setContentText(localizedContext.getString(R.string.notification_alert_one_minute))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ALERT_ID, alert)

        // Auto-cancel after 5 s so the alert doesn't linger
        serviceScope.launch {
            delay(5_000)
            nm.cancel(NOTIFICATION_ALERT_ID)
        }

        // ── Live Update chip colour change (Android 16+) ───────────────────
        // Also update the chip to amber for one tick as an in-chip visual cue.
        // setOnlyAlertOnce(false) is passed but the chip expand is system-determined.
        if (Build.VERSION.SDK_INT >= 36) {
            val urgentNotification = buildNotification(
                durationSeconds, countdownEndEpochMs, remainingSeconds, alertOnce = false
            )
            nm.notify(NOTIFICATION_ID, urgentNotification)
        }
    }

    // ── Notification builders ──────────────────────────────────────────────────

    /**
     * Builds the countdown notification using the system Chronometer API.
     * [countdownEndEpochMs] = the wall-clock time when the timer reaches zero.
     * The OS renders the countdown in the notification without any Gradle-side polling.
     */
    private fun getLocalizedContext(): Context {
        val localeManager = getSystemService(android.app.LocaleManager::class.java)
        val localeList = localeManager.applicationLocales
        if (localeList.isEmpty) return this
        val locale = localeList.get(0) ?: return this
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        return createConfigurationContext(config)
    }

    private fun buildNotification(
        durationSeconds: Long,
        countdownEndEpochMs: Long,
        remainingSeconds: Long,
        alertOnce: Boolean = true,
    ): Notification {
        val localizedContext = getLocalizedContext()
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

        if (Build.VERSION.SDK_INT >= 36) {
            val segmentColor = if (alertOnce) {
                ContextCompat.getColor(this, R.color.bamboo_green_40)
            } else {
                // On the alert tick use an urgency colour so the chip visually changes
                ContextCompat.getColor(this, R.color.notification_alert_color)
            }

            val progressStyle = Notification.ProgressStyle()
                .setProgress((durationSeconds - remainingSeconds).toInt())
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(durationSeconds.toInt())
                        .setColor(segmentColor)
                )
                .setProgressEndIcon(
                    Icon.createWithResource(this, R.drawable.ic_lock_notification)
                        .setTint(ContextCompat.getColor(this, R.color.notification_icon_tint))
                )

            val builder = Notification.Builder(this, NightjarApp.CHANNEL_TIMER_ID)
                .setSmallIcon(R.drawable.ic_lock_notification)
                .setContentTitle(localizedContext.getString(R.string.notification_title))
                .setWhen(countdownEndEpochMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(alertOnce)
                .setShowWhen(true)
                .setContentIntent(tapIntent)
                .setStyle(progressStyle)
                .addExtras(android.os.Bundle().apply {
                    putBoolean("android.requestPromotedOngoing", true)
                })

            // Only show Cancel action when commitment mode is off
            if (!commitmentMode) {
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_cancel),
                        localizedContext.getString(R.string.notification_action_cancel),
                        cancelIntent
                    ).build()
                )
            }

            return builder.build()
        } else {
            val lockBitmap = getLockBitmap(localizedContext)

            val builder = NotificationCompat.Builder(this, NightjarApp.CHANNEL_TIMER_ID)
                .setSmallIcon(R.drawable.ic_lock_notification)
                .setContentTitle(localizedContext.getString(R.string.notification_title))
                .setWhen(countdownEndEpochMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setContentIntent(tapIntent)
                .setProgress(
                    durationSeconds.toInt(),
                    (durationSeconds - remainingSeconds).toInt(),
                    false
                )
                .setLargeIcon(lockBitmap)

            if (!commitmentMode) {
                builder.addAction(
                    R.drawable.ic_cancel,
                    localizedContext.getString(R.string.notification_action_cancel),
                    cancelIntent
                )
            }

            builder.extras.apply {
                putBoolean("android.requestPromotedOngoing", true)
            }

            return builder.build()
        }
    }

    private fun getLockBitmap(context: Context): android.graphics.Bitmap? {
        var bitmap = cachedLockBitmap
        if (bitmap == null) {
            val lockDrawable = ContextCompat.getDrawable(context, R.drawable.ic_lock_notification)
            lockDrawable?.mutate()
                ?.setTint(ContextCompat.getColor(this, R.color.notification_icon_tint))
            bitmap = lockDrawable?.toBitmap(
                width = 120,
                height = 120,
                config = android.graphics.Bitmap.Config.ARGB_8888
            )
            cachedLockBitmap = bitmap
        }
        return bitmap
    }

    private fun buildFinishedNotification(): Notification {
        val localizedContext = getLocalizedContext()
        return NotificationCompat.Builder(this, NightjarApp.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(localizedContext.getString(R.string.notification_finished))
            .setOngoing(false)
            .build()
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    companion object {
        const val ACTION_START = "com.bl4ckswordsman.nightjar.action.START_TIMER"
        const val ACTION_STOP = "com.bl4ckswordsman.nightjar.action.STOP_TIMER"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_COMMITMENT_MODE = "extra_commitment_mode"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ALERT_ID = 1002
        private const val ONE_MINUTE_SECONDS = 60L

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

        fun stopIntent(context: Context): Intent =
            Intent(context, LockTimerService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
