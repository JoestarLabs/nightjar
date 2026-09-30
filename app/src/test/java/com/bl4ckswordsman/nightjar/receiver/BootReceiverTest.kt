package com.bl4ckswordsman.nightjar.receiver

import android.content.Context
import android.content.Intent
import com.bl4ckswordsman.nightjar.data.TimerPreferences
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.service.LockTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var mockDataSource: TimerPreferencesDataSource
    private lateinit var receiver: BootReceiver

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDataSource = mock()
        receiver = BootReceiver()
    }

    @Test
    fun `resumes timer with commitment mode true when timer was active with commitmentMode enabled`() = runTest {
        val now = 1_000_000L
        val startedAt = now - 60_000L // 60 seconds ago
        val duration = 300L // 300s total duration
        // Expected remaining: 300 - 60 = 240 seconds

        val prefs = TimerPreferences(
            lastDurationSeconds = duration,
            startedAtMillis = startedAt,
            commitmentMode = true
        )
        whenever(mockDataSource.preferences).thenReturn(MutableStateFlow(prefs))

        var launchedIntent: Intent? = null
        receiver.handleBoot(
            context = context,
            preferencesDataSource = mockDataSource,
            currentTimeMillis = now,
            startService = { _, intent ->
                launchedIntent = intent
            }
        )

        val intent = checkNotNull(launchedIntent)
        assertEquals(LockTimerService.ACTION_START, intent.action)
        assertEquals(240L, intent.getLongExtra(LockTimerService.EXTRA_DURATION_SECONDS, -1L))
        assertTrue(intent.getBooleanExtra(LockTimerService.EXTRA_COMMITMENT_MODE, false))
    }

    @Test
    fun `resumes timer with commitment mode false when timer was active with commitmentMode disabled`() = runTest {
        val now = 1_000_000L
        val startedAt = now - 60_000L
        val duration = 300L

        val prefs = TimerPreferences(
            lastDurationSeconds = duration,
            startedAtMillis = startedAt,
            commitmentMode = false
        )
        whenever(mockDataSource.preferences).thenReturn(MutableStateFlow(prefs))

        var launchedIntent: Intent? = null
        receiver.handleBoot(
            context = context,
            preferencesDataSource = mockDataSource,
            currentTimeMillis = now,
            startService = { _, intent ->
                launchedIntent = intent
            }
        )

        val intent = checkNotNull(launchedIntent)
        assertEquals(LockTimerService.ACTION_START, intent.action)
        assertEquals(240L, intent.getLongExtra(LockTimerService.EXTRA_DURATION_SECONDS, -1L))
        assertFalse(intent.getBooleanExtra(LockTimerService.EXTRA_COMMITMENT_MODE, true))
    }

    @Test
    fun `does not resume service when remaining time is 5 seconds or less`() = runTest {
        val now = 1_000_000L
        val startedAt = now - 296_000L // 296 seconds elapsed
        val duration = 300L // 4 seconds remaining (<= 5s)

        val prefs = TimerPreferences(
            lastDurationSeconds = duration,
            startedAtMillis = startedAt,
            commitmentMode = true
        )
        whenever(mockDataSource.preferences).thenReturn(MutableStateFlow(prefs))

        var serviceStarted = false
        receiver.handleBoot(
            context = context,
            preferencesDataSource = mockDataSource,
            currentTimeMillis = now,
            startService = { _, _ ->
                serviceStarted = true
            }
        )

        assertFalse("Service should not be started if <= 5s remain", serviceStarted)
    }

    @Test
    fun `does not resume service when timer was not started`() = runTest {
        val now = 1_000_000L
        val prefs = TimerPreferences(
            lastDurationSeconds = 300L,
            startedAtMillis = 0L,
            commitmentMode = true
        )
        whenever(mockDataSource.preferences).thenReturn(MutableStateFlow(prefs))

        var serviceStarted = false
        receiver.handleBoot(
            context = context,
            preferencesDataSource = mockDataSource,
            currentTimeMillis = now,
            startService = { _, _ ->
                serviceStarted = true
            }
        )

        assertFalse("Service should not be started when startedAtMillis is 0", serviceStarted)
    }

    @Test
    fun `does not resume service when duration is 0`() = runTest {
        val now = 1_000_000L
        val prefs = TimerPreferences(
            lastDurationSeconds = 0L,
            startedAtMillis = now - 10_000L,
            commitmentMode = true
        )
        whenever(mockDataSource.preferences).thenReturn(MutableStateFlow(prefs))

        var serviceStarted = false
        receiver.handleBoot(
            context = context,
            preferencesDataSource = mockDataSource,
            currentTimeMillis = now,
            startService = { _, _ ->
                serviceStarted = true
            }
        )

        assertFalse("Service should not be started when duration is 0", serviceStarted)
    }

    @Test
    fun `ignores irrelevant broadcast actions`() {
        val irrelevantIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        // onReceive should early-return without attempting Hilt resolution or service launch
        receiver.onReceive(context, irrelevantIntent)
    }
}
