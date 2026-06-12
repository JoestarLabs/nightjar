@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.bl4ckswordsman.nightjar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.bl4ckswordsman.nightjar.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Expressive Material 3 Title component that performs a stretchy width & weight
 * variable font animation on entry. Tapping on the title triggers the animation again.
 */
@Composable
fun AnimatedAppTitle(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // ── Title stretch/weight animatables ───────────────────────────────────────
    val titleWidth = remember { Animatable(25f) }
    val titleWeight = remember { Animatable(100f) }

    suspend fun animateTitle() {
        coroutineScope {
            launch { titleWidth.snapTo(25f) }
            launch { titleWeight.snapTo(100f) }
        }
        coroutineScope {
            launch {
                titleWidth.animateTo(
                    targetValue = 120f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessLow,
                    )
                )
            }
            launch {
                titleWeight.animateTo(
                    targetValue = 800f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness    = Spring.StiffnessLow,
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        animateTitle()
    }

    val animatedTitleFontFamily = remember(titleWidth.value, titleWeight.value) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.width(titleWidth.value),
                    FontVariation.weight(titleWeight.value.toInt())
                )
            )
        )
    }

    val draggableState = rememberDraggableState { delta ->
        val currentVal = titleWidth.value
        val newVal = (currentVal + delta * 0.5f).coerceIn(25f, 150f)
        scope.launch {
            titleWidth.snapTo(newVal)
        }
    }

    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = animatedTitleFontFamily
        ),
        modifier = modifier.draggable(
            orientation = Orientation.Horizontal,
            state = draggableState,
            onDragStopped = {
                scope.launch {
                    titleWidth.animateTo(
                        targetValue = 120f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium,
                        )
                    )
                }
            }
        )
    )
}
