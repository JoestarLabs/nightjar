package com.bl4ckswordsman.nightjar.service

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for [LockTimerService.startIntent] and [LockTimerService.stopIntent].
 *
 * These factory methods build the [Intent] objects that callers send to the foreground service.
 * Testing them in isolation ensures the correct action string, component class, and extras are
 * always set — catching any future refactors that silently break callers.
 */
@RunWith(RobolectricTestRunner::class)
class LockTimerServiceIntentTest {

    private val context get() = RuntimeEnvironment.getApplication()

    // ── startIntent ───────────────────────────────────────────────────────────

    @Test
    fun `startIntent sets ACTION_START as the intent action`() {
        val intent = LockTimerService.startIntent(context, durationSeconds = 60)
        assertEquals(LockTimerService.ACTION_START, intent.action)
    }

    @Test
    fun `startIntent targets LockTimerService component`() {
        val intent = LockTimerService.startIntent(context, durationSeconds = 60)
        assertEquals(LockTimerService::class.java.name, intent.component?.className)
    }

    @Test
    fun `startIntent stores the supplied durationSeconds extra`() {
        val expectedDuration = 3_600L
        val intent = LockTimerService.startIntent(context, durationSeconds = expectedDuration)
        assertEquals(
            expectedDuration,
            intent.getLongExtra(LockTimerService.EXTRA_DURATION_SECONDS, -1L)
        )
    }

    @Test
    fun `startIntent stores commitmentMode extra as false by default`() {
        val intent = LockTimerService.startIntent(context, durationSeconds = 120)
        assertFalse(intent.getBooleanExtra(LockTimerService.EXTRA_COMMITMENT_MODE, true))
    }

    @Test
    fun `startIntent stores commitmentMode extra as true when explicitly set`() {
        val intent = LockTimerService.startIntent(
            context,
            durationSeconds = 120,
            commitmentMode = true
        )
        assertTrue(intent.getBooleanExtra(LockTimerService.EXTRA_COMMITMENT_MODE, false))
    }

    // ── stopIntent ────────────────────────────────────────────────────────────

    @Test
    fun `stopIntent sets ACTION_STOP as the intent action`() {
        val intent = LockTimerService.stopIntent(context)
        assertEquals(LockTimerService.ACTION_STOP, intent.action)
    }

    @Test
    fun `stopIntent targets LockTimerService component`() {
        val intent = LockTimerService.stopIntent(context)
        assertEquals(LockTimerService::class.java.name, intent.component?.className)
    }

    @Test
    fun `stopIntent stores force extra as false by default`() {
        val intent = LockTimerService.stopIntent(context)
        assertFalse(intent.getBooleanExtra(LockTimerService.EXTRA_FORCE_STOP, true))
    }

    @Test
    fun `stopIntent stores force extra as true when explicitly set`() {
        val intent = LockTimerService.stopIntent(context, force = true)
        assertTrue(intent.getBooleanExtra(LockTimerService.EXTRA_FORCE_STOP, false))
    }
}
