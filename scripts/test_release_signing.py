"""CI integration checks using ONLY short-lived, randomly generated test keys.

Never pass production credentials to this program. Nothing is uploaded.
"""
import hashlib
import os
from pathlib import Path
import re
import secrets
import subprocess
import tempfile
import zipfile

from verify_release import check_version, verify

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "app/build.gradle.kts"
APK = ROOT / "app/build/outputs/apk/release/app-release.apk"
GRADLE = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
NAMES = ["AFM_RELEASE_KEYSTORE_PATH", "AFM_RELEASE_STORE_PASSWORD",
         "AFM_RELEASE_KEY_ALIAS", "AFM_RELEASE_KEY_PASSWORD"]
SENSITIVE = []


def run(command, env, success=True, message=None):
    result = subprocess.run(command, cwd=ROOT, env=env, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if any(value in result.stdout for value in SENSITIVE):
        raise AssertionError("A test credential appeared in child-process output (output suppressed)")
    if (result.returncode == 0) != success or (message and message not in result.stdout):
        # Output has been checked against every generated password before printing.
        print(result.stdout[-12000:])
        raise AssertionError("Unexpected command result")
    return result.stdout


def gradle(env, success=True, message=None, tasks=(":app:assembleRelease",)):
    return run([GRADLE, "--no-daemon", "--no-configuration-cache", "--no-build-cache", *tasks],
               env, success, message)


def rejected(call, label):
    try:
        call()
    except (ValueError, subprocess.CalledProcessError):
        print(f"PASS: rejected {label}")
    else:
        raise AssertionError(f"Accepted {label}")


def main():
    base = dict(os.environ)
    if any(base.get(name) for name in NAMES):
        raise SystemExit("Run signing tests without Release credentials")
    tools = Path(base["ANDROID_HOME"]) / "build-tools/36.0.0"
    original = BUILD.read_text(encoding="utf-8")
    version = re.search(r'versionName = "([^"]+)"', original)[1]
    code = int(re.search(r'versionCode = (\d+)', original)[1])
    gradle(base, tasks=(":app:testDebugUnitTest", ":app:assembleDebug"))
    print("PASS: Debug build and unit tests without Release inputs")
    gradle(base, False, "Release signing requires:")
    print("PASS: absent signing inputs fail closed")
    with tempfile.TemporaryDirectory(prefix="afm-test-", dir=base.get("RUNNER_TEMP")) as temp:
        try:
            signing = dict(base)
            password = secrets.token_hex(24)
            wrong = secrets.token_hex(24)
            SENSITIVE.extend([password, wrong])
            signing.update(dict(zip(NAMES, [str(Path(temp) / "test.p12"), password, "afm-test", password])))
            run(["keytool", "-genkeypair", "-storetype", "PKCS12", "-keystore", signing[NAMES[0]],
                 "-alias", "afm-test", "-keyalg", "RSA", "-keysize", "3072", "-validity", "2",
                 "-dname", "CN=AFM Disposable CI Test", "-storepass:env", NAMES[1],
                 "-keypass:env", NAMES[3]], signing)
            cert = subprocess.check_output(
                ["keytool", "-exportcert", "-keystore", signing[NAMES[0]], "-alias", "afm-test",
                 "-storepass:env", NAMES[1]], env=signing, stderr=subprocess.DEVNULL)
            expected = hashlib.sha256(cert).hexdigest()
            for name in NAMES:
                partial = dict(signing)
                del partial[name]
                gradle(partial, False, "Release signing requires:")
            print("PASS: each missing signing input fails")
            bad_path = dict(signing, AFM_RELEASE_KEYSTORE_PATH=str(Path(temp) / "missing.p12"))
            gradle(bad_path, False, "Release keystore file does not exist")
            corrupt = Path(temp) / "corrupt.p12"
            corrupt.write_bytes(b"not a keystore")
            gradle(dict(signing, AFM_RELEASE_KEYSTORE_PATH=str(corrupt)), False, "Cannot unlock Release private key")
            for name, value in [(NAMES[2], "wrong-alias"), (NAMES[1], wrong), (NAMES[3], wrong)]:
                invalid = dict(signing)
                invalid[name] = value
                gradle(invalid, False, "Cannot unlock Release private key")
            print("PASS: missing/corrupt file, wrong alias/store password/key password fail")
            gradle(signing)
            verify(APK, version, code, 0, expected, tools)
            print("PASS: Release A signature, certificate, package, SDK and version")
            tampered = Path(temp) / "tampered.apk"
            tampered.write_bytes(APK.read_bytes())
            with zipfile.ZipFile(tampered, "a") as archive:
                archive.writestr("tamper-test.txt", "modified after signing")
            rejected(lambda: verify(tampered, version, code, 0, expected, tools), "tampered APK")
            rejected(lambda: verify(APK, version, code, 0, "0" * 64, tools), "different certificate pin")
            rejected(lambda: verify(APK, "99.0.0", code, 0, expected, tools), "incorrect versionName")
            rejected(lambda: check_version(version, code, code), "non-increasing versionCode")
            rejected(lambda: verify(ROOT / "app/build/outputs/apk/debug/app-debug.apk",
                                   version, code, 0, expected, tools), "Debug APK")
            candidate = Path(temp) / "candidate"
            run(["python3", "scripts/verify_release.py", "--apk", str(APK), "--version", version,
                 "--code", str(code), "--previous-code", "0", "--fingerprint", expected,
                 "--build-tools", str(tools), "--output", str(candidate)], base)
            files = {p.name for p in candidate.iterdir()}
            assert files == {f"audio-focus-mixer-v{version}.apk", "SHA256SUMS", "release-metadata.json"}
            checksum, name = (candidate / "SHA256SUMS").read_text().split()
            assert checksum == hashlib.sha256((candidate / name).read_bytes()).hexdigest()
            print("PASS: candidate allowlist and SHA256SUMS")
            run(["python3", "scripts/verify_release.py", "--apk", str(APK), "--version", version,
                 "--code", str(code), "--previous-code", "0", "--fingerprint", expected,
                 "--build-tools", str(tools), "--output", str(candidate)], base, False, "FileExistsError")
            print("PASS: existing candidate output cannot be overwritten")
            # Only the disposable checkout is modified, and always restored.
            BUILD.write_text(original.replace(f"versionCode = {code}", f"versionCode = {code + 1}"), encoding="utf-8")
            gradle(signing)
            verify(APK, version, code + 1, code, expected, tools)
            print("PASS: Release B has higher versionCode and identical certificate")
            other = dict(signing, AFM_RELEASE_KEYSTORE_PATH=str(Path(temp) / "other.p12"))
            run(["keytool", "-genkeypair", "-storetype", "PKCS12", "-keystore", other[NAMES[0]],
                 "-alias", "afm-test", "-keyalg", "RSA", "-keysize", "3072", "-validity", "2",
                 "-dname", "CN=AFM Other Disposable CI Test", "-storepass:env", NAMES[1],
                 "-keypass:env", NAMES[3]], other)
            different = Path(temp) / "different-key.apk"
            signer = tools / ("apksigner.bat" if os.name == "nt" else "apksigner")
            run([str(signer), "sign", "--ks", other[NAMES[0]], "--ks-key-alias", "afm-test",
                 "--ks-pass", "env:" + NAMES[1], "--key-pass", "env:" + NAMES[3],
                 "--out", str(different), str(APK)], other)
            rejected(lambda: verify(different, version, code + 1, code, expected, tools), "valid APK signed by a different key")
        finally:
            BUILD.write_text(original, encoding="utf-8")
    print("PASS: temporary keys removed; no credentials or APKs uploaded")
    print("NOT EVALUATED: device installation, data retention, runtime UI/Shizuku regression")


if __name__ == "__main__":
    main()
