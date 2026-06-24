
# ![Nightjar Banner](.github/assets/banner.png)

Nightjar is a Material 3 Expressive lock timer application for Android designed to help parents manage device screen time with playful, intuitive controls.

## Features
- **Expressive Zen Timer Dial**: A clean, interactive circular dial to easily adjust and track active countdowns.
- **Duration Picker Sheet**: A playful, Material 3 bottom sheet for precise hour, minute, and second configurations.
- **Sunset warning (Rising Wave)**: A fullscreen, gyroscope-responsive liquid wave overlay that rises from the bottom to gently alert children when their time is almost up.
- **Collapsing Settings App Bar**: Collapses dynamically on scroll.

## Download

### Install via Obtainium

[![Get it on Obtainium](.github/assets/badge_obtainium.png)](http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/JoestarLabs/nightjar/releases)

### GitHub Release

Download a GitHub actions build (scanned by 🛡VirusTotal) here:
<p>
  <a href="https://github.com/JoestarLabs/nightjar/releases/latest"><img src="https://img.shields.io/github/v/release/JoestarLabs/nightjar" alt="GitHub release (latest by date)"></a>
</p>

## Building from Source
### Build Requirements
- Android SDK (API level 33+)
- JDK 17
- Gradle 9+

### Build release APK
To compile an unsigned release version of the application:
```bash
./gradlew assembleRelease
```
