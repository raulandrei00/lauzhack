package com.playwise.gametheory

import kotlin.random.Random

enum class Difficulty(val title: String, val description: String) {
    NOOB("Noob", "Makes random legal moves"),
    INTUITIVE("Intuitive", "Follows a simple human-looking rule"),
    GTO("GTO", "Chooses a winning move when one exists")
}

enum class PlayMode { SINGLE_PLAYER, MULTIPLAYER }

enum class BoardLevel(val title: String, val description: String) {
    BEGINNER("Beginner", "Classic board: 3, 4, 5"),
    EASY("Easy", "3 piles, 1–5 stones each"),
    MEDIUM("Medium", "4 piles, 1–10 stones each"),
    HARD("💀 Hard", "5–10 piles, 1–64 stones each");

    fun createPiles(random: Random = Random.Default): List<NimPile> = (when (this) {
        BEGINNER -> listOf(3, 4, 5)
        EASY -> randomWinningBoard(3, 5, random)
        MEDIUM -> randomWinningBoard(4, 10, random)
        HARD -> randomWinningBoard(random.nextInt(5, 11), 64, random)
    }).toNimPiles()
}

private fun randomWinningBoard(count: Int, maxPileSize: Int, random: Random): List<Int> {
    val piles = MutableList(count) { random.nextInt(1, maxPileSize + 1) }
    if (piles.reduce(Int::xor) == 0) {
        val index = random.nextInt(count)
        piles[index] = if (piles[index] == maxPileSize) 1 else piles[index] + 1
    }
    return piles
}

fun newNimMatch(
    level: BoardLevel,
    difficulty: Difficulty,
    random: Random = Random.Default
): NimMatch = NimMatch(piles = level.createPiles(random), level = level, difficulty = difficulty)

data class Move(val pile: Int, val count: Int)

data class NimMatch(
    val piles: List<NimPile> = listOf(3, 4, 5).toNimPiles(),
    val level: BoardLevel = BoardLevel.BEGINNER,
    val difficulty: Difficulty = Difficulty.INTUITIVE,
    val winner: String? = null,
    val lastMove: String = "Your turn. Pick a pile and remove at least one stone.",
    val turns: Int = 0,
    val currentPlayer: Int = 0
) {
    val finished: Boolean get() = winner != null

    fun play(move: Move, random: Random = Random.Default): NimMatch {
        if (finished || move.pile !in piles.indices || move.count !in 1..piles[move.pile].stones) return this
        val afterHuman = piles.toMutableList().also {
            it[move.pile] = it[move.pile].copy(stones = it[move.pile].stones - move.count)
        }
        if (afterHuman.all { it.stones == 0 }) {
            return copy(piles = afterHuman, winner = "You", lastMove = "You took the last stone. You win!", turns = turns + 1)
        }

        val reply = chooseAiMove(afterHuman.map { it.stones }, difficulty, random)
        val afterAi = afterHuman.toMutableList().also {
            it[reply.pile] = it[reply.pile].copy(stones = it[reply.pile].stones - reply.count)
        }
        val aiWon = afterAi.all { it.stones == 0 }
        return copy(
            piles = afterAi,
            winner = if (aiWon) "AI" else null,
            lastMove = if (aiWon) "AI took the last stone from pile ${reply.pile + 1}. AI wins."
                else "You took ${move.count} from pile ${move.pile + 1}. AI took ${reply.count} from pile ${reply.pile + 1}.",
            turns = turns + 1
        )
    }

    fun playMultiplayer(move: Move): NimMatch {
        if (finished || move.pile !in piles.indices || move.count !in 1..piles[move.pile].stones) return this
        val player = currentPlayer + 1
        val nextPiles = piles.toMutableList().also {
            it[move.pile] = it[move.pile].copy(stones = it[move.pile].stones - move.count)
        }
        val won = nextPiles.all { it.stones == 0 }
        return copy(
            piles = nextPiles,
            winner = if (won) "Player $player" else null,
            lastMove = if (won) "Player $player took the last stone and wins!"
                else "Player $player took ${move.count} from pile ${move.pile + 1}.",
            turns = turns + 1,
            currentPlayer = if (won) currentPlayer else 1 - currentPlayer
        )
    }
}

fun chooseAiMove(piles: List<Int>, difficulty: Difficulty, random: Random = Random.Default): Move {
    require(piles.any { it > 0 })
    val legal = piles.flatMapIndexed { index, size -> (1..size).map { Move(index, it) } }
    return when (difficulty) {
        Difficulty.NOOB -> legal.random(random)
        Difficulty.INTUITIVE -> {
            // A plausible but fallible heuristic: clear a pile if possible,
            // otherwise take two stones from the largest pile.
            val largest = piles.indices.maxBy { piles[it] }
            Move(largest, if (piles[largest] <= 2) piles[largest] else 2)
        }
        Difficulty.GTO -> {
            val xor = piles.reduce(Int::xor)
            legal.firstOrNull { move -> (xor xor piles[move.pile] xor (piles[move.pile] - move.count)) == 0 }
                ?: legal.random(random)
        }
    }
}
