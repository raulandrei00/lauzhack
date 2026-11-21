# Graph games

Owns the graph algorithm lessons and the local two-player graph games. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `GraphGamesScreen(onBack)` in `com.playwise.graph`.
- Keep graph rules and board generation independent of platform APIs.
- Teaching content (primer, per-lesson and per-duel guides with pseudocode, worked examples, quizzes and resources) is plain data in `GraphLearning.kt`, rendered by `GraphLearningScreen.kt`.
- Planar Color & Connect supports 2–10 vertices and 1–6 paints. It uses the exact small-graph minor test through six vertices and a Kuratowski subdivision test for larger boards. `PlanarLayout.kt` keeps a clear drawing when possible and rearranges vertices after an added edge. Rejected edge taps explain the exact rule: duplicate edge, matching paints, edge-count limit, K5, or K3,3. Planarity also excludes K3,3; checking K5 alone is insufficient.
- Each graph duel has settings: vertex count for the network games, paint count for the coloring games, a budget for Shortest-Path Challenge, and grid dimensions for Dots and Boxes. Starting with new settings creates a fresh game.
- **Draw your own graph** (`GraphSketchScreen.kt`): you sketch with a finger or mouse, and the app recognizes the drawing and turns it into an editable `TapBoard`.
  - **Recognition** (`SketchRecognizer.kt`) uses stroke geometry, not ML. Each stroke is measured for closure (gap compared with size), length compared with its bounding-box diagonal, roundness (spread of distances from the centre), aspect ratio, and its sharpest corner (after resampling to 32 points). Thresholds are in dp.
    - Closed round strokes or tiny taps become vertices. Overlapping ones merge.
    - Open strokes become edges. Each end snaps to a vertex within its radius + 30 dp, or creates an implicit vertex there. A line through a drawn circle splits into two edges.
    - A short V touching an edge end, or a hook at the end of a line, makes that edge directed.
    - Self-loops, huge circles and stray marks are ignored, each with a plain-language note.
    - Vertices are labelled left to right.
  - **Why not a neural network:** the shapes are geometric primitives that measurement separates reliably. No training data or model download is needed, it is instant and offline on all three targets, and every decision can be explained to the learner. ML would only help with handwritten weights or labels, so the editor sets those by tapping.
  - **Editing** (`DrawnGraph.kt`): the graph is immutable, so undo is a stack of earlier versions. You can drag vertices, tap two vertices to connect them, add vertices, erase vertices or edges, set weights (0–99) and direction, and use Tidy (Fruchterman–Reingold force layout) or Circle layouts. The limit is 26 vertices (A–Z).
  - **Analysis:** degrees (with the handshake-lemma check), components, cycle/tree/forest, bipartite, SCCs when any edge is one-way, and a Dijkstra cheapest-route finder that highlights the path.
  - `TapBoard` gained optional `onNodeDrag` and `onEmptyTap` parameters. Only the editor passes them, so the other boards keep their original tap handling.
  - `SketchRecognizerTest` covers circles, snapping, implicit vertices, dots, splitting, both arrow styles, duplicates, self-loops, merging, open circles, the example drawing, editing, analysis, routes, layouts and sketch fitting.
  - It was also driven with simulated pointer strokes off-screen, from drawing through to editing (connect, drag, select edge). Real finger drawing on a phone or tablet has not been tried yet.
- Run `./gradlew :features:graph:desktopTest` (Windows: `.\gradlew.bat :features:graph:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add a public screen or data API here, then wire it into `composeApp` navigation.
