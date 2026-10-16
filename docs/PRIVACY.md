# Privacy policy

**Audio Focus Mixer** is a local-only app. It has no account registration, ads, tracking/analytics SDKs, or Internet permission.

## Data accessed

- **Installed apps:** The Android `QUERY_ALL_PACKAGES` permission is used to display apps you may want to control. Names, package identifiers, system-app flags, and icons are read locally.
- **Audio focus modes:** With your Shizuku authorization, the app checks and changes Android's `TAKE_AUDIO_FOCUS` AppOp for selected packages.
- **Restore records:** Before making a change, the app saves the package identifier and original AppOps mode in private Android app storage. This enables restoration.
- **Language choice:** On Android 12 and earlier, AndroidX AppCompat saves your selected app language in local app storage. On Android 13 and later, Android stores the per-app locale in system settings. No locale preference is transmitted.

## Data sharing and storage

The app does **not** transmit this information to a developer-operated server, a third-party analytics provider, or an advertising service. There is no application `INTERNET` permission.

Shizuku is a **separate application and service** you install and authorize. Shizuku's own operation and policies are outside this app. Opening the Shizuku/GitHub help links uses external software (such as your browser).

Restore records are retained until you restore the relevant settings or remove the app. Uninstalling the app deletes its saved records but may not undo the system AppOps changes. **Restore before uninstalling.**

## Contact and changes

For questions or corrections, use the repository's GitHub issue tracker. Material privacy changes will be documented in the changelog and this file.

Last updated: 2026-10-15.
