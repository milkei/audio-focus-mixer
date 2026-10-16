# Windows 고정 서명 키 관리

운영용 키는 **사용자가 자신의 Windows PC에서 직접 한 번만 생성**해요.
Codex에 키스토어·개인키·비밀번호·Base64를 전달하지 마세요.
이번 구현은 후보 APK까지만 만들며, 태그 생성·정식 공개·main 병합은 별도 승인이 필요해요.
전체 절차와 실패 시 복구는 [RELEASING.md](RELEASING.md)를 참고하세요.

## 생성과 백업

JDK 21의 `keytool`을 PATH에 준비하고 아래 명령을 직접 실행해요.
저장소 밖의 사용자 홈에 PKCS12 형식의 `.p12`를 만들어요.
이미 파일이 있으면 덮어쓰지 않고 재사용해요.

```powershell
$keyDirectory = Join-Path $HOME 'AndroidSigning\AudioFocusMixer'
New-Item -ItemType Directory -Force -Path $keyDirectory | Out-Null
$keyFile = Join-Path $keyDirectory 'audio-focus-mixer-release.p12'
if (Test-Path -LiteralPath $keyFile) { throw '기존 키를 재사용하세요.' }
keytool -genkeypair -storetype PKCS12 -keystore $keyFile `
  -alias afm-release-2026 -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000
if ($LASTEXITCODE -ne 0) { throw '키 생성 실패' }
keytool -list -v -storetype PKCS12 -keystore $keyFile -alias afm-release-2026
if ($LASTEXITCODE -ne 0) { throw '인증서 확인 실패' }
```

비밀번호는 명령 인수가 아닌 **인터랙티브 입력**으로 입력해요. 이 PKCS12는
저장소와 개인키에 같은 강한 비밀번호를 사용해요. 인증서 주체 정보는 공개되므로
민감한 개인정보를 넣지 마세요. RSA 3072, 유효기간 10,000일을 사용하며
이후 버전에서도 같은 키를 재사용해요. 표시되는 **인증서 SHA-256 fingerprint**를
기록해요. 키스토어 파일의 SHA-256과는 달라요.

원본은 소유자만 접근 가능한 저장소 외부 경로에 보관하고 공유·동기화 폴더를
피하세요. 별도 매체에 **암호화된 오프라인 백업**을 만들고 복구 후 `keytool -list`로
인증서를 확인해요. Alias와 비밀번호는 비밀번호 관리자 등에 안전하게 별도 보관해요.
GitHub Secrets는 자동화용 복사본일 뿐 백업이 아니에요. 키를 잃으면 기존 설치본의
업데이트가 어려워지고, 유출되면 악성 업데이트 서명에 사용될 수 있어요.

## GitHub UI 등록

1. Settings → Environments에서 `release`를 만들어요.
2. Deployment branches and tags를 **Selected branches and tags / Branch: main만**
   허용하도록 설정해요. 태그·와일드카드 브랜치는 허용하지 않아요.
3. 지원한다면 필수 승인자, 자기 승인 금지, 관리자 우회 금지를 설정해요.
4. **Environment Secrets**에 다음 네 항목을 등록해요. 저장소 전체 Secret으로
   대체하지 마세요.
   - `AFM_RELEASE_KEYSTORE_BASE64`: 기존 `.p12` 전체의 Base64
   - `AFM_RELEASE_STORE_PASSWORD`: 키스토어 비밀번호
   - `AFM_RELEASE_KEY_ALIAS`: 생성한 Alias
   - `AFM_RELEASE_KEY_PASSWORD`: 위 PKCS12에서는 같은 비밀번호
5. Environment **Variable** `AFM_RELEASE_CERT_SHA256`에 확인한 공개 인증서
   fingerprint를 등록해요. 64자리 16진수이며 콜론은 있어도 돼요.

Windows 클립보드 기록·클라우드 동기화를 끈 개인 세션에서 아래 명령으로
Base64를 출력 없이 클립보드에 넣어요.

```powershell
$keyFile = Join-Path $HOME 'AndroidSigning\AudioFocusMixer\audio-focus-mixer-release.p12'
Set-Clipboard -Value ([Convert]::ToBase64String([IO.File]::ReadAllBytes($keyFile)))
```

GitHub UI에 붙여넣어 저장한 직후 `Set-Clipboard -Value ''`를 실행하고 클립보드
기록도 지워요. **Base64는 암호화가 아니에요.** 비밀번호는 비밀번호 관리자에서
GitHub UI로 직접 입력하고 터미널 명령·기록·파일·화면 녹화에 남기지 마세요.

비공개 저장소는 요금제에 따라 Environment·브랜치 제한·승인 기능이 달라요.
main 전용 서버 측 브랜치 제한을 설정할 수 없으면 Secrets를 등록하지 말고
[로컬 서명 절차](RELEASING.md#local-signing)를 사용해요. 승인 기능이 없으면
소유자가 실행 전 수동 승인 사실을 기록하되 main 브랜치 제한은 유지해야 해요.
보안 제약 완화나 요금제 변경은 별도 승인 대상이에요.

## 공개 후보 APK 접근 정책

**최종 결정:** 저장소를 Public으로 전환한 뒤 GitHub Actions에서 고정 키로
서명한 후보 APK를 만들면, GitHub에 로그인하고 공개 저장소를 읽을 수 있는
사용자가 **정식 GitHub Release 게시 전에도 해당 후보 아티팩트를 다운로드**할
수 있어요. 14일 보관 기간이 끝나도 누군가 이미 받은 파일을 회수할 수는 없어요.

따라서 후보 생성 워크플로를 수동 실행하는 행동 자체를 **서명된 APK의 외부
다운로드 허용에 대한 사용자 승인**으로 취급해요. 자동 릴리스는 사용하지
않으며, 공식 버전 태그와 GitHub Release 공개에는 **별도 승인**이 필요해요.

## 빌드와 배포

Actions의 **Release candidate**를 `main`에서 수동 실행하고 소스와 같은
versionName과 versionCode를 입력해요. `acknowledge_public_candidate` 확인란을
직접 선택해야 서명 작업을 실행할 수 있어요. 체크하지 않으면 건너뛰어요.
이 확인은 후보 아티팩트 공개 범위에 대한 승인일 뿐, 공식 릴리스 게시는 아니에요. `docs/release-state.json`에는 실제 마지막
공식 배포 버전이 기록되어야 하며, 첫 공식 배포 전에는 0/null이에요.
공식 배포 승인 후 해당 값을 갱신한 PR을 반영하고, 다음 버전은 반드시 더 높은
versionCode를 사용해요. 후보는 공식 배포가 아니며 자동 태그·Release 공개는 없어요.

후보에는 APK, `SHA256SUMS`, 공개 인증서/버전/커밋 정보만 들어가요.
다운로드 후 `Get-FileHash -Algorithm SHA256 <APK>` 및 SDK의
`apksigner verify --verbose --print-certs <APK>`로 원본 인증서와 비교해요.
검증 실패·키 불일치·서명 없는 APK를 정식 배포본으로 취급하면 안 돼요.

Debug에서 최초 Release로 전환할 때는 **기존 앱 실행 → 변경사항 복원 → 실제
AppOps 복원 확인 → 기존 Debug 삭제 → 승인된 Release 설치 → Shizuku 재승인 →
믹싱 재설정** 순서를 지켜요. 복원이 불확실하면 기존 앱을 삭제하지 말고 먼저
해결해요. 이전 모드를 모른 채 무조건 초기화하거나 allow로 바꾸지 마세요.
이후 동일 패키지·동일 키·높은 versionCode의 Release끼리는 앱 데이터를 유지하며
업데이트할 수 있어요. 실기기 설치·설정 유지 검증 결과는 CI 서명 검사와 구분해요.

키 오류가 나면 새 키를 만들지 말고 경로·PKCS12 형식·Alias·비밀번호와 백업을
확인해요. 인증서 불일치 시 배포를 멈추고 원본 및 이전 공식 APK와 비교해요.
키 분실 시 오프라인 백업을 복구하고, 유출 의심 시 자동화용 Secrets를 폐기하고
접근 기록을 조사해요. 무단 키 교체나 AppOps 복원 없는 재설치를 안내하지 마세요.
