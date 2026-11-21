# Game theory games

Owns Nim and future game-theory games. Shared Kotlin code lives in `src/commonMain`; its tests live in `src/commonTest`.

- Public app entry point: `GameTheoryPathScreen(onBack)` in `com.playwise.gametheory`.
- `Nim.kt` and `StaircaseNim.kt` contain pure rules and AI choices; their screen files contain Compose UI. `NimPile.kt` is the shared pile model and `XorWorkbench.kt` is the shared subset calculator.
- Run `./gradlew :features:game-theory:desktopTest` (Windows: `.\gradlew.bat :features:game-theory:desktopTest`).

This module should not depend on `composeApp` or the other feature modules. Add new games here and expose their screen to the app shell.


plan:
we want a child in africa to be able to open this game and, level by level, be engaged in more
and more complex nim-games. starting with standard nim, then staircase nim,

we need a few more steps before
''' then chocolate bar nim, then coins nim, then other nim variations.'''
since this is already pretty complex.

## Current progression

The app shows a connected level path. Levels 1 and 2 are playable; later nodes are previews only. Winning a level marks it complete for the current visit to the path.

1. **Classic Nim** — remove stones from one pile; learn XOR. The existing Beginner/Easy/Medium/Hard board settings remain inside this level.
2. **Staircase Nim** — move any positive number of coins from step `i > 0` down exactly one step to `i - 1`. Step 0 is the floor and its coins cannot move. The last player able to move wins. The player investigates which subset of steps helps predict the result.
3. **Take 1 or 2** *(suggested)* — one pile with restricted moves, introducing repeating winning/losing positions.
4. **Two mini-games** *(suggested)* — play two small independent take-away games together, introducing composition.
5. **Find the missing value** *(suggested)* — calculate small mex and Grundy values by hand.
6. **Chocolate bar SG** *(future milestone)* — apply those values to a more complex board.

Levels 3–6 have no rules engine or playable screen yet. The Staircase Nim AI offers the same Noob, Intuitive, and GTO settings as Classic Nim. New staircase boards give the first player a winning move. Player-facing explanations do not disclose the winning subset.

Both playable levels use `NimPile(position, stones)`. Their XOR workbench lets the learner tap any subset of pile or step cards; it displays that subset's XOR score and updates after each move. The calculator never selects a subset for the learner. The board's own pile taps remain dedicated to choosing a move.
