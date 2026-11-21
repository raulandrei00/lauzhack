package com.playwise.gametheory

import kotlin.test.*

class NimLearningTest {
    private val guides = listOf("primer" to nimPrimer) + (1..6).map { "level $it" to nimGuide(it) }

    @Test fun everyGuideIsComplete() {
        for ((name, g) in guides) {
            assertTrue(g.summary.isNotBlank(), name)
            assertTrue(g.intuition.size >= 2, name)
            assertTrue(g.terms.size >= 3, name)
            assertTrue(g.exampleSteps.size >= 3, name)
            assertTrue(g.tryThis.isNotEmpty(), name)
            assertTrue(g.pseudocode.lines().size >= 4, name)
            assertTrue(g.whyItWorks.isNotBlank(), name)
            assertTrue(g.pitfalls.size >= 2, name)
            assertTrue(g.quiz.size >= 3, name)
            assertTrue(g.resources.any { it.url.startsWith("https://") }, name)
            g.resources.forEach { assertTrue(it.url.isEmpty() || (it.url.startsWith("https://") && ' ' !in it.url), "$name ${it.url}") }
        }
    }

    // Worked examples and quiz answers, checked against the real game code.
    @Test fun level1ExamplesHold() {
        assertEquals(2, 3 xor 4 xor 5)
        assertEquals(Move(0, 2) to true, optimalNimMove(listOf(3, 4, 5)))
        assertEquals(Move(1, 3) to true, optimalNimMove(listOf(1, 4, 0)))
        assertEquals(0, 1 xor 2 xor 3)
        assertEquals(Move(1, 5) to true, optimalNimMove(listOf(2, 7)))
    }

    @Test fun level2ExamplesHold() {
        assertEquals(StairMove(3, 1) to true, optimalStairMove(listOf(0, 2, 1, 3, 2)))
        assertEquals(0, oddStepXor(listOf(0, 2, 2, 2, 2)))
        assertEquals(0, oddStepXor(listOf(0, 3, 5, 3, 9)))
    }

    @Test fun takeLevelExamplesHold() {
        val upTo2 = listOf(1, 2)
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0), (0..6).map { grundy(it, upTo2) })
        assertEquals(0, grundy(9, upTo2))
        assertEquals(3, grundy(11, listOf(1, 2, 3)))
        assertEquals(2, grundy(20, (1..5).toList()))
        // level 4: piles 5 (cap 2) and 7 (cap 3)
        val four = TakeMatch(4, listOf(5, 7).toNimPiles(), listOf(listOf(1, 2), listOf(1, 2, 3)))
        assertEquals(1, four.value)
        assertEquals(TakeSuggestion(TakeMove(1, 1), true), four.bestMove())
        assertEquals(0, TakeMatch(4, listOf(6, 8).toNimPiles(), four.allowed).value)
        // level 5: period-7 table 0101232 and the 6, 5 example
        val moves = listOf(1, 3, 4)
        assertEquals(listOf(0, 1, 0, 1, 2, 3, 2, 0, 1, 0, 1, 2, 3, 2), (0..13).map { grundy(it, moves) })
        val five = TakeMatch(5, listOf(6, 5).toNimPiles(), listOf(moves, moves))
        assertEquals(1, five.value)
        assertEquals(0, five.after(TakeMove(1, 1)).value)   // the guide's move wins
        assertTrue(five.bestMove().winning)
    }

    @Test fun chocolateExampleHolds() {
        assertEquals(2, chocolateGrundy(ChocolateBoard(listOf(2, 2))))
        assertEquals(0, chocolateGrundy(ChocolateBoard(listOf(2, 1))))
        assertEquals(1, chocolateGrundy(ChocolateBoard(listOf(1, 1))))
        assertEquals(1, chocolateGrundy(ChocolateBoard(listOf(2, 0))))
        assertEquals(Bite(1, 1) to true, ChocolateBoard(listOf(2, 2)).bestBite())
    }

    @Test fun aiMoveDiffFindsTheChangedPile() {
        assertEquals(Move(2, 3), diffMove(listOf(4, 5, 6), listOf(4, 5, 3)))
        assertNull(diffMove(listOf(1, 2), listOf(1, 2)))
    }
}
