package com.playwise.graph

import kotlin.test.*

class GraphTest {
    @Test fun shortestPathsRespectCostsAndUnreachableNodes() {
        val graph = LessonGraph(5, listOf(GraphEdge(0, 1, 9), GraphEdge(0, 2, 1), GraphEdge(2, 1, 1), GraphEdge(1, 3, 2)))
        assertEquals(listOf(0, 2, 1, 4, Int.MAX_VALUE), shortestDistances(graph, 0, GraphTopic.DIJKSTRA))
        assertEquals(listOf(0, 1, 1, 2, Int.MAX_VALUE), shortestDistances(graph, 0, GraphTopic.BFS))
        assertNull(routeCost(graph, listOf(0, 3)))
        assertEquals(4, routeCost(graph, listOf(0, 2, 1, 3)))
    }
    @Test fun zeroOneCanImproveAlreadyDiscoveredVertex() {
        val graph = LessonGraph(4, listOf(GraphEdge(0, 1, 1), GraphEdge(0, 2, 0), GraphEdge(2, 1, 0), GraphEdge(1, 3, 1)), true)
        assertEquals(listOf(0, 0, 0, 1), shortestDistances(graph, 0, GraphTopic.ZERO_ONE))
        assertEquals(Int.MAX_VALUE, shortestDistances(graph, 3, GraphTopic.ZERO_ONE)[0])
        assertFailsWith<IllegalArgumentException> { shortestDistances(LessonGraph(2, listOf(GraphEdge(0, 1, 2))), 0, GraphTopic.ZERO_ONE) }
    }
    @Test fun componentsRespectDirectionAndArticulations() {
        assertEquals(setOf(setOf(0, 1, 2), setOf(3, 4, 5)), stronglyConnectedComponents(graphLevel(GraphTopic.SCC, 0)))
        assertEquals(setOf(setOf(0, 1, 2), setOf(2, 3, 4), setOf(4, 5)), biconnectedComponents(graphLevel(GraphTopic.BICONNECTED, 0)))
        assertEquals(setOf(setOf(0, 1, 2), setOf(3, 4), setOf(5)), biconnectedComponents(graphLevel(GraphTopic.BICONNECTED, 2)))
        assertEquals(setOf(setOf(0), setOf(1)), stronglyConnectedComponents(LessonGraph(2, emptyList(), true)))
        assertEquals(setOf(setOf(0)), biconnectedComponents(LessonGraph(1, emptyList())))
    }
    @Test fun allRouteBoardsMatchIndependentExhaustiveSearch() {
        for (topic in listOf(GraphTopic.DIJKSTRA, GraphTopic.BFS, GraphTopic.ZERO_ONE)) for (level in 0..2) {
            val graph = graphLevel(topic, level)
            fun search(path: List<Int>): Int {
                if (path.last() == 5) return routeCost(graph, path)!!
                return graph.neighbors(path.last()).filter { it.first !in path }
                    .minOfOrNull { search(path + it.first) } ?: Int.MAX_VALUE
            }
            assertEquals(search(listOf(0)), shortestDistances(graph, 0, topic)[5], "$topic board $level")
            assertTrue(search(listOf(0)) < Int.MAX_VALUE)
        }
    }
    @Test fun allSccBoardsPartitionVerticesAndAreMutuallyReachable() {
        for (level in 0..2) {
            val graph = graphLevel(GraphTopic.SCC, level)
            val groups = stronglyConnectedComponents(graph)
            assertEquals((0 until graph.size).toSet(), groups.flatten().toSet())
            assertEquals(graph.size, groups.sumOf { it.size })
            for (u in 0 until graph.size) for (v in 0 until graph.size) {
                val mutual = shortestDistances(graph, u, GraphTopic.BFS)[v] != Int.MAX_VALUE && shortestDistances(graph, v, GraphTopic.BFS)[u] != Int.MAX_VALUE
                assertEquals(mutual, groups.any { u in it && v in it })
            }
        }
    }
}
