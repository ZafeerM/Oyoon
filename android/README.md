# android/

Gradle project for everything that runs on the JVM. **Open this folder in Android Studio.**

`settings.gradle.kts` includes three modules:

- `:app`: the Android app. Depends on `:core`.
- `:core`: pure Kotlin maths, no Android. Depends only on Kotlin + kotlinx.serialization.
- `:harness`: laptop command-line tool. Depends on `:core`.

The app and the harness run the exact same `:core` code, so the published numbers come from the code the app runs.

## Build and run

Needs Android Studio (2026.2 or newer, for AGP 9.4). No other setup is needed: the Gradle wrapper,
pinned versions (`gradle/libs.versions.toml`) and the face model are all in the repo.

1. Open this `android/` folder in Android Studio and let Gradle sync. Studio creates `local.properties`
   (not committed) and offers to install SDK Platform 37 if it is missing.
2. Turn on USB debugging on the phone, plug it in, pick it in the device list and press Run.

Command line (needs `JAVA_HOME` set to JDK 17 or newer, for example Android Studio's `jbr` folder):

```bash
./gradlew :app:installDebug
```

## Folder convention

Kotlin sources live in `src/main/kotlin/` without the root package folders (the Kotlin style guide's
"omit the common root package" rule). Each module has one root package, and subfolders below it
match the rest of the package name:

- `app/src/main/kotlin/MainActivity.kt` has `package oyoon.app`
- `app/src/main/kotlin/landmarker/OverlayView.kt` has `package oyoon.app.landmarker`
- `core/src/main/kotlin/model/…` will have `package oyoon.core.model`
