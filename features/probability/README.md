# Probability games

Owns future betting, expected-value, distribution, and options games. `ProbabilityGamesScreen(onBack)` is a navigable placeholder; no probability game is implemented yet.

Put shared rules and Compose screens in `src/commonMain`; put rules tests in `src/commonTest` when a game is added. Keep this module independent of `composeApp` and the other feature modules. The app shell already links to the probability path.
