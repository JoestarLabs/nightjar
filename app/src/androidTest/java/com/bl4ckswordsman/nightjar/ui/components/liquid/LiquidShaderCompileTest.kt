package com.bl4ckswordsman.nightjar.ui.components.liquid

import android.graphics.RuntimeShader
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidShaderCompileTest {

    @Test
    fun liquidCausticsAgslCompiles() {
        val shader = RuntimeShader(LIQUID_CAUSTICS_AGSL)
        assertNotNull(shader)
    }
}
