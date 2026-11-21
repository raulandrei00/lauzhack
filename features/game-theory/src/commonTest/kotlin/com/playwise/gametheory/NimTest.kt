package com.playwise.gametheory

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NimTest {
    @Test
    fun zeroXorDrawIsCorrectedBeforePlayerStarts() {
        val alwaysLowest = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        val match = newNimMatch(BoardLevel.MEDIUM, Difficulty.GTO, alwaysLowest)
        assertEquals(0, match.turns)
        assertEquals(null, match.winner)
        assertTrue(xorScore(match.piles, match.piles.map { it.position }.toSet()) != 0)
    }

    @Test
    fun boardLevelsHaveTheRequestedSizesAndRanges() {
        val random = Random(42)
        repeat(100) {
            assertEquals(listOf(3, 4, 5), BoardLevel.BEGINNER.createPiles(random).map { it.stones })
            val easy = BoardLevel.EASY.createPiles(random)
            assertEquals(3, easy.size)
            assertTrue(easy.all { it.stones in 1..5 })
            assertTrue(xorScore(easy, easy.map { it.position }.toSet()) != 0)
            val medium = BoardLevel.MEDIUM.createPiles(random)
            assertEquals(4, medium.size)
            assertTrue(medium.all { it.stones in 1..10 })
            assertTrue(xorScore(medium, medium.map { it.position }.toSet()) != 0)
            val hard = BoardLevel.HARD.createPiles(random)
            assertTrue(hard.size in 5..10)
            assertTrue(hard.all { it.stones in 1..64 })
            assertTrue(xorScore(hard, hard.map { it.position }.toSet()) != 0)
        }
    }

    @Test
    fun newMatchKeepsTheLevelAndOpponent() {
        val match = newNimMatch(BoardLevel.HARD, Difficulty.GTO, Random(7))
        assertEquals(BoardLevel.HARD, match.level)
        assertEquals(Difficulty.GTO, match.difficulty)
        assertEquals(0, match.turns)
        assertEquals(null, match.winner)
        assertTrue(xorScore(match.piles, match.piles.map { it.position }.toSet()) != 0)
    }

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
        val result = NimMatch(piles = listOf(0, 1, 0).toNimPiles()).play(Move(1, 1))
        assertEquals("You", result.winner)
        assertEquals(listOf(0, 0, 0), result.piles.map { it.stones })
    }

    @Test
    fun illegalMoveDoesNotChangeGame() {
        val match = NimMatch()
        assertEquals(match, match.play(Move(0, 4)))
    }
}
