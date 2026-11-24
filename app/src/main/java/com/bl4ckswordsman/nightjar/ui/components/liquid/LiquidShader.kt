package com.bl4ckswordsman.nightjar.ui.components.liquid

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import org.intellij.lang.annotations.Language

@Language("AGSL")
internal const val LIQUID_METABALL_AGSL = """
    uniform shader inputShader;
    uniform float uThreshold;
    uniform float uNormalStep;
    uniform float uShadingEnabled;
    uniform half3 uTop;
    uniform half3 uMid;
    uniform half3 uDeep;

    half4 main(float2 p) {
        float d = inputShader.eval(p).a;
        if (d < uThreshold - 0.04) {
            return half4(0.0);
        }

        float edge = smoothstep(uThreshold - 0.04, uThreshold + 0.04, d);
        float depth = clamp((d - uThreshold) / max(1.0 - uThreshold, 0.001), 0.0, 1.0);

        half3 col = mix(uTop, mix(uMid, uDeep, half(depth)), half(depth * 0.78));

        if (uShadingEnabled > 0.5) {
            float dx = inputShader.eval(p + float2(uNormalStep, 0.0)).a
                     - inputShader.eval(p - float2(uNormalStep, 0.0)).a;
            float dy = inputShader.eval(p + float2(0.0, uNormalStep)).a
                     - inputShader.eval(p - float2(0.0, uNormalStep)).a;
            float3 n = normalize(float3(-dx, -dy, 0.14));

            float spec = pow(max(dot(n, normalize(float3(-0.35, -0.65, 0.68))), 0.0), 10.0);
            float fresnel = pow(1.0 - max(n.z, 0.0), 2.2);
            col += half3(spec * 0.14) + uTop * half(fresnel * 0.24);
        }

        float alpha = mix(0.60, 0.82, depth) * edge;
        return half4(col * half(alpha), half(alpha));
    }
"""

private const val TAG = "LiquidShader"

/**
 * Holds a compiled AGSL metaball [RuntimeShader] or `null` if compilation fails on a device/driver.
 */
internal class LiquidMetaballShaderHolder(
    private val shader: RuntimeShader,
) {
    fun buildRenderEffect(
        threshold: Float,
        normalStepPx: Float,
        shadingEnabled: Boolean,
        top: Color,
        mid: Color,
        deep: Color,
    ): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("uThreshold", threshold)
        shader.setFloatUniform("uNormalStep", normalStepPx)
        shader.setFloatUniform("uShadingEnabled", if (shadingEnabled) 1f else 0f)
        shader.setFloatUniform("uTop", top.red, top.green, top.blue)
        shader.setFloatUniform("uMid", mid.red, mid.green, mid.blue)
        shader.setFloatUniform("uDeep", deep.red, deep.green, deep.blue)
        return RenderEffect.createRuntimeShaderEffect(shader, "inputShader").asComposeRenderEffect()
    }
}

@Composable
internal fun rememberLiquidMetaballShaderHolder(enabled: Boolean): LiquidMetaballShaderHolder? {
    return remember(enabled) {
        if (!enabled) return@remember null
        runCatching {
            LiquidMetaballShaderHolder(RuntimeShader(LIQUID_METABALL_AGSL))
        }.onFailure { error ->
            Log.w(TAG, "AGSL liquid metaball shader unavailable; using fallback rendering", error)
        }.getOrNull()
    }
}

/**
 * Generates a cached radial quartic-falloff metaball sprite (`(1 - r^2)^2`) for additive density summing.
 */
internal fun createMetaballSpriteBitmap(sizePx: Int = 64): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(sizePx * sizePx)
    val half = (sizePx - 1) * 0.5f
    val invHalf = 1f / half
    val peakAlpha = 0.45f

    for (y in 0 until sizePx) {
        val ny = (y - half) * invHalf
        val row = y * sizePx
        for (x in 0 until sizePx) {
            val nx = (x - half) * invHalf
            val r2 = nx * nx + ny * ny
            if (r2 < 1f) {
                val falloff = (1f - r2) * (1f - r2) * peakAlpha
                val channel = (falloff * 255f + 0.5f).toInt().coerceIn(0, 255)
                pixels[row + x] = AndroidColor.argb(channel, 255, 255, 255)
            } else {
                pixels[row + x] = 0
            }
        }
    }
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bitmap.asImageBitmap()
}
