"""Fail closed before packaging a candidate. No private signing inputs needed."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


def require(condition, message):
    if not condition:
        raise ValueError(message)


def fingerprint(value):
    result = value.replace(":", "").strip().lower()
    require(re.fullmatch(r"[0-9a-f]{64}", result), "Expected a SHA-256 certificate fingerprint")
    return result


def check_version(version, code, previous):
    require(re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version), "Version must be X.Y.Z")
    require(0 <= previous < code <= 2100000000, "versionCode must exceed the published baseline")


def verify(apk, version, code, previous, expected, tools):
    check_version(version, code, previous)
    expected = fingerprint(expected)
    suffix = ".bat" if os.name == "nt" else ""
    signature = subprocess.check_output(
        [str(tools / ("apksigner" + suffix)), "verify", "--verbose", "--print-certs",
         "--min-sdk-version", "26", str(apk)], text=True,
    )
    digests = re.findall(r"^Signer #\d+ certificate SHA-256 digest: (\S+)$", signature, re.M)
    require(len(digests) == 1 and fingerprint(digests[0]) == expected,
            "APK signer does not match the pinned Release certificate")
    require("CN=Android Debug" not in signature, "Debug certificate is forbidden")
    require(re.search(r"Verified using v2 scheme .*: true", signature), "APK must have a valid v2 signature")
    aapt = tools / ("aapt.exe" if os.name == "nt" else "aapt")
    badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    require(package is not None, "Cannot read APK package metadata")
    require(package.groups() == ("dev.milkei.audiofocusmixer", str(code), version),
            "APK package/version differs from the requested release")
    require("sdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging,
            "Unexpected Android SDK range")
    require("application-debuggable" not in badging, "Release APK must not be debuggable")
    return expected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--code", type=int, required=True)
    parser.add_argument("--previous-code", type=int, required=True)
    parser.add_argument("--fingerprint", required=True)
    parser.add_argument("--build-tools", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    cert = verify(args.apk, args.version, args.code, args.previous_code, args.fingerprint, args.build_tools)
    # Refuse to mix old candidates or overwrite any existing output directory.
    args.output.mkdir(parents=True, exist_ok=False)
    target = args.output / f"audio-focus-mixer-v{args.version}.apk"
    shutil.copyfile(args.apk, target)
    with target.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    (args.output / "SHA256SUMS").write_text(f"{digest}  {target.name}\n", encoding="utf-8")
    (args.output / "release-metadata.json").write_text(json.dumps({
        "versionName": args.version, "versionCode": args.code,
        "previousVersionCode": args.previous_code, "certificateSha256": cert,
        "apkSha256": digest, "sourceCommit": os.environ.get("GITHUB_SHA", "local"),
        "minSdk": 26, "targetSdk": 36,
    }, indent=2) + "\n", encoding="utf-8")
    print("PASS: signature, pinned certificate, package, SDK and version; candidate checksummed")


if __name__ == "__main__":
    main()
