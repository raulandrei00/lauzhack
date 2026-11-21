# Playwise: developer and agent handoff

## Start here

Playwise is a Kotlin Multiplatform educational app. Read `README.md`, `GETTING_STARTED.md`, this file, and any future `AGENTS.md` before editing. The repository is `lauzhack/`, not its parent `HACK/`. Check `git status` before making changes; preserve unrelated work. No agent instruction file was present when the graph games were implemented.

The README's Romanian outline asks for a game, theory (potentially an interactive PDF), and implementation per topic. Its five graph topics are now playable: Dijkstra, BFS, 0–1 BFS, vertex-biconnected components, and SCC. Theory and algorithm pseudocode are embedded in each game; there are no generated lesson PDFs. Nim remains available. Other game-theory variants and probability topics remain future work.

## Product and source material

`docs/` contains the supplied hackathon briefs, not application runtime assets. The PDFs and Nexthink slide deck were read for context. The relevant educational brief emphasizes a learning experience rather than a chatbot; the Kotlin brief asks for shared logic and an app demonstrated on at least two platforms. Target availability or successful compilation alone does not prove that demo requirement. The other sponsor briefs are not additional graph-game requirements.

The README lists topics but does not specify game mechanics. The implementation interprets shortest-path topics as route-building puzzles and connectivity topics as maximal-component discovery puzzles. Keep this distinction explicit when extending the lessons.

## Build and run

Requirements: JDK 17+, internet for initial dependency resolution. Gradle wrapper 8.11.1 is included. Kotlin 2.2.20, Compose Multiplatform 1.9.0, Android Gradle plugin 8.9.2. No backend, credentials, AI API, database, or extra graph library is required.

From the repository root in PowerShell:

```powershell
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:desktopTest
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

| File under `composeApp/src` | Responsibility |
| --- | --- |
| `commonMain/kotlin/com/playwise/app/App.kt` | Theme, home navigation, existing Nim UI; Graphs card enters graph games |
| `commonMain/kotlin/com/playwise/app/Graphs.kt` | Graph model, topic explanations, algorithms, deterministic board catalog, route-cost validation |
| `commonMain/kotlin/com/playwise/app/GraphGamesScreen.kt` | Topic picker, board drawing, route/component input, feedback, hints, progress and replay |
| `commonMain/kotlin/com/playwise/app/Nim.kt` | Existing Nim rules and AI |
| `commonTest/kotlin/com/playwise/app/GraphTest.kt` | Graph correctness and board validation tests |
| `commonTest/kotlin/com/playwise/app/NimTest.kt` | Existing Nim regression tests |
| `desktopMain`, `androidMain`, `wasmJsMain` | Thin platform entry points calling shared Compose UI |

Keep graph logic free of platform APIs. All new functionality lives in commonMain so all targets share the same rules.

## Rules and invariants

- Boards have six labeled nodes A–F. Every topic has three authored boards, wrapping to board one after completion. `graphLevel` uses level modulo three.
- Route games start at A and end at F. Only adjacent, unvisited nodes can be appended. Undo removes the last step; restart clears the attempt. Cycles are unnecessary for an optimal route with nonnegative costs, including zero-cost edges.
- BFS boards use unit weights. Dijkstra uses nonnegative weights. 0–1 BFS rejects other weights. Any route achieving the optimal cost is accepted, not just one stored solution.
- SCC boards are directed. The drawing includes arrows and the text road list provides explicit direction. Groups must be maximal, and each vertex belongs to exactly one group.
- Biconnected means vertex-biconnected blocks of an undirected graph. Articulation vertices may occur in several answers. Bridges count as two-vertex blocks; isolated vertices count as singleton blocks. This is not edge-biconnectivity and not merely articulation-point identification.
- A component claim is an unordered vertex set. Partial groups, unions of separate groups, and duplicate claims do not advance progress. Claiming a correct group clears the selection.
- `LessonGraph` validates endpoints, no self-loops, unique edges (unordered for undirected graphs), and weights 0–1000. Algorithms assume small in-memory graphs. SCC and biconnected DFS are recursive; use iterative traversal before scaling to large datasets. `Int.MAX_VALUE` denotes unreachable nodes.
- Dijkstra uses a linear minimum scan; adjacency is cached. BFS uses FIFO; 0–1 BFS uses a deque with distance relaxation. SCC uses Kosaraju; biconnected blocks use low-link DFS plus an edge stack.
- `routeCost` sums stored weights and rejects invalid paths. BFS's generic distance function ignores stored weights, so keep BFS lesson edges at weight one.

## Learning interaction and state

Every board has a mission, explicit road list, diagram, node buttons, answer check, restart, and hint. Route games show accumulated cost; component games show claimed groups. After two incorrect checks the theory opens automatically. A hint reveals target route cost or a node in an unclaimed component. Successful answers expose theory and a next-board button. Each topic includes explanatory prose, implementation pseudocode, and complexity.

State uses Compose `remember`. Board changes reset the board state; leaving a topic resets its attempts. Topic completion lasts only while the graph screen remains mounted and counts a topic after any successful board. It is not persistent mastery tracking or completion of all three boards. No storage, telemetry, scoring backend, or saved accounts exist.

The diagram is illustrative; labeled buttons are the input controls. The text edge list duplicates visual direction/cost information. The parent screen supplies vertical scrolling. The six-node layout is fixed; extend the layout alongside any graph-size changes. Color is supplemented by text/check marks.

## Verification and manual checks

Run `:composeApp:desktopTest` and `:composeApp:compileKotlinWasmJs` after shared-code edits. Tests cover shortest paths/unreachable nodes, invalid routes, 0–1 relaxation, directed reachability, articulation overlap, bridges, singleton components, exhaustive optimal-route comparison for all nine route boards, and SCC partition/mutual-reachability checks. Keep Nim tests passing.

Manual smoke test on desktop and a WasmGC browser:

1. Open Graphs from home; open each of the five topics.
2. In route games, attempt a nonoptimal route, undo/restart, then reach F optimally; check next board and replay.
3. In component games, reject a partial group, accept a full group, reject a duplicate, and complete all groups. Verify shared articulation nodes can be reused.
4. Check hints, automatic theory after mistakes, back navigation, and narrow-window scrolling.
5. Confirm Nim still opens and plays. Check Android separately when an SDK/device is available.

Verification on 2026-11-21: `:composeApp:desktopTest` and `:composeApp:compileKotlinWasmJs` succeeded. The five graph test methods and three existing Nim tests passed. Automated compilation does not establish visual correctness, keyboard/screen-reader behavior, Android runtime compatibility, or a successful two-platform demo. No UI automation is included.

## Extending safely

Add topics through `GraphTopic`, `graphLevel`, UI mode selection, and tests together. Add board cases with independently computed expected answers; do not only test an algorithm against itself. Keep exact maximal-set checking for components. Do not replace biconnected blocks with ordinary connected components. For weighted path changes ensure BFS unit-cost and 0–1 weight constraints remain valid. For arbitrary graphs address overflow and recursion limits.

Potential follow-ups: persistent progress, more levels, algorithm-step visualizations, printable/PDF lessons, localization, light theme, interactive node selection, and device/browser UI verification. These are not implemented promises.
