# android/

Gradle project for everything that runs on the JVM. **Open this folder in Android Studio.**

`settings.gradle.kts` includes three modules:

- `:app`: the Android app. Depends on `:core`.
- `:core`: pure Kotlin maths, no Android. Depends only on Kotlin + kotlinx.serialization.
- `:harness`: laptop command-line tool. Depends on `:core`.

The app and the harness run the exact same `:core` code, so the published numbers come from the code the app runs.
