# :harness

Laptop command-line tool (owned by the Core devs). Runs `:core` on recorded snapshots and fixtures without a phone, to produce the numbers used in the paper.

Depends on `:core`.

Kotlin/JVM application. Entry point: `src/main/kotlin/Main.kt` (`oyoon.harness.MainKt`).
Run with `./gradlew :harness:run` from `android/`.
