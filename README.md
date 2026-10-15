# Audio Focus Mixer

Android의 오디오 포커스 요청을 앱별로 제어하는 작은 Shizuku 기반 개인용 앱.

[Volume++](https://github.com/noel-digital-fan/volume_plus_plus)의 **Audio Mixing** 구현을 참고하되, 커스텀 볼륨 패널·접근성 서비스·오버레이·부스터는 포함하지 않는다.

## 동작
- 켜기: `appops set <package> TAKE_AUDIO_FOCUS ignore`
- 끄기: 이 앱이 저장한 **변경 이전 AppOps 모드**로 복원. 이전 상태가 없으면 `default`.
- 조회: `appops get <package> TAKE_AUDIO_FOCUS`
- Shizuku UserService(shell UID)에서 실행. 성공 여부를 확인한 후 실제 모드를 재조회한다.

앱별 음량을 직접 제어하거나 오디오 신호를 믹싱하지 않는다. 포커스 요청 동작을 바꾸므로 플레이어/기기/Android 버전에 따라 결과가 달라진다.

## 기능
- Shizuku 실행·권한·서비스 연결 진단 및 재시도
- 설치 앱 검색/시스템 앱 숨기기
- 스위치에 **실제 시스템 모드** 표시
- 변경 전 모드 보관 및 이 앱이 변경한 설정만 일괄 복원
- 한국어 UI, 기존 Android 볼륨 패널 유지

## 환경 및 빌드

Android 8.0(API 26)+, [Shizuku](https://github.com/RikkaApps/Shizuku)가 필요하다. 비루팅 기기에서는 Shizuku를 무선 디버깅이나 ADB로 시작하고 이 앱을 승인한다.

Android Studio에서 Android SDK 36 + JDK 17 이상을 준비한다. Gradle 9.5.0과 Android Gradle Plugin 9.3.0을 사용한다. **Gradle Wrapper 바이너리는 초기 MVP에 아직 추가하지 않았으므로** 로컬 Gradle 9.5.0이 필요하다.

```powershell
gradle :app:assembleDebug :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions에서도 디버그 APK를 빌드하여 artifact로 업로드한다.

## 사용법
1. Shizuku를 설치·실행하고 앱을 열어 권한을 허용한다.
2. 앱을 찾아 오디오 믹싱을 켠 뒤 해당 앱의 재생을 다시 시작한다.
3. 스위치를 끄면 변경 전 모드로 되돌린다.
4. **앱을 삭제하기 전에는** 이 앱이 변경한 설정을 일괄 복원한다. 앱 삭제만으로 AppOps가 원복되지는 않는다.

## 한계 및 주의
- 다른 설정 앱(예: Volume++)도 같은 AppOps를 변경할 수 있으므로 상태는 항상 재조회한다.
- 이 앱이 켜기 전에 저장하지 않은 외부 설정은 일괄 복원하지 않는다.
- 일부 플레이어는 포커스 제어 변경 시 재생/일시정지 동작이 불안정할 수 있다.
- `QUERY_ALL_PACKAGES`를 사용하는 개인 설치용 앱으로, Google Play 배포에는 별도의 정책 검토와 범위 축소가 필요하다.
- Xperia 1 VII에서 새로운 앱 APK의 실기기 동작은 **아직 검증되지 않았다**.

## 라이선스
[noel-digital-fan/volume_plus_plus](https://github.com/noel-digital-fan/volume_plus_plus)의 Shizuku/AppOps 서비스 구조에서 영감을 받아 구현했다. 원저작권 및 MIT 라이선스 고지는 [LICENSE](LICENSE)에 포함되어 있다.
