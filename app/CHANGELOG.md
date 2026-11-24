# Changelog

## [0.0.7](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.6...nightjar-v0.0.7) (2026-11-24)


### Features

* **overlay:** add 1D wave physics, AGSL caustics, and flow-coupled bubbles to FluidLiquidOverlay ([16a80ce](https://github.com/JoestarLabs/nightjar/commit/16a80ce77f00398e0171bbf7c91ca800c0e4d51c))
* **overlay:** replace 1D wave line with 2D particle fluid and translucent AGSL metaball shader ([8e1dc17](https://github.com/JoestarLabs/nightjar/commit/8e1dc17f94d0354c4ffc8d88f10aab2044cbc986))
* **settings:** add copy debug info option in About section ([e1e22cc](https://github.com/JoestarLabs/nightjar/commit/e1e22cc54b5b51a0ec83cceffbc4b0ab7cac3341)), closes [#51](https://github.com/JoestarLabs/nightjar/issues/51)
* **ui:** add adaptive EffectsQuality policy for power, thermal, and accessibility signals ([1e41d8e](https://github.com/JoestarLabs/nightjar/commit/1e41d8e2f0fb93fdfec9ec4c22e08625b533b15e))


### Bug Fixes

* address code review findings across timer lifecycle, effects quality, and fluid simulation ([223c928](https://github.com/JoestarLabs/nightjar/commit/223c92808e50c9582969b5d0fee4c622cde8811f))
* **build,theme:** align Java 17 target, BuildConfig versionName, windowBackground, and single 1-min notify ([029e686](https://github.com/JoestarLabs/nightjar/commit/029e686169b0322a8f0d0cba427a9db16479278a))
* **notification:** throttle Live Update posts and set accent color ([b99d3b8](https://github.com/JoestarLabs/nightjar/commit/b99d3b8b4213a3c63802b168d0ae7efb5d5b629d)), closes [#52](https://github.com/JoestarLabs/nightjar/issues/52)
* **overlay:** calibrate full-screen liquid fill, lighten water feel, remove bubbles and caustic lines ([8c87f8d](https://github.com/JoestarLabs/nightjar/commit/8c87f8d525fae49ec0205a3ef1688a70d9feaa11))
* **overlay:** compensate accelerometer for display rotation in landscape ([3985a5f](https://github.com/JoestarLabs/nightjar/commit/3985a5fadad43d0179c641329f479a5dcd6dc94b)), closes [#53](https://github.com/JoestarLabs/nightjar/issues/53)
* **overlay:** fix Float32 epoch precision loss, slosh density scaling, and zero-width guards ([3811233](https://github.com/JoestarLabs/nightjar/commit/3811233e9bb88c8305563f351004d89dbc355f6a))
* **overlay:** slightly reduce liquid transparency and refine droplet size ([6192966](https://github.com/JoestarLabs/nightjar/commit/61929660df246bcf8549dfa900b8f4b4bb754f32))
* **service,boot,data:** harden lifecycle, race conditions, and DataStore resilience ([8f80b27](https://github.com/JoestarLabs/nightjar/commit/8f80b277ca3096b1c324299433528a54a5812f55))
* **service:** align countdown tick phase and clamp notification progress ([17b83a0](https://github.com/JoestarLabs/nightjar/commit/17b83a0b5f037fc407c91ec232b8c2b225c0d1e9))
* **timer:** guard expired-timer cleanup by start time and handle service shutdown IO errors ([b4f9334](https://github.com/JoestarLabs/nightjar/commit/b4f93343e6bcd4cf53e16ee6df41447d0973204d))
* **ui,tests:** localize DurationPickerSheet, fix gesture/permission bugs, and repair androidTest build ([731aa5f](https://github.com/JoestarLabs/nightjar/commit/731aa5fdfdcee1f47551cefdbfcb65ae374346ff))
* **ui:** scale liquid bubbles in dp, fix settings dropdown row layout, and configure sheet enabledValues ([3266f12](https://github.com/JoestarLabs/nightjar/commit/3266f12a5692b5b8db9365db2cb2c9c4f1fb7fa0))


### Performance Improvements

* eliminate idle animations, frame allocations, and deprecated Compose APIs ([b8de849](https://github.com/JoestarLabs/nightjar/commit/b8de8499f80ae22906dfc52d0fff13a1cde1101f))

## [0.0.6](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.5...nightjar-v0.0.6) (2026-11-11)


### Features

* **overlay:** add multi-style sunset warning animations ([ecf9f01](https://github.com/JoestarLabs/nightjar/commit/ecf9f017f6db667bc3e6bc3e0030282d56302d9a))
* **overlay:** include all 35 MaterialShapes and eliminate overlapping ([6ff08a9](https://github.com/JoestarLabs/nightjar/commit/6ff08a9bb1a64db3303fcc533a749e3bfe636c32))
* **overlay:** integrate Material 3 Expressive motionScheme and calibrate 0.5s lock buffer ([7a3553a](https://github.com/JoestarLabs/nightjar/commit/7a3553a63cd9a6206243984cc105e08e9cc9e89f))
* **overlay:** tighten MaterialShapes spacing and add Phase 4 blur/frost to plan ([562254a](https://github.com/JoestarLabs/nightjar/commit/562254ae194a3a4a3e1ab2168d22792f7c1b904a))
* **settings:** add live fullscreen preview and clarify sunset warning animation setting ([2685c46](https://github.com/JoestarLabs/nightjar/commit/2685c46888b960d89e29e96490c289358114ea3e))
* **ui:** add liquid ripple effect on timer start ([e99a389](https://github.com/JoestarLabs/nightjar/commit/e99a3899e2a7518939a81bed532a104360e463c4))
* **ui:** add Morphe-inspired reverse circular wavy progress countdown indicator ([07346ec](https://github.com/JoestarLabs/nightjar/commit/07346ec029a87b9c4a8c80fa28759754fcd60b14))
* **ui:** invert wavy progress ripple motion to flow clockwise ([2eb676d](https://github.com/JoestarLabs/nightjar/commit/2eb676d1c263a08c616bede0d33663bab90a3c88))
* **ui:** update timer font to M3 expressive condensed with dynamic crescendo ([37cf56f](https://github.com/JoestarLabs/nightjar/commit/37cf56f6c49481eedb0393b5eace275b70055ff4))


### Bug Fixes

* **overlay:** correct fluid tilt orientation and use native MaterialShapes ([4a3c93c](https://github.com/JoestarLabs/nightjar/commit/4a3c93cfd393f69339ade767e88dcfbaab7d2fcf))
* **overlay:** unify MaterialShapes falling physics and impact landing ([05a8f01](https://github.com/JoestarLabs/nightjar/commit/05a8f01877163b53461b0af210876ec2637e4d84))
* **ui:** address code review findings across timer text, sunset settings, and wavy progress ([5a30b25](https://github.com/JoestarLabs/nightjar/commit/5a30b253f9b70db903087edd5bba1cdf6146dfda))
* **ui:** apply direct sp scaling and separate preview state from persisted sunset animation ([5783079](https://github.com/JoestarLabs/nightjar/commit/5783079b0603761bfa0982d3ae545f2cf892b65f))
* **ui:** persist preview state across recreation and derive drag center from measured pointer bounds ([571a9d4](https://github.com/JoestarLabs/nightjar/commit/571a9d46fcf8c8c7bcaf6a59aa9022a1a5ad8825))
* **ui:** reorder dialogs, capture updated drag callback, and add UI recreation tests ([20dc274](https://github.com/JoestarLabs/nightjar/commit/20dc274969addc23ed2cbeca00e2897060b97ec9))
* **ui:** use full native amplitude to ensure persistent wavy progress indicator ([6d8f2b1](https://github.com/JoestarLabs/nightjar/commit/6d8f2b1ae318bec605aa620809459652c56ee967))


### Documentation

* remove unbuilt blur planning and achieve 100% docstring coverage ([cff532b](https://github.com/JoestarLabs/nightjar/commit/cff532b794867190aa8895ffafa438351beae6fd))


### Tests

* **ui:** verify onSecondsChanged callback replacement during active drag gesture ([b35ffc5](https://github.com/JoestarLabs/nightjar/commit/b35ffc5af8c20f1bc8699de78ec1d36d04bdce80))
* **viewmodel:** add test for setSunsetAnimation ([3fc985f](https://github.com/JoestarLabs/nightjar/commit/3fc985f21f4588ee4aaf090dc8743569fbb078cc))

## [0.0.5](https://github.com/JoestarLabs/nightjar/compare/nightjar-v0.0.4...nightjar-v0.0.5) (2026-11-08)


### Features

* add GitHub repository link to settings screen and update about section strings ([d24d4fa](https://github.com/JoestarLabs/nightjar/commit/d24d4fa79be01304984eea55bd5b79699111499d))
* implement emergency unlock slider to allow overriding commitment mode ([f80e64b](https://github.com/JoestarLabs/nightjar/commit/f80e64be38a8afb9a232405d3c7bcb21864cd168))
* **notification:** display error icon in status bar chip during final minute ([a2ef3e6](https://github.com/JoestarLabs/nightjar/commit/a2ef3e643d5c7e05104757c12c22433ec28dcd0f))
* **notification:** make live update chip turn amber persistently at 1-minute mark ([ab2bf2d](https://github.com/JoestarLabs/nightjar/commit/ab2bf2d0046eafbe0ff5cb273d1d5ed7789144fd))
* **sunset:** add audible chime warning on sunset transition ([a979457](https://github.com/JoestarLabs/nightjar/commit/a9794579791012f9c2b74ae4c067832747c8ff13))


### Bug Fixes

* derive versionCode from versionName instead of using Unix timestamp ([#30](https://github.com/JoestarLabs/nightjar/issues/30)) ([b5ab9dc](https://github.com/JoestarLabs/nightjar/commit/b5ab9dc333ebb869ff35dfed89fde015f0d159ad))
* **security:** preserve commitment mode across device reboots and updates ([12872ec](https://github.com/JoestarLabs/nightjar/commit/12872ec79a8bfb1b06416df5360d0d157dfa068d))
* **security:** preserve commitment mode across device reboots in BootReceiver ([1a41d9b](https://github.com/JoestarLabs/nightjar/commit/1a41d9b0a71e1dc93e96572f219cca2b55b3987b))
* **ui:** expand tap target for setting toggle rows ([63b03bc](https://github.com/JoestarLabs/nightjar/commit/63b03bcc6f8704d557c5ee9a3feefec8107c0413))
* **ui:** pass frequency parameter to yAtX in RisingWaveOverlay ([821e10b](https://github.com/JoestarLabs/nightjar/commit/821e10b102b7e10afec041ad6ccbb497cfca2951))
* **ui:** synchronize switch thumb press animation with row interaction source ([7f02ccc](https://github.com/JoestarLabs/nightjar/commit/7f02ccc4de99051bd712a18a29fcdd650cd9970e))


### Performance Improvements

* optimize wave animation path generation with cubic Bézier curves ([4c5ae79](https://github.com/JoestarLabs/nightjar/commit/4c5ae79579bdf21c918068daf4c851684ccba0dd))
* replace wave lineTo loop with cubic Bézier path builder in RisingWaveOverlay ([853b603](https://github.com/JoestarLabs/nightjar/commit/853b603dfa8b509df2342114c7eec94dc4a7e069))


### Documentation

* document calculateVersionCode in build.gradle.kts ([6f89b5b](https://github.com/JoestarLabs/nightjar/commit/6f89b5bb12bfd2356e3cfbc7f64a31676e362326))

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
