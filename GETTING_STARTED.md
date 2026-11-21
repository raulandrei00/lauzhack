# Playwise prototype

Playwise is a Kotlin Multiplatform learning app with one shared Compose UI for Android, desktop, and web. Play normal-play Nim and five graph lessons. Probability lessons remain planned.

## Run

Install JDK 17 or newer. Android also needs the Android SDK; the browser target needs a browser with WasmGC support. Open this folder in IntelliJ IDEA with the Kotlin Multiplatform plugin, or run:

```powershell
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :composeApp:installDebug
```

On macOS/Linux, replace `.\gradlew.bat` with `sh ./gradlew`. Android's `installDebug` needs a running emulator or attached device. The web build can also be produced with `:composeApp:wasmJsBrowserDistribution`.

## Nim

Open the game-theory path from the home screen. **Level 1, Classic Nim:** click a pile, choose how many stones to remove, and take your turn. The last player to take a stone wins. **Noob** chooses a random legal move. **Intuitive** uses a simple fixed heuristic. **GTO** uses Nim XOR: when possible it leaves the pile XOR at zero. The strategy card under the board explains the opening move.

Board level is independent of opponent difficulty. **Beginner** uses the fixed 3, 4, 5 board; **Easy** randomizes three piles with 1–5 stones each; **Medium** randomizes four piles with 1–10 stones each; **💀 Hard** randomizes 5–10 piles with 1–64 stones each. Changing level or selecting **New game** starts a fresh board.

The player always takes the first turn. Every starting board has a nonzero pile XOR, so the player has a winning move under optimal play.

**Level 2, Staircase Nim:** move one or more coins from a step to the step directly below it. Coins on step 0 cannot move. The last mover wins. New boards give the first player a winning move. The player-facing lesson asks which subset of steps matters without revealing it. The path also shows suggested later levels as locked previews; they are not games yet. Completing a playable level marks it on the path for the current visit.

Both levels include an XOR calculator below the board. Tap any combination of pile or step cards to see its score; this selection is independent of the pile selected for a move. The score updates as counts change, and a new board clears the calculator selection.

Run Nim rules tests with `:features:game-theory:desktopTest`.

## Graph games

The Graphs screen starts with six **Play together** games: Sprouts, Graph Coloring Duel, Shannon Switching, Node Kayles, Shortest-Path Challenge, and Dots and Boxes. They use local two-player turns on one screen; no account or network is needed. The five solo algorithm lessons are listed below them.

Sprouts: tap an existing dot, tap bends, then tap an endpoint; choose the new-dot bend and commit. Lines cannot cross. The board uses polygonal curves and a concession button, not automatic detection of every possible remaining curve. Shortest-Path Challenge: spend up to five sabotage points, lock the costs, then let the second player construct an optimal route.

Open **Graphs** from the home screen for Dijkstra, BFS, 0–1 BFS, biconnected components, and SCC. Each has random boards, hints, answer feedback, theory, and implementation pseudocode. Choose nodes with the labeled buttons below the diagram; use Check delivery or Claim component to submit. **New random board** generates another puzzle; **Restart** retries the same puzzle. In two-player games, **New random game** resets all state and generates a new board. See [DEVELOPER_HANDOFF.md](DEVELOPER_HANDOFF.md) for rules, architecture, setup, tests, and extension guidance.

## Work in parallel

The Gradle modules have separate source and test folders:

| Module | Owns | Tests |
| --- | --- | --- |
| `features/graph` | Graph lessons and two-player games | `:features:graph:desktopTest` |
| `features/game-theory` | Nim and future strategy games | `:features:game-theory:desktopTest` |
| `features/probability` | Probability path scaffold and future games | Add `commonTest` with the first game |

`composeApp` owns the home screen, navigation, theme, and Android/desktop/web entry points. It depends on all three feature modules. Feature modules do not depend on each other or on `composeApp`, so each person can work in their own module. Run `:composeApp:assembleDebug`, `:composeApp:compileKotlinDesktop`, and `:composeApp:compileKotlinWasmJs` after changing the app integration.
