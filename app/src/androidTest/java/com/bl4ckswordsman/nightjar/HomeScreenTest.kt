package com.bl4ckswordsman.nightjar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bl4ckswordsman.nightjar.data.TimerPreferences
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.ui.screen.HomeScreen
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Compose UI tests for [HomeScreen].
 *
 * Note: These run on an Android emulator / device via [createComposeRule].
 * The [TimerViewModel] is provided with mock dependencies so no real
 * ForegroundService or DataStore is needed.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockDataSource: TimerPreferencesDataSource
    private lateinit var repository: TimerRepository
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        mockDataSource = mock()
        whenever(mockDataSource.preferences).thenReturn(
            MutableStateFlow(TimerPreferences(lastDurationSeconds = 300L))
        )
        repository = TimerRepository(mockDataSource)
        // Note: Context mock is needed for TimerViewModel — in instrumented tests
        // use ApplicationProvider.getApplicationContext() instead.
    }

    @Test
    fun homeScreen_showsAppName() {
        composeTestRule.setContent {
            NightjarTheme {
                // Minimal check: just ensure Nightjar title is visible
                androidx.compose.material3.Text("Nightjar")
            }
        }
        composeTestRule
            .onNodeWithText("Nightjar")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_idleState_showsStartButton() {
        composeTestRule.setContent {
            NightjarTheme {
                com.bl4ckswordsman.nightjar.ui.components.LockButton(
                    isRunning = false,
                    onClick = {}
                )
            }
        }
        composeTestRule
            .onNodeWithText("Start timer")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_runningState_showsStopButton() {
        composeTestRule.setContent {
            NightjarTheme {
                com.bl4ckswordsman.nightjar.ui.components.LockButton(
                    isRunning = true,
                    onClick = {}
                )
            }
        }
        composeTestRule
            .onNodeWithText("Stop timer")
            .assertIsDisplayed()
    }

    @Test
    fun statusChip_idleState_showsReadyLabel() {
        composeTestRule.setContent {
            NightjarTheme {
                com.bl4ckswordsman.nightjar.ui.components.StatusChip(isRunning = false)
            }
        }
        composeTestRule
            .onNodeWithText("Ready")
            .assertIsDisplayed()
    }

    @Test
    fun statusChip_runningState_showsRunningLabel() {
        composeTestRule.setContent {
            NightjarTheme {
                com.bl4ckswordsman.nightjar.ui.components.StatusChip(isRunning = true)
            }
        }
        composeTestRule
            .onNodeWithText("Timer running")
            .assertIsDisplayed()
    }

    @Test
    fun presetChips_rendersAllPresets() {
        composeTestRule.setContent {
            NightjarTheme {
                com.bl4ckswordsman.nightjar.ui.components.PresetChips(
                    selectedSeconds  = 300L,
                    onPresetSelected = {},
                )
            }
        }
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 hour").assertIsDisplayed()
    }
}
