package com.bl4ckswordsman.nightjar.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.SunsetAnimation
import kotlinx.coroutines.delay

/**
 * Fullscreen interactive preview dialog for sunset warning animations.
 *
 * Simulates a 10-second sunset warning progression with active device tilt tracking,
 * allowing instant side-by-side comparison of all animation styles.
 *
 * Choosing preview chips updates local dialog state only, without altering persisted settings
 * or triggering overlay permission dialogs until confirmed via [onApplyAnimation].
 *
 * @param initialAnimation The currently configured sunset warning animation.
 * @param onApplyAnimation Invoked when the user confirms and applies the selected animation style.
 * @param onDismiss Invoked when closing the preview dialog without saving changes.
 */
@Composable
fun SunsetAnimationPreviewDialog(
    initialAnimation: SunsetAnimation,
    onApplyAnimation: (SunsetAnimation) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val accelerometer = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    var currentTilt by remember { mutableFloatStateOf(0f) }
    var currentGravityAngleRad by remember { mutableFloatStateOf(0f) }
    var previewAnimation by remember { mutableStateOf(initialAnimation) }

    // Accelerometer sensor lifecycle bound strictly to preview visibility
    DisposableEffect(sensorManager, accelerometer) {
        if (sensorManager == null || accelerometer == null) return@DisposableEffect onDispose {}

        var filteredGx = 0f
        var filteredGy = 9.8f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val x = event.values[0]
                val y = event.values[1]
                filteredGx = filteredGx * 0.85f + x * 0.15f
                filteredGy = filteredGy * 0.85f + y * 0.15f
                currentTilt = (filteredGx / 5.5f).coerceIn(-1f, 1f)
                val xyMagnitude = kotlin.math.hypot(filteredGx, filteredGy)
                if (xyMagnitude > 1.2f) {
                    currentGravityAngleRad = kotlin.math.atan2(-filteredGx, filteredGy)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Looping simulated countdown timeline (10s countdown with 1.2s pause at completion)
    val totalSeconds = 10L
    var loopStartedAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var remainingSeconds by remember { mutableLongStateOf(totalSeconds) }

    LaunchedEffect(previewAnimation) {
        // Reset loop smoothly when switching animation styles
        loopStartedAtMillis = System.currentTimeMillis()
        remainingSeconds = totalSeconds
    }

    LaunchedEffect(Unit) {
        val totalMillis = totalSeconds * 1000L
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - loopStartedAtMillis
            if (elapsed >= totalMillis + 1200L) {
                loopStartedAtMillis = System.currentTimeMillis()
                remainingSeconds = totalSeconds
            } else {
                remainingSeconds = ((totalMillis - elapsed) / 1000L).coerceIn(0L, totalSeconds)
            }
            delay(50L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // Live animation layer
            when (previewAnimation) {
                SunsetAnimation.FLUID_LIQUID -> {
                    FluidLiquidOverlay(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        gravityAngleRad = currentGravityAngleRad,
                        startedAtMillis = loopStartedAtMillis
                    )
                }
                SunsetAnimation.MATERIAL_SHAPES -> {
                    MaterialShapesOverlay(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        gravityAngleRad = currentGravityAngleRad,
                        startedAtMillis = loopStartedAtMillis
                    )
                }
                SunsetAnimation.SNOWFLAKES -> {
                    SnowflakesOverlay(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        gravityAngleRad = currentGravityAngleRad,
                        startedAtMillis = loopStartedAtMillis
                    )
                }
                SunsetAnimation.CLASSIC_WAVE -> {
                    RisingWaveOverlay(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        tilt = currentTilt,
                        startedAtMillis = loopStartedAtMillis
                    )
                }
                SunsetAnimation.OFF -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                            tonalElevation = 6.dp,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_sunset_animation_preview_off_desc),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }
            }

            // Top control bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_sunset_animation_preview),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApplyAnimation(previewAnimation) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.settings_sunset_animation_preview_apply),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.settings_sunset_animation_preview_close)
                        )
                    }
                }
            }

            // Bottom style switcher pill
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        SunsetAnimation.FLUID_LIQUID to R.string.settings_sunset_animation_fluid_liquid,
                        SunsetAnimation.MATERIAL_SHAPES to R.string.settings_sunset_animation_material_shapes,
                        SunsetAnimation.SNOWFLAKES to R.string.settings_sunset_animation_snowflakes,
                        SunsetAnimation.CLASSIC_WAVE to R.string.settings_sunset_animation_classic_wave,
                        SunsetAnimation.OFF to R.string.settings_sunset_animation_off
                    ).forEach { (anim, labelRes) ->
                        val isSelected = previewAnimation == anim
                        FilterChip(
                            selected = isSelected,
                            onClick = { previewAnimation = anim },
                            label = { Text(stringResource(labelRes)) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
