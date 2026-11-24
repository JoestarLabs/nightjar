package com.bl4ckswordsman.nightjar.service

import android.app.LocaleManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bl4ckswordsman.nightjar.MainActivity
import com.bl4ckswordsman.nightjar.NightjarApp
import com.bl4ckswordsman.nightjar.R

/**
 * Builds foreground countdown, 1-minute alert, and completion notifications for [LockTimerService].
 */
internal class TimerNotificationFactory(private val context: Context) {

    private var cachedLockBitmap: Bitmap? = null
    private var cachedUiNightMode: Int? = null

    /**
     * Constructs the ongoing countdown foreground notification with Live Update chip styling on API 36+.
     */
    fun buildCountdownNotification(
        durationSeconds: Long,
        countdownEndEpochMs: Long,
        remainingSeconds: Long,
        commitmentMode: Boolean,
        alertOnce: Boolean = true,
        isUrgent: Boolean = LockTimerService.isUrgent(remainingSeconds),
    ): Notification {
        val localizedContext = getLocalizedContext()
        val tapIntent = createMainActivityPendingIntent(requestCode = 0)
        val cancelIntent = PendingIntent.getService(
            context,
            1,
            LockTimerService.stopIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val smallIconRes = LockTimerService.resolveSmallIconRes(isUrgent)

        if (Build.VERSION.SDK_INT >= 36) {
            val segmentColor = ContextCompat.getColor(
                context,
                LockTimerService.resolveSegmentColorRes(isUrgent)
            )
            val endIconTint = ContextCompat.getColor(
                context,
                if (isUrgent) R.color.notification_alert_color else R.color.notification_icon_tint
            )
            val progressStyle = Notification.ProgressStyle()
                .setProgress((durationSeconds - remainingSeconds).toInt())
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(durationSeconds.toInt())
                        .setColor(segmentColor)
                )
                .setProgressEndIcon(
                    Icon.createWithResource(context, smallIconRes).setTint(endIconTint)
                )

            val builder = Notification.Builder(context, NightjarApp.CHANNEL_TIMER_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(localizedContext.getString(R.string.notification_title))
                .setWhen(countdownEndEpochMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(alertOnce)
                .setShowWhen(true)
                .setContentIntent(tapIntent)
                .setStyle(progressStyle)
                .addExtras(Bundle().apply {
                    putBoolean("android.requestPromotedOngoing", true)
                })

            if (!commitmentMode) {
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_cancel),
                        localizedContext.getString(R.string.notification_action_cancel),
                        cancelIntent
                    ).build()
                )
            }

            return builder.build()
        } else {
            val builder = NotificationCompat.Builder(context, NightjarApp.CHANNEL_TIMER_ID)
                .setSmallIcon(smallIconRes)
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
                .setLargeIcon(getLockBitmap(localizedContext))

            if (!commitmentMode) {
                builder.addAction(
                    R.drawable.ic_cancel,
                    localizedContext.getString(R.string.notification_action_cancel),
                    cancelIntent
                )
            }

            return builder.build()
        }
    }

    /** Builds the high-importance 1-minute remaining heads-up alert notification. */
    fun buildOneMinuteAlertNotification(): Notification {
        val localizedContext = getLocalizedContext()
        val tapIntent = createMainActivityPendingIntent(requestCode = 2)
        return NotificationCompat.Builder(context, NightjarApp.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(localizedContext.getString(R.string.notification_alert_title))
            .setContentText(localizedContext.getString(R.string.notification_alert_one_minute))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
    }

    /** Builds the brief completion notification displayed when countdown finishes. */
    fun buildFinishedNotification(): Notification {
        val localizedContext = getLocalizedContext()
        return NotificationCompat.Builder(context, NightjarApp.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_lock_notification)
            .setContentTitle(localizedContext.getString(R.string.notification_finished))
            .setOngoing(false)
            .build()
    }

    private fun createMainActivityPendingIntent(requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun getLocalizedContext(): Context {
        val localeManager = ContextCompat.getSystemService(context, LocaleManager::class.java)
        val localeList = localeManager?.applicationLocales
        if (localeList == null || localeList.isEmpty) return context
        val locale = localeList.get(0) ?: return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun getLockBitmap(localizedContext: Context): Bitmap? {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (cachedLockBitmap == null || cachedUiNightMode != nightMode) {
            val lockDrawable = ContextCompat.getDrawable(
                localizedContext,
                R.drawable.ic_lock_notification
            )
            lockDrawable?.mutate()
                ?.setTint(ContextCompat.getColor(context, R.color.notification_icon_tint))
            cachedLockBitmap = lockDrawable?.toBitmap(
                width = 120,
                height = 120,
                config = Bitmap.Config.ARGB_8888
            )
            cachedUiNightMode = nightMode
        }
        return cachedLockBitmap
    }
}
