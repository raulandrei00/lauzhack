package com.playwise.gametheory

/**
 * Teaching content for the Nim journey. Pure data (no Compose), so it is unit-tested:
 * NimLearningTest checks every worked example against the real game code.
 */
data class NimResource(val title: String, val url: String, val note: String)

data class NimQuiz(val question: String, val answer: String)

data class NimGuide(
    val summary: String,
    val intuition: List<String>,
    val terms: List<Pair<String, String>>,
    val exampleTitle: String,
    val exampleSteps: List<String>,
    /** Small experiments to run on the board. */
    val tryThis: List<String>,
    val pseudocode: String,
    val whyItWorks: String,
    val pitfalls: List<String>,
    val quiz: List<NimQuiz>,
    val resources: List<NimResource>,
    val inThisApp: String,
)

private val wikiNim = NimResource("Wikipedia: Nim", "https://en.wikipedia.org/wiki/Nim",
    "History, the XOR rule, and many variants, including the misère version.")
private val cpSprague = NimResource("CP-Algorithms: Sprague-Grundy theorem and Nim", "https://cp-algorithms.com/game_theory/sprague-grundy-nim.html",
    "A clear proof with mex, Grundy values, and code.")
private val wikiSprague = NimResource("Wikipedia: Sprague-Grundy theorem", "https://en.wikipedia.org/wiki/Sprague%E2%80%93Grundy_theorem",
    "The theorem that turns every game in this journey into Nim.")
private val ferguson = NimResource("Thomas S. Ferguson: Game Theory, Part I (free notes)", "https://www.math.ucla.edu/~tom/Game_Theory/comb.pdf",
    "University notes on impartial games: P/N positions, Nim, subtraction games, Sprague-Grundy. Has exercises with solutions.")
private val winningWays = NimResource("Berlekamp, Conway, Guy: Winning Ways for your Mathematical Plays", "",
    "The classic book on combinatorial games, full of playful examples.")
private val bouton = NimResource("Charles L. Bouton: \"Nim, a game with a complete mathematical theory\" (Annals of Mathematics, 1901)", "",
    "The first paper to solve Nim with binary arithmetic.")

/** The basics every level builds on. */
val nimPrimer = NimGuide(
    summary = "In these games nothing is hidden and nothing is left to luck. With perfect play, every position is already a win or a loss for the player about to move.",
    intuition = listOf(
        "Both players can make the same moves, and whoever makes the last move wins. Games like this are called impartial games under normal play.",
        "A position is LOSING (also called a P-position) if every move you can make hands your opponent a winning position.",
        "A position is WINNING (also called an N-position) if at least one move hands your opponent a losing position.",
        "The finished game, with no moves left, is losing for the player to move: they cannot move, so they lose. Starting from there, you can label every position, one step at a time.",
    ),
    terms = listOf(
        "Normal play" to "The player who makes the last move wins. Equivalently, the player who cannot move loses.",
        "Impartial game" to "Both players have exactly the same moves from every position. Chess is not impartial (you can only move your own pieces); Nim is.",
        "P-position (losing)" to "The Previous player, who just moved, will win with perfect play. You do not want to be the one to move here.",
        "N-position (winning)" to "The Next player, who is about to move, will win with perfect play.",
        "Perfect play" to "Always choosing a best move. The GTO opponent in this app plays perfectly whenever it can.",
        "Strategy" to "A rule that tells you which move to make in every position.",
    ),
    exampleTitle = "Labeling a pile where you may take 1 or 2 stones",
    exampleSteps = listOf(
        "0 stones: no moves, so LOSING for the player to move.",
        "1 stone: take 1 and leave 0, a losing position for your opponent. WINNING.",
        "2 stones: take 2 and leave 0. WINNING.",
        "3 stones: you can only leave 2 or 1, and both are winning for your opponent. LOSING.",
        "4 and 5 stones: you can move to 3 (losing). WINNING. 6 stones: you can only reach 5 or 4. LOSING.",
        "Pattern: the losing sizes are 0, 3, 6, 9, ... Level 3 explains why.",
    ),
    tryThis = listOf(
        "Before each move, ask: \"Can I leave my opponent a losing position?\"",
        "Play against Noob, then against GTO. Notice how GTO never gives you a second chance once you slip.",
    ),
    pseudocode = """
        // Label positions as winning or losing, starting from the end.
        memo = {}
        function isWinning(position):
            if position in memo: return memo[position]
            for each move from position:
                if not isWinning(position after move):
                    memo[position] = true        // found a move to a LOSING position
                    return true
            memo[position] = false               // every move helps the opponent
            return false                         // (also true when there are no moves)
    """.trimIndent(),
    whyItWorks = "Every game in this journey ends after finitely many moves, because stones, coins or chocolate squares keep running out. So we can reason backwards from the end (mathematicians call this induction). The finished game is losing. A position is winning exactly when some move reaches a losing position, and losing exactly when no move does. Each position therefore gets exactly one label.",
    pitfalls = listOf(
        "Thinking that moving first always helps. In a losing position, moving first is the problem.",
        "Grabbing as many stones as possible. Big moves are not better moves.",
        "Confusing normal play with misère play, where whoever takes the last stone LOSES. Every game in this journey uses normal play.",
    ),
    quiz = listOf(
        NimQuiz("Take 1 or 2 stones per turn. You face 7 stones. Winning or losing?", "Winning: take 1 and leave 6, which is a multiple of 3 and therefore losing for your opponent."),
        NimQuiz("A position has three possible moves, and all three lead to winning positions. What is it?", "Losing. Every move you make hands your opponent a win."),
        NimQuiz("Why is a position with no moves left a loss for the player to move?", "Under normal play, the player who cannot move loses."),
    ),
    resources = listOf(wikiNim, ferguson,
        NimResource("Wikipedia: Combinatorial game theory", "https://en.wikipedia.org/wiki/Combinatorial_game_theory", "The field these games come from."),
        winningWays),
    inThisApp = "Each opponent's moves live in Nim.kt, StaircaseNim.kt, SubtractionGames.kt and Chocolate.kt. The GTO opponent always tries to leave you a losing position.",
)

fun nimGuide(level: Int): NimGuide = when (level) {
    1 -> NimGuide(
        summary = "XOR all pile sizes. Zero means the player to move is losing. Always try to leave your opponent a zero.",
        intuition = listOf(
            "Write each pile size in binary (1s and 0s). Look at each column: is the number of 1s even or odd?",
            "If every column is even, the board is \"balanced\". Any move you make unbalances it, and your opponent can always rebalance it.",
            "Whoever keeps the board balanced after their own turn eventually takes the last stone.",
            "Adding up the columns while ignoring carries, keeping only odd or even, is called XOR (written ⊕ or xor).",
        ),
        terms = listOf(
            "Binary" to "Writing numbers with only 1s and 0s: 1 = 001, 2 = 010, 3 = 011, 4 = 100, 5 = 101.",
            "XOR" to "Column by column: the answer has a 1 where an odd number of the inputs have a 1. 3 xor 5 = 011 xor 101 = 110 = 6.",
            "Nim-sum" to "The XOR of all pile sizes.",
            "Balanced board" to "A board whose nim-sum is 0. It is a losing position for the player to move.",
        ),
        exampleTitle = "Winning from 3, 4, 5",
        exampleSteps = listOf(
            "In binary: 3 = 011, 4 = 100, 5 = 101.",
            "Columns: the 4s column has two 1s (even), the 2s column one 1 (odd), the 1s column two 1s (even). So the nim-sum is 010 = 2.",
            "Find a pile with a 1 in the leftmost column of the nim-sum (the 2s column): pile 1, which has 3 = 011.",
            "New size = 3 xor 2 = 1. So take 3 - 1 = 2 stones from pile 1, leaving 1, 4, 5.",
            "Check: 001 xor 100 xor 101 = 000. Balanced, so your opponent is now in a losing position.",
            "If they empty pile 3 (leaving 1, 4, 0, nim-sum 5), take 3 from pile 2 (leaving 1, 1, 0, nim-sum 0). Keep rebalancing until you take the last stone.",
        ),
        tryThis = listOf(
            "Select every pile in the XOR calculator. Make a move and watch the score: after a good move it shows 0.",
            "Two equal piles (like 5 and 5) are balanced. Copy whatever your opponent does on the other pile and you'll win.",
            "Try Hard mode: with 64-stone piles, guessing is hopeless, but the binary method still works.",
        ),
        pseudocode = """
            function bestNimMove(piles):
                x = 0
                for each pile size a: x = x xor a      // the nim-sum
                if x == 0:
                    return "no winning move: take 1 stone and hope"
                for each pile index i:
                    target = piles[i] xor x
                    if target < piles[i]:              // this pile has x's top bit
                        return take (piles[i] - target) from pile i
        """.trimIndent(),
        whyItWorks = "From nim-sum 0, any move changes exactly one pile, which changes at least one binary digit, so the nim-sum stops being 0. From a nonzero nim-sum x, take the highest 1-bit of x. Some pile a has a 1 there, and a xor x clears that bit, so it is smaller than a. Shrinking that pile to a xor x makes the nim-sum 0. The empty board has nim-sum 0, so by working backwards from the end, the zero positions are exactly the losing ones. Charles Bouton proved this in 1901.",
        pitfalls = listOf(
            "Adding instead of XOR-ing. 3 + 5 = 8, but 3 xor 5 = 6.",
            "Picking a pile without the top bit of the nim-sum. Then a xor x is bigger than a, so it would mean adding stones.",
            "Panicking when the nim-sum is 0 on your turn. You can't force a win, so make a small move and wait for a mistake.",
        ),
        quiz = listOf(
            NimQuiz("Piles 1, 2, 3. Winning or losing for the player to move?", "Losing: 01 xor 10 xor 11 = 00."),
            NimQuiz("Piles 2 and 7. What is the winning move?", "Nim-sum 2 xor 7 = 5. 7 xor 5 = 2, so take 5 from the 7-pile and leave 2 and 2."),
            NimQuiz("What is 6 xor 6? And 6 xor 0?", "6 xor 6 = 0 (every column has two 1s). 6 xor 0 = 6."),
        ),
        resources = listOf(wikiNim, cpSprague, ferguson, bouton,
            NimResource("Wikipedia: Exclusive or", "https://en.wikipedia.org/wiki/Exclusive_or", "More about XOR, the operation behind every computer.")),
        inThisApp = "optimalNimMove() in OptimalHints.kt and chooseAiMove() (the GTO branch) in Nim.kt.",
    )

    2 -> NimGuide(
        summary = "Spoiler: only the odd-numbered steps matter. Treat steps 1, 3, 5, ... as Nim piles and ignore the rest.",
        intuition = listOf(
            "Sliding coins from an odd step down to an even step is like removing stones from a Nim pile. They leave the odd steps.",
            "Sliding coins from an even step down onto an odd step adds coins there, but you can always undo it: slide those same coins one more step down.",
            "So the even steps are a trap and a decoy. The real game is Nim on the odd steps.",
        ),
        terms = listOf(
            "Step" to "A pile with a number. Step 0 is the floor, and coins there are out of the game.",
            "Odd steps" to "Steps 1, 3, 5, ... They decide the game.",
            "Reversible move" to "A move your opponent can cancel straight away, like sliding coins off an even step.",
        ),
        exampleTitle = "Steps: floor 0, step 1 = 2, step 2 = 1, step 3 = 3, step 4 = 2",
        exampleSteps = listOf(
            "Odd steps hold 2 (step 1) and 3 (step 3). Their XOR: 10 xor 11 = 01 = 1.",
            "Which odd step can be fixed? Step 1: 2 xor 1 = 3, bigger, so no. Step 3: 3 xor 1 = 2, smaller, so yes.",
            "Slide 1 coin from step 3 down to step 2. The odd steps are now 2 and 2, with XOR 0.",
            "Your opponent slides 2 coins from step 4 onto step 3, which becomes 4 (odd XOR 2 xor 4 = 6).",
            "Undo it: slide 2 coins from step 3 down to step 2. Step 3 is back to 2 and the XOR is 0 again.",
        ),
        tryThis = listOf(
            "Use the XOR calculator with different groups of steps. Which group's score is always 0 after the GTO opponent moves?",
            "Count how often the GTO opponent moves coins off an even step, and what you could do next.",
        ),
        pseudocode = """
            function bestStairMove(steps):          // steps[0] is the floor
                x = 0
                for i = 1, 3, 5, ...: x = x xor steps[i]
                if x == 0: return "no winning move"
                for each odd i:
                    target = steps[i] xor x
                    if target < steps[i]:
                        return slide (steps[i] - target) coins from step i to step i-1
        """.trimIndent(),
        whyItWorks = "A slide from an odd step removes coins from one odd pile, exactly like a Nim move. A slide from an even step adds coins to the odd step below, and you answer by sliding the same number of coins from that odd step down to the next even step. That restores the odd counts exactly. From an odd-step XOR of 0, every odd move breaks the zero, and every even move can be undone. From a nonzero XOR, the Nim construction lowers one odd step to reach zero. Each slide moves coins closer to the floor, so the game always ends.",
        pitfalls = listOf(
            "XOR-ing every step. The even steps and the floor do not count.",
            "Forgetting that coins land on the step below. They don't vanish until they reach the floor.",
            "Treating the floor as a pile. Floor coins can never move again.",
        ),
        quiz = listOf(
            NimQuiz("Steps 1 to 4 hold 3, 5, 3, 9. Winning or losing for the player to move?", "Losing: the odd steps are 3 and 3, and 3 xor 3 = 0."),
            NimQuiz("Your opponent slides 4 coins from step 2 to step 1. What is your reply?", "Slide those 4 coins from step 1 to the floor. The odd steps return to where they were."),
            NimQuiz("Why can't the game go on forever?", "Every move moves coins down, and coins on the floor never move again."),
        ),
        resources = listOf(ferguson, wikiNim, cpSprague),
        inThisApp = "optimalStairMove() in OptimalHints.kt, oddStepXor() and chooseStairMove() in StaircaseNim.kt.",
    )

    3 -> NimGuide(
        summary = "If you may take 1 to n stones, the losing sizes are multiples of n + 1. Take the remainder, then complete each round to n + 1.",
        intuition = listOf(
            "With n = 3, whatever your opponent takes (1, 2 or 3), you can take the rest of 4 (3, 2 or 1).",
            "So if the pile is a multiple of 4 after your turn, it stays a multiple of 4 after each round, until it hits 0 on your turn.",
            "A pile's value is its remainder after dividing by n + 1. This is your first Grundy value.",
        ),
        terms = listOf(
            "Subtraction game" to "A pile game where the move set says how many stones you may take.",
            "Remainder (mod)" to "What is left after dividing. 11 mod 4 = 3, because 11 = 2 × 4 + 3.",
            "Period" to "The win/lose pattern repeats every n + 1 sizes.",
            "Grundy value" to "A number for each position. 0 means losing. Here it equals the size mod (n + 1).",
        ),
        exampleTitle = "Take up to 3, pile of 11",
        exampleSteps = listOf(
            "n + 1 = 4. 11 mod 4 = 3, so the pile is winning.",
            "Take 3, leaving 8, a multiple of 4.",
            "Opponent takes 1 (7 left), so you take 3 (4 left). Opponent takes 2 (2 left), so you take 2 (0 left). You win.",
            "Every round, your take plus theirs = 4.",
        ),
        tryThis = listOf(
            "Change n with the buttons and find the new losing sizes with the Grundy table.",
            "Play a whole game where your take plus the computer's take always equals n + 1.",
        ),
        pseudocode = """
            function bestTake(pile, n):
                r = pile mod (n + 1)
                if r == 0: return "no winning move"
                return take r stones
        """.trimIndent(),
        whyItWorks = "From a multiple of n + 1, taking 1 to n always lands on a non-multiple. From a non-multiple with remainder r, taking r lands on a multiple. 0 is a multiple and is losing, so working backwards from the end, the multiples are exactly the losing sizes. The reachable values from size s are all the other remainders, so the smallest missing one (mex) is s mod (n + 1).",
        pitfalls = listOf(
            "Using multiples of n instead of n + 1.",
            "Taking the most you can at the start. The remainder is what matters.",
        ),
        quiz = listOf(
            NimQuiz("Take up to 2. Pile of 9. Winning or losing?", "Losing: 9 is a multiple of 3."),
            NimQuiz("Take up to 5. Pile of 20. What do you take?", "20 mod 6 = 2. Take 2 and leave 18."),
            NimQuiz("Take up to 3. Why is 4 losing?", "You can only leave 3, 2 or 1, and your opponent then takes them all."),
        ),
        resources = listOf(
            NimResource("Wikipedia: Subtraction game", "https://en.wikipedia.org/wiki/Subtraction_game", "Subtraction games and their periodic patterns."),
            ferguson, cpSprague),
        inThisApp = "grundy(stones, allowed) and TakeMatch in SubtractionGames.kt.",
    )

    4 -> NimGuide(
        summary = "Two separate games played side by side act like Nim with piles equal to their values. XOR the values, not the stone counts.",
        intuition = listOf(
            "Each pile has its own rule: pile 1 lets you take up to 2, pile 2 up to 3.",
            "Turn each pile into its value with the previous level's trick: pile 1 is worth size mod 3, pile 2 size mod 4.",
            "Then pretend those values are Nim piles and use the XOR rule from level 1.",
        ),
        terms = listOf(
            "Sum of games" to "Several games side by side. On your turn you move in exactly one of them.",
            "Value (Grundy value)" to "The size of the Nim pile that a game behaves like.",
            "Sprague-Grundy theorem" to "The value of a sum of games is the XOR of the values of its parts.",
        ),
        exampleTitle = "Piles 5 (take up to 2) and 7 (take up to 3)",
        exampleSteps = listOf(
            "Values: 5 mod 3 = 2 and 7 mod 4 = 3. XOR: 10 xor 11 = 01 = 1, not 0, so you can win.",
            "Target for pile 1: 2 xor 1 = 3. A take-up-to-2 pile only ever has values 0, 1 or 2, so that won't work.",
            "Target for pile 2: 3 xor 1 = 2. Take 1 from the 7-pile, leaving 6, worth 6 mod 4 = 2.",
            "New values 2 and 2: XOR 0. Your opponent is now in a losing position.",
            "Notice that the stone counts (5 xor 6 = 3) don't tell you this. Only the values do.",
        ),
        tryThis = listOf(
            "Fill in both Grundy tables, then compare the computer's moves with them.",
            "Find a position where both piles have the same value. Why is it losing?",
        ),
        pseudocode = """
            value(pile) = grundy(pile.stones, pile.allowed)     // here: stones mod (cap + 1)
            function bestMove(piles):
                x = value(pile 1) xor value(pile 2)
                if x == 0: return "no winning move"
                for each pile p and each allowed take t:
                    if value(p after taking t) xor value(other pile) == 0:
                        return take t from p
        """.trimIndent(),
        whyItWorks = "A Grundy value g means the game can move to positions of every smaller value 0, 1, ..., g - 1, but never to another position of value g. That is exactly how a Nim pile of size g behaves. Some moves can raise the value, but the opponent can always lower it back again. So the combination works like Nim with piles g1 and g2, and level 1's XOR proof applies unchanged.",
        pitfalls = listOf(
            "XOR-ing stone counts instead of values.",
            "Forgetting that each pile has its own take limit.",
        ),
        quiz = listOf(
            NimQuiz("Pile A: take up to 2, 6 stones. Pile B: take up to 3, 8 stones. Who wins?", "The values are 6 mod 3 = 0 and 8 mod 4 = 0, so the XOR is 0 and the player to move loses."),
            NimQuiz("Values 1 and 3. What value should the 3-pile get?", "1 xor 3 = 2, so lower the 3-pile's value from 3 to 2."),
            NimQuiz("Can one pile's move change the other pile's value?", "No. The games are independent, which is why XOR works."),
        ),
        resources = listOf(wikiSprague, cpSprague, ferguson),
        inThisApp = "TakeMatch.value and bestMove() in SubtractionGames.kt.",
    )

    5 -> NimGuide(
        summary = "When moves are irregular (take 1, 3 or 4), compute each pile's value with mex, the smallest number you can't reach.",
        intuition = listOf(
            "mex means \"minimum excluded\": list the values you can reach in one move, then pick the smallest whole number that is NOT in the list.",
            "Build the table from 0 upwards. Each new value only needs values you have already worked out.",
            "For moves {1, 3, 4} the values repeat: 0 1 0 1 2 3 2, 0 1 0 1 2 3 2, ...",
        ),
        terms = listOf(
            "mex" to "Minimum excluded value. mex{0, 1, 3} = 2, and mex{1, 2} = 0.",
            "Grundy table" to "The list g(0), g(1), g(2), ... for one pile rule.",
            "Period" to "The length after which the table repeats. For {1, 3, 4} it is 7.",
        ),
        exampleTitle = "Building the table for moves {1, 3, 4}",
        exampleSteps = listOf(
            "g(0) = 0, because there are no moves.",
            "g(1): reach 0, whose value is 0. mex{0} = 1.",
            "g(2): reach 1 (value 1). mex{1} = 0. So 2 stones is LOSING.",
            "g(3): reach 2 (0) and 0 (0). mex{0} = 1. g(4): reach 3 (1), 1 (1), 0 (0). mex{0, 1} = 2.",
            "g(5): reach 4 (2), 2 (0), 1 (1). mex{0, 1, 2} = 3. g(6): reach 5 (3), 3 (1), 2 (0). mex = 2.",
            "Piles 6 and 5: values 2 xor 3 = 1. Take 1 from the 5-pile (to 4, value 2) and the values are 2 xor 2 = 0.",
        ),
        tryThis = listOf(
            "Use the step-by-step table: predict each value before you press the button.",
            "Find the first size that repeats the pattern. Can you explain why?",
        ),
        pseudocode = """
            function grundyTable(maxSize, moves):
                g[0] = 0
                for s = 1 to maxSize:
                    reachable = { g[s - m] for each m in moves if m <= s }
                    g[s] = mex(reachable)
                return g

            function mex(set):
                v = 0
                while v in set: v = v + 1
                return v
        """.trimIndent(),
        whyItWorks = "By definition of mex, a position of value 0 has no move to value 0, and a position of value g > 0 has moves to every value below g. That is the Nim-pile property, so a value-0 position is losing by the same backwards-from-the-end argument as level 1. For two piles, the Sprague-Grundy theorem says to XOR their values.",
        pitfalls = listOf(
            "Thinking that mex is the maximum. mex{0, 1, 5} = 2, not 5 or 6.",
            "Including moves that take more stones than the pile has.",
            "Assuming the value equals the size. g(2) = 0 here even though 2 > 0.",
        ),
        quiz = listOf(
            NimQuiz("mex{0, 2, 3} = ?", "1"),
            NimQuiz("With moves {1, 3, 4}, is a pile of 7 winning?", "No: g(7) = mex{g(6), g(4), g(3)} = mex{2, 2, 1} = 0, so it is losing."),
            NimQuiz("Piles 2 and 9, moves {1, 3, 4}. Who wins?", "g(2) = 0 and g(9) = 0, so the XOR is 0 and the player to move loses."),
        ),
        resources = listOf(cpSprague, wikiSprague,
            NimResource("Wikipedia: Mex (mathematics)", "https://en.wikipedia.org/wiki/Mex_(mathematics)", "The definition, with examples from game theory."),
            ferguson),
        inThisApp = "grundy(stones, allowed) in SubtractionGames.kt computes exactly this table.",
    )

    else -> NimGuide(
        summary = "Any game can be given a Grundy value: list the shapes one bite away, then take the mex of their values.",
        intuition = listOf(
            "A bite eats a square and everything to its right and below. The top-left square is poison, and whoever is left with only poison has lost.",
            "Every bite leaves a smaller shape, so we can value shapes from the smallest up, just like the piles in level 5.",
            "Fun fact: on a full rectangle bigger than 1 × 1, the first player can always win, even though nobody knows a simple rule for how!",
        ),
        terms = listOf(
            "Shape" to "The chocolate that is left. Here it is described by the column heights.",
            "Successor" to "A shape reachable with one bite.",
            "Chomp" to "The famous name of this game (David Gale, 1974).",
            "Strategy stealing" to "A proof that a first-player win exists without saying which move wins.",
        ),
        exampleTitle = "The 2 × 2 bar",
        exampleSteps = listOf(
            "The poison alone has value 0, because there are no moves.",
            "A 1 × 2 strip (2 squares in a row or column) can only be bitten down to the poison: mex{0} = 1.",
            "The L-shape (2 × 2 minus the bottom-right square) can become either 1 × 2 strip, both worth 1: mex{1} = 0. LOSING.",
            "The full 2 × 2: its bites lead to the two strips (1) and the L-shape (0). mex{0, 1} = 2.",
            "Winning move: bite the bottom-right square (column 2, row 2), leaving the L-shape for your opponent.",
        ),
        tryThis = listOf(
            "On a square bar, try biting the square diagonally next to the poison, then copy your opponent's bites mirrored.",
            "Play against GTO and open the hint after each of its bites. It should say you have no winning move, because GTO leaves you value 0.",
        ),
        pseudocode = """
            memo = {}
            function value(shape):
                if shape in memo: return memo[shape]
                reachable = { value(shape after bite b) for each safe square b }
                memo[shape] = mex(reachable)         // poison only: empty set, value 0
                return memo[shape]

            function bestBite(shape):
                return any bite b with value(shape after b) == 0, if one exists
        """.trimIndent(),
        whyItWorks = "Each bite removes at least one square, so the recursion always reaches the poison-only shape (value 0). By the mex definition, value 0 means no bite reaches value 0, and any other value means some bite does. So value-0 shapes are exactly the losing ones. Strategy-stealing argument for full rectangles: suppose the second player could always win. The first player bites only the bottom-right square. If the reply that \"wins\" were some bite X, the first player could have played X directly, because X also removes the bottom-right square. Contradiction.",
        pitfalls = listOf(
            "Biting a square and forgetting that everything right and below it is eaten too.",
            "Biting the poison. It is never a legal move.",
            "Assuming that taking more chocolate is better. Look at the value instead.",
        ),
        quiz = listOf(
            NimQuiz("Only the poison and one square to its right remain. Who wins?", "The player to move: bite that square and leave only poison."),
            NimQuiz("What is the value of the L-shape (2 × 2 without the corner)?", "0. Both bites lead to strips of value 1, so it is losing."),
            NimQuiz("Does strategy stealing tell you the winning first move?", "No. It only proves that a winning first move exists."),
        ),
        resources = listOf(
            NimResource("Wikipedia: Chomp", "https://en.wikipedia.org/wiki/Chomp", "Rules, the strategy-stealing proof, and open problems."),
            cpSprague, winningWays),
        inThisApp = "chocolateGrundy() and bestBite() in Chocolate.kt.",
    )
}
