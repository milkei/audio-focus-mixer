# Performance & profiling

Audio Focus Mixer does not poll in the background. AppOps changes remain in Android after the application is closed, without a persistent timer, accessibility service or overlay.

## Optimizations

| Hot path | Before | Optimized implementation |
| --- | --- | --- |
| App icon | PackageManager icon loading and bitmap conversion during Compose composition | Deferred background decoding via coroutine, sized to the displayed icon and cached |
| Icon RAM | No shared bounded bitmap cache | 2 MiB byte-budgeted LRU, entries evicted as needed |
| Audio-focus reads | Every visible row may simultaneously invoke its own \`appops get\` child process | Two concurrent reads at most, with cancellable waiters and no busy loop |
| Saved restore mode | Synchronous SharedPreferences \`commit()\` may execute on UI thread | Commit and bulk restore run on \`Dispatchers.IO\` |
| Lazy list reuse | Stable keys only | Stable keys and content types for settings, status, app rows and footer |

### Tradeoffs

- Limiting concurrent AppOps requests reduces **peak process churn**, not the total number of reads. A long list may take longer to query when opened quickly. Do not reduce the limit further without device measurements.
- The bitmap cache retains up to 2 MiB of extra memory to avoid repeated decoding while scrolling. It is **not** a claim of lower total PSS in every scenario.
- Refresh and resume intentionally read actual AppOps states again: they must reflect changes made by other apps.
- The Shizuku user service stays in its existing non-daemon mode. Automatically killing it on every background transition might interrupt an in-flight setting/restore operation; this is intentionally left unchanged.
- There is no claim of a measured CPU, RAM or battery reduction without before/after device profiles.

## How to compare on Android

Compare the same phone, OS build, installed-app count and application build type, at similar battery/thermal states. Use **release/profileable** builds for final animation and scroll performance comparisons. Debug builds can exaggerate lazy-list jank.

Suggested scenarios:

1. **Idle**: let the app sit on the main screen without interacting for at least 30 seconds.
2. **Cold start**: force-stop the app, open it, and note time until apps are visible.
3. **Scrolling**: swipe through the list several times and repeat the same pattern between runs.
4. **Refresh**: tap Refresh while connected to Shizuku. Observe system CPU and UI responsiveness.
5. **Toggle/restore**: verify mode changes, original mode restoration, process restart, and interrupted Shizuku behavior.

Capture memory and CPU samples locally with:

~~~powershell
# Run after opening the app on a connected Android device.
.\scripts\profile-android.ps1 -Label before -Scenario idle
.\scripts\profile-android.ps1 -Label after -Scenario idle
~~~

The script writes \`.perf-results/\` (Git-ignored): \`dumpsys meminfo\`, a short \`top\` sampling window, and the process list for the app's foreground and Shizuku user-service processes. These files are local and should **not** be shared publicly without review.

The package to profile is \`dev.milkei.audiofocusmixer\`. When comparing PSS, inspect the app's **TOTAL PSS** and separately include any related Shizuku user-service process. Process RSS is not equivalent to private RAM. See Google's [dumpsys documentation](https://developer.android.com/tools/dumpsys) and [Android Studio Profiler](https://developer.android.com/studio/profile).

### Update/debug APK warning

CI debug APKs are not a stable signed release channel. Signing keys can differ. **Restore AppOps modes before uninstalling or replacing an APK that can't update in place.**

## 한국어 요약

- **화면 그리기:** 앱 아이콘은 백그라운드에서 읽고 최대 2MiB만 캐시해요.
- **CPU 순간 부하:** 화면에 보이는 앱들의 AppOps 상태 조회를 동시에 최대 2개로 제한해요.
- **UI 멈춤:** 이전 설정을 저장하거나 전체 복원할 때 디스크 쓰기를 UI 스레드 밖에서 실행해요.
- **측정 주의:** 아직 실제 기기에서 CPU·메모리·배터리 절감 수치를 측정하지 않았어요. 같은 조건에서 이전 버전과 개선 버전의 PSS·CPU·스크롤 반응을 비교해야 해요.
- **안전:** 서명 키가 다른 APK로 교체하려면 먼저 믹싱 설정을 복원해주세요.
