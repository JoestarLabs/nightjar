package com.bl4ckswordsman.nightjar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugInfoFormatterTest {

    @Test
    fun `formatOneUiVersion parses major minor and patch versions`() {
        assertEquals("One UI 8.0", DebugInfoFormatter.formatOneUiVersion("80000"))
        assertEquals("One UI 6.1.1", DebugInfoFormatter.formatOneUiVersion("60101"))
        assertEquals("One UI 7.0", DebugInfoFormatter.formatOneUiVersion(" 70000 "))
        assertNull(DebugInfoFormatter.formatOneUiVersion(null))
        assertNull(DebugInfoFormatter.formatOneUiVersion(""))
        assertNull(DebugInfoFormatter.formatOneUiVersion("invalid"))
        assertNull(DebugInfoFormatter.formatOneUiVersion("500"))
    }

    @Test
    fun `detectRomVersion identifies OEM skins and returns null on stock`() {
        assertEquals(
            "One UI 8.0",
            DebugInfoFormatter.detectRomVersion("samsung") { key ->
                if (key == "ro.build.version.oneui") "80000" else null
            }
        )
        assertEquals(
            "HyperOS OS2.0.1.0",
            DebugInfoFormatter.detectRomVersion("Xiaomi") { key ->
                if (key == "ro.mi.os.version.name") "OS2.0.1.0" else null
            }
        )
        assertEquals(
            "OxygenOS 15.0",
            DebugInfoFormatter.detectRomVersion("OnePlus") { key ->
                if (key == "ro.build.version.oplusrom") "15.0" else null
            }
        )
        assertEquals(
            "LineageOS 22.1",
            DebugInfoFormatter.detectRomVersion("Google") { key ->
                if (key == "ro.lineage.version") "22.1" else null
            }
        )
        assertNull(DebugInfoFormatter.detectRomVersion("Google") { null })
    }

    @Test
    fun `format renders clean diagnostic block with and without ROM version`() {
        val withRom = DebugInfo(
            appVersionName = "0.0.6",
            appVersionCode = 6,
            buildType = "release",
            androidRelease = "16",
            sdkInt = 36,
            securityPatch = "2026-09-05",
            manufacturer = "samsung",
            brand = "samsung",
            model = "SM-S921B",
            device = "e1s",
            buildDisplay = "BP2A.250605.031",
            romVersion = "One UI 8.0",
            appLocale = "en",
            uiTheme = "Light",
            deviceAdminEnabled = true,
            notificationsEnabled = true,
            overlayEnabled = true,
            promotedNotificationsAllowed = true,
        )

        val formatted = DebugInfoFormatter.format(withRom)
        assertTrue(formatted.contains("App: Nightjar 0.0.6 (6, release)"))
        assertTrue(formatted.contains("Android: 16 (API 36)"))
        assertTrue(formatted.contains("Device: samsung SM-S921B (samsung/e1s)"))
        assertTrue(formatted.contains("ROM: One UI 8.0"))
        assertTrue(formatted.contains("PromotedNotifications=true"))

        val withoutRom = withRom.copy(romVersion = null, promotedNotificationsAllowed = null)
        val formattedStock = DebugInfoFormatter.format(withoutRom)
        assertFalse(formattedStock.contains("ROM:"))
        assertFalse(formattedStock.contains("PromotedNotifications="))
    }
}
