package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bl4ckswordsman.nightjar.ui.theme.NotoSerifJPFamily

/**
 * Formats [seconds] as HH:MM:SS or MM:SS and displays it as a large
 * serif time string. Used in the centre of [ZenTimerDial].
 */
@Composable
fun TimerText(
    seconds: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%02d:%02d:%02d".format(h, m, s)
               else       "%02d:%02d".format(m, s)

    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(
            fontFamily = NotoSerifJPFamily,
            fontWeight = if (isRunning) FontWeight.Normal else FontWeight.Light,
            color = if (isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        ),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
