package com.playwise.gametheory

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaircaseNimTest {
    @Test
    fun generatedBoardsGiveFirstPlayerAWinningMove() {
        val random = Random(21)
        repeat(100) {
            val match = newStaircaseMatch(random = random)
            assertEquals(0, match.steps[0].stones)
            assertEquals(5, match.steps.size)
            assertTrue(match.steps.drop(1).all { it.stones in 1..4 })
            assertTrue(oddStepXor(match.steps.map { it.stones }) != 0)
            assertEquals(0, match.turns)
        }
    }

    @Test
    fun moveTransfersCoinsAndFloorCoinsStayOutOfPlay() {
        val match = StaircaseMatch(steps = listOf(0, 1, 0).toNimPiles())
        val result = match.play(StairMove(1, 1))
        assertEquals(listOf(1, 0, 0), result.steps.map { it.stones })
        assertEquals("You", result.winner)
        assertEquals(result, result.play(StairMove(1, 1)))
    }

    @Test
    fun illegalMoveDoesNotChangeState() {
        val match = StaircaseMatch()
        assertEquals(match, match.play(StairMove(0, 1)))
        assertEquals(match, match.play(StairMove(2, 2)))
    }

    @Test
    fun gtoLeavesOddStepXorAtZeroWheneverPossible() {
        for (a in 0..3) for (b in 0..3) for (c in 0..3) for (d in 0..3) {
            val steps = listOf(0, a, b, c, d)
            if (oddStepXor(steps) == 0) continue
            val move = chooseStairMove(steps, Difficulty.GTO, Random(1))
            assertTrue(move.step in 1..4 && move.count in 1..steps[move.step])
            val after = steps.toMutableList().also {
                it[move.step] -= move.count
                it[move.step - 1] += move.count
            }
            assertEquals(0, oddStepXor(after), "GTO failed for $steps")
        }
    }

    @Test
    fun multiplayerAlternatesPlayersAndLastSlideWins() {
        val start = StaircaseMatch(steps = listOf(0, 1, 1).toNimPiles())
        val afterPlayerOne = start.playMultiplayer(StairMove(2, 1))
        assertEquals(1, afterPlayerOne.currentPlayer)
        assertEquals(listOf(0, 2, 0), afterPlayerOne.steps.map { it.stones })
        assertEquals(null, afterPlayerOne.winner)

        val end = afterPlayerOne.playMultiplayer(StairMove(1, 2))
        assertEquals("Player 2", end.winner)
        assertEquals(listOf(2, 0, 0), end.steps.map { it.stones })
    }
}
