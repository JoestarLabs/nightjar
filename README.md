
# ![Nightjar Banner](.github/assets/banner.png)

Nightjar is a Material 3 Expressive lock timer application for Android designed for flexible, standalone screen-time limits—whether for personal focus, temporary device sharing, or quick boundaries for kids when setups like Family Link aren't practical.

## Features
- **Expressive Zen Timer Dial**: A clean, interactive circular dial with a Material 3 Expressive wavy progress ring to adjust and track active countdowns.
- **Duration Picker Sheet**: A playful, Material 3 bottom sheet for precise hour, minute, and second configurations.
- **Sunset Warning Suite**: Fullscreen warning overlays (Fluid Liquid, Material Shapes, Snowflakes, and Classic Wave) with live preview and an optional gentle audio chime to alert users before the screen locks.
- **Commitment Mode & Emergency Unlock**: Prevent canceling active timers prematurely, with a 5-second hold and slide-to-unlock emergency override.
- **Live Update Notifications**: Ongoing countdown notification with Android 16+ Live Update status-bar chip and 1-minute urgency visual cue.
- **Collapsing Settings App Bar**: Collapses dynamically on scroll with per-app language selection (English / Svenska).
- **Open Source License Tracking**: Built-in dependencies and license viewer powered by AboutLibraries.

## Download

### Install via Obtainium

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/JoestarLabs/nightjar/releases">
  <img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="100" />
</a>

### GitHub Release

Download a GitHub actions build (scanned by 🛡VirusTotal) here:
<p>
  <a href="https://github.com/JoestarLabs/nightjar/releases/latest"><img src="https://img.shields.io/github/v/release/JoestarLabs/nightjar" alt="GitHub release (latest by date)"></a>
</p>

## Building from Source
### Build Requirements
- Android SDK (API level 33+)
- JDK 17+
- Gradle 9+

### Build release APK
To compile an unsigned release version of the application:
```bash
./gradlew assembleRelease
```

### Run checks
To run unit tests and Android Lint locally:
```bash
./gradlew lint test
```
