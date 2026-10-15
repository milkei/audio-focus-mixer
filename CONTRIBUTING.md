# Contributing

Thanks for helping keep Audio Focus Mixer focused, accessible, and maintainable.

## Scope

The project is a **minimal audio-focus controller**, not a replacement volume panel or a full audio mixer. Propose new features with that scope in mind. Please do not add analytics, advertising, network dependencies, unnecessary permissions, or hidden/root-only APIs without discussion.

## Development

- Android SDK 36, JDK 17+, Gradle Wrapper 9.8.1
- Kotlin + Jetpack Compose / Material 3
- English strings: `app/src/main/res/values/strings.xml`
- Korean strings: `app/src/main/res/values-ko/strings.xml`

On Windows (PowerShell):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

On Linux/macOS:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## Pull requests

1. Open an issue before large behavioral or permission changes.
2. Keep changes scoped; add tests for parsing, state verification, and restoration logic.
3. Update **both** language resource files for user-visible copy. See [Translation guide](docs/TRANSLATING.md).
4. Run the Gradle tests/build and include results in the PR.
5. Check Shizuku permission-denied, unavailable, and disconnected states. Don't present unverified writes as successful.
6. Preserve copyright notices and attribution when reusing third-party code.

## Reporting problems

Include Android version, manufacturer/model, Shizuku version/backend, the affected app, the expected and actual behavior, and reproduction steps. **Redact personal information and secrets from logs.**

For vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of posting exploit details publicly.

By participating, you agree to communicate respectfully and constructively.
