# Audio Focus Mixer

**Audio focus controls for the apps you choose. Nothing more.**

[![Android CI](https://github.com/milkei/audio-focus-mixer/actions/workflows/android.yml/badge.svg)](https://github.com/milkei/audio-focus-mixer/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84)

**English** · [한국어](README.ko.md)

A lightweight, open-source Android app that changes the `TAKE_AUDIO_FOCUS` AppOp for individual apps through [Shizuku](https://github.com/RikkaApps/Shizuku). When you enable mixing for an app, Android may let it play without interrupting another player.

Audio Focus Mixer is **not** a sound-processing mixer and does **not** offer per-app volume sliders. It leaves your device's original volume panel and hardware buttons untouched.

## Highlights

- **One clear control per app** — find installed apps and toggle audio focus requests on or off.
- **Real system state** — switches reflect the actual AppOps setting, not merely a cached preference.
- **Safe restore workflow** — record the prior mode before changing it; restore just the changes made through this app.
- **Root-free option** — runs through Shizuku's authorized shell-UID user service.
- **Small footprint** — no accessibility service, overlay, advertising, analytics, account, or Internet permission.
- **English and Korean** — choose **Follow system / 한국어 / English** from the app's Language button. The selection syncs with Android 13+ per-app language settings and is supported on older Android versions too.
- **Enabled apps first, without jumpy toggles** — apps enabled through this app are grouped at the top after you reopen or explicitly refresh; toggling a switch never moves its row while you're using the list.

## Requirements

- Android **8.0 (API 26)** or later.
- Shizuku installed and running via ADB or wireless debugging. **Root is not required.**
- Shizuku permission granted to Audio Focus Mixer.

Individual Android versions, OEM audio policies, and media apps may react differently. Android 16 on a Sony Xperia 1 VII has been tested by the project owner, but universal device compatibility is **not** guaranteed.

## Install & use

Download the **signed beta APK** from [GitHub Releases — v0.2.0](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0). This app is distributed directly through GitHub, not Google Play.

1. Install and start [Shizuku](https://github.com/RikkaApps/Shizuku).
2. Download `audio-focus-mixer-v0.2.0.apk` and `SHA256SUMS` from the [release page](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0). Verify the APK's SHA-256 against the checksum file, then install it.
3. Open Audio Focus Mixer and grant Shizuku access.
4. Find an app, enable mixing, and restart playback in that app.
5. Disable mixing for an app to restore its previous mode, or use **Restore changes** to restore all changes tracked by Audio Focus Mixer.

**Updates:** Install a newer APK signed with the same Release key and a higher `versionCode` over the existing Release to keep app data and settings. Check the release notes and checksum before updating.

**Before uninstalling:** Use **Restore changes** and confirm the original AppOps modes were restored. Changes to Android AppOps may survive app removal even though the app's restore records do not.

**Using an old CI Debug APK?** Its signing certificate may differ from the signed Release. Before changing installations, restore and verify AppOps, then uninstall the Debug version if required. See the [migration instructions](docs/RELEASING.md#moving-from-debug-to-the-first-fixed-key-release). CI Debug APKs and Actions candidates are **not** official releases.

## How it works

The privileged [Shizuku UserService](app/src/main/java/dev/milkei/audiofocusmixer/service/AppOpsUserService.kt) performs only validated `TAKE_AUDIO_FOCUS` operations:

```text
appops get <package> TAKE_AUDIO_FOCUS
appops set <package> TAKE_AUDIO_FOCUS ignore
```

The app stores the original mode before its first modification, checks command exit status, and reads the mode again after writing. Restoring re-applies the saved mode. If an external tool changed the mode to another value, bulk restore preserves that external change.

This technique changes **audio focus requests**, not the audio streams themselves. A player may still pause, duck, or behave differently depending on its implementation.

## Build from source

Install Android SDK 36 and JDK 17+ (CI uses JDK 21). The repository includes the verified Gradle 9.8.1 wrapper.

```powershell
git clone https://github.com/milkei/audio-focus-mixer.git
cd audio-focus-mixer
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

On Linux or macOS use `./gradlew`. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions & privacy

| Permission | Why it exists |
| --- | --- |
| `moe.shizuku.manager.permission.API_V23` | Requests access to Shizuku's privileged user service. |
| `android.permission.QUERY_ALL_PACKAGES` | Lets you search **any** installed app, not just apps with a launcher icon. |

There is no `INTERNET` permission, no account, and no tracking SDK. App names, installed-package information, and the saved restore modes stay on your device. See [Privacy](docs/PRIVACY.md).

**Google Play:** `QUERY_ALL_PACKAGES` is restricted. This project's current manifest is intended for sideload/open-source distribution, **not** a Play Store submission without further policy review.

## Performance

App icons are decoded off the UI thread and cached within a **2 MiB** memory budget. Visible-row AppOps reads are limited to **two concurrent requests**, and settings writes are dispatched off the UI thread. This reduces potential scroll-time CPU bursts and UI-thread stalls; **actual battery, CPU and RAM savings have not yet been measured on a device**. See [Performance and profiling](docs/PERFORMANCE.md) for a reproducible ADB workflow.

## Contributing & trust

- [Contributing](CONTRIBUTING.md) · [Translation guide](docs/TRANSLATING.md)
- [Security policy](SECURITY.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Changelog](CHANGELOG.md) · [Release checklist](docs/RELEASING.md)

## Attribution & license

Inspired by and adapted from the audio-mixing architecture in [Volume++](https://github.com/noel-digital-fan/volume_plus_plus) by **noel-digital-fan** (MIT). Shizuku APIs are provided by **RikkaW/RikkaApps**. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Audio Focus Mixer is an independent project and is **not affiliated with** Volume++ or Shizuku.
