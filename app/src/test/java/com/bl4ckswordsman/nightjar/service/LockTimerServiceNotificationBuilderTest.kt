package com.bl4ckswordsman.nightjar.service

import android.app.Notification
import com.bl4ckswordsman.nightjar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LockTimerServiceNotificationBuilderTest {

    @Test
    fun `buildNotification includes cancel action when commitment mode is off`() {
        val service = Robolectric.setupService(LockTimerService::class.java)
        val notification = service.buildNotification(
            durationSeconds = 300L,
            countdownEndEpochMs = System.currentTimeMillis() + 300_000L,
            remainingSeconds = 200L,
        )

        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertEquals(
            service.getString(R.string.notification_title),
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        )
        assertNotNull(notification.actions)
        assertEquals(1, notification.actions.size)
        assertEquals(
            service.getString(R.string.notification_action_cancel),
            notification.actions[0].title
        )
    }
}
