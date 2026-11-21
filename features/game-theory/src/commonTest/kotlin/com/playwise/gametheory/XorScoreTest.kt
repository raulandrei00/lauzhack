package com.playwise.gametheory

import kotlin.test.Test
import kotlin.test.assertEquals

class XorScoreTest {
    @Test
    fun scoreUsesExactlyTheSelectedPiles() {
        val piles = listOf(3, 4, 5).toNimPiles()
        assertEquals(0, xorScore(piles, emptySet()))
        assertEquals(3, xorScore(piles, setOf(0)))
        assertEquals(6, xorScore(piles, setOf(0, 2)))
        assertEquals(2, xorScore(piles, setOf(0, 1, 2)))
    }

    @Test
    fun scoreFollowsUpdatedPileCounts() {
        val before = listOf(0, 2, 1, 3).toNimPiles()
        val after = before.toMutableList().also { it[1] = it[1].copy(stones = 1) }
        assertEquals(1, xorScore(before, setOf(1, 3)))
        assertEquals(2, xorScore(after, setOf(1, 3)))
    }
}
