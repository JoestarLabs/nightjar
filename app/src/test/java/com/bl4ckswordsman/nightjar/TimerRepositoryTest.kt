package com.bl4ckswordsman.nightjar

import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TimerRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TimerRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val mockDataSource = mock<TimerPreferencesDataSource>()
        repository = TimerRepository(mockDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val state = repository.timerState.first()
        assertTrue("Initial state should be Idle", state is TimerState.Idle)
    }

    @Test
    fun `updateState reflects in flow`() = runTest {
        val running = TimerState.Running(
            totalSeconds = 300L,
            remainingSeconds = 250L,
            startedAtMillis = System.currentTimeMillis(),
        )
        repository.updateState(running)
        val state = repository.timerState.first()
        assertEquals(running, state)
    }

    @Test
    fun `updateState to Finished then Idle`() = runTest {
        repository.updateState(TimerState.Finished)
        assertEquals(TimerState.Finished, repository.currentState)

        repository.updateState(TimerState.Idle)
        assertEquals(TimerState.Idle, repository.currentState)
    }

    @Test
    fun `Running state progressFraction is correct`() {
        val state = TimerState.Running(
            totalSeconds = 100L,
            remainingSeconds = 50L,
            startedAtMillis = 0L,
        )
        assertEquals(0.5f, state.progressFraction, 0.001f)
    }

    @Test
    fun `Running state progressFraction at start is 0`() {
        val state = TimerState.Running(
            totalSeconds = 100L,
            remainingSeconds = 100L,
            startedAtMillis = 0L,
        )
        assertEquals(0f, state.progressFraction, 0.001f)
    }

    @Test
    fun `Running state progressFraction near end is close to 1`() {
        val state = TimerState.Running(
            totalSeconds = 100L,
            remainingSeconds = 1L,
            startedAtMillis = 0L,
        )
        assertEquals(0.99f, state.progressFraction, 0.001f)
    }

    @Test
    fun `currentState matches last updateState call`() {
        repository.updateState(TimerState.Finished)
        assertFalse(repository.currentState is TimerState.Idle)
        assertTrue(repository.currentState is TimerState.Finished)
    }
}
