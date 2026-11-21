package com.playwise.gametheory

import kotlin.random.Random

data class TakeMove(val pile: Int, val count: Int)
data class TakeSuggestion(val move: TakeMove, val winning: Boolean)

fun grundy(stones: Int, allowed: List<Int>): Int {
    require(stones >= 0 && allowed.isNotEmpty() && allowed.all { it > 0 })
    val values = IntArray(stones + 1)
    for (size in 1..stones) {
        val reachable = allowed.filter { it <= size }.map { values[size - it] }.toSet()
        var mex = 0
        while (mex in reachable) mex++
        values[size] = mex
    }
    return values[stones]
}

data class TakeMatch(
    val level: Int,
    val piles: List<NimPile>,
    val allowed: List<List<Int>>,
    val difficulty: Difficulty = Difficulty.INTUITIVE,
    val winner: String? = null,
    val lastMove: String = "Your move. Choose one pile and a legal amount.",
    val turns: Int = 0
) {
    init { require(level in 3..5 && piles.size == allowed.size) }
    val finished: Boolean get() = winner != null
    val value: Int get() = piles.indices.fold(0) { xor, i -> xor xor grundy(piles[i].stones, allowed[i]) }

    fun legalMoves(): List<TakeMove> = piles.indices.flatMap { i ->
        allowed[i].filter { it <= piles[i].stones }.map { TakeMove(i, it) }
    }

    fun after(move: TakeMove): TakeMatch {
        require(move in legalMoves())
        return copy(piles = piles.toMutableList().also {
            it[move.pile] = it[move.pile].copy(stones = it[move.pile].stones - move.count)
        })
    }

    fun play(move: TakeMove, random: Random = Random.Default): TakeMatch {
        if (finished || move !in legalMoves()) return this
        val afterHuman = after(move)
        if (afterHuman.legalMoves().isEmpty()) return afterHuman.copy(
            winner = "You", lastMove = "You took the last legal move. You win!", turns = turns + 1
        )
        val reply = when (difficulty) {
            Difficulty.NOOB -> afterHuman.legalMoves().random(random)
            Difficulty.INTUITIVE -> afterHuman.legalMoves().maxBy { afterHuman.piles[it.pile].stones }
            Difficulty.GTO -> afterHuman.bestMove().move
        }
        val afterAi = afterHuman.after(reply)
        return afterAi.copy(
            winner = if (afterAi.legalMoves().isEmpty()) "AI" else null,
            lastMove = "You took ${move.count} from pile ${move.pile + 1}; AI took ${reply.count} from pile ${reply.pile + 1}.",
            turns = turns + 1
        )
    }
}

fun TakeMatch.bestMove(): TakeSuggestion {
    val legal = legalMoves()
    require(legal.isNotEmpty())
    val winning = legal.firstOrNull { after(it).value == 0 }
    return TakeSuggestion(winning ?: legal.first(), winning != null)
}

fun newTakeMatch(level: Int, cap: Int = 3, difficulty: Difficulty = Difficulty.INTUITIVE,
                 random: Random = Random.Default): TakeMatch {
    require(level in 3..5 && cap in 2..5)
    val allowed = when (level) {
        3 -> listOf((1..cap).toList())
        4 -> listOf(listOf(1, 2), listOf(1, 2, 3))
        else -> listOf(listOf(1, 3, 4), listOf(1, 3, 4))
    }
    repeat(64) {
        val counts = List(allowed.size) { random.nextInt(5, 17) }
        val match = TakeMatch(level, counts.toNimPiles(), allowed, difficulty)
        if (match.value != 0) return match
    }
    val fallback = when (level) { 3 -> listOf(11); 4 -> listOf(5, 7); else -> listOf(6, 5) }
    return TakeMatch(level, fallback.toNimPiles(), allowed, difficulty)
}
