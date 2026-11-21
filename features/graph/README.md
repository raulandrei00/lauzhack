# Graph games

Owns the graph algorithm lessons and the local two-player graph games. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `GraphGamesScreen(onBack)` in `com.playwise.graph`.
- Keep graph rules and board generation independent of platform APIs.
- Teaching content (primer, per-lesson and per-duel guides with pseudocode, worked examples, quizzes and resources) is plain data in `GraphLearning.kt`, rendered by `GraphLearningScreen.kt`.
- Planar Color & Connect supports 2–10 vertices and 1–6 paints. It uses the exact small-graph minor test through six vertices and a Kuratowski subdivision test for larger boards. `PlanarLayout.kt` keeps a clear drawing when possible and rearranges vertices after an added edge. Rejected edge taps explain the exact rule: duplicate edge, matching paints, edge-count limit, K5, or K3,3. Planarity also excludes K3,3; checking K5 alone is insufficient.
- Each graph duel has settings: vertex count for the network games, paint count for the coloring games, a budget for Shortest-Path Challenge, and grid dimensions for Dots and Boxes. Starting with new settings creates a fresh game.
- Run `./gradlew :features:graph:desktopTest` (Windows: `.\gradlew.bat :features:graph:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add a public screen or data API here, then wire it into `composeApp` navigation.
