package com.bl4ckswordsman.nightjar.service

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract for playing an audio alert when timer transitions into the sunset warning window.
 */
interface SunsetAudioPlayer {
    /**
     * Plays the sunset warning chime. Non-blocking and silent-profile-aware.
     */
    fun playChime()
}

@Singleton
class AndroidSunsetAudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SunsetAudioPlayer {

    override fun playChime() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return

            val ringtone = RingtoneManager.getRingtone(context, alertUri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play sunset warning chime", e)
        }
    }

    companion object {
        private const val TAG = "SunsetAudioPlayer"
    }
}
