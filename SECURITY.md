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

## Release signing and supply chain

The owner generates one production PKCS12 key outside Git and keeps an encrypted
offline backup. GitHub Environment secrets are automation copies. Never submit
private keys, keystores, Base64 keystores or passwords in reports or chat.
Certificate SHA-256 fingerprints and APK checksums are public verification data.

The manual candidate workflow accepts only this repository's main branch, uses
pinned action SHAs and read-only contents permission, and requires the `release`
Environment with an exact main-only server-side branch restriction before secrets
are provisioned. PR checks have no production secrets or Environment access.
Candidate signing additionally requires an explicit owner acknowledgement that,
once the repository is public, the signed APK artifact is downloadable by
repository readers before official publication. Candidate generation and
publishing a GitHub Release are separate approval decisions.
If branch restriction is unavailable, use owner-controlled local signing; do not
move secrets to repository scope to bypass protection.

Missing/incorrect signing inputs fail Release builds. Verified candidates must match
the owner's public certificate pin, package and expected version. Keys are restored
only to restricted temporary files, removed on exit/always cleanup, and excluded from
artifact allowlists and caches. An artifact is not approval to publish. See
[release/key management and incident recovery](docs/RELEASING.md).

For suspected signing-key exposure, stop signing, revoke automation copies, preserve
private audit evidence and assess installed-user migration before replacing keys.
Do not publish compromised material or promise seamless key rotation on Android 8+.
