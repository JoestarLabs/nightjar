package com.bl4ckswordsman.nightjar.ui.components.liquid

import android.graphics.RuntimeShader
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language

@Language("AGSL")
internal const val LIQUID_CAUSTICS_AGSL = """
    uniform float uBaseY;
    uniform float uTime;
    uniform float uDepthScale;
    uniform half3 uTop;
    uniform half3 uMid;
    uniform half3 uDeep;

    half4 main(float2 p) {
        float depth = max(p.y - uBaseY, 0.0);
        float absorb = exp(-depth * uDepthScale);
        float2 q = p * 0.012;
        float c = sin(q.x * 3.1 + uTime * 1.3 + sin(q.y * 2.3 + uTime));
        c = pow(clamp(0.5 + 0.5 * c * sin(q.y * 2.7 - uTime * 0.9), 0.0, 1.0), 6.0);
        half3 col = mix(uDeep, mix(uMid, uTop, half(absorb)), half(absorb)) + half(c * absorb * 0.35);
        return half4(col, half(mix(0.97, 0.88, absorb)));
    }
"""

private const val TAG = "LiquidShader"

/**
 * Holds a compiled AGSL caustic [RuntimeShader] and its reusable [ShaderBrush],
 * or `null` if shader compilation fails on the current GPU/driver or test environment.
 */
internal class LiquidShaderHolder(
    private val shader: RuntimeShader,
    val brush: ShaderBrush,
) {
    fun update(
        baseY: Float,
        timeSec: Float,
        depthScale: Float,
        top: Color,
        mid: Color,
        deep: Color,
    ) {
        shader.setFloatUniform("uBaseY", baseY)
        shader.setFloatUniform("uTime", timeSec)
        shader.setFloatUniform("uDepthScale", depthScale)
        shader.setFloatUniform("uTop", top.red, top.green, top.blue)
        shader.setFloatUniform("uMid", mid.red, mid.green, mid.blue)
        shader.setFloatUniform("uDeep", deep.red, deep.green, deep.blue)
    }
}

@Composable
internal fun rememberLiquidShaderHolder(enabled: Boolean): LiquidShaderHolder? {
    return remember(enabled) {
        if (!enabled) return@remember null
        runCatching {
            val shader = RuntimeShader(LIQUID_CAUSTICS_AGSL)
            LiquidShaderHolder(shader = shader, brush = ShaderBrush(shader))
        }.onFailure { error ->
            Log.w(TAG, "AGSL liquid shader unavailable; falling back to gradient brush", error)
        }.getOrNull()
    }
}
