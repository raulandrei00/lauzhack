# Game theory games

Owns Nim and future game-theory games. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `NimScreen(onBack)` in `com.playwise.gametheory`.
- `Nim.kt` contains the pure rules and AI choices; `NimScreen.kt` contains the Compose UI.
- Run `./gradlew :features:game-theory:desktopTest` (Windows: `.\gradlew.bat :features:game-theory:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add new games here and expose their screen to the app shell.
