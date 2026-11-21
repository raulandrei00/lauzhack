# Playwise: developer and agent handoff

## Start here

Playwise is a Kotlin Multiplatform educational app. Read `README.md`, `GETTING_STARTED.md`, this file, and any future `AGENTS.md` before editing. The repository is `lauzhack/`, not its parent `HACK/`. Check `git status` before making changes; preserve unrelated work. No agent instruction file was present when the graph games were implemented.

The README's Romanian outline asks for a game, theory (potentially an interactive PDF), and implementation per topic. Its five graph topics are now playable: Dijkstra, BFS, 0–1 BFS, vertex-biconnected components, and SCC. Each lesson and duel has an in-app teaching guide (see "Teaching guides" below); there are no generated lesson PDFs. Nim remains available. Other game-theory variants and probability topics remain future work.

## Product and source material

`docs/` contains the supplied hackathon briefs, not application runtime assets. The PDFs and Nexthink slide deck were read for context. The relevant educational brief emphasizes a learning experience rather than a chatbot; the Kotlin brief asks for shared logic and an app demonstrated on at least two platforms. Target availability or successful compilation alone does not prove that demo requirement. The other sponsor briefs are not additional graph-game requirements.

The README lists topics but does not specify game mechanics. The implementation interprets shortest-path topics as route-building puzzles and connectivity topics as maximal-component discovery puzzles. Keep this distinction explicit when extending the lessons.

## Build and run

Requirements: JDK 17+, internet for initial dependency resolution. Gradle wrapper 8.11.1 is included. Kotlin 2.2.20, Compose Multiplatform 1.9.0, Android Gradle plugin 8.9.2. No backend, credentials, AI API, database, or extra graph library is required.

From the repository root in PowerShell:

```powershell
.\gradlew.bat :composeApp:run
.\gradlew.bat :features:graph:desktopTest :features:game-theory:desktopTest
.\gradlew.bat :composeApp:compileKotlinWasmJs
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
```

On macOS/Linux use `sh ./gradlew`. Web needs a browser supporting WasmGC; Gradle provisions its JS tools. Android needs an SDK with API 35, plus an emulator/device for `:composeApp:installDebug`; minSdk is 24. Set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`.

For this Windows workspace only, a portable JDK and dependency cache were installed in the parent folder:

```powershell
$env:JAVA_HOME='C:\Users\selim\Desktop\HACK\.tools\java17\jdk-17.0.21.1+2'
$env:GRADLE_USER_HOME='C:\Users\selim\Desktop\HACK\.tools\gradle'
```

These paths are machine-specific, not project requirements. Set these variables before wrapper commands if Java is not on PATH. First builds can take several minutes while downloading dependencies. Do not commit caches, build output, SDKs, or local paths.

## Architecture

| Module/source | Responsibility |
| --- | --- |
| `composeApp/src/commonMain/.../App.kt` | Theme, home navigation, and links to the three feature screens |
| `features/graph/src/commonMain` | Graph model, rules, random boards, lessons, and two-player screens |
| `features/graph/src/commonTest` | Graph correctness, duel, and board-generation tests |
| `features/game-theory/src/commonMain` | Six-level Nim journey (Classic, Staircase, Take up to n, Two mini-games, mex, Chocolate). Rules and AI; tap-to-play ball piles (`NimUi.kt`); teaching guides (`NimLearning.kt`, `LearningPanels.kt`), the binary XOR workbench and the Grundy table explorer. Details in `features/game-theory/README.md` |
| `features/game-theory/src/commonTest` | Nim, Staircase, subtraction-game and Chocolate tests, plus `NimLearningTest`, which checks the worked examples against the game code |
| `features/probability/src/commonMain` | Navigable probability path scaffold; future games go here |
| `composeApp/src/{desktopMain,androidMain,wasmJsMain}` | Thin platform entry points calling the shared app shell |

Each feature is an independent Gradle module. Keep rules free of platform APIs and put shared functionality in each module's `commonMain`. Feature modules must not depend on `composeApp` or on each other; add public screens and wire them in the app shell.

## Rules and invariants

- Algorithm lesson boards have six labeled nodes A–F and are generated randomly on entry and when requesting another board. `graphLevel` remains as deterministic test fixtures only; the UI uses `randomLesson`.
- Route games start at A and end at F. Only adjacent, unvisited nodes can be appended. Undo removes the last step; restart clears the attempt. Cycles are unnecessary for an optimal route with nonnegative costs, including zero-cost edges.
- BFS boards use unit weights. Dijkstra uses nonnegative weights. 0–1 BFS rejects other weights. Any route achieving the optimal cost is accepted, not just one stored solution.
- SCC boards are directed. The drawing includes arrows and the text road list provides explicit direction. Groups must be maximal, and each vertex belongs to exactly one group.
- Biconnected means vertex-biconnected blocks of an undirected graph. Articulation vertices may occur in several answers. Bridges count as two-vertex blocks; isolated vertices count as singleton blocks. This is not edge-biconnectivity and not merely articulation-point identification.
- A component claim is an unordered vertex set. Partial groups, unions of separate groups, and duplicate claims do not advance progress. Claiming a correct group clears the selection.
- `LessonGraph` validates endpoints, no self-loops, unique edges (unordered for undirected graphs), and weights 0–1000. Algorithms assume small in-memory graphs. SCC and biconnected DFS are recursive; use iterative traversal before scaling to large datasets. `Int.MAX_VALUE` denotes unreachable nodes.
- Dijkstra uses a linear minimum scan; adjacency is cached. BFS uses FIFO; 0–1 BFS uses a deque with distance relaxation. SCC uses Kosaraju; biconnected blocks use low-link DFS plus an edge stack.
- `routeCost` sums stored weights and rejects invalid paths. BFS's generic distance function ignores stored weights, so keep BFS lesson edges at weight one.

## Learning interaction and state

Every board has a mission, explicit road list, diagram, node buttons, answer check, restart, and hint. Route games show accumulated cost; component games show claimed groups. After two incorrect checks the theory opens automatically. A hint reveals target route cost or a node in an unclaimed component. Successful answers expose theory and a next-board button. The theory panel is the topic's full teaching guide.

State uses Compose `remember`. Board changes reset the board state; leaving a topic resets its attempts. Topic completion lasts only while the graph screen remains mounted and counts a topic after any successful board. It is not persistent mastery tracking or completion of all three boards. No storage, telemetry, scoring backend, or saved accounts exist.

The board is the input: players tap vertices and edges directly on it (see "Tap-to-play boards" below). A collapsible "Show roads as text" list repeats direction and cost information. The parent screen supplies vertical scrolling. Six-node boards use the fixed skewed-hexagon `sixNodeLayout`; extend the layouts alongside any graph-size changes. Color is always paired with text (paint names, START/GOAL, "locked"/"cut", "shared" badges).

## Tap-to-play boards

`TapBoard.kt` is a shared Canvas component. Callers describe each vertex (`NodeLook`: fill, pulsing "tap me" ring, steady selection ring, badge, red cross, ghost) and each edge (`EdgeLook`: color, width, dashes, arrow, label pill, tappable). Taps are hit-tested in this order: the nearest vertex within about 38 dp, then the nearest tappable edge within 22 dp. Callbacks go through `rememberUpdatedState`, so the lambdas always see current state. Edge-label placement avoids other labels and other roads. The skewed hexagon stops the three long diagonals meeting in one point. `TurnBanner`, `Feedback`, `ResultCard`, `PaintPicker` and `RoadListToggle` are the shared UI pieces.

Interactions: route lessons extend the path by tapping a glowing neighbor; tapping the current dot steps back; reaching F checks automatically. Component lessons toggle dots, then a big Claim button; feedback says "too much" or "can grow". Coloring: pick a paint, then tap a dot; illegal taps explain which neighbor clashes, and dead vertices show "stuck!". Shannon, sabotage and Dots and Boxes are played by tapping roads. Kayles takes two taps: the first previews which dots will vanish, the second confirms. In Planar, tap a dot, then either a paint or a second dot (to draw a line); rejected lines explain why. Game rules and `DuelState`/`PlanarGame` are unchanged.

Verification: the screens were rendered off-screen at phone width (480 px) and checked visually, and simulated presses confirmed vertex, edge and empty-space hit testing. That temporary harness was removed. Real touch on Android and in a browser has not been tried.

## Verification and manual checks

Run `:features:graph:desktopTest`, `:features:game-theory:desktopTest`, and `:composeApp:compileKotlinWasmJs` after shared-code edits. Run `:composeApp:assembleDebug` for Android integration. Tests cover shortest paths/unreachable nodes, invalid routes, 0–1 relaxation, directed reachability, articulation overlap, bridges, singleton components, exhaustive optimal-route comparison for route boards, SCC partition/mutual-reachability checks, graph duels, random boards, and Nim.

Manual smoke test on desktop and a WasmGC browser:

1. Open Graphs from home; open each of the five topics.
2. In route games, attempt a nonoptimal route, undo/restart, then reach F optimally; check next board and replay.
3. In component games, reject a partial group, accept a full group, reject a duplicate, and complete all groups. Verify shared articulation nodes can be reused.
4. Check hints, automatic theory after mistakes, back navigation, and narrow-window scrolling.
5. Confirm Nim still opens and plays. Check Android separately when an SDK/device is available.

Verification on 2026-11-21: `:composeApp:desktopTest` and `:composeApp:compileKotlinWasmJs` succeeded. The five graph test methods and three existing Nim tests passed. Automated compilation does not establish visual correctness, keyboard/screen-reader behavior, Android runtime compatibility, or a successful two-platform demo. No UI automation is included.

## Extending safely

## Six local two-player games (2026-11-21 addition)

The Graphs hub now offers six games above the original five algorithm lessons. `GraphDuels.kt` contains immutable game states and pure rule transitions, `GraphDuelScreen.kt` contains the shared Compose UI, and `GraphDuelTest.kt` tests those transitions. `DuelKind` holds rules and teaching text. All games are hot-seat/local two-player, with restart and a mathematics panel; there is no AI opponent or multiplayer service. Leaving a game discards its state. Existing lesson completion statistics do not include these games.

| Game | Exact behavior |
| --- | --- |
| Graph Coloring Duel | Three colors. P1 Builder and P2 Blocker both make legal coloring moves. Builder wins on complete coloring, Blocker when no legal move anywhere remains. Recoloring is forbidden. A blocked vertex does not immediately end the game while another legal move exists. |
| Shannon Switching | P1 Connector claims edges; P2 Cutter deletes only unclaimed edges. Claimed edges are protected. Connectivity is checked both on claimed edges and on all uncut edges, detecting either terminal condition immediately. Terminals A and F. |
| Node Kayles | Choose a surviving vertex, remove it and its neighbors; last legal move wins. Removed nodes cannot be selected again. |
| Shortest-Path Challenge | Explicit two-phase variant: P1 increases edge weights by one per point, at most five points total, then voluntarily locks costs (unused budget is allowed). P2 constructs a simple A–F route and can undo before submitting. Matching Dijkstra's final optimum wins for P2; a more costly submitted route wins for P1. The benchmark is not shown before submission. This is not an alternating-move pursuit game. |
| Dots and Boxes | 3x3 dots / 2x2 squares. Claim edges; completing either or both adjacent squares assigns them to the mover and retains the turn. Otherwise turn changes. All edges taken ends the game; scores determine winner or draw. |

### Added verification checklist

After this addition, desktop tests passed (16 total: 8 duel, 5 algorithm, 3 Nim), and `compileKotlinWasmJs` succeeded. A desktop `run` invocation exited successfully without reporting an exception, but no persistent app window was confirmed; interactive visual validation is still required.

Tests cover conflicting colors and both coloring outcomes, Kayles removals and terminal state, Shannon protected edges/path/cut victories, sabotage budget and phase gating, optimal/nonoptimal route results, double-square capture and retained turns, completed Dots and Boxes boards.

Manual checks still needed on each target: play both roles through terminal states; inspect labels and edge lists at narrow widths; check restart. Web compilation alone is not browser interaction testing. Android still requires separate SDK/device verification.

### Extension guidance

Add topics through `GraphTopic`, `randomLesson`, UI mode selection, and tests together. Add board cases with independently computed expected answers; do not only test an algorithm against itself. Keep exact maximal-set checking for components. Do not replace biconnected blocks with ordinary connected components. For weighted path changes ensure BFS unit-cost and 0–1 weight constraints remain valid. For arbitrary graphs address overflow and recursion limits.

## Random board generation (current behavior)

This section supersedes the original fixed-size/three-board descriptions above. All eleven graph activities generate a board on entry and on their new-board button. Recomposition does not regenerate it. Lesson Restart retains the same puzzle; New random board/Next random board generates another without a three-level limit. Duel New random game creates an entirely fresh state. Random draws can occasionally repeat a previous topology, especially grid sizes.

`RandomBoards.kt` owns generators taking an injectable `kotlin.random.Random`; UI uses `Random.Default`, tests use explicit seeds. Keep `duelGraph` and `graphLevel` as deterministic rule-test fixtures. Production UI uses `randomDuel` and `randomLesson` instead.

- Coloring: random connected six-node graph generated from a hidden three-color assignment, guaranteeing a possible complete coloring.
- Shannon: a randomly labeled ring with A/F opposite, plus optional edges; direct A–F edges are excluded. Initial terminals have two independent routes, avoiding an immediate claim win or single-edge cut win.
- Kayles: random spanning path plus extra edges, six vertices.
- Shortest-path duel / route lessons: connected random topology. Weights are random 1–9 for Dijkstra and the duel, 0–1 for 0–1 BFS, exactly one for BFS. Sabotage changes costs after generation.
- SCC: two randomly sized/labeled strongly connected groups, internal random edges and a forward-only link between groups.
- Biconnected: two randomly sized/labeled cycle/bridge blocks sharing one articulation vertex. Counts stay beginner-friendly while membership, edges, and block sizes vary.
- Dots and Boxes: random rectangular lattice with 3–5 rows and columns of dots (4–16 squares). `DuelState.gridColumns` plus vertex count determines dimensions. Capture rules, scores, labels, and drawing all use these dimensions. Arbitrary edges would change the game, so randomness is in grid dimensions.

`RandomBoardsTest` checks seed reproducibility and variety, connectivity, three-colorability, Shannon cut resistance, rectangular-grid scoring to completion, valid component solutions, and route costs against exhaustive simple-path search over 100 seeds. No seeds are persisted in player state yet.

Potential follow-ups: persistent progress, more levels, algorithm-step visualizations, printable/PDF lessons, localization, light theme, interactive node selection, and device/browser UI verification. These are not implemented promises.

## Teaching guides

`GraphLearning.kt` holds pure-data `TeachingGuide`s: `graphPrimer`, `GraphTopic.guide` and `DuelKind.guide`. Each guide has a summary, intuition, vocabulary, a worked example on a small fixed graph, full pseudocode, complexity, a correctness argument, common mistakes, a 3-question self-check, resources and a pointer into the source. `GraphLearningScreen.kt` renders a guide as collapsible sections. The idea and pseudocode sections start open, pseudocode scrolls horizontally, quiz answers are revealed on tap, and links open through `LocalUriHandler` with the URL also shown as text. The hub has a "Graph basics" primer card. Lessons are listed in learning order: BFS, 0–1 BFS, Dijkstra, SCC, Biconnected. The old `theory`/`implementation`/`lesson` enum fields were removed.

`GraphLearningTest` checks that every guide is complete and every link is https, and runs each worked example against the real algorithms. If you edit an example trace, update that test too. The external links were written from knowledge, not fetched, so check them in a browser before a demo. Text is English only.

## Draw your own graph

The Graphs hub has a "Draw your own graph" card that opens `GraphSketchScreen`. Freehand strokes are classified by geometry in `SketchRecognizer`: loops and dots become vertices, lines become edges, and a V-shape or hook makes an edge one-way. The result becomes an editable `DrawnGraph`, drawn with the shared `TapBoard` and analysed with Dijkstra, SCCs, cycles and bipartiteness. A neural network was considered and rejected: no training data, an offline WasmGC/Android target, and the need to explain decisions. Handwritten weights are set by tapping instead. See `features/graph/README.md` for thresholds, tests and limits. Real touch drawing on devices still needs manual testing.

## Planar Color & Connect

A separate sixth duel; the original Coloring Duel is unchanged. Entry uses a random count of 2–6 isolated vertices and three colors. Settings allow n=2–6 and k=1–4, applied only when Start with these settings is pressed. No initial edges are generated, per the rules. Two players alternate exactly one permanent coloring or one edge addition. Both colored endpoints must differ; uncolored endpoints may be connected. Recoloring, self-loops, duplicate edges, and nonplanar additions are rejected without consuming a turn. After every accepted move, both move types are checked. If neither is possible, the mover wins; coloring every vertex alone is not a win.

`features/graph/src/commonMain/kotlin/com/playwise/graph/PlanarGame.kt` owns the state and exact bounded planarity test. For at most six vertices, a K3,3 minor must already be a subgraph; a K5 minor is a subgraph or requires one edge contraction. The test enumerates these cases. Do NOT raise the six-vertex limit without replacing this algorithm with a general planarity test. `PlanarGameTest.kt` covers forbidden graphs/minors, legal dense planar graphs, rejected moves, alternation, both terminal conditions, and seeded full games.

The board is explicitly a schematic, not a computed planar embedding: visible crossings do not determine legality. A future crossing-free layout should be based on a planar embedding, without rejecting moves merely because straight segments intersect. Background: https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.planarity.check_planarity.html . No NetworkX runtime dependency is added.
