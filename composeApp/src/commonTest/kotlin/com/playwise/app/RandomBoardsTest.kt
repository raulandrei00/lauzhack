package com.playwise.app

import kotlin.random.Random
import kotlin.test.*

class RandomBoardsTest {
    @Test fun seededBoardsAreReproducibleAndVary() {
        for (kind in DuelKind.entries.filter { it != DuelKind.SPROUTS }) {
            assertEquals(randomDuel(kind,Random(42)),randomDuel(kind,Random(42)))
            assertTrue((0..30).map { randomDuel(kind,Random(it)).graph }.toSet().size > 1,kind.name)
        }
        for (topic in GraphTopic.entries) {
            assertEquals(randomLesson(topic,Random(42)),randomLesson(topic,Random(42)))
            assertTrue((0..30).map { randomLesson(topic,Random(it)) }.toSet().size > 1)
        }
        assertEquals(randomSprouts(Random(42)),randomSprouts(Random(42)))
    }
    @Test fun generatedGamesRespectTheirConstraints() {
        repeat(100) { seed ->
            for (kind in listOf(DuelKind.COLORING,DuelKind.KAYLES,DuelKind.PATH,DuelKind.SHANNON)) {
                val graph = randomDuel(kind,Random(seed)).graph
                assertTrue(shortestDistances(graph,0,GraphTopic.BFS).all { it != Int.MAX_VALUE })
                if (kind == DuelKind.COLORING) {
                    val colors = IntArray(graph.size) { -1 }
                    fun solve(u: Int): Boolean {
                        if (u == graph.size) return true
                        for (c in 0..2) if (graph.neighbors(u).none { colors[it.first] == c }) {
                            colors[u] = c
                            if (solve(u+1)) return true
                        }
                        colors[u] = -1
                        return false
                    }
                    assertTrue(solve(0))
                }
                if (kind == DuelKind.SHANNON) {
                    assertTrue(graph.neighbors(0).none { it.first == 5 })
                    for (edge in graph.edges) assertTrue(shortestDistances(graph.copy(edges = graph.edges-edge),0,GraphTopic.BFS).last() != Int.MAX_VALUE)
                }
            }
            val sprouts = randomSprouts(Random(seed))
            assertTrue(sprouts.dots.size in 3..6)
            for ((i,a) in sprouts.dots.withIndex()) {
                assertEquals(0,a.degree)
                assertTrue(a.point.x in .02f.. .98f && a.point.y in .02f.. .98f)
                sprouts.dots.drop(i+1).forEach { assertTrue(a.point.distance(it.point) > .2f) }
            }
        }
    }
    @Test fun randomGridsScoreEveryBoxAndFinish() {
        repeat(40) { seed ->
            var state = randomDuel(DuelKind.BOXES,Random(seed))
            val columns = state.gridColumns
            val rows = state.graph.size/columns
            assertTrue(columns in 3..5 && rows in 3..5)
            assertEquals(rows*(columns-1)+columns*(rows-1),state.graph.edges.size)
            state.graph.edges.indices.shuffled(Random(seed)).forEach { state = state.takeEdge(it) }
            assertEquals((columns-1)*(rows-1),state.boxes.size)
            assertNotNull(state.result)
        }
    }
    @Test fun randomLessonsHaveValidSolutions() {
        repeat(100) { seed ->
            for (topic in GraphTopic.entries) {
                val graph = randomLesson(topic,Random(seed))
                when (topic) {
                    GraphTopic.SCC -> {
                        val groups = stronglyConnectedComponents(graph)
                        assertEquals(2,groups.size)
                        assertEquals((0..5).toSet(),groups.flatten().toSet())
                    }
                    GraphTopic.BICONNECTED -> {
                        val blocks = biconnectedComponents(graph)
                        assertEquals(2,blocks.size)
                        assertEquals((0..5).toSet(),blocks.flatten().toSet())
                        assertEquals(1,blocks.first().intersect(blocks.last()).size)
                    }
                    else -> {
                        if (topic == GraphTopic.BFS) assertTrue(graph.edges.all { it.weight == 1 })
                        if (topic == GraphTopic.ZERO_ONE) assertTrue(graph.edges.all { it.weight in 0..1 })
                        fun search(path: List<Int>): Int {
                            if (path.last() == 5) return routeCost(graph,path)!!
                            return graph.neighbors(path.last()).filter { it.first !in path }.minOfOrNull { search(path+it.first) } ?: Int.MAX_VALUE
                        }
                        val best = shortestDistances(graph,0,topic).last()
                        assertTrue(best != Int.MAX_VALUE)
                        assertEquals(search(listOf(0)),best)
                    }
                }
            }
        }
    }
}
