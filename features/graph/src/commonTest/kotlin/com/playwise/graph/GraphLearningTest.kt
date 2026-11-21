package com.playwise.graph

import kotlin.test.*

class GraphLearningTest {
    private val guides = listOf("primer" to graphPrimer) +
        GraphTopic.entries.map { it.name to it.guide } +
        DuelKind.entries.map { it.name to it.guide }

    @Test fun everyGuideHasEveryTeachingSection() {
        for ((name, g) in guides) {
            assertTrue(g.summary.isNotBlank(), name)
            assertTrue(g.intuition.isNotEmpty(), name)
            assertTrue(g.terms.size >= 3, name)
            assertTrue(g.exampleSteps.size >= 3, name)
            assertTrue(g.pseudocode.lines().size >= 5, name)
            assertTrue(g.complexity.isNotEmpty(), name)
            assertTrue(g.whyItWorks.isNotBlank(), name)
            assertTrue(g.pitfalls.isNotEmpty(), name)
            assertTrue(g.quiz.size >= 3, name)
            assertTrue(g.inThisApp.isNotBlank(), name)
        }
    }

    @Test fun resourcesAreHttpsLinksOrBookCitations() {
        for ((name, g) in guides) {
            assertTrue(g.resources.any { it.url.startsWith("https://") }, "$name needs at least one online resource")
            g.resources.forEach { r ->
                assertTrue(r.url.isEmpty() || (r.url.startsWith("https://") && ' ' !in r.url), "$name: ${r.url}")
                assertTrue(r.title.isNotBlank() && r.note.isNotBlank(), name)
            }
        }
    }

    // The worked examples are checked against the real implementations so the text cannot drift.
    @Test fun dijkstraWorkedExampleMatchesImplementation() {
        // A=0 B=1 C=2 F=3: A-B 4, A-C 1, C-B 2, B-F 1, C-F 6
        val g = LessonGraph(4, listOf(GraphEdge(0, 1, 4), GraphEdge(0, 2, 1), GraphEdge(2, 1, 2), GraphEdge(1, 3, 1), GraphEdge(2, 3, 6)))
        assertEquals(listOf(0, 3, 1, 4), shortestDistances(g, 0, GraphTopic.DIJKSTRA))
        assertEquals(4, routeCost(g, listOf(0, 2, 1, 3)))
        assertEquals(7, routeCost(g, listOf(0, 2, 3)))
    }

    @Test fun bfsAndZeroOneWorkedExamplesMatchImplementation() {
        // A B C D E F = 0..5: A-B, A-C, B-C, B-D, C-E, D-F, E-F
        val bfs = LessonGraph(6, listOf(0 to 1, 0 to 2, 1 to 2, 1 to 3, 2 to 4, 3 to 5, 4 to 5).map { GraphEdge(it.first, it.second) })
        assertEquals(listOf(0, 1, 1, 2, 2, 3), shortestDistances(bfs, 0, GraphTopic.BFS))
        // A=0 B=1 C=2 F=3: A-B 1, A-C 0, C-B 0, B-F 1
        val zeroOne = LessonGraph(4, listOf(GraphEdge(0, 1, 1), GraphEdge(0, 2, 0), GraphEdge(2, 1, 0), GraphEdge(1, 3, 1)))
        assertEquals(listOf(0, 0, 0, 1), shortestDistances(zeroOne, 0, GraphTopic.ZERO_ONE))
    }

    @Test fun componentWorkedExamplesMatchImplementation() {
        val undirected = LessonGraph(6, listOf(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 2, 4 to 5).map { GraphEdge(it.first, it.second) })
        assertEquals(setOf(setOf(0, 1, 2), setOf(2, 3, 4), setOf(4, 5)), biconnectedComponents(undirected))
        val directed = LessonGraph(6, listOf(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 5, 5 to 3).map { GraphEdge(it.first, it.second) }, directed = true)
        assertEquals(setOf(setOf(0, 1, 2), setOf(3, 4, 5)), stronglyConnectedComponents(directed))
        val withReturn = LessonGraph(6, directed.edges + GraphEdge(5, 0), directed = true)
        assertEquals(setOf((0..5).toSet()), stronglyConnectedComponents(withReturn))
    }

    @Test fun planarExampleFactsHold() {
        val k5 = LessonGraph(5, (0 until 5).flatMap { a -> (a + 1 until 5).map { GraphEdge(a, it) } })
        assertFalse(isSmallPlanar(k5))
        assertTrue(isSmallPlanar(LessonGraph(5, k5.edges.drop(1))))
        val k33 = LessonGraph(6, (0..2).flatMap { a -> (3..5).map { GraphEdge(a, it) } })
        assertFalse(isSmallPlanar(k33))
    }
}
