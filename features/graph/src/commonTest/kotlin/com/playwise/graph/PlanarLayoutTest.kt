package com.playwise.graph

import androidx.compose.ui.geometry.Offset
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanarLayoutTest {
    @Test fun addingAnEdgeRearrangesCrossedCircleDrawing() {
        val graph = LessonGraph(4, listOf(GraphEdge(0, 2), GraphEdge(1, 3)))
        val circle = circleLayout(4)
        assertTrue(!isCrossingFree(graph, circle))
        val arranged = planarLayout(graph, circle)
        assertTrue(isCrossingFree(graph, arranged))
        assertEquals(4, arranged.distinct().size)
    }

    @Test fun densePlanarGraphGetsStraightCrossingFreeDrawing() {
        val edges = (0 until 6).flatMap { a -> (a + 1 until 6).map { b -> GraphEdge(a, b) } }
            .filterNot { it.from / 2 == it.to / 2 }
        val octahedron = LessonGraph(6, edges)
        assertTrue(isSmallPlanar(octahedron))
        assertTrue(isCrossingFree(octahedron, planarLayout(octahedron)))
    }

    @Test fun everyAddedLegalEdgeKeepsTheDrawingClear() {
        repeat(80) { seed ->
            val random = Random(seed)
            val n = random.nextInt(2, 7)
            var game = PlanarGame(LessonGraph(n, emptyList()))
            var positions = circleLayout(n)
            while (true) {
                val legal = (0 until n).flatMap { a -> (a + 1 until n).mapNotNull { b ->
                    if (game.canAdd(a, b)) a to b else null
                } }
                if (legal.isEmpty()) break
                val (a, b) = legal.random(random)
                game = game.add(a, b)
                positions = planarLayout(game.graph, positions)
                assertTrue(isCrossingFree(game.graph, positions), "Crossed after adding $a-$b for seed $seed")
            }
        }
    }
    @Test fun tenNodeWheelCanBeRedrawnWithoutCrossings() {
        val wheel = LessonGraph(10, (1..9).map { GraphEdge(0, it) } +
            (1..9).map { GraphEdge(it, if (it == 9) 1 else it + 1) })
        val arranged = planarLayout(wheel)
        assertTrue(isCrossingFree(wheel, arranged))
    }
    @Test fun tenNodePlantedPlanarBoardsCanBeRedrawn() {
        repeat(6) { seed ->
            val random = Random(seed)
            val planted = List(10) { Offset(.1f + random.nextFloat() * .8f, .1f + random.nextFloat() * .8f) }
            var graph = LessonGraph(10, emptyList())
            val pairs = (0 until 10).flatMap { a -> (a + 1 until 10).map { b -> GraphEdge(a, b) } }.shuffled(random)
            for (edge in pairs) {
                val candidate = graph.copy(edges = graph.edges + edge)
                if (isCrossingFree(candidate, planted)) graph = candidate
            }
            assertTrue(isPlanarGraph(graph))
            assertTrue(isCrossingFree(graph, planarLayout(graph)), "seed $seed")
        }
    }
    @Test fun tenNodeGameKeepsEachAcceptedEdgeDrawable() {
        val random = Random(91)
        var game = PlanarGame(LessonGraph(10, emptyList()), 6)
        var positions = circleLayout(10)
        repeat(18) {
            val legal = (0 until 10).flatMap { a -> (a + 1 until 10).mapNotNull { b ->
                if (game.canAdd(a, b)) a to b else null
            } }
            if (legal.isEmpty()) return@repeat
            val (a, b) = legal.random(random)
            game = game.add(a, b)
            positions = planarLayout(game.graph, positions)
            assertTrue(isCrossingFree(game.graph, positions), "After edge $a-$b")
        }
    }
    @Test fun stackedTenNodeTriangulationsCanBeDrawn() {
        repeat(5) { seed ->
            val random = Random(seed + 210)
            val edges = mutableSetOf(0 to 1, 1 to 2, 0 to 2)
            val faces = mutableListOf(listOf(0, 1, 2))
            for (node in 3 until 10) {
                val face = faces.removeAt(random.nextInt(faces.size))
                face.forEach { neighbor -> edges.add(minOf(node, neighbor) to maxOf(node, neighbor)) }
                faces.add(listOf(face[0], face[1], node))
                faces.add(listOf(face[1], face[2], node))
                faces.add(listOf(face[2], face[0], node))
            }
            val graph = LessonGraph(10, edges.map { GraphEdge(it.first, it.second) })
            assertTrue(isPlanarGraph(graph))
            assertTrue(isCrossingFree(graph, planarLayout(graph)), "seed $seed")
        }
    }
}
