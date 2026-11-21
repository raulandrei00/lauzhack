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
- **Directions, paint, import, and playing games on your own graph** (`CustomBoards.kt`).
  - **Directions:** "One-way mode" makes new lines point from the first vertex tapped to the second. "Make all one-way" and "Make all two-way" convert every line. Per-line direction is still set on the selected line.
  - **Paint tool:** six paints plus an eraser. Clashing lines are dashed red, and the analysis reports whether the colouring is proper and how many paints it uses.
    - "Auto-color" calls `bestColoring`. Up to 14 vertices it runs an exact backtracking search with symmetry breaking, so the result is the chromatic number. Larger graphs use the DSatur greedy rule. It returns null if more than 6 paints would be needed. Paint survives moves and layouts.
  - **Text import/export:** `graphToText` and `parseGraphText` use the format `A-B 3, B>C, A<D, E`. `-` is two-way, `>`/`->` and `<`/`<-` are one-way, an optional weight follows (≤ 99), and a lone letter is an isolated vertex. Parsing is case-insensitive, and errors are worded for children. Imported graphs get a circle layout, then Tidy.
  - **Play a game on your graph:** the editor lists all 11 games, with START/GOAL pickers.
    - `gameProblem` returns a plain-language reason when a game can't use the graph:
      - not enough vertices, START = GOAL, or GOAL unreachable (respecting arrows only for games that keep them)
      - 0–1 BFS weights other than 0/1
      - SCC without one-way lines
      - Shannon with a direct START–GOAL line (an instant win)
      - Planar with more than 10 vertices or a non-planar graph
      - Dots and Boxes always, since it needs a grid
    - `gameNote` explains when arrows are treated as two-way. The lessons for BFS, 0–1 BFS, Dijkstra and SCC keep directions, as does the Shortest-Path Challenge. Biconnected, Coloring, Shannon, Kayles and Planar treat lines as two-way.
    - `playableBoard` renumbers the vertices so START is A and GOAL is the last letter, which every game assumes. It converts two-way lines into two arrows when a game keeps directions, and passes the learner's positions through.
    - `GraphLesson`, `RouteLesson`, `ComponentLesson`, `GraphDuelScreen` and each duel accept an optional `PlayableBoard`. On a custom board, "New board" becomes "Play again" and the Dots setting is hidden. Planar starts from the learner's layout, rearranged only if lines cross.
    - In a directed Shortest-Path Challenge, sabotaging a two-way road raises both of its arrows for one point, and the price is shown once.
  - The drawn graph, undo history and START/GOAL live in `SketchSession`, held by `GraphGamesScreen`, so "Back to my graph" returns to exactly where you were.
  - `CustomBoardsTest` covers text round-trips and errors, one-way helpers, paint preservation, exact chromatic numbers (cycles, K4, Petersen = 3, K7 → null), greedy colouring, renumbering, direction handling, every refusal reason, two-way sabotage, and that every allowed game builds a valid starting state.
  - The editor, Dijkstra and Coloring on a custom graph, the Path duel, and clash highlighting were rendered off-screen and inspected. Real touch play has not been tried.
- Run `./gradlew :features:graph:desktopTest` (Windows: `.\gradlew.bat :features:graph:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add a public screen or data API here, then wire it into `composeApp` navigation.
