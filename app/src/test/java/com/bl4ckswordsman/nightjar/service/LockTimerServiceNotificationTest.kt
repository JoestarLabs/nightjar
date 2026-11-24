package com.bl4ckswordsman.nightjar.service

import com.bl4ckswordsman.nightjar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockTimerServiceNotificationTest {

    // ── T004: Standard Green Segment when remainingSeconds > 60 ──────────────

    @Test
    fun isUrgent_returnsFalse_whenRemainingSecondsGreaterThanSixty() {
        assertFalse(LockTimerService.isUrgent(remainingSeconds = 61))
        assertFalse(LockTimerService.isUrgent(remainingSeconds = 120))
        assertFalse(LockTimerService.isUrgent(remainingSeconds = 3600))
    }

    @Test
    fun resolveSegmentColorRes_returnsBambooGreen_whenNotUrgent() {
        val colorRes = LockTimerService.resolveSegmentColorRes(isUrgent = false)
        assertEquals(R.color.bamboo_green_40, colorRes)
    }

    // ── T005: Urgency Amber Segment when remainingSeconds <= 60 ──────────────

    @Test
    fun isUrgent_returnsTrue_whenRemainingSecondsLessThanOrEqualToSixty() {
        assertTrue("Exactly 60s must be urgent", LockTimerService.isUrgent(remainingSeconds = 60))
        assertTrue("59s must be urgent", LockTimerService.isUrgent(remainingSeconds = 59))
        assertTrue("1s must be urgent", LockTimerService.isUrgent(remainingSeconds = 1))
        assertTrue("0s must be urgent", LockTimerService.isUrgent(remainingSeconds = 0))
    }

    @Test
    fun resolveSegmentColorRes_returnsAlertColor_whenUrgent() {
        val colorRes = LockTimerService.resolveSegmentColorRes(isUrgent = true)
        assertEquals(R.color.notification_alert_color, colorRes)
    }

    @Test
    fun isUrgent_isTrueForInitialShortDuration() {
        val shortDuration = 30L
        assertTrue(
            "Timer started with 30s must immediately evaluate to urgent",
            LockTimerService.isUrgent(remainingSeconds = shortDuration)
        )
    }

    @Test
    fun resolveSmallIconRes_returnsStandardLock_whenNotUrgent() {
        val iconRes = LockTimerService.resolveSmallIconRes(isUrgent = false)
        assertEquals(R.drawable.ic_lock_notification, iconRes)
    }

    @Test
    fun resolveSmallIconRes_returnsAlertIcon_whenUrgent() {
        val iconRes = LockTimerService.resolveSmallIconRes(isUrgent = true)
        assertEquals(R.drawable.outline_error_24, iconRes)
    }

    @Test
    fun shouldUpdateNotification_throttlesOneSecondTicksBetweenIntervals() {
        assertFalse(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 300L,
                remainingSeconds = 299L,
                lastNotifiedRemaining = 300L,
                isUrgent = false,
                lastNotifiedUrgent = false,
                isAlertTick = false,
            )
        )
        assertTrue(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 300L,
                remainingSeconds = 285L,
                lastNotifiedRemaining = 300L,
                isUrgent = false,
                lastNotifiedUrgent = false,
                isAlertTick = false,
            )
        )
    }

    @Test
    fun shouldUpdateNotification_scalesIntervalForLongTimers() {
        // 3600s / 20 = 180s interval
        assertFalse(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 3600L,
                remainingSeconds = 3500L,
                lastNotifiedRemaining = 3600L,
                isUrgent = false,
                lastNotifiedUrgent = false,
                isAlertTick = false,
            )
        )
        assertTrue(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 3600L,
                remainingSeconds = 3420L,
                lastNotifiedRemaining = 3600L,
                isUrgent = false,
                lastNotifiedUrgent = false,
                isAlertTick = false,
            )
        )
    }

    @Test
    fun shouldUpdateNotification_firesImmediatelyOnUrgencyOrAlertTick() {
        assertTrue(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 300L,
                remainingSeconds = 59L,
                lastNotifiedRemaining = 60L,
                isUrgent = true,
                lastNotifiedUrgent = true,
                isAlertTick = true,
            )
        )
        assertTrue(
            LockTimerService.shouldUpdateNotification(
                durationSeconds = 300L,
                remainingSeconds = 60L,
                lastNotifiedRemaining = 61L,
                isUrgent = true,
                lastNotifiedUrgent = false,
                isAlertTick = false,
            )
        )
    }
}
