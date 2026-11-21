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
    @Test fun tenVertexRuleRejectsSubdivisionsAndAllowsDensePlanarGraphs() {
        val k5 = complete(5)
        val subdividedK5 = LessonGraph(10,
            k5.edges.filterNot { it.from == 0 && it.to == 1 } +
                listOf(GraphEdge(0, 5), GraphEdge(5, 1)))
        assertFalse(isPlanarGraph(subdividedK5))
        val k33 = (0..2).flatMap { a -> (3..5).map { b -> GraphEdge(a, b) } }
        val subdividedK33 = LessonGraph(10,
            k33.filterNot { it.from == 0 && it.to == 3 } +
                listOf(GraphEdge(0, 6), GraphEdge(6, 3)))
        assertFalse(isPlanarGraph(subdividedK33))
        val wheel = LessonGraph(10, (1..9).map { GraphEdge(0, it) } +
            (1..9).map { GraphEdge(it, if (it == 9) 1 else it + 1) })
        assertTrue(isPlanarGraph(wheel))
        assertEquals(10, PlanarGame(LessonGraph(10, emptyList()), 6).graph.size)
    }
    @Test fun tenVertexRuleMatchesReferenceGraphs() {
        // Fixed graph masks checked with NetworkX's check_planarity implementation.
        val planar = listOf(2390726084097L, 27489267646548L, 18151875018971L,
            4539847819300L, 2776587780118L, 18870396537986L, 6048558256841L)
        val nonplanar = listOf(5294199455986L, 8836442427483L, 17701708550438L,
            10448005154377L, 31205114743298L, 3484405495933L, 7126466382789L)
        fun graph(mask: Long): LessonGraph {
            var bit = 0
            val edges = buildList {
                for (a in 0 until 10) for (b in a + 1 until 10) {
                    if (mask and (1L shl bit) != 0L) add(GraphEdge(a, b))
                    bit++
                }
            }
            return LessonGraph(10, edges)
        }
        planar.forEach { assertTrue(isPlanarGraph(graph(it)), "Expected planar: $it") }
        nonplanar.forEach { assertFalse(isPlanarGraph(graph(it)), "Expected nonplanar: $it") }
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
    @Test fun rejectedEdgesReportTheRuleTheyBreak() {
        val start = PlanarGame(LessonGraph(5, emptyList()), 3)
        assertEquals(EdgeRejection.LOOP, start.edgeRejection(0, 0))
        assertEquals(EdgeRejection.INVALID_NODE, start.edgeRejection(0, 9))
        assertEquals(EdgeRejection.ALREADY_CONNECTED, start.add(0, 1).edgeRejection(0, 1))
        assertEquals(EdgeRejection.SAME_COLOR,
            start.copy(colors = mapOf(0 to 1, 1 to 1)).edgeRejection(0, 1))

        val almostK5 = complete(5).let { it.copy(edges = it.edges.dropLast(1)) }
        assertEquals(EdgeRejection.K5, PlanarGame(almostK5).edgeRejection(3, 4))
        val almostK33 = LessonGraph(6,
            (0..2).flatMap { a -> (3..5).map { b -> GraphEdge(a, b) } }.dropLast(1))
        assertEquals(EdgeRejection.K33, PlanarGame(almostK33).edgeRejection(2, 5))

        val outer = (1..6).map { GraphEdge(it, if (it == 6) 1 else it + 1) }
        val maximal = LessonGraph(7, outer + (1..6).map { GraphEdge(0, it) } +
            listOf(GraphEdge(1, 3), GraphEdge(1, 4), GraphEdge(1, 5)))
        assertTrue(isPlanarGraph(maximal))
        assertEquals(EdgeRejection.EDGE_LIMIT, PlanarGame(maximal).edgeRejection(2, 4))
        assertNull(start.edgeRejection(0, 1))
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
