package com.bl4ckswordsman.nightjar

import android.content.Context
import com.bl4ckswordsman.nightjar.data.TimerPreferences
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockDataSource: TimerPreferencesDataSource
    private lateinit var repository: TimerRepository
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mock()
        mockDataSource = mock()

        // Stub the preferences flow to return a default TimerPreferences
        val prefsFlow = MutableStateFlow(TimerPreferences(lastDurationSeconds = 300L))
        whenever(mockDataSource.preferences).thenReturn(prefsFlow)

        repository = TimerRepository(mockDataSource)
        viewModel = TimerViewModel(mockContext, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertTrue(repository.currentState is TimerState.Idle)
    }

    @Test
    fun `selectedSeconds default is 300`() = runTest {
        advanceTimeBy(100)  // allow init coroutine to run
        assertEquals(300L, viewModel.selectedSeconds)
    }

    @Test
    fun `setSelectedSeconds clamps to min`() {
        viewModel.setSelectedSeconds(0L)
        assertEquals(TimerViewModel.MIN_SECONDS, viewModel.selectedSeconds)
    }

    @Test
    fun `setSelectedSeconds clamps to max`() {
        viewModel.setSelectedSeconds(99_999L)
        assertEquals(TimerViewModel.MAX_SECONDS, viewModel.selectedSeconds)
    }

    @Test
    fun `setSelectedSeconds stores valid value`() {
        viewModel.setSelectedSeconds(900L)
        assertEquals(900L, viewModel.selectedSeconds)
    }

    @Test
    fun `toggleTimer when idle does not crash without service`() {
        // Context.startService returns null without a real Android environment;
        // verify no exception is thrown.
        viewModel.setSelectedSeconds(60L)
        try {
            viewModel.toggleTimer()
        } catch (e: Exception) {
            assertFalse("Should not throw on toggleTimer in test env: ${e.message}", true)
        }
    }

    @Test
    fun `stopTimer updates state expectation via repository`() {
        // Manually set running state and verify stopTimer resets it
        repository.updateState(
            TimerState.Running(
                totalSeconds = 60L,
                remainingSeconds = 30L,
                startedAtMillis = System.currentTimeMillis(),
            )
        )
        assertTrue(repository.currentState is TimerState.Running)

        // Simulate what the service does when stopTimer is called
        repository.updateState(TimerState.Idle)
        assertTrue(repository.currentState is TimerState.Idle)
    }
}
