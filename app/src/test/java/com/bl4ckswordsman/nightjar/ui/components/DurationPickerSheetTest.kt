package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DurationPickerSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "sv")
    fun durationPickerSheet_displaysSwedishStrings_whenLocaleIsSv() {
        composeTestRule.setContent {
            NightjarTheme {
                DurationPickerSheet(
                    initialSeconds = 0L,
                    onDismissRequest = {},
                    onConfirm = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ange anpassad tid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Timmar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Minuter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sekunder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rensa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ställ in timer").assertIsNotEnabled()
    }
}
