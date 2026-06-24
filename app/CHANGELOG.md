# Changelog

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
