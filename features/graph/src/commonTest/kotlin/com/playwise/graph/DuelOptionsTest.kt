package com.playwise.graph

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DuelOptionsTest {
    @Test fun settingsReachAllDuelGenerators() {
        val options = DuelOptions(nodes = 10, paints = 6, budget = 10, columns = 6, rows = 5)
        for (kind in DuelKind.entries) {
            val state = randomDuel(kind, Random(42), options)
            if (kind == DuelKind.BOXES) {
                assertEquals(30, state.graph.size)
                assertEquals(6, state.gridColumns)
            } else assertEquals(10, state.graph.size)
            if (kind == DuelKind.COLORING) {
                assertEquals(6, state.paletteSize)
                assertTrue(state.legalColor(0, 5))
            }
            if (kind == DuelKind.SHANNON || kind == DuelKind.PATH || kind == DuelKind.KAYLES || kind == DuelKind.COLORING)
                assertTrue(shortestDistances(state.graph, 0, GraphTopic.BFS).all { it != Int.MAX_VALUE })
            if (kind == DuelKind.SHANNON)
                assertTrue(state.graph.neighbors(0).none { it.first == 9 })
            if (kind == DuelKind.PATH) assertEquals(10, state.budget)
        }
    }
}
