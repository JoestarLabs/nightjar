package com.bl4ckswordsman.nightjar.ui.components

import androidx.annotation.StringRes
import com.bl4ckswordsman.nightjar.R
import com.bl4ckswordsman.nightjar.data.SunsetAnimation

/**
 * Returns the localized display label string resource for this [SunsetAnimation].
 */
@StringRes
fun SunsetAnimation.labelRes(): Int = when (this) {
    SunsetAnimation.FLUID_LIQUID -> R.string.settings_sunset_animation_fluid_liquid
    SunsetAnimation.CLASSIC_WAVE -> R.string.settings_sunset_animation_classic_wave
    SunsetAnimation.MATERIAL_SHAPES -> R.string.settings_sunset_animation_material_shapes
    SunsetAnimation.SNOWFLAKES -> R.string.settings_sunset_animation_snowflakes
    SunsetAnimation.OFF -> R.string.settings_sunset_animation_off
}
