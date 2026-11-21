# Graph games

Owns the graph algorithm lessons and the local two-player graph games. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `GraphGamesScreen(onBack)` in `com.playwise.graph`.
- Keep graph rules and board generation independent of platform APIs.
- Run `./gradlew :features:graph:desktopTest` (Windows: `.\gradlew.bat :features:graph:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add a public screen or data API here, then wire it into `composeApp` navigation.
