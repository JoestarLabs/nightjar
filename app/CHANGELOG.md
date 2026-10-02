# Changelog

## [0.1.0](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.4...nightjar-v0.1.0) (2026-10-02)


### Features

* add GitHub repository link to settings screen and update about section strings ([d24d4fa](https://github.com/JoestarLabs/nightjar/commit/d24d4fa79be01304984eea55bd5b79699111499d))
* implement emergency unlock slider to allow overriding commitment mode ([f80e64b](https://github.com/JoestarLabs/nightjar/commit/f80e64be38a8afb9a232405d3c7bcb21864cd168))
* **notification:** display error icon in status bar chip during final minute ([a2ef3e6](https://github.com/JoestarLabs/nightjar/commit/a2ef3e643d5c7e05104757c12c22433ec28dcd0f))
* **notification:** make live update chip turn amber persistently at 1-minute mark ([ab2bf2d](https://github.com/JoestarLabs/nightjar/commit/ab2bf2d0046eafbe0ff5cb273d1d5ed7789144fd))
* **sunset:** add audible chime warning on sunset transition ([a979457](https://github.com/JoestarLabs/nightjar/commit/a9794579791012f9c2b74ae4c067832747c8ff13))


### Bug Fixes

* **security:** preserve commitment mode across device reboots and updates ([12872ec](https://github.com/JoestarLabs/nightjar/commit/12872ec79a8bfb1b06416df5360d0d157dfa068d))
* **security:** preserve commitment mode across device reboots in BootReceiver ([1a41d9b](https://github.com/JoestarLabs/nightjar/commit/1a41d9b0a71e1dc93e96572f219cca2b55b3987b))
* **ui:** expand tap target for setting toggle rows ([63b03bc](https://github.com/JoestarLabs/nightjar/commit/63b03bcc6f8704d557c5ee9a3feefec8107c0413))
* **ui:** pass frequency parameter to yAtX in RisingWaveOverlay ([821e10b](https://github.com/JoestarLabs/nightjar/commit/821e10b102b7e10afec041ad6ccbb497cfca2951))
* **ui:** synchronize switch thumb press animation with row interaction source ([7f02ccc](https://github.com/JoestarLabs/nightjar/commit/7f02ccc4de99051bd712a18a29fcdd650cd9970e))


### Performance Improvements

* optimize wave animation path generation with cubic Bézier curves ([4c5ae79](https://github.com/JoestarLabs/nightjar/commit/4c5ae79579bdf21c918068daf4c851684ccba0dd))
* replace wave lineTo loop with cubic Bézier path builder in RisingWaveOverlay ([853b603](https://github.com/JoestarLabs/nightjar/commit/853b603dfa8b509df2342114c7eec94dc4a7e069))

## [0.0.4](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.3...nightjar-v0.0.4) (2026-07-29)


### Features

* integrate AboutLibraries for license management and add a dedicated dependencies screen to settings ([922c6f6](https://github.com/JoestarLabs/nightjar/commit/922c6f6beb440a63e2c90711c24c7367a4b72adc))
* **settings:** open system per-app language settings on Android 13+ ([e82133a](https://github.com/JoestarLabs/nightjar/commit/e82133adc0a7ac1decf0c4399f0124bce4d24978))


### Bug Fixes

* **timer:** synchronize timer displays and fix countdown drift ([d4fb001](https://github.com/JoestarLabs/nightjar/commit/d4fb001300c40b2da2425827e71adbaf1b402332))
* **ui:** improve text and UI scaling for accessibility ([a3a54bd](https://github.com/JoestarLabs/nightjar/commit/a3a54bd841c0bd7bd50403da5ee5661f609bfa22))
* **ui:** restore getValue imports required by Kotlin property delegates ([0b1e8cd](https://github.com/JoestarLabs/nightjar/commit/0b1e8cd32dafb79121c3f2a2404fdd78b57e9a1b))


### Performance Improvements

* **ui:** precompute tick mark radians in ZenTimerDial and clean up unused imports ([10f3817](https://github.com/JoestarLabs/nightjar/commit/10f3817307d0707df98e01e1bcebc0e0494b8fad))

## [0.0.3](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.2...nightjar-v0.0.3) (2026-06-24)


### Features

* add more interactive touch animations and input constraints to AnimatedAppTitle ([a7734ff](https://github.com/JoestarLabs/nightjar/commit/a7734ffd5a12c4e3c8f9989f8d146ae6d1f05c5a))


### Performance Improvements

* cache notification lock icon bitmap and hoist tick mark angles list ([b93d0b4](https://github.com/JoestarLabs/nightjar/commit/b93d0b4f2c56695fe7d38e0e15ed782ebf633133))


### Miscellaneous Chores

* refactor code and XML formatting for improved readability ([ed9655f](https://github.com/JoestarLabs/nightjar/commit/ed9655f7db80207147f37fb7a18b33f6116fd04c))

## [0.0.2](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.1...nightjar-v0.0.2) (2026-06-13)

### Features

* add commitment mode to prevent timer cancellation and implement a one-minute remaining
  notification
  alert ([14f70f7](https://github.com/JoestarLabs/nightjar/commit/14f70f753d0a71a86ac9bac7158cd6ad5afe0e16))
* add custom duration picker sheet and integrate into home screen for timer
  adjustment ([4915f68](https://github.com/JoestarLabs/nightjar/commit/4915f6804e739759c07183284aa8b5b04dd89021))
* add dynamic shape morphing to LockButton, scale-offset effects to PresetChips, and breathing
  animation to
  StatusChip ([bbee1d1](https://github.com/JoestarLabs/nightjar/commit/bbee1d17e93c4152c1b6cca0a89b37763824a194))
* add lifecycle-based refresh trigger to update permission statuses on screen resume and improve UI
  descriptions ([b97dece](https://github.com/JoestarLabs/nightjar/commit/b97decedd62d5d438e3ed7f11d39d4b4c4a697e2))
* apply tint to lock notification icon in
  LockTimerService ([2f2dc3e](https://github.com/JoestarLabs/nightjar/commit/2f2dc3e1fca7c44acaa10493c09bab404a47b030))
* encapsulate app title variable font animation into a reusable AnimatedAppTitle
  component ([10f6f63](https://github.com/JoestarLabs/nightjar/commit/10f6f6317a433725fcc9b7a38257513a58a16aad))
* implement animated squiggly dial arc and configurable timer presets in
  settings ([27e837d](https://github.com/JoestarLabs/nightjar/commit/27e837df3b65613e06144f6c8a79099da07de898))
* implement enhanced timer progress notification with Android 16+ ProgressStyle
  support ([84db38e](https://github.com/JoestarLabs/nightjar/commit/84db38e63117db2f7a37de154842b6deadba3708))
* implement sunset mode with custom rising wave overlay and permission
  management ([49c6772](https://github.com/JoestarLabs/nightjar/commit/49c67728c2f8ce77a7eb97e5be3be5b33d6325dc))
* implement variable font animations for TimerText and tactile physics for ZenTimerDial
  interaction ([326f17f](https://github.com/JoestarLabs/nightjar/commit/326f17f3cac32d0a25506be125a060d0b0bb0610))
* replace Jetpack Navigation with activity-based navigation for
  settings ([ad3d1bf](https://github.com/JoestarLabs/nightjar/commit/ad3d1bf7b40c476b9d4d2872651251e2f119a8a9))
* replace system fonts with Google Sans Flex variable font and implement animated title typography
  on Home
  screen. ([d9ab371](https://github.com/JoestarLabs/nightjar/commit/d9ab37102fefe8e856e4117342ba2c40c5b3b6c7))
* update app icon foreground and
  background ([8f79331](https://github.com/JoestarLabs/nightjar/commit/8f7933194d8f5c446298f953873c6a643a76ad66))
* update ZenTimerDial animation to use linear tweening during active
  countdowns ([2ab9fa3](https://github.com/JoestarLabs/nightjar/commit/2ab9fa380c9a158373feebb494edb2bf8c95e87a))
* upgrade SettingsScreen to use MediumFlexibleTopAppBar with descriptive
  subtitle ([e8cce72](https://github.com/JoestarLabs/nightjar/commit/e8cce721215bc7712ae9a7c62d7b1d3d04a6dbf6))

### Bug Fixes

* trigger release
  0.0.2 ([5d6f8dc](https://github.com/JoestarLabs/nightjar/commit/5d6f8dc091cdbdb8b8000bff675c872493048b05))
