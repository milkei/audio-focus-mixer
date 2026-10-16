# Signed release procedure

This process builds **candidates only**. Publishing a GitHub Release, creating a tag,
merging changes, or changing repository visibility requires separate owner approval.
The existing Android workflow continues to produce testing-only Debug APKs.

## Trust and prerequisites

The current baseline is AGP 9.4.1, Gradle 9.8.1 (distribution SHA-256 pinned in
`gradle-wrapper.properties`), CI JDK 21, compile/target SDK 36, Build Tools 36.0.0,
minimum Android 8.0 / API 26, application ID `dev.milkei.audiofocusmixer`,
versionName `0.2.0`, versionCode `2`. No dependency upgrade is part of signing setup.

Read the [Windows key guide (한국어)](SIGNING.ko.md) before provisioning.
The owner generates the production private key on their Windows PC. Never send a
keystore, password, Base64 value or private key to Codex chat, issues, PRs or logs.
A test key produced by `scripts/test_release_signing.py` is disposable and is never
a production identity. Do not give production credentials to that test script.

## One-time Windows key provisioning (owner only)

Use a JDK 21 `keytool` on PATH. These commands never accept a password as a command
argument. The `.p12` extension matches the explicitly selected PKCS12 format.
RSA 3072, SHA256withRSA and 10,000 days are chosen for long-term sideload updates.
Use a strong unique password from a password manager. PKCS12 uses the same password
for store and key here; supply that same value for both password Secrets.

```powershell
$keyDirectory = Join-Path $HOME 'AndroidSigning\AudioFocusMixer'
New-Item -ItemType Directory -Force -Path $keyDirectory | Out-Null
$keyFile = Join-Path $keyDirectory 'audio-focus-mixer-release.p12'
if (Test-Path -LiteralPath $keyFile) { throw 'Key already exists. Reuse it; do not overwrite.' }
keytool -genkeypair -storetype PKCS12 -keystore $keyFile `
  -alias afm-release-2026 -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000
if ($LASTEXITCODE -ne 0) { throw 'Key creation failed' }
keytool -list -v -storetype PKCS12 -keystore $keyFile -alias afm-release-2026
if ($LASTEXITCODE -ne 0) { throw 'Certificate inspection failed' }
```

Answer password and certificate-subject prompts interactively. Use a non-sensitive
subject because the certificate is public. Record the **SHA-256 certificate**
fingerprint from `keytool -list -v` (not the keystore file hash). Confirm it again
after restoring a backup. Do not regenerate a key on later releases.

Keep the original outside Git; limit Windows file access to the owner, avoid
shared/synced folders, and keep a separate **encrypted offline backup** on different
media. Store the password and alias securely and separately. Test the backup with
`keytool -list`. GitHub Secrets are automation copies, never the sole backup.
Losing the key may prevent updates to installed APKs. A stolen key can sign malicious
updates. Do not delete an old key or improvise key rotation after an incident.

## GitHub Environment and secret registration (owner UI)

1. In repository **Settings → Environments**, create `release` explicitly.
2. Set **Deployment branches and tags → Selected branches and tags**, with exactly
   branch `main`, no tag rules or wildcard branches. Use required reviewers,
   prevent self-review and disable admin bypass where the plan supports them.
3. Review who can write `main`, dispatch Actions and administer the Environment;
   enable branch protection and required review/checks where available.
4. Add **Environment secrets**, never repository/organization-wide signing secrets:

   | Secret | Value |
   | --- | --- |
   | `AFM_RELEASE_KEYSTORE_BASE64` | Full Base64 of the one production `.p12` |
   | `AFM_RELEASE_STORE_PASSWORD` | Store password |
   | `AFM_RELEASE_KEY_ALIAS` | `afm-release-2026` (or your chosen alias) |
   | `AFM_RELEASE_KEY_PASSWORD` | Same password for this PKCS12 key |

5. Add Environment **variable** `AFM_RELEASE_CERT_SHA256`: the independently checked
   public SHA-256 certificate fingerprint, 64 hex digits, optional colons. Record
   the pin in the first approved release description and compare it on every run.
   A changed pin requires explicit owner investigation, not automatic acceptance.

To prepare Base64 without printing it or saving a temporary file, use a private
Windows session with clipboard history/cloud sync disabled, then run:

```powershell
$keyFile = Join-Path $HOME 'AndroidSigning\AudioFocusMixer\audio-focus-mixer-release.p12'
Set-Clipboard -Value ([Convert]::ToBase64String([IO.File]::ReadAllBytes($keyFile)))
```

Paste into the Environment secret's value field in GitHub UI, save, then immediately
run `Set-Clipboard -Value ''` and clear any clipboard history. Base64 is **not
encryption**. Enter passwords directly from the password manager into GitHub UI;
never type them into a shell command, echo them, enable shell tracing, or use screen
recording while provisioning. Do not store a `.env`/properties file in this checkout.
GitHub will not show saved secret values again; keep the offline backup.

### If Environment protection is unavailable

GitHub plan/visibility controls availability of private Environment secrets, branch
policies and reviewer gates. At implementation time this private repository's branch
protection API returned HTTP 403 (plan upgrade required); no `release` Environment
was configured. This is not proof that every Environment feature is available.
See [GitHub's Environment requirements](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments).

The job-level `main` condition protects this workflow, but a writer could change a
workflow on another branch. The **server-side Environment main-only policy** is
therefore mandatory before storing keys. Do not silently replace Environment secrets
with repository secrets or remove protection. If exact branch restrictions cannot be
enforced, leave Actions signing disabled/unprovisioned and use the local signing
procedure below with the owner's key. Reviewer gates, if unavailable, require the
owner's documented manual approval before dispatch; this does not waive the exact
branch restriction. Changing plans or the security design requires owner approval.

## Local signing

From a reviewed `main` checkout, with JDK 21 / Android SDK installed, provide only
process environment variables. Passwords are prompted without echo, not passed in
command-line arguments or Gradle properties. Windows `Read-Host -AsSecureString`
protects input; the signing process necessarily receives plaintext in its environment.
Use a trusted private session and close it afterwards.

```powershell
try {
  $env:AFM_RELEASE_KEYSTORE_PATH = Join-Path $HOME 'AndroidSigning\AudioFocusMixer\audio-focus-mixer-release.p12'
  $env:AFM_RELEASE_KEY_ALIAS = 'afm-release-2026'
  $signingPassword = Read-Host 'PKCS12 password' -AsSecureString
  $env:AFM_RELEASE_STORE_PASSWORD = [Net.NetworkCredential]::new('', $signingPassword).Password
  $env:AFM_RELEASE_KEY_PASSWORD = $env:AFM_RELEASE_STORE_PASSWORD
  .\gradlew.bat --no-daemon --no-configuration-cache --no-build-cache :app:assembleRelease
  if ($LASTEXITCODE -ne 0) { throw 'Release build failed; do not distribute any output' }
} finally {
  'AFM_RELEASE_KEYSTORE_PATH','AFM_RELEASE_STORE_PASSWORD','AFM_RELEASE_KEY_ALIAS','AFM_RELEASE_KEY_PASSWORD' |
    ForEach-Object { Remove-Item -LiteralPath "Env:$_" -ErrorAction SilentlyContinue }
  if ($signingPassword) { $signingPassword.Dispose(); Remove-Variable signingPassword }
}
```

Do not use Gradle `--debug`, build scans or configuration/build caches with production
credentials. There is no Debug fallback. Missing values, invalid file/format, alias or
password cause the Release prerequisite to fail; old APKs left in `app/build` after
failure are not valid candidates. `assembleDebug` and `testDebugUnitTest` need no keys.

Verify locally using the **previous actually published** versionCode and public pin:

```powershell
python scripts/verify_release.py --apk app/build/outputs/apk/release/app-release.apk `
  --version 0.2.0 --code 2 --previous-code 0 --fingerprint '<PUBLIC_SHA256_FINGERPRINT>' `
  --build-tools "$env:ANDROID_HOME\build-tools\36.0.0" --output release-candidate
if ($LASTEXITCODE -ne 0) { throw 'Candidate verification failed' }
```

The example `0` baseline is valid only before the first official release. A candidate
output directory must not already exist. Preserve old evidence separately; do not
ignore verification errors or substitute an unsigned/Debug APK.

## Version and candidate workflow

1. Review latest `main` and require green Android and disposable signing checks.
   Keep application ID, minSdk and the production certificate unchanged.
2. For every subsequent release, increase `versionCode` in `app/build.gradle.kts`
   above the last published value and set `versionName` to the intended `X.Y.Z`.
   No version bump is included solely for introducing this pipeline.
3. Audit `docs/release-state.json` against the last actually published release's
   retained `release-metadata.json`. It is `0`/`null` until the first official release.
   After an approved publication, promptly record that release's code/name in a
   reviewed main PR **before dispatching the next candidate**. This baseline is
   owner-maintained; the workflow cannot discover an unrecorded external publication.
4. On **Actions → Release candidate → Run workflow**, select `main`, enter the exact
   versionName (without `v`) and versionCode, and complete any Environment approval.
   Treat dispatch authorization as permission to create a candidate only.
5. The workflow checks out the triggering main SHA, validates Wrapper/distribution,
   runs translation/unit checks, restores the PKCS12 to a mode-600 temporary file,
   signs without caches, then deletes the key on shell exit and in `always()` cleanup.
   Secrets exist only in the signing step. Runners are GitHub-hosted and ephemeral;
   forced runner loss cannot guarantee cleanup, so do not use persistent self-hosted
   runners without a separate design. Pinned action SHAs must be reviewed on updates.
6. `apksigner verify` checks all supported Android versions from API 26 and requires
   v2. It then checks a single pinned Release signer, rejects the Android Debug
   certificate/debuggable builds, and verifies application ID, SDK and version.
7. Download the candidate artifact. Only these files are uploaded (14-day retention):
   `audio-focus-mixer-vX.Y.Z.apk`, `SHA256SUMS`, `release-metadata.json` (public hashes,
   version and source SHA). No keystore, properties, Gradle cache or signing logs.
8. Independently check `Get-FileHash -Algorithm SHA256 <APK>` against `SHA256SUMS`
   and run `apksigner verify --verbose --print-certs <APK>` from SDK Build Tools.
   Compare the certificate with the original key and prior published APK.

A successful build is not a device/runtime PASS. With a dedicated test device and
**disposable keys**, install version A, create settings (including tracked restore
modes), then `adb install -r` a higher-code B signed by the same test key. Verify
settings and restore records remain. Try higher-code C signed by a different key;
expect `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and unchanged A/B data. Restore AppOps
before uninstalling any fixture. Never run this test over a user's real installation.
The CI integration test verifies A/B certificate continuity and wrong-pin rejection;
it does not claim actual device installation or data-retention testing.

## Moving from Debug to the first fixed-key Release

Different signatures can require uninstalling the old Debug APK. AppOps can survive
uninstall while the app's restore records do not. Follow this order:

1. Open the **existing** Audio Focus Mixer installation.
2. Run **Restore changes** (변경사항 복원).
3. Confirm actual AppOps restoration for the affected apps, using app state and,
   where needed, `adb shell appops get <package> TAKE_AUDIO_FOCUS` against recorded
   prior modes. If restoration is incomplete/uncertain, keep the old installation
   and resolve it before uninstalling. Never assume every prior mode was `allow`.
4. Uninstall the old Debug APK only after restoration is confirmed.
5. Install the first approved fixed-key Release APK.
6. Grant Shizuku access again.
7. Reapply desired mixing settings.

Later releases with the same package/key and increasing versionCode can update in
place while retaining data. Prefer `adb install -r` or the Android package installer;
never advise deleting app data or uninstalling to work around an unexplained failure.

## Publication checklist — requires separate explicit approval

No workflow here creates tags, GitHub Releases or overwrites published assets.
Before approval, test languages, Shizuku connect/deny/disconnect, mixing ON/OFF,
prior-mode restore, enabled-first ordering and scrolling on device. Check source,
full Git history, logs and artifacts for secrets; `.gitignore` is only an accident
barrier. Preserve Volume++ attribution and dependency notices.

Prepare a draft release description using the template below. After owner approval,
use a new tag `vX.Y.Z` pointing to the verified candidate's **exact main SHA**. Confirm
that no tag/release or same-name asset exists, and that tag, versionName and
versionCode agree. If they already exist, stop and investigate; never overwrite.
Publish the exact verified APK, checksum and metadata without rebuilding. A future
automated publisher must retain those checks and the certificate pin.

```text
Audio Focus Mixer vX.Y.Z (versionCode N)
Changes: <reviewed changelog summary>
Requires Android 8.0/API 26+ and running Shizuku with permission granted.
Install: download audio-focus-mixer-vX.Y.Z.apk and allow installation from this source.
Update: install over a prior fixed-key Release, preserving app data.
Debug migration: Restore changes → verify real AppOps → uninstall Debug → install
Release → reauthorize Shizuku → reapply mixing. See docs/RELEASING.md.
Certificate SHA-256: <independently verified public fingerprint>
APK SHA-256: <SHA256SUMS value>
Source commit: <verified main SHA>
Limitations: owner previously tested Xperia 1 VII / Android 16; other OEM/OS/media-app
combinations are not universally verified. <This release's actual device results>
```

## Failure and recovery

- **Missing secrets/pin or branch restriction:** provision the protected Environment;
  do not remove gates or move signing secrets to repository scope.
- **Cannot unlock key:** check PKCS12 format/path, alias with interactive `keytool
  -list -v`, and store/key password. Restore the encrypted backup if damaged. Never
  generate a replacement identity just to make CI green.
- **Fingerprint mismatch/Debug/unsigned output:** stop distribution. Compare original
  keystore certificate, configured pin, previous official APK and source SHA. Do not
  update the expected pin automatically. Preserve only non-secret diagnostic evidence.
- **Version mismatch/stale baseline:** fix source/version-state through review,
  increase the code, and build a new candidate; do not edit APK metadata after signing.
- **Failed build/upload:** no official release occurred. Fix the cause, inspect
  cleanup, rerun from reviewed main. Never reuse leftover output from a failed run.
- **Install failure:** check installed/candidate certificates and code first. Do not
  uninstall until AppOps restore and data implications have been resolved.
- **Lost key:** recover and validate offline backup. Without it, updating the same
  installed signing identity may be impossible. Document a migration with restoration;
  do not promise transparent replacement.
- **Suspected compromise:** stop candidate dispatches, revoke automation copies and
  review Actions/access history. Preserve incident evidence privately and coordinate
  a signed-update/migration response; sideload key rotation is not automatically
  compatible with the supported Android range. Never publish the compromised key.

Official references: [Android signing](https://developer.android.com/studio/publish/app-signing),
[apksigner](https://developer.android.com/tools/apksigner),
[GitHub Environments](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments).
