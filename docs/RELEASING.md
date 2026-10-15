# Release checklist

This repository is open-source application source code; a **public repository** and a **production APK release** are separate milestones.

## Before a public release

1. CI is green on the intended main-branch commit.
2. Verify Korean/English, light/dark themes, large font scale, Shizuku connect/deny/disconnect, app switching, and restoring changes on device.
3. Review the entire **Git history, Actions logs, artifacts, manifest permissions, and documentation** for private data before changing visibility.
4. Retain original Volume++ MIT attribution and dependency notices.
5. Document known device/OS limitations rather than promising universal mixing.
6. Set repository description, topics, security advisory reporting, and appropriate branch protections in GitHub Settings.

## Signing

**Do not promote the CI debug APK to an official production release.**

A production release needs a long-lived, privately stored Android signing keystore. Keep it **outside Git, Actions artifacts, and public documentation**. Preserve the same key across updates to avoid breaking installs. Do not commit keystore passwords or signing files.

A release workflow and secrets can be added once a signing/distribution approach is approved.

## Public-source distribution

The Gradle Wrapper provides a reproducible build path. Users can build locally or inspect testing-only CI artifacts. Test APKs may have incompatible debug signatures between builds, so users must restore AppOps before uninstalling.

## Manual GitHub steps

The connected GitHub integration used for maintenance cannot currently change repository visibility or create signed Android releases. An owner can change visibility in **Settings → General → Danger Zone → Change repository visibility → Public** after reviewing all implications.
