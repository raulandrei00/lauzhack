package com.playwise.app

import kotlin.random.Random

enum class Difficulty(val title: String, val description: String) {
    NOOB("Noob", "Makes random legal moves"),
    INTUITIVE("Intuitive", "Follows a simple human-looking rule"),
    GTO("GTO", "Plays the optimal Nim strategy")
}

data class Move(val pile: Int, val count: Int)

data class NimMatch(
    val piles: List<Int> = listOf(3, 4, 5),
    val difficulty: Difficulty = Difficulty.INTUITIVE,
    val winner: String? = null,
    val lastMove: String = "Your turn. Pick a pile and remove at least one stone.",
    val turns: Int = 0
) {
    val finished: Boolean get() = winner != null

    fun play(move: Move, random: Random = Random.Default): NimMatch {
        if (finished || move.pile !in piles.indices || move.count !in 1..piles[move.pile]) return this
        val afterHuman = piles.toMutableList().also { it[move.pile] -= move.count }
        if (afterHuman.all { it == 0 }) {
            return copy(piles = afterHuman, winner = "You", lastMove = "You took the last stone. You win!", turns = turns + 1)
        }

        val reply = chooseAiMove(afterHuman, difficulty, random)
        val afterAi = afterHuman.toMutableList().also { it[reply.pile] -= reply.count }
        val aiWon = afterAi.all { it == 0 }
        return copy(
            piles = afterAi,
            winner = if (aiWon) "AI" else null,
            lastMove = if (aiWon) "AI took the last stone from pile ${reply.pile + 1}. AI wins."
                else "You took ${move.count} from pile ${move.pile + 1}. AI took ${reply.count} from pile ${reply.pile + 1}.",
            turns = turns + 1
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
