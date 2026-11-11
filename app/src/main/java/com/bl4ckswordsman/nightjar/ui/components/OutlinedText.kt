package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle

/**
 * Text rendered with a stroke outline behind the fill for high readability
 * over animated or moving background overlays.
 */
@Composable
fun OutlinedText(
    text: String,
    style: TextStyle,
    outlineColor: Color,
    fillColor: Color,
    outlineWidth: Float,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            style = style.copy(
                color = outlineColor,
                drawStyle = Stroke(
                    width = outlineWidth,
                    join = StrokeJoin.Round
                )
            )
        )
        Text(
            text = text,
            style = style.copy(
                color = fillColor
            )
        )
    }
}
