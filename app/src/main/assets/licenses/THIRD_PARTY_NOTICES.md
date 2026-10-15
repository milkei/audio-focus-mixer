# Third-party acknowledgements

Audio Focus Mixer is an independent open-source application. It includes or draws from the following projects. Inclusion here does not imply endorsement or affiliation.

| Component | Project / owner | License | Use |
| --- | --- | --- | --- |
| Audio mixing architecture | [Volume++](https://github.com/noel-digital-fan/volume_plus_plus), noel-digital-fan | MIT | Reference and adaptation for Shizuku-based `TAKE_AUDIO_FOCUS` control |
| Shizuku API / Provider | [Shizuku-API](https://github.com/RikkaApps/Shizuku-API), RikkaW | MIT | Runtime privileged-service API integration |
| AndroidX / Jetpack Compose | [AndroidX](https://github.com/androidx/androidx) | Apache-2.0 | Native Android UI and platform libraries |
| Gradle Wrapper (scripts and JAR) | [Gradle](https://github.com/gradle/gradle) | Apache-2.0 | Reproducible Gradle invocation |

The source distribution retains the Volume++ project's 2026 copyright notice in [LICENSE](LICENSE). The Gradle Wrapper's embedded script headers remain intact. The wrapper binary and scripts were copied from the Volume++ repository (at its then-current main revision) and are verified in CI with the Gradle Wrapper validation action. Gradle distribution integrity is pinned with `distributionSha256Sum`.

This project **does not** contain or redistribute the Shizuku manager APK or its launcher images. End users obtain Shizuku separately.

License texts and upstream copyright notices remain the property of their respective holders. See the linked original projects for their complete licenses and notices.
