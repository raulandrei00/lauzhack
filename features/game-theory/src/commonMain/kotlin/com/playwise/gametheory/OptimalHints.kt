package com.playwise.gametheory

fun optimalNimMove(piles: List<Int>): Pair<Move, Boolean> {
    val score = piles.fold(0, Int::xor)
    val index = piles.indices.firstOrNull { (piles[it] xor score) < piles[it] }
    if (index != null) return Move(index, piles[index] - (piles[index] xor score)) to true
    val first = piles.indexOfFirst { it > 0 }
    require(first >= 0)
    return Move(first, 1) to false
}

fun optimalStairMove(steps: List<Int>): Pair<StairMove, Boolean> {
    val score = oddStepXor(steps)
    val index = steps.indices.firstOrNull { it % 2 == 1 && (steps[it] xor score) < steps[it] }
    if (index != null) return StairMove(index, steps[index] - (steps[index] xor score)) to true
    val first = (1..steps.lastIndex).first { steps[it] > 0 }
    return StairMove(first, 1) to false
}
