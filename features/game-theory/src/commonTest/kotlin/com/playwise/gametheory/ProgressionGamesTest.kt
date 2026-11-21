package com.playwise.gametheory

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressionGamesTest {
    @Test
    fun hintsGiveZeroXorMovesForWinningNimAndStaircaseBoards() {
        for (a in 0..5) for (b in 0..5) for (c in 0..5) {
            val piles = listOf(a, b, c)
            if ((a xor b xor c) != 0) {
                val (move, winning) = optimalNimMove(piles)
                assertTrue(winning)
                val next = piles.toMutableList().also { it[move.pile] -= move.count }
                assertEquals(0, next.fold(0, Int::xor))
            }
            val steps = listOf(0, a, b, c)
            if (oddStepXor(steps) != 0) {
                val (move, winning) = optimalStairMove(steps)
                assertTrue(winning)
                val next = steps.toMutableList().also {
                    it[move.step] -= move.count; it[move.step - 1] += move.count
                }
                assertEquals(0, oddStepXor(next))
            }
        }
    }

    @Test
    fun takeUpToNHasPeriodicValuesAndGeneratedWinningBoards() {
        for (cap in 2..5) for (stones in 0..40) {
            assertEquals(stones % (cap + 1), grundy(stones, (1..cap).toList()))
        }
        val random = Random(12)
        for (level in 3..5) repeat(30) {
            val match = newTakeMatch(level, random = random)
            assertTrue(match.value != 0)
            val suggestion = match.bestMove()
            assertTrue(suggestion.winning)
            assertEquals(0, match.after(suggestion.move).value)
        }
    }

    @Test
    fun mexGameValuesAndLegalMoves() {
        val allowed = listOf(1, 3, 4)
        assertEquals(listOf(0, 1, 0, 1, 2, 3, 2), (0..6).map { grundy(it, allowed) })
        val match = TakeMatch(5, listOf(1, 0).toNimPiles(), listOf(allowed, allowed))
        assertEquals(listOf(TakeMove(0, 1)), match.legalMoves())
        assertEquals("You", match.play(TakeMove(0, 1)).winner)
        assertEquals(match, match.play(TakeMove(0, 3)))
    }

    @Test
    fun chocolateBitesKeepPoisonAndGrundyRuleFindsWinningMove() {
        val terminal = ChocolateBoard(listOf(1, 0, 0))
        assertTrue(terminal.legalMoves().isEmpty())
        assertEquals(0, chocolateGrundy(terminal))
        val board = ChocolateBoard(listOf(3, 3, 3))
        assertFalse(Bite(0, 0) in board.legalMoves())
        assertEquals(listOf(3, 2, 2), board.bite(Bite(1, 2)).heights)
        for (heights in listOf(listOf(2, 2), listOf(3, 3), listOf(3, 3, 3))) {
            val shape = ChocolateBoard(heights)
            val value = chocolateGrundy(shape)
            val (move, winning) = shape.bestBite()
            assertEquals(value != 0, winning)
            if (winning) assertEquals(0, chocolateGrundy(shape.bite(move)))
        }
        repeat(20) {
            val match = newChocolateMatch(random = Random(it))
            assertTrue(chocolateGrundy(match.board) != 0)
        }
    }
}
