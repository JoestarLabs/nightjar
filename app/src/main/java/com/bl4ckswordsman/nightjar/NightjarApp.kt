package com.bl4ckswordsman.nightjar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NightjarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService<NotificationManager>() ?: return

        // ── Timer countdown channel (silent, persistent) ───────────────────
        NotificationChannel(
            CHANNEL_TIMER_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }.also { nm.createNotificationChannel(it) }

        // ── One-shot alert channel (heads-up, fires once at 1-minute mark) ─
        NotificationChannel(
            CHANNEL_ALERT_ID,
            getString(R.string.notification_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_alert_channel_description)
            setShowBadge(false)
        }.also { nm.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_TIMER_ID = "nightjar_timer_v1"
        const val CHANNEL_ALERT_ID = "nightjar_timer_alert_v1"
    }
}
