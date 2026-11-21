package com.playwise.gametheory

import kotlin.random.Random

data class Bite(val column: Int, val row: Int)

/** Column heights, left to right. The poisoned square is (0, 0). */
data class ChocolateBoard(val heights: List<Int>) {
    init { require(heights.isNotEmpty() && heights[0] >= 1 && heights.all { it >= 0 } && heights.zipWithNext().all { (a, b) -> a >= b }) }

    fun legalMoves(): List<Bite> = heights.indices.flatMap { column ->
        (0 until heights[column]).filter { column != 0 || it != 0 }.map { Bite(column, it) }
    }

    fun bite(move: Bite): ChocolateBoard {
        require(move in legalMoves())
        return ChocolateBoard(heights.mapIndexed { column, height ->
            if (column >= move.column) minOf(height, move.row) else height
        })
    }
}

fun chocolateGrundy(board: ChocolateBoard, memo: MutableMap<ChocolateBoard, Int> = mutableMapOf()): Int {
    memo[board]?.let { return it }
    val reachable = board.legalMoves().map { chocolateGrundy(board.bite(it), memo) }.toSet()
    var value = 0
    while (value in reachable) value++
    memo[board] = value
    return value
}

fun ChocolateBoard.bestBite(): Pair<Bite, Boolean> {
    val legal = legalMoves()
    require(legal.isNotEmpty())
    val memo = mutableMapOf<ChocolateBoard, Int>()
    val winning = legal.firstOrNull { chocolateGrundy(bite(it), memo) == 0 }
    return (winning ?: legal.first()) to (winning != null)
}

data class ChocolateMatch(
    val board: ChocolateBoard = ChocolateBoard(listOf(3, 3, 3)),
    val difficulty: Difficulty = Difficulty.INTUITIVE,
    val winner: String? = null,
    val lastMove: String = "Your move. Bite a safe square; it and all squares right and below disappear.",
    val turns: Int = 0
) {
    val finished: Boolean get() = winner != null

    fun play(move: Bite, random: Random = Random.Default): ChocolateMatch {
        if (finished || move !in board.legalMoves()) return this
        val afterHuman = board.bite(move)
        if (afterHuman.legalMoves().isEmpty()) return copy(board = afterHuman, winner = "You",
            lastMove = "You left only the poisoned square. You win!", turns = turns + 1)
        val reply = when (difficulty) {
            Difficulty.NOOB -> afterHuman.legalMoves().random(random)
            Difficulty.INTUITIVE -> afterHuman.legalMoves().maxBy { bite ->
                board.heights.sum() - afterHuman.bite(bite).heights.sum()
            }
            Difficulty.GTO -> afterHuman.bestBite().first
        }
        val afterAi = afterHuman.bite(reply)
        return copy(board = afterAi, winner = if (afterAi.legalMoves().isEmpty()) "AI" else null,
            lastMove = "You bit (${move.column + 1}, ${move.row + 1}); AI bit (${reply.column + 1}, ${reply.row + 1}).",
            turns = turns + 1)
    }
}

fun newChocolateMatch(difficulty: Difficulty = Difficulty.INTUITIVE, random: Random = Random.Default): ChocolateMatch {
    val choices = listOf(listOf(2, 2), listOf(3, 3), listOf(3, 3, 3), listOf(3, 3, 3, 3))
    val winning = choices.map(::ChocolateBoard).filter { chocolateGrundy(it) != 0 }
    return ChocolateMatch(board = winning.random(random), difficulty = difficulty)
}
