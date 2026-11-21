package com.playwise.gametheory

/** One numbered pile or staircase step, shared by both Nim games. */
data class NimPile(val position: Int, val stones: Int) {
    init {
        require(position >= 0)
        require(stones >= 0)
    }
}

fun List<Int>.toNimPiles(): List<NimPile> = mapIndexed { index, stones -> NimPile(index, stones) }

/** XOR only the piles the learner selected; an empty selection has score zero. */
fun xorScore(piles: List<NimPile>, selectedPositions: Set<Int>): Int = piles
    .filter { it.position in selectedPositions }
    .fold(0) { score, pile -> score xor pile.stones }
