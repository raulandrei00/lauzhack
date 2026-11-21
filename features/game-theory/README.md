# Game theory games

Owns the game-theory progression. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `GameTheoryPathScreen(onBack)` in `com.playwise.gametheory`.
- Rules and AI choices live in `Nim.kt`, `StaircaseNim.kt`, `SubtractionGames.kt`, and `Chocolate.kt`; their screen files contain Compose UI. `NimPile.kt` is the shared pile model and `XorWorkbench.kt` is the shared subset calculator.
- Run `./gradlew :features:game-theory:desktopTest` (Windows: `.\gradlew.bat :features:game-theory:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add new games here and expose their screen to the app shell.


plan:
we want a child in africa to be able to open this game and, level by level, be engaged in more
and more complex nim-games. starting with standard nim, then staircase nim,

we need a few more steps before
''' then chocolate bar nim, then coins nim, then other nim variations.'''
since this is already pretty complex.

## Current progression

The app shows a connected path of six playable levels. Winning a level marks it complete for the current visit to the path. Each game has a hidden theory panel with a short idea, proof, and method for finding an optimal move. A separate hint reveals a move for the current position and explains XOR.

1. **Classic Nim** — remove stones from one pile; learn XOR. The existing Beginner/Easy/Medium/Hard board settings remain inside this level.
2. **Staircase Nim** — move any positive number of coins from step `i > 0` down exactly one step to `i - 1`. Step 0 is the floor and its coins cannot move. The last player able to move wins. The player investigates which subset of steps helps predict the result.
3. **Take up to n** — one pile; choose n from 2 through 5 and remove 1 through n stones. The pile's value repeats every n + 1.
4. **Two mini-games** — choose one of two piles on each turn; the piles have take limits 2 and 3. Combine their values with XOR.
5. **Find the missing value** — two piles where a move removes 1, 3, or 4 stones. Compute values using mex and combine them with XOR.
6. **Chocolate bar SG** — bite a safe square of a small chocolate bar, removing that square and all squares to its right and below. The top-left poisoned square cannot be bitten. The last legal bite wins. Compute shape values with mex.

All levels offer Noob, Intuitive, and GTO opponents. New boards give the first player a winning move. The level 2 winning subset is disclosed only when the player opens its theory or hint.

Levels 1–5 use `NimPile(position, stones)`. Levels 1 and 2 have an XOR workbench that lets the learner tap any subset of pile or step cards; it displays that subset's XOR score and updates after each move. The calculator never selects a subset for the learner. The board's own pile taps remain dedicated to choosing a move.
