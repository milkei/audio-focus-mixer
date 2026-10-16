# Audio Focus Mixer

**필요한 앱의 오디오 포커스만 간편하게 제어해요.**

[![Android CI](https://github.com/milkei/audio-focus-mixer/actions/workflows/android.yml/badge.svg)](https://github.com/milkei/audio-focus-mixer/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[English](README.md) · **한국어**

Audio Focus Mixer는 [Shizuku](https://github.com/RikkaApps/Shizuku)를 이용해 앱별 `TAKE_AUDIO_FOCUS` 설정을 바꾸는 가벼운 오픈소스 Android 앱이에요. 특정 앱의 믹싱을 켜면 다른 앱의 재생을 방해하지 않고 소리가 함께 나도록 도울 수 있어요.

**오디오 신호를 직접 합성하거나 앱별 볼륨을 조절하는 기능은 아니에요.** 휴대폰의 기본 볼륨 패널과 물리 볼륨 버튼은 그대로 유지해요.

## 주요 기능

- 설치된 앱을 검색해 앱별 믹싱을 켜거나 끌 수 있어요.
- 저장된 스위치 값이 아니라 **실제 시스템 AppOps 상태**를 확인해요.
- 변경 전 모드를 저장하고, 이 앱에서 변경한 항목만 복원해요.
- Shizuku를 활용하므로 루팅 없이 사용할 수 있어요.
- 접근성 서비스·화면 오버레이·광고·분석 도구·계정·인터넷 권한을 사용하지 않아요.
- 앱 화면의 **언어** 버튼으로 시스템 기본값·한국어·English 중 선택할 수 있어요. Android 13 이상에서는 시스템의 앱별 언어 설정과도 연동돼요.
- 이 앱에서 믹싱을 켠 앱은 **다시 열거나 새로고침할 때 상단으로 모여요.** 스위치를 켜거나 끄는 순간에는 앱이 현재 위치에 그대로 있어요.

## 요구 사항

Android 8.0(API 26) 이상과 실행 중인 Shizuku가 필요해요. Shizuku는 ADB 또는 무선 디버깅으로 실행하고, 앱 접근 권한을 허용해야 해요. 루팅은 필요하지 않아요.

Sony Xperia 1 VII의 Android 16 환경에서 기능이 확인됐지만, 모든 기기와 앱에서 같은 재생 결과가 보장되지는 않아요.

## 설치와 사용

[GitHub Releases — v0.2.0](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0)에서 **고정 서명된 베타 APK**를 받을 수 있어요. Google Play에 등록된 앱은 아니에요.

1. [Shizuku](https://github.com/RikkaApps/Shizuku)를 설치하고 실행해요.
2. [릴리스 페이지](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0)에서 `audio-focus-mixer-v0.2.0.apk`와 `SHA256SUMS`를 받아 APK의 SHA-256이 일치하는지 확인한 뒤 설치해요.
3. Audio Focus Mixer를 열고 Shizuku 접근 권한을 허용해요.
4. 원하는 앱의 믹싱을 켜고 해당 앱의 재생을 다시 시작해요.
5. 스위치를 끄면 해당 앱의 이전 설정을 복원하고, **변경사항 복원**으로 이 앱에서 기록한 변경사항을 한꺼번에 복원할 수 있어요.

**업데이트:** 같은 Release 키로 서명되고 `versionCode`가 높아진 APK는 기존 Release 위에 설치해 앱 데이터와 설정을 유지할 수 있어요. 업데이트 전 릴리스 안내와 SHA-256 체크섬을 확인해 주세요.

**앱 삭제 전 주의:** 반드시 **변경사항 복원**을 실행하고 실제 AppOps 설정이 돌아왔는지 확인해 주세요. 앱 삭제 후에도 시스템 설정이 남을 수 있지만 복원 기록은 사라져요.

**예전 CI Debug APK를 사용 중인가요?** 서명이 달라 Release로 전환할 때 기존 앱 삭제가 필요할 수 있어요. 먼저 AppOps를 복원·확인하고, 필요한 경우 Debug 앱을 제거해 주세요. 자세한 내용은 [기존 Debug 앱 전환 안내](docs/RELEASING.md#moving-from-debug-to-the-first-fixed-key-release)를 참고해 주세요. CI Debug APK와 Actions 후보 APK는 공식 배포본이 아니에요.

## 빌드하기

Android SDK 36과 JDK 17 이상을 준비해주세요. Gradle 9.8.1 Wrapper가 저장소에 포함되어 있어요.

```powershell
git clone https://github.com/milkei/audio-focus-mixer.git
cd audio-focus-mixer
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK 경로: `app/build/outputs/apk/debug/app-debug.apk`

## 동작 방식과 한계

Shizuku UserService에서 앱의 `TAKE_AUDIO_FOCUS` AppOps 모드를 `ignore`로 설정하고, 변경 뒤 실제 값을 확인해요. 이전 모드를 저장하므로 복원할 수 있어요. 다른 도구가 그 사이 값을 다르게 바꿨다면 일괄 복원에서 덮어쓰지 않아요.

이 방식은 오디오 스트림을 직접 섞는 기술이 아니라 시스템의 포커스 요청 동작을 조정하는 방식이에요. 일부 플레이어나 제조사에서는 일시정지·소리 줄이기 등이 다르게 동작할 수 있어요.

`QUERY_ALL_PACKAGES` 권한은 기기의 모든 앱을 검색하는 핵심 기능에 사용해요. Google Play에서는 제한되는 권한이므로 현재 구성 그대로는 스토어에 등록하기 어려울 수 있어요. 인터넷 권한과 추적 도구는 없으며, 앱 목록이나 설정을 서버로 보내지 않아요.

자세한 내용은 [개인정보 안내](docs/PRIVACY.ko.md)를 확인해주세요.

## 성능과 측정

앱 아이콘을 UI 스레드 밖에서 불러오고 **최대 2MiB**만 캐시해요. 화면에 나타난 앱의 AppOps 상태 조회는 **동시에 최대 2개**로 제한하고, 설정 파일 쓰기는 백그라운드 스레드에서 처리해요. 스크롤 중 CPU 부하와 화면 지연을 줄이기 위한 변경이지만, **실기기 CPU·RAM·배터리 절감량은 아직 측정하지 않았어요.** 측정 방법은 [성능 검증 안내](docs/PERFORMANCE.md)에서 확인할 수 있어요.

## 참여와 라이선스

[기여 안내](CONTRIBUTING.md) · [보안 정책](SECURITY.md) · [서드파티 고지](THIRD_PARTY_NOTICES.md) · [변경 기록](CHANGELOG.md)

Volume++ 제작자 **noel-digital-fan**의 MIT 라이선스 소스를 참고해 제작한 독립 프로젝트예요. Volume++ 또는 Shizuku의 공식 제품이 아니에요. 출처와 라이선스는 [LICENSE](LICENSE) 및 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 명시했어요.
