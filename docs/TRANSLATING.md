# Translating Audio Focus Mixer

The user interface uses Android string resources, never fixed-language strings in Compose.

- English (fallback): `app/src/main/res/values/strings.xml`
- Korean: `app/src/main/res/values-ko/strings.xml`
- Supported locale declaration: `app/src/main/res/xml/locales_config.xml`

When adding or changing copy, keep the same string names, positional placeholders (`%1$s`, `%1$d`, etc.), and meaning in all languages.

**Korean style:** Use consistent polite **-해요체** in sentences. Action labels can be short imperative or noun phrases (e.g., `다시 연결하기`). Avoid casual 반말, unnecessary developer jargon, and unexplained acronyms.

**English style:** Plain, concise language. Don't imply guaranteed simultaneous playback; the feature depends on Android audio-focus policies and player behavior.

Users can select the app language from the Language button: **Follow system**, **한국어** or **English**. Android 13+ also synchronizes this preference with system per-app language settings. On Android 8–12, AppCompat stores the locale locally via auto-storage.

Before submitting a translation PR, build with `./gradlew :app:assembleDebug` and check narrow screens, long app names, large text, error messages, and confirmation dialogs.
