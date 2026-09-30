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
}
