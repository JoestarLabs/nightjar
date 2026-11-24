package com.bl4ckswordsman.nightjar.ui.screen

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.TimerPreferences
import com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSource
import com.bl4ckswordsman.nightjar.data.TimerRepository
import com.bl4ckswordsman.nightjar.data.TimerState
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests verifying UI state survival, dialog behaviors, and settings persistence in [SettingsScreen].
 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var mockDataSource: TimerPreferencesDataSource
    private lateinit var repository: TimerRepository
    private lateinit var viewModel: TimerViewModel
    private val preferencesFlow = MutableStateFlow(
        TimerPreferences(
            customPresets = listOf(300L, 900L, 1800L, 3600L) // 5, 15, 30, 60 minutes
        )
    )

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDataSource = mock()
        whenever(mockDataSource.preferences).thenReturn(preferencesFlow)
        repository = TimerRepository(mockDataSource)
        viewModel = TimerViewModel(context, repository)
    }

    /**
     * Verifies that the preset dialog's editable text field state survives screen recreation
     * via rememberSaveable and can be persisted successfully.
     */
    @Test
    fun presetDialog_stateSurvivesRecreation_andSavesEditedPresets() = runTest {
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        // Open the presets dialog
        val presetsTitle = context.getString(R.string.settings_custom_presets_title)
        composeTestRule.onNodeWithText(presetsTitle).performScrollTo().performClick()

        val dialogTitle = context.getString(R.string.dialog_presets_title)
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Edit preset 1 (5 -> 10) and preset 2 (15 -> 20)
        composeTestRule.onNode(hasText("5") and hasSetTextAction()).performTextReplacement("10")
        composeTestRule.onNode(hasText("15") and hasSetTextAction()).performTextReplacement("20")

        // Emulate screen / activity recreation
        restorationTester.emulateSavedInstanceStateRestore()

        // Dialog should remain open after recreation
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Verify edited values persisted across recreation rather than re-reading initial presets
        composeTestRule.onNode(hasText("10") and hasSetTextAction()).assertIsDisplayed()
        composeTestRule.onNode(hasText("20") and hasSetTextAction()).assertIsDisplayed()

        // Click Save button
        val saveButtonText = context.getString(R.string.dialog_btn_save)
        composeTestRule.onNodeWithText(saveButtonText).performClick()

        composeTestRule.waitForIdle()

        // Verify that updated presets (10, 20, 30, 60) were saved
        verify(mockDataSource).saveCustomPresets(eq(listOf(10L, 20L, 30L, 60L)))
    }

    @Test
    fun toggleRows_fullRowTap_persistsUpdatedPreferences() = runTest {
        composeTestRule.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        // Commitment mode (default false -> true)
        composeTestRule.onNodeWithText(context.getString(R.string.settings_commitment_mode_title))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        verify(mockDataSource).saveCommitmentMode(true)

        // Sunset mode (default true -> false)
        composeTestRule.onNodeWithText(context.getString(R.string.settings_sunset_mode_title))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        verify(mockDataSource).saveSunsetMode(false)

        // Sunset audio alert (default true -> false)
        composeTestRule.onNodeWithText(context.getString(R.string.settings_sunset_audio_title))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        verify(mockDataSource).saveSunsetAudioEnabled(false)
    }

    @Test
    fun sunsetDurationDropdown_selectingOption_persistsDuration() = runTest {
        composeTestRule.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_sunset_duration_title))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_sunset_duration_format, 45L))
            .performClick()
        composeTestRule.waitForIdle()

        verify(mockDataSource).saveSunsetDuration(45L)
    }

    @Test
    fun runningTimer_disablesToggleRows() = runTest {
        repository.updateState(
            TimerState.Running(
                totalSeconds = 300L,
                remainingSeconds = 250L,
                startedAtMillis = 1_000L
            )
        )

        composeTestRule.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_commitment_mode_title))
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        verify(mockDataSource, never()).saveCommitmentMode(any())
    }

    @Test
    fun presetDialog_invalidInput_showsErrorAndDoesNotSave() = runTest {
        composeTestRule.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_custom_presets_title))
            .performScrollTo()
            .performClick()

        composeTestRule.onNode(hasText("5") and hasSetTextAction()).performTextReplacement("999")
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_btn_save)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(context.getString(R.string.dialog_invalid_input)).assertExists()
        verify(mockDataSource, never()).saveCustomPresets(any())
    }

    @Test
    fun copyDebugInfo_copiesFormattedSnapshotToClipboard() = runTest {
        composeTestRule.setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    timerViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_about_debug_info))
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        val clipboard = context.getSystemService(ClipboardManager::class.java)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            clipboard?.primaryClip != null
        }
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assert(clipText.contains("App: Nightjar "))
        assert(clipText.contains("Android: "))
        assert(clipText.contains("Permissions: "))
    }
}
