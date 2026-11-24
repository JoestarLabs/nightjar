package com.bl4ckswordsman.nightjar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bl4ckswordsman.nightjar.ui.components.LockButton
import com.bl4ckswordsman.nightjar.ui.components.PresetChips
import com.bl4ckswordsman.nightjar.ui.components.StatusChip
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for Nightjar home screen components.
 *
 * Note: These run on an Android emulator / device via [createComposeRule].
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_idleState_showsStartButton() {
        composeTestRule.setContent {
            NightjarTheme {
                LockButton(
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
                LockButton(
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
    fun homeScreen_lockedState_showsTimerLocked() {
        composeTestRule.setContent {
            NightjarTheme {
                LockButton(
                    isRunning = true,
                    isLocked = true,
                    onClick = {}
                )
            }
        }
        composeTestRule
            .onNodeWithText("Timer locked")
            .assertIsDisplayed()
    }

    @Test
    fun statusChip_idleState_showsReadyLabel() {
        composeTestRule.setContent {
            NightjarTheme {
                StatusChip(isRunning = false)
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
                StatusChip(isRunning = true)
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
                PresetChips(
                    selectedSeconds = 300L,
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
