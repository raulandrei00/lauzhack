package com.playwise.graph

import kotlin.random.Random
import kotlin.test.*

class PlanarGameTest {
    private fun complete(n: Int) = LessonGraph(n,(0 until n).flatMap { a -> (a+1 until n).map { b -> GraphEdge(a,b) } })
    @Test fun exactPlanarityIncludesForbiddenMinors() {
        assertTrue(isSmallPlanar(complete(4)))
        val k5 = complete(5)
        assertFalse(isSmallPlanar(k5))
        assertTrue(isSmallPlanar(k5.copy(edges=k5.edges.dropLast(1))))
        val k33 = LessonGraph(6,(0..2).flatMap { a -> (3..5).map { b -> GraphEdge(a,b) } })
        assertFalse(isSmallPlanar(k33))
        assertTrue(isSmallPlanar(k33.copy(edges=k33.edges.dropLast(1))))
        val subdivided = LessonGraph(6,k5.edges.filterNot { it.from == 0 && it.to == 1 }+listOf(GraphEdge(0,5),GraphEdge(5,1)))
        assertFalse(isSmallPlanar(subdivided))
        assertTrue(isSmallPlanar(subdivided.copy(edges = subdivided.edges.dropLast(1))))
        // Octahedron: dense planar six-vertex graph, not outerplanar.
        assertTrue(isSmallPlanar(complete(6).copy(edges=complete(6).edges.filterNot { it.from/2 == it.to/2 })))
    }
    @Test fun colorsArePermanentAndBothMovesAlternate() {
        val start = PlanarGame(LessonGraph(4,emptyList()),3)
        assertTrue(start.graph.edges.isEmpty())
        val a = start.color(0,0)
        assertEquals(1,a.turn)
        assertEquals(a,a.color(0,1))
        val b = a.add(0,1)
        assertEquals(0,b.turn)
        assertFalse(b.canColor(1,0))
        assertEquals(b,b.add(1,0))
        assertEquals(b,b.add(0,0))
        assertEquals(b,b.color(8,0))
        val c = b.color(2,0)
        assertFalse(c.canAdd(0,2))
        assertTrue(c.canAdd(1,2)) // uncolored endpoint is allowed
    }
    @Test fun nonplanarEdgeIsRejectedWithoutConsumingTurn() {
        val edges = (0..2).flatMap { a -> (3..5).map { b -> GraphEdge(a,b) } }.dropLast(1)
        val game = PlanarGame(LessonGraph(6,edges))
        assertFalse(game.canAdd(2,5))
        assertEquals(game,game.add(2,5))
    }
    @Test fun winnerRequiresBothMoveTypesExhausted() {
        val colored = PlanarGame(LessonGraph(2,emptyList()),2).color(0,0).color(1,1)
        assertNull(colored.winner)
        val end = colored.add(0,1)
        assertEquals(0,end.winner)
        assertFalse(end.hasMove())
        assertEquals(end,end.color(0,1))
        val blocked = PlanarGame(LessonGraph(2,emptyList()),1).add(0,1).color(0,0)
        assertEquals(1,blocked.winner) // uncolored B cannot be colored; no edge remains
    }
    @Test fun randomGamesTerminateAndPreserveInvariants() {
        repeat(50) { seed ->
            val random = Random(seed)
            var game = PlanarGame(LessonGraph(random.nextInt(2,7),emptyList()),random.nextInt(1,5))
            var moves = 0
            while (game.winner == null) {
                val choices = buildList {
                    for (u in 0 until game.graph.size) {
                        for (c in 0 until game.paletteSize) if (game.canColor(u,c)) add(Triple(true,u,c))
                        for (v in u+1 until game.graph.size) if (game.canAdd(u,v)) add(Triple(false,u,v))
                    }
                }
                assertTrue(choices.isNotEmpty())
                val (color,u,v) = choices.random(random)
                game = if (color) game.color(u,v) else game.add(u,v)
                assertTrue(isSmallPlanar(game.graph))
                assertTrue(++moves <= 21)
            }
            assertFalse(game.hasMove())
        }
    }
}
