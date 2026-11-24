/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Animation Module
 * File: LiquidRippleModifier.kt
 * Description: AGSL liquid ripple displacement modifier.
 *
 * Attribution:
 * Derived directly from Sameera Wijerathna's Essentials project (sameerasw/essentials v19.0),
 * specifically the pull-down liquid ripple transition to the device details screen.
 * Licensed under MIT License.
 */

package com.bl4ckswordsman.nightjar.ui.animation

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val LIQUID_RIPPLE_AGSL = """
    uniform shader inputShader;
    uniform float2 uOrigin;
    uniform float uTime;
    uniform float uAmplitude;
    uniform float uFrequency;
    uniform float uDecay;
    uniform float uSpeed;

    half4 main(float2 fragCoord) {
        float2 pos = fragCoord;
        float2 delta = pos - uOrigin;
        float distance = length(delta);
        float delay = distance / uSpeed;
        float time = max(0.0, uTime - delay);
        
        float wave1 = uAmplitude * sin(uFrequency * time) * exp(-uDecay * time);
        
        float subTime = max(0.0, time - 0.22);
        float wave2 = (uAmplitude * 0.55) * sin(uFrequency * 1.15 * subTime) * exp(-(uDecay * 0.8) * subTime);
        
        float totalWave = wave1 + wave2;
        float2 n = distance > 0.001 ? delta / distance : float2(0.0, 0.0);
        float2 newPos = pos + totalWave * n;
        
        float highlight = 0.16 * (totalWave / max(1.0, uAmplitude));
        
        return inputShader.eval(newPos) + half4(highlight, highlight, highlight, 0.0);
    }
"""

/**
 * Applies a hardware-accelerated AGSL liquid ripple distortion radiating from [origin].
 *
 * @param trigger Incrementing integer value to trigger a new ripple animation.
 * @param origin The center offset coordinates from which the ripple radiates.
 * @param enabled Whether the ripple effect is enabled.
 * @param durationMillis Duration of the ripple animation in milliseconds.
 * @param amplitudeDp Maximum wave displacement amplitude in dp.
 * @param frequency Spatial frequency of the ripple wave.
 * @param decay Rate at which the wave decays over time and distance.
 * @param speedDp Radial propagation speed in dp per second.
 */
@Composable
fun Modifier.liquidRipple(
    trigger: Int,
    origin: Offset,
    enabled: Boolean = true,
    durationMillis: Int = 2800,
    amplitudeDp: Float = 34f,
    frequency: Float = 12f,
    decay: Float = 4.5f,
    speedDp: Float = 1400f,
): Modifier {
    val density = LocalDensity.current
    val animTime = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            animTime.snapTo(0f)
            animTime.animateTo(
                targetValue = durationMillis / 1000f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
            animTime.snapTo(0f)
        }
    }

    if (!enabled) return this

    val shader = remember { RuntimeShader(LIQUID_RIPPLE_AGSL) }

    return this.graphicsLayer {
        val currentTime = animTime.value
        val maxTime = durationMillis / 1000f
        if (currentTime > 0f && currentTime < maxTime) {
            val densityVal = density.density
            val amplitude = amplitudeDp * densityVal
            val speed = speedDp * densityVal

            val ox = if (origin != Offset.Zero && origin.isSpecified) origin.x else size.width / 2f
            val oy = if (origin != Offset.Zero && origin.isSpecified) origin.y else size.height / 2f

            shader.setFloatUniform("uOrigin", ox, oy)
            shader.setFloatUniform("uTime", currentTime)
            shader.setFloatUniform("uAmplitude", amplitude)
            shader.setFloatUniform("uFrequency", frequency)
            shader.setFloatUniform("uDecay", decay)
            shader.setFloatUniform("uSpeed", speed)

            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader").asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}
