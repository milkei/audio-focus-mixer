# Changelog

This project follows the general structure of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Releases will use versioned GitHub tags when published.

## [Unreleased]

### Release infrastructure
- Add fail-closed PKCS12 Release signing, separate from Debug builds and tests.
- Add a main-only manual candidate workflow with certificate/package/version checks,
  SHA-256 checksums, restricted temporary key handling and no automatic publication.
- Require a deliberate candidate-public-access acknowledgement before signing,
  and document that public-repository Actions artifacts can be downloaded.
- Add disposable-key signing integration checks without production secrets/artifacts.
- Document Windows key generation, encrypted backups, Environment provisioning,
  failure recovery and safe Debug-to-Release AppOps restoration.
- Keep app behavior, dependency versions and version 0.2.0 / code 2 unchanged.

### UX
- Prioritize verified in-app-enabled audio-focus apps when the list is reopened or explicitly refreshed; never reorder in response to a toggle or scrolling.
- Add an in-app system/Korean/English language picker with AndroidX AppCompat backward compatibility to Android 8.0.

### Performance
- Decode installed-app icons off the UI thread, size them for display, and cap icon cache memory at 2 MiB.
- Bound visible-row `TAKE_AUDIO_FOCUS` lookups to two simultaneous Shizuku requests.
- Move synchronous restore-mode writes and bulk restore processing to `Dispatchers.IO`.
- Provide content types for lazy-list composition reuse and add concurrency/cancellation tests.
- Document device-side CPU and memory profiling; no unmeasured speedup claims.


### Maintenance
- Consolidated compatible Android build-tool and CI-action updates while keeping `compileSdk 36`.
- Paused API 37-dependent Compose BOM and AndroidX Core updates until a deliberate SDK migration.
- Grouped weekly Dependabot updates to reduce review noise and coordinated Gradle/AGP upgrades.

## [0.2.0] — 2026-10-15

### Added
- English and Korean interface, including system per-app language selection on Android 13+.
- Dynamic Material 3 light/dark appearance, installed app icons, edge-to-edge safe areas.
- Public-facing README, privacy policy, contribution and security guidance.
- Gradle Wrapper and validation in CI.

### Changed
- Friendly, consistent Korean polite-form copy.
- Improved connection and error-state messaging.

## [0.1.0] — 2026-10-15

### Added
- Minimal Shizuku-powered `TAKE_AUDIO_FOCUS` per-app controls.
- Installed app search, real AppOps state checks, prior-mode backup, bulk restore.
- Android CI with unit tests and debug APK artifacts.

The original MVP was tested on Xperia 1 VII (Android 16) by the device owner. Full device/OS compatibility testing remains open.
