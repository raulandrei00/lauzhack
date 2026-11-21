package com.playwise.graph

import kotlin.test.*

class CustomBoardsTest {
    private fun graph(text: String) = (parseGraphText(text) as ParseResult.Ok).graph
    private val lesson = { t: GraphTopic -> GameChoice.Lesson(t) }
    private val duel = { k: DuelKind -> GameChoice.Duel(k) }

    // ---- text import / export ----

    @Test fun textRoundTrips() {
        val g = graph("A-B 3, b>c; C-D\nE")
        assertEquals(5, g.size)
        assertEquals(listOf(DrawnEdge(0, 1, 3, false), DrawnEdge(1, 2, 1, true), DrawnEdge(2, 3, 1, false)), g.edges)
        assertEquals("A-B 3, B>C, C-D, E", graphToText(g))
        assertEquals(g.edges, graph(graphToText(g)).edges)
        assertEquals(listOf(DrawnEdge(1, 0, 1, true)), graph("A<B").edges)
        assertEquals(listOf(DrawnEdge(0, 2, 7, true)), graph("A->C:7").edges)
        assertTrue(g.vertices.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test fun badTextExplainsTheProblem() {
        for ((text, words) in listOf("" to "at least one", "A-A" to "two different", "A~B" to "don't understand", "A 5" to "needs a line", "A-B 500" to "up to")) {
            val r = parseGraphText(text)
            assertTrue(r is ParseResult.Error && words in r.message, "$text -> $r")
        }
    }

    // ---- directions and colors ----

    @Test fun oneWayHelpers() {
        val g = DrawnGraph(List(3) { DrawnVertex(0f, 0f) }).addEdge(2, 0, oneWay = true)
        assertEquals(DrawnEdge(2, 0, 1, true), g.edges.single())
        assertFalse(g.allTwoWay().anyDirected)
        assertTrue(g.allTwoWay().allOneWay().edges.all { it.directed })
    }

    @Test fun paintSurvivesEditsAndLayouts() {
        val g = graph("A-B, B-C").paint(0, 2).paint(2, 2)
        assertEquals(2, g.moveVertex(0, .3f, .3f).vertices[0].color)
        assertEquals(2, tidyArrangement(g).vertices[0].color)
        assertEquals(2, circleArrangement(g).vertices[2].color)
        assertEquals(emptyList(), g.colorClashes())
        assertEquals(listOf(0, 1), g.paint(1, 2).colorClashes())   // B matches both A and C
        assertEquals(listOf(0), g.paint(1, 2).paint(2, 1).colorClashes())
        assertTrue(g.clearPaint().vertices.all { it.color == null })
    }

    @Test fun bestColoringFindsTheChromaticNumber() {
        fun check(text: String, expected: Int) {
            val g = graph(text)
            val r = bestColoring(g)!!
            assertEquals(expected, r.count, text)
            assertTrue(r.exact)
            assertEquals(emptyList(), g.withColoring(r.colors).colorClashes(), text)
        }
        check("A-B, B-C, C-D, D-A", 2)                       // even cycle
        check("A-B, B-C, C-D, D-E, E-A", 3)                  // odd cycle
        check("A-B, A-C, A-D, B-C, B-D, C-D", 4)             // K4
        check("A, B, C", 1)                                  // no edges
        // Petersen graph: chromatic number 3
        check("A-B, B-C, C-D, D-E, E-A, A-F, B-G, C-H, D-I, E-J, F-H, H-J, J-G, G-I, I-F", 3)
        // K7 needs 7 > 6 paints
        val k7 = (0 until 7).flatMap { a -> (a + 1 until 7).map { b -> "${'A' + a}-${'A' + b}" } }.joinToString(", ")
        assertNull(bestColoring(graph(k7)))
    }

    @Test fun bigGraphsUseGreedyColoring() {
        val ring = (0 until 20).joinToString(", ") { "${'A' + it}-${'A' + (it + 1) % 20}" }
        val r = bestColoring(graph(ring))!!
        assertFalse(r.exact)
        assertTrue(r.count in 2..3)
        assertEquals(emptyList(), graph(ring).withColoring(r.colors).colorClashes())
    }

    // ---- playing games on a custom graph ----

    @Test fun endsAreRenumberedToAandLast() {
        val g = graph("A-B, B-C, C-D")              // play from C to A
        val (ordered, order) = g.withEnds(2, 0)
        assertEquals(listOf(2, 1, 3, 0), order)      // new A = old C, new D = old A
        assertEquals(setOf(0 to 1, 1 to 3, 0 to 2), ordered.edges.map { minOf(it.a, it.b) to maxOf(it.a, it.b) }.toSet())
        val board = playableBoard(g, 2, 0, lesson(GraphTopic.BFS))
        assertEquals(2, shortestDistances(board.graph, 0, GraphTopic.BFS).last())   // C→B→A
        assertEquals(ordered.vertices[0].x, board.positions[0].x)
    }

    @Test fun directionsBecomeArrowsOnlyWhereTheGameUsesThem() {
        val g = graph("A>B, B-C")
        val dij = playableBoard(g, 0, 2, lesson(GraphTopic.DIJKSTRA)).graph
        assertTrue(dij.directed)
        assertEquals(3, dij.edges.size)                // A→B, B→C, C→B
        val kayles = playableBoard(g, 0, 2, duel(DuelKind.KAYLES)).graph
        assertFalse(kayles.directed)
        assertEquals(2, kayles.edges.size)
        assertEquals("Arrows are treated as two-way roads in this game.", gameNote(g, duel(DuelKind.KAYLES)))
        assertNull(gameNote(g, lesson(GraphTopic.DIJKSTRA)))
    }

    @Test fun eligibilityExplainsEveryRefusal() {
        val g = graph("A-B 2, B-C, C-D, D-A")
        assertNull(gameProblem(g, 0, 2, lesson(GraphTopic.DIJKSTRA)))
        assertTrue("weight 0 or 1" in gameProblem(g, 0, 2, lesson(GraphTopic.ZERO_ONE))!!)
        assertTrue("one-way" in gameProblem(g, 0, 2, lesson(GraphTopic.SCC))!!)
        assertNull(gameProblem(g.allOneWay(), 0, 2, lesson(GraphTopic.SCC)))
        assertTrue("grid" in gameProblem(g, 0, 2, duel(DuelKind.BOXES))!!)
        assertTrue("different" in gameProblem(g, 1, 1, duel(DuelKind.PATH))!!)
        assertTrue("joined directly" in gameProblem(g, 0, 1, duel(DuelKind.SHANNON))!!)
        assertNull(gameProblem(g, 0, 2, duel(DuelKind.SHANNON)))
        // One-way arrows block the route only for games that keep directions.
        val oneWay = graph("A<B")
        assertTrue("one-way arrows" in gameProblem(oneWay, 0, 1, lesson(GraphTopic.BFS))!!)
        assertNull(gameProblem(oneWay, 1, 0, lesson(GraphTopic.BFS)))
        // Non-planar K5 cannot start a planar game; a square can.
        val k5 = graph((0 until 5).flatMap { a -> (a + 1 until 5).map { b -> "${'A' + a}-${'A' + b}" } }.joinToString(", "))
        assertTrue("crossing" in gameProblem(k5, 0, 1, duel(DuelKind.PLANAR))!!)
        assertNull(gameProblem(g, 0, 1, duel(DuelKind.PLANAR)))
        assertTrue("Add some" in gameProblem(DrawnGraph(), 0, 0, duel(DuelKind.KAYLES))!!)
    }

    @Test fun sabotageRaisesBothDirectionsOfATwoWayRoad() {
        val board = playableBoard(graph("A-B 2, B>C 3"), 0, 2, duel(DuelKind.PATH))
        assertTrue(board.graph.directed)
        val i = board.graph.edges.indexOfFirst { it.from == 0 && it.to == 1 }
        val after = DuelState(DuelKind.PATH, board.graph).raiseCost(i)
        assertEquals(listOf(3, 3), after.graph.edges.filter { minOf(it.from, it.to) == 0 }.map { it.weight })
        assertEquals(3, after.graph.edges.single { it.from == 1 && it.to == 2 }.weight)   // one-way road untouched
        assertEquals(4, after.budget)                                                      // one point spent
    }

    @Test fun everyAllowedGameBuildsAValidStartingState() {
        val g = graph("A-B 2, B-C, C-D 4, D-A, B-D, D>E")
        for (choice in allGameChoices) {
            if (gameProblem(g, 0, 4, choice) != null) continue
            val board = playableBoard(g, 0, 4, choice)
            assertEquals(g.size, board.positions.size, choice.title)
            when (choice) {
                is GameChoice.Lesson -> if (choice.usesEnds) assertNotEquals(Int.MAX_VALUE, shortestDistances(board.graph, 0, choice.topic).last(), choice.title)
                is GameChoice.Duel -> when (choice.kind) {
                    DuelKind.PLANAR -> PlanarGame(board.graph, 3)   // constructor validates planarity
                    else -> DuelState(choice.kind, board.graph)
                }
            }
        }
    }
}
