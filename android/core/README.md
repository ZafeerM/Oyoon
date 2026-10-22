# :core

Pure Kotlin, **no Android dependencies** (owned by the Core devs). Only Kotlin + kotlinx.serialization.

Used by `:app` on the phone and by `:harness` on a laptop. Sources are in `src/main/kotlin/` (root package `oyoon.core`, so `model/` is `oyoon.core.model`):

- `model/`: data types
- `geometry/`: points and distance helpers
- `gate/`: validity gate
- `measure/`: iris scale and measurements
- `filter/`: One-Euro filter
- `fit/`: fit scoring and ranking

## Build

Kotlin/JVM module (`org.jetbrains.kotlin.jvm`, bytecode target 17). No Android plugin, so `:core`
cannot import Android classes by accident. Tests: `./gradlew :core:test`.
