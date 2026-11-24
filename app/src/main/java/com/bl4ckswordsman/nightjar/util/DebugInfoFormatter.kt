package com.bl4ckswordsman.nightjar.util

import android.app.LocaleManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.bl4ckswordsman.nightjar.BuildConfig
import com.bl4ckswordsman.nightjar.receiver.LockDeviceAdminReceiver
import java.io.IOException

/**
 * Sanitized diagnostic snapshot of the app, device, OS, and permission states for bug reports.
 */
data class DebugInfo(
    val appVersionName: String,
    val appVersionCode: Int,
    val buildType: String,
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val buildDisplay: String,
    val romVersion: String?,
    val appLocale: String,
    val uiTheme: String,
    val deviceAdminEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val overlayEnabled: Boolean,
    val promotedNotificationsAllowed: Boolean?,
)

/**
 * Collects and formats non-PII diagnostic device/app information for bug-report clipboard sharing.
 */
object DebugInfoFormatter {

    /**
     * Collects live diagnostic metadata from [context] and [Build].
     * Safe to invoke off the main thread because it may query `getprop` for OEM ROM version.
     */
    fun collect(
        context: Context,
        getProperty: (String) -> String? = ::readSystemProperty,
    ): DebugInfo {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val localeTag = localeManager?.applicationLocales
            ?.toLanguageTags()
            ?.takeIf { it.isNotEmpty() }
            ?: "system"

        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val uiTheme = when (nightMode) {
            Configuration.UI_MODE_NIGHT_YES -> "Dark"
            Configuration.UI_MODE_NIGHT_NO -> "Light"
            else -> "Unknown"
        }

        val notificationsEnabled = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val promotedAllowed = if (Build.VERSION.SDK_INT >= 36) {
            context.getSystemService(NotificationManager::class.java)
                ?.canPostPromotedNotifications()
        } else {
            null
        }

        return DebugInfo(
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            buildType = BuildConfig.BUILD_TYPE,
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            buildDisplay = Build.DISPLAY.orEmpty(),
            romVersion = detectRomVersion(Build.MANUFACTURER.orEmpty(), getProperty),
            appLocale = localeTag,
            uiTheme = uiTheme,
            deviceAdminEnabled = LockDeviceAdminReceiver.isEnabled(context),
            notificationsEnabled = notificationsEnabled,
            overlayEnabled = Settings.canDrawOverlays(context),
            promotedNotificationsAllowed = promotedAllowed,
        )
    }

    /**
     * Formats [info] as a clean multi-line plain-text block suitable for pasting into issue trackers.
     */
    fun format(info: DebugInfo): String = buildString {
        appendLine("App: Nightjar ${info.appVersionName} (${info.appVersionCode}, ${info.buildType})")
        appendLine("Android: ${info.androidRelease} (API ${info.sdkInt})")
        if (info.securityPatch.isNotBlank()) {
            appendLine("Security Patch: ${info.securityPatch}")
        }
        appendLine("Device: ${info.manufacturer} ${info.model} (${info.brand}/${info.device})")
        if (!info.romVersion.isNullOrBlank()) {
            appendLine("ROM: ${info.romVersion}")
        }
        if (info.buildDisplay.isNotBlank()) {
            appendLine("Build: ${info.buildDisplay}")
        }
        appendLine("Locale: ${info.appLocale} | Theme: ${info.uiTheme}")
        append(
            "Permissions: Admin=${info.deviceAdminEnabled}, " +
                "Notifications=${info.notificationsEnabled}, " +
                "Overlay=${info.overlayEnabled}"
        )
        if (info.promotedNotificationsAllowed != null) {
            append(", PromotedNotifications=${info.promotedNotificationsAllowed}")
        }
    }

    /**
     * Detects common OEM skin / custom ROM version strings from standard system properties.
     */
    internal fun detectRomVersion(
        manufacturer: String,
        getProperty: (String) -> String?,
    ): String? {
        val mfr = manufacturer.trim().lowercase()

        // Samsung One UI
        val oneUi = formatOneUiVersion(getProperty("ro.build.version.oneui"))
        if (oneUi != null) return oneUi
        if (mfr == "samsung") {
            getProperty("ro.build.PDA")?.takeIf { it.isNotBlank() }?.let { return "One UI ($it)" }
        }

        // Xiaomi / Redmi / POCO (HyperOS / MIUI)
        getProperty("ro.mi.os.version.name")?.takeIf { it.isNotBlank() }?.let { return "HyperOS $it" }
        getProperty("ro.miui.ui.version.name")?.takeIf { it.isNotBlank() }?.let { return "MIUI $it" }

        // OnePlus / OPPO / Realme
        getProperty("ro.build.version.oplusrom")?.takeIf { it.isNotBlank() }?.let {
            val label = if (mfr == "oneplus") "OxygenOS" else "ColorOS"
            return "$label $it"
        }

        // Nothing OS
        getProperty("ro.nothing.version.id")?.takeIf { it.isNotBlank() }?.let { return "Nothing OS $it" }

        // LineageOS
        getProperty("ro.lineage.version")?.takeIf { it.isNotBlank() }?.let { return "LineageOS $it" }

        return null
    }

    /**
     * Converts Samsung's integer `ro.build.version.oneui` property (e.g. `"80000"`, `"60101"`)
     * into a human-readable string (e.g. `"One UI 8.0"`, `"One UI 6.1.1"`).
     */
    internal fun formatOneUiVersion(rawVersion: String?): String? {
        val code = rawVersion?.trim()?.toIntOrNull() ?: return null
        if (code < 10000) return null
        val major = code / 10000
        val minor = (code % 10000) / 100
        val patch = code % 100
        return if (patch > 0) "One UI $major.$minor.$patch" else "One UI $major.$minor"
    }

    private fun readSystemProperty(key: String): String? = try {
        val process = ProcessBuilder("getprop", key)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (process.waitFor() == 0 && output.isNotEmpty()) output else null
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    }
}
