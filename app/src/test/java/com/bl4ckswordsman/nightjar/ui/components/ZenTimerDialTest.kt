package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying touch interaction, layout constraints, and gesture behavior of [ZenTimerDial].
 */
@RunWith(RobolectricTestRunner::class)
class ZenTimerDialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Verifies that dragging an idle dial constrained below its requested size calculates
     * angles using the constrained center rather than the unconstrained adapted size.
     */
    @Test
    fun idleDial_whenConstrainedBelowRequestedSize_calculatesDurationFromConstrainedCenter() {
        var recordedSeconds: Long? = null

        composeTestRule.setContent {
            // Constrain dial parent to 160.dp x 160.dp while requested size is 300.dp
            Box(modifier = Modifier.size(160.dp)) {
                ZenTimerDial(
                    selectedSeconds = 0L,
                    runningSeconds = null,
                    maxSeconds = 3600L,
                    size = 300.dp,
                    onSecondsChanged = { recordedSeconds = it },
                    modifier = Modifier.testTag("zen_dial")
                )
            }
        }

        // Drag from center to the right edge (3 o'clock / 90 degrees -> 900 seconds)
        composeTestRule.onNodeWithTag("zen_dial").performTouchInput {
            swipe(
                start = center,
                end = Offset(width.toFloat() - 1f, center.y),
                durationMillis = 200
            )
        }

        composeTestRule.waitForIdle()

        // At 3 o'clock (90 degrees), 90 / 360 * 3600 = 900 seconds.
        // If center had been derived from unconstrained 300.dp, the resulting angle would differ significantly.
        val result = recordedSeconds
        assertTrue("Expected recordedSeconds to be non-null after drag", result != null)
        assertEquals(900L, result)
    }

    /**
     * Verifies that updating the [ZenTimerDial] onSecondsChanged callback lambda during an active
     * drag does not cancel or reset the gesture coroutine and delivers subsequent drag events to the
     * replacement callback without requiring a new pointer-down event.
     */
    @Test
    fun idleDial_whenCallbackChanges_invokesUpdatedCallback() {
        var callback1Value: Long? = null
        var callback2Value: Long? = null

        var callback: (Long) -> Unit by mutableStateOf({ callback1Value = it })

        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                ZenTimerDial(
                    selectedSeconds = 0L,
                    runningSeconds = null,
                    maxSeconds = 3600L,
                    size = 200.dp,
                    onSecondsChanged = callback,
                    modifier = Modifier.testTag("zen_dial_callback")
                )
            }
        }

        composeTestRule.onNodeWithTag("zen_dial_callback").performTouchInput {
            down(center)
            moveTo(Offset(width.toFloat() - 1f, center.y))
        }

        // Update the callback instance
        callback = { callback2Value = it }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("zen_dial_callback").performTouchInput {
            moveTo(Offset(center.x, height.toFloat() - 1f))
            up()
        }

        composeTestRule.waitForIdle()

        assertEquals(900L, callback1Value)
        assertEquals(1800L, callback2Value)
    }
}
