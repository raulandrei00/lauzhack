# Playwise prototype

Playwise is a Kotlin Multiplatform learning app with one shared Compose UI for Android, desktop, and web. The first playable lesson is normal-play Nim. The README lists future graph and probability paths.

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

Run rules tests with `:composeApp:desktopTest`.
