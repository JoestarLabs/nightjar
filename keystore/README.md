# Keystore Setup for Nightjar

This directory is intentionally empty in the repository. Keystore files are **never** committed to
git.

## Generating Your Release Keystore (one-time setup)

Run the following command on your development machine:

```bash
keytool -genkey -v \
  -keystore nightjar-release.keystore \
  -alias nightjar \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Keep the resulting `nightjar-release.keystore` file **safe, offline, and backed up**.
Losing it means you can never publish updates to the same Play Store / IzzyOnDroid listing.

---

## Setting Up GitHub Secrets

1. **Base64-encode the keystore:**
   ```bash
   base64 -w 0 nightjar-release.keystore > nightjar-release.keystore.b64
   ```

2. **Add the following secrets to your GitHub repository** (`Settings → Secrets → Actions`):

   | Secret name                 | Value                                    |
      |-----------------------------|------------------------------------------|
   | `SIGNING_KEYSTORE`          | Contents of `nightjar-release.keystore.b64` |
   | `SIGNING_KEYSTORE_PASSWORD` | The keystore password you chose above    |
   | `SIGNING_KEY_ALIAS`         | `nightjar` (or whatever alias you used)  |
   | `SIGNING_KEY_PASSWORD`      | The key password you chose above         |
   | `VT_API_KEY`                | Your VirusTotal API key                  |

3. **Delete `nightjar-release.keystore.b64`** from disk after copying the value to GitHub.

---

## Local Release Builds (optional)

If you want to build a signed release locally, set the environment variables before running Gradle:

```bash
export SIGNING_KEYSTORE_PATH=/path/to/nightjar-release.keystore
export SIGNING_KEYSTORE_PASSWORD=your_password
export SIGNING_KEY_ALIAS=nightjar
export SIGNING_KEY_PASSWORD=your_key_password

./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release.apk`

---

## How It Works in CI

The `release-build.yml` workflow:

1. Decodes `SIGNING_KEYSTORE` (base64) → `/tmp/nightjar.keystore`
2. Sets `SIGNING_KEYSTORE_PATH` env var so Gradle reads it in `signingConfigs`
3. Runs `./gradlew assembleRelease` (Gradle signs during build)
4. Runs `ilharp/sign-android-release` for secondary verification
5. Creates a GitHub Release with the signed APK attached
6. Cleans up the temp keystore file

The built-in `GITHUB_TOKEN` is used — no personal access token (PAT) required.
