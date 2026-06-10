package com.bl4ckswordsman.nightjar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Static fallback color schemes ────────────────────────────────────────────
// Used on devices without Dynamic Color support (Android < 12).

private val ZenLightColorScheme = lightColorScheme(
    primary          = BambooGreen40,
    onPrimary        = Color.White,
    primaryContainer = BambooGreen90,
    onPrimaryContainer = BambooGreen10,

    secondary          = WashiClay40,
    onSecondary        = Color.White,
    secondaryContainer = WashiClay90,
    onSecondaryContainer = WashiClay10,

    tertiary          = ToriiRed40,
    onTertiary        = Color.White,
    tertiaryContainer = ToriiRed90,
    onTertiaryContainer = ToriiRed10,

    error          = ErrorRed,
    errorContainer = ErrorRedContainer,
    onError        = Color.White,
    onErrorContainer = OnErrorRedContainer,

    background = SurfaceWarm,
    onBackground = NeutralVariant10,

    surface        = SurfaceWarm,
    onSurface      = NeutralVariant10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,

    outline       = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim          = InkBlack,
)

private val ZenDarkColorScheme = darkColorScheme(
    primary          = BambooGreen80,
    onPrimary        = BambooGreen20,
    primaryContainer = BambooGreen30,
    onPrimaryContainer = BambooGreen90,

    secondary          = WashiClay80,
    onSecondary        = WashiClay20,
    secondaryContainer = WashiClay30,
    onSecondaryContainer = WashiClay90,

    tertiary          = ToriiRed80,
    onTertiary        = ToriiRed20,
    tertiaryContainer = ToriiRed30,
    onTertiaryContainer = ToriiRed90,

    error          = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError        = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    background = SurfaceInk,
    onBackground = NeutralVariant90,

    surface        = SurfaceInk,
    onSurface      = NeutralVariant90,
    surfaceVariant = NeutralVariant20,
    onSurfaceVariant = NeutralVariant80,

    outline       = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim          = InkBlack,
)

// ── NightjarTheme ─────────────────────────────────────────────────────────────
@Composable
fun NightjarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses the device wallpaper palette on Android 12+.
    // Falls back to zen palette on older devices.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> ZenDarkColorScheme
        else      -> ZenLightColorScheme
    }

    // Sync status bar / nav bar icon colours with the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NightjarTypography,
        shapes      = NightjarShapes,
        content     = content
    )
}