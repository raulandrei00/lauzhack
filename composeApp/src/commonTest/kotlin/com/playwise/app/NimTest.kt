package com.playwise.app

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NimTest {
    @Test
    fun gtoLeavesZeroXorWhenWinningMoveExists() {
        for (a in 0..5) for (b in 0..5) for (c in 0..5) {
            val piles = listOf(a, b, c)
            val xor = a xor b xor c
            if (xor == 0) continue
            val move = chooseAiMove(piles, Difficulty.GTO, Random(1))
            assertTrue(move.count in 1..piles[move.pile])
            val next = piles.toMutableList().also { it[move.pile] -= move.count }
            assertEquals(0, next.reduce(Int::xor), "GTO failed for $piles")
        }
    }

    @Test
    fun takingLastStoneEndsGameBeforeAiMoves() {
        val result = NimMatch(piles = listOf(0, 1, 0)).play(Move(1, 1))
        assertEquals("You", result.winner)
        assertEquals(listOf(0, 0, 0), result.piles)
    }

    @Test
    fun illegalMoveDoesNotChangeGame() {
        val match = NimMatch()
        assertEquals(match, match.play(Move(0, 4)))
    }
}
