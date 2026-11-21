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

Click a pile, choose how many stones to remove, and take your turn. The last player to take a stone wins. **Noob** chooses a random legal move. **Intuitive** uses a simple fixed heuristic. **GTO** uses Nim XOR: when possible it leaves the pile XOR at zero. The strategy card under the board explains the opening move.

Board level is independent of opponent difficulty. **Beginner** uses the fixed 3, 4, 5 board; **Easy** randomizes three piles with 1–5 stones each; **Medium** randomizes four piles with 1–10 stones each; **💀 Hard** randomizes 5–10 piles with 1–64 stones each. Changing level or selecting **New game** starts a fresh board.

The player always takes the first turn. Every starting board has a nonzero pile XOR, so the player has a winning move under optimal play.

Run rules tests with `:composeApp:desktopTest`.

## Graph games

Open **Graphs** from the home screen for Dijkstra, BFS, 0–1 BFS, biconnected components, and SCC. Each has three boards, hints, answer feedback, theory, and implementation pseudocode. Choose nodes with the labeled buttons below the diagram; use Check delivery or Claim component to submit. See [DEVELOPER_HANDOFF.md](DEVELOPER_HANDOFF.md) for rules, architecture, setup, tests, and extension guidance.
