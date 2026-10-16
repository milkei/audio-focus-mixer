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

**고정 서명 베타 APK 배포:** [Audio Focus Mixer v0.2.0](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0)을 GitHub Releases에서 받아 설치할 수 있어요. Google Play 등록 앱은 아니며, CI Debug APK는 테스트용이에요.

1. [Shizuku](https://github.com/RikkaApps/Shizuku)를 설치하고 실행해주세요.
2. [v0.2.0 릴리스 페이지](https://github.com/milkei/audio-focus-mixer/releases/tag/v0.2.0)에서 `audio-focus-mixer-v0.2.0.apk`와 `SHA256SUMS`를 받아 확인한 다음 설치해요. 직접 소스에서 빌드할 수도 있지만, [CI Debug APK](https://github.com/milkei/audio-focus-mixer/actions/workflows/android.yml)는 테스트용이에요.
3. Audio Focus Mixer를 열고 Shizuku 접근 권한을 허용해주세요.
4. 원하는 앱을 찾아 믹싱을 켠 다음, 해당 앱에서 재생을 다시 시작해주세요.
5. 스위치를 끄거나 **변경사항 복원**을 눌러 이전 설정으로 되돌릴 수 있어요.

**앱 삭제 전에는 반드시 변경사항을 복원해주세요.** 앱을 삭제해도 시스템 AppOps 설정이 자동으로 원복되지 않을 수 있어요.

**Debug APK 주의:** CI 실행마다 서명 키가 달라질 수 있어요. 다른 실행의 APK로 덮어쓰기가 실패하면 기존 앱을 삭제해야 할 수 있으니, 그전에 변경사항을 복원해주세요. 안정적인 정식 배포를 위해서는 별도의 고정된 서명 키가 필요해요.

### 최초 고정 서명 Release로 전환하기

첫 고정 서명 베타 APK는 `2aadde0426890ab4ae3a9fe9fc788fc856b363bf`
커밋에서 빌드했고, SHA-256 체크섬과 서명 인증서 지문을 검증했으며
Xperia 1 VII(Android 16)에서 동작을 확인했어요.

**v0.2.0 무결성:** APK SHA-256
`328343ac8db9e32cf042e78a814cc3a8cd56f911da6d731f61a1f66deb982d49`;
서명 인증서 SHA-256
`C3:1F:F8:09:A7:B0:F9:12:1C:CD:6F:96:A5:DF:D4:5D:AD:FE:C8:08:8E:71:25:59:73:33:17:7C:5E:5C:D6:98`.
설치 전에 다운로드한 APK와 릴리스의 `SHA256SUMS`가 일치하는지 확인해요.
[서명·배포 절차](docs/RELEASING.md)도 참고할 수 있어요.

**서명 후보 APK의 공개 범위:** 저장소가 Public이면 GitHub Actions 아티팩트를
저장소 읽기 권한이 있는 사용자가 다운로드할 수 있어요. 후보 생성은 소유자가
확인란을 직접 선택한 경우에만 실행해요. 후보 아티팩트는 공식 Release가
아니며, GitHub Releases 게시에는 별도의 승인이 필요해요. [Windows 키 관리 안내](docs/SIGNING.ko.md)도 참고해주세요.
이전 **CI Debug APK**에서 첫 고정 서명 Release로 전환한다면 아래 순서를 따라주세요.

1. 기존 Audio Focus Mixer를 실행하고 **변경사항 복원**을 눌러요.
2. 대상 앱의 **실제 AppOps 모드가 복원됐는지 확인**해요. 불확실하면 기존 앱을
   유지한 채 먼저 해결하고, 모르는 이전 모드를 무조건 초기화하지 마세요.
3. 기존 Debug APK를 제거해요. 서명이 다르면 최초 전환 시 삭제가 필요할 수 있어요.
4. 승인된 고정 서명 Release APK를 설치해요.
5. Shizuku 권한을 다시 승인하고 필요한 앱의 믹싱 설정을 재적용해요.

삭제하면 앱의 복원 기록은 사라져도 시스템 AppOps는 남을 수 있어요.
이후 같은 패키지·Release 키와 더 높은 versionCode의 버전끼리는 앱 데이터를
유지하며 업데이트할 수 있어요. 공개된 APK 체크섬과 인증서 fingerprint를 확인하고,
CI Debug APK나 배포 후보를 공식 Release로 취급하지 마세요.

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
