package com.bl4ckswordsman.nightjar.ui.components.liquid

import android.graphics.RuntimeShader
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidShaderCompileTest {

    @Test
    fun liquidMetaballAgslCompiles() {
        val shader = RuntimeShader(LIQUID_METABALL_AGSL)
        assertNotNull(shader)
    }
}
