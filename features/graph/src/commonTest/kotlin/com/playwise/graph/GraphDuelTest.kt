package com.playwise.graph

import kotlin.test.*

class GraphDuelTest {
    @Test fun coloringRejectsConflictsAndRecognizesBothWinners() {
        val graph = LessonGraph(3,listOf(GraphEdge(0,1),GraphEdge(1,2)))
        val start = DuelState(DuelKind.COLORING,graph)
        val first = start.color(0,0)
        assertEquals(first, first.color(1,0))
        assertEquals(first, first.color(0,1))
        val win = first.color(1,1).color(2,0)
        assertTrue(win.result!!.contains("Builder"))
        assertEquals(win,win.color(2,2))
        val star = LessonGraph(4,listOf(GraphEdge(0,1),GraphEdge(0,2),GraphEdge(0,3)))
        assertTrue(DuelState(DuelKind.COLORING,star).color(1,0).color(2,1).color(3,2).result!!.contains("Blocker"))
    }
    @Test fun kaylesRemovesNeighborsAndStopsAtWin() {
        val start = DuelState(DuelKind.KAYLES,LessonGraph(3,listOf(GraphEdge(0,1),GraphEdge(1,2))))
        val won = start.takeNode(1)
        assertEquals(setOf(0,1,2),won.removed)
        assertTrue(won.result!!.contains("Player 1"))
        assertEquals(won,won.takeNode(0))
        assertEquals(start,start.takeNode(-1))
    }
    @Test fun shannonDetectsClaimedPathsAndCuts() {
        val graph = LessonGraph(3,listOf(GraphEdge(0,1),GraphEdge(1,2),GraphEdge(0,2)))
        val start = DuelState(DuelKind.SHANNON,graph)
        assertTrue(start.takeEdge(2).result!!.contains("Connector"))
        val next = start.takeEdge(0)
        assertEquals(next,next.takeEdge(0))
        val line = DuelState(DuelKind.SHANNON,LessonGraph(3,listOf(GraphEdge(0,1),GraphEdge(1,2))))
        assertTrue(line.takeEdge(0).takeEdge(1).result!!.contains("Cutter"))
    }
    @Test fun sabotageBudgetAndRoutingPhasesAreEnforced() {
        var game = DuelState(DuelKind.PATH)
        assertEquals(game,game.travel(1))
        repeat(5) { game = game.raiseCost(0) }
        assertEquals(0,game.budget)
        assertEquals(game,game.raiseCost(0))
        game = game.lockRoads()
        assertEquals(game,game.raiseCost(1))
        assertEquals(game,game.submitRoute())
        assertEquals(game,game.travel(5))
        fun bestPath(path: List<Int>): List<Int>? {
            if (path.last() == 5) return path
            return game.graph.neighbors(path.last()).filter { it.first !in path }.mapNotNull { bestPath(path+it.first) }.minByOrNull { routeCost(game.graph,it)!! }
        }
        for (node in bestPath(listOf(0))!!.drop(1)) game = game.travel(node)
        assertTrue(game.submitRoute().result!!.contains("Router"))
        val bad = DuelState(DuelKind.PATH).lockRoads().travel(2).travel(1).travel(3).travel(4).travel(5).submitRoute()
        assertTrue(bad.result!!.contains("Saboteur"))
    }
    @Test fun boxesCaptureTwoSquaresAndKeepTheTurn() {
        val base = DuelState(DuelKind.BOXES)
        val shared = base.graph.edges.indexOfFirst { it.from == 1 && it.to == 4 }
        val surrounding = listOf(0 to 1,0 to 3,3 to 4,1 to 2,2 to 5,4 to 5)
        val claimed = surrounding.map { pair -> base.graph.edges.indexOfFirst { it.from == pair.first && it.to == pair.second } }.associateWith { 1 }
        val state = base.copy(claimed = claimed)
        val next = state.takeEdge(shared)
        assertEquals(mapOf(0 to 0,1 to 0),next.boxes)
        assertEquals(0,next.turn)
        assertEquals(next,next.takeEdge(shared))
        var full = base
        base.graph.edges.indices.forEach { full = full.takeEdge(it) }
        assertNotNull(full.result)
        assertEquals(4,full.boxes.size)
    }
}
