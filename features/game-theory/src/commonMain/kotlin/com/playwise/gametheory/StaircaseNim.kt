package com.playwise.gametheory

import kotlin.random.Random

data class StairMove(val step: Int, val count: Int)

fun oddStepXor(steps: List<Int>): Int = steps.indices
    .filter { it % 2 == 1 }
    .fold(0) { xor, step -> xor xor steps[step] }

fun newStaircaseMatch(
    difficulty: Difficulty = Difficulty.INTUITIVE,
    random: Random = Random.Default
): StaircaseMatch {
    val steps = MutableList(5) { if (it == 0) 0 else random.nextInt(1, 5) }
    if (oddStepXor(steps) == 0) {
        steps[1] = if (steps[1] == 4) 1 else steps[1] + 1
    }
    return StaircaseMatch(steps = steps.toNimPiles(), difficulty = difficulty)
}

data class StaircaseMatch(
    val steps: List<NimPile> = listOf(0, 2, 1, 3, 2).toNimPiles(),
    val difficulty: Difficulty = Difficulty.INTUITIVE,
    val winner: String? = null,
    val lastMove: String = "Your move. Slide coins down one step.",
    val turns: Int = 0,
    val currentPlayer: Int = 0
) {
    val finished: Boolean get() = winner != null

    fun play(move: StairMove, random: Random = Random.Default): StaircaseMatch {
        if (finished || move.step !in 1..steps.lastIndex || move.count !in 1..steps[move.step].stones) return this
        val afterHuman = moved(steps, move)
        if (afterHuman.drop(1).all { it.stones == 0 }) {
            return copy(steps = afterHuman, winner = "You", lastMove = "You made the last move. You win!", turns = turns + 1)
        }
        val reply = chooseStairMove(afterHuman.map { it.stones }, difficulty, random)
        val afterAi = moved(afterHuman, reply)
        val aiWon = afterAi.drop(1).all { it.stones == 0 }
        return copy(
            steps = afterAi,
            winner = if (aiWon) "AI" else null,
            lastMove = if (aiWon) "AI made the last move. AI wins."
                else "You slid ${move.count} from step ${move.step}. AI slid ${reply.count} from step ${reply.step}.",
            turns = turns + 1
        )
    }

    fun playMultiplayer(move: StairMove): StaircaseMatch {
        if (finished || move.step !in 1..steps.lastIndex || move.count !in 1..steps[move.step].stones) return this
        val player = currentPlayer + 1
        val nextSteps = moved(steps, move)
        val won = nextSteps.drop(1).all { it.stones == 0 }
        return copy(
            steps = nextSteps,
            winner = if (won) "Player $player" else null,
            lastMove = if (won) "Player $player made the last move and wins!"
                else "Player $player slid ${move.count} from step ${move.step}.",
            turns = turns + 1,
            currentPlayer = if (won) currentPlayer else 1 - currentPlayer
        )
    }
}

private fun moved(steps: List<NimPile>, move: StairMove): List<NimPile> = steps.toMutableList().also {
    it[move.step] = it[move.step].copy(stones = it[move.step].stones - move.count)
    it[move.step - 1] = it[move.step - 1].copy(stones = it[move.step - 1].stones + move.count)
}

fun chooseStairMove(
    steps: List<Int>,
    difficulty: Difficulty,
    random: Random = Random.Default
): StairMove {
    require(steps.drop(1).any { it > 0 })
    val legal = (1..steps.lastIndex).flatMap { step ->
        (1..steps[step]).map { count -> StairMove(step, count) }
    }
    return when (difficulty) {
        Difficulty.NOOB -> legal.random(random)
        Difficulty.INTUITIVE -> {
            val highest = (1..steps.lastIndex).last { steps[it] > 0 }
            StairMove(highest, minOf(2, steps[highest]))
        }
        Difficulty.GTO -> legal.firstOrNull { move ->
            val after = steps.toMutableList().also {
                it[move.step] -= move.count
                it[move.step - 1] += move.count
            }
            oddStepXor(after) == 0
        }
            ?: legal.random(random)
    }
}
