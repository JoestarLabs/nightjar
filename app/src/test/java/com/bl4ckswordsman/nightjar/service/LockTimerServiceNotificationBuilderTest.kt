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

    @Test
    fun `buildNotification clamps out-of-range remainingSeconds without crashing`() {
        val service = Robolectric.setupService(LockTimerService::class.java)
        val notification = service.buildNotification(
            durationSeconds = 60L,
            countdownEndEpochMs = System.currentTimeMillis() + 60_000L,
            remainingSeconds = 120L, // e.g. backwards clock jump
        )
        assertEquals(0, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(60, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
    }

    @Test
    fun `ceiling remaining and next tick delay stay aligned to 1s boundaries`() {
        val endTimeMs = 90_000L
        // 5ms after start -> still 90s remaining, next tick in 995ms (at t=1000ms)
        val remAt5ms = LockTimerService.computeCeilingRemainingSeconds(endTimeMs, nowMs = 5L)
        assertEquals(90L, remAt5ms)
        assertEquals(995L, LockTimerService.computeNextTickDelayMs(endTimeMs, remAt5ms, nowMs = 5L))

        // At t=89_000ms -> 1s remaining, next tick in 1000ms (at t=90_000ms = endTimeMs)
        val remAt89s = LockTimerService.computeCeilingRemainingSeconds(endTimeMs, nowMs = 89_000L)
        assertEquals(1L, remAt89s)
        assertEquals(1_000L, LockTimerService.computeNextTickDelayMs(endTimeMs, remAt89s, nowMs = 89_000L))
    }
}
