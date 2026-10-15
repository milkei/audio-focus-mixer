# Security policy

## Supported versions

Security fixes target the current `main` branch and the most recent tagged release, when one exists. Experimental CI artifacts are not stable distribution builds.

## Reporting a vulnerability

Please **avoid opening a public issue containing exploit details or credentials**. Use GitHub's private vulnerability-reporting feature for this repository **if it is enabled**. If that feature is unavailable, open an issue containing only a non-sensitive high-level request for a private contact channel; do not include reproduction steps that could harm users.

Do not share device identifiers, tokens, private logs, or Shizuku authorization data in public reports.

## Security model

- This app's Shizuku UserService is scoped to `TAKE_AUDIO_FOCUS` AppOps reads and writes for syntactically validated Android package names and approved modes.
- Shizuku authorization is explicitly granted by the user and is separate from normal Android app permissions.
- This application has no Internet permission or remote analytics.
- Android AppOps configuration may persist after the app is closed or uninstalled. Restore changes before uninstalling.
- CI debug APKs use a development signing configuration and are **not production releases**.

No formal security audit has been conducted.
