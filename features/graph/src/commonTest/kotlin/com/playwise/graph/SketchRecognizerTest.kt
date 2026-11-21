package com.playwise.graph

import kotlin.math.hypot
import kotlin.test.*

class SketchRecognizerTest {
    private val r = SketchRecognizer
    private fun circle(x: Float, y: Float, rad: Float = 22f, wobble: Float = 2f) = r.circleStroke(Pt(x, y), rad, wobble = wobble)
    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) = r.lineStroke(Pt(x1, y1), Pt(x2, y2))
    private fun pairs(res: SketchResult) = res.edges.map { minOf(it.from, it.to) to maxOf(it.from, it.to) }.toSet()

    @Test fun circlesAndLinesBecomeVerticesAndEdges() {
        val res = r.recognize(listOf(
            circle(50f, 100f), circle(200f, 100f), circle(125f, 220f),
            line(72f, 100f, 178f, 100f),            // A - C (sorted left to right: A=50, B=125, C=200)
            line(60f, 120f, 115f, 200f),            // A - B
        ))
        assertEquals(3, res.vertices.size)
        assertEquals(setOf(0 to 2, 0 to 1), pairs(res))
        assertEquals(listOf(StrokeKind.VERTEX, StrokeKind.VERTEX, StrokeKind.VERTEX, StrokeKind.EDGE, StrokeKind.EDGE), res.kinds)
        assertTrue(res.edges.none { it.directed })
    }

    @Test fun edgeEndsSnapEvenWhenTheyStopShortOrOvershoot() {
        val res = r.recognize(listOf(circle(50f, 50f), circle(250f, 50f), line(90f, 55f, 255f, 45f)))
        assertEquals(2, res.vertices.size)
        assertEquals(setOf(0 to 1), pairs(res))
    }

    @Test fun looseLineEndsCreateVerticesAndShareThem() {
        // Two lines, no circles, meeting at the same point: 3 vertices, 2 edges.
        val res = r.recognize(listOf(line(20f, 20f, 150f, 20f), line(155f, 25f, 150f, 160f)))
        assertEquals(3, res.vertices.size)
        assertEquals(2, res.edges.size)
        assertTrue(res.vertices.none { it.drawn })
        assertTrue(res.notes.any { "no circle" in it })
    }

    @Test fun tapsAreDots() {
        val res = r.recognize(listOf(listOf(Pt(10f, 10f)), listOf(Pt(200f, 10f), Pt(202f, 11f)), line(10f, 10f, 200f, 10f)))
        assertEquals(2, res.vertices.size)
        assertEquals(1, res.edges.size)
    }

    @Test fun lineThroughACircleIsSplitIntoTwoEdges() {
        val res = r.recognize(listOf(circle(50f, 100f), circle(150f, 100f), circle(250f, 100f), line(50f, 100f, 250f, 100f)))
        assertEquals(setOf(0 to 1, 1 to 2), pairs(res))
    }

    @Test fun vArrowheadMakesEdgeOneWay() {
        val a = Pt(50f, 100f); val b = Pt(250f, 100f)
        val tip = Pt(228f, 100f)
        val res = r.recognize(listOf(circle(a.x, a.y), circle(b.x, b.y), line(72f, 100f, 228f, 100f), r.arrowStroke(a, tip)))
        assertEquals(listOf(SketchEdge(0, 1, true)), res.edges)
        assertEquals(StrokeKind.ARROW, res.kinds[3])
    }

    @Test fun arrowheadAtTheStartPointsTheOtherWay() {
        val res = r.recognize(listOf(circle(50f, 100f), circle(250f, 100f), line(72f, 100f, 228f, 100f), r.arrowStroke(Pt(250f, 100f), Pt(72f, 100f))))
        assertEquals(listOf(SketchEdge(1, 0, true)), res.edges)
    }

    @Test fun hookAtTheEndOfALineMakesItOneWay() {
        val shaft = line(72f, 100f, 228f, 100f)
        val hook = r.lineStroke(Pt(228f, 100f), Pt(212f, 88f), 6).drop(1)
        val res = r.recognize(listOf(circle(50f, 100f), circle(250f, 100f), shaft + hook))
        assertEquals(listOf(SketchEdge(0, 1, true)), res.edges)
    }

    @Test fun duplicateLinesAndSelfLoopsAreHandled() {
        val loopBack = r.lineStroke(Pt(60f, 80f), Pt(120f, 20f)) + r.lineStroke(Pt(120f, 20f), Pt(62f, 82f))
        val res = r.recognize(listOf(circle(50f, 100f), circle(250f, 100f), line(72f, 100f, 228f, 100f), line(70f, 104f, 230f, 104f), loopBack))
        assertEquals(1, res.edges.size)
        assertEquals(StrokeKind.IGNORED, res.kinds[4])
        assertTrue(res.notes.any { "same vertex" in it }, res.toString())
    }

    @Test fun overlappingCirclesMergeAndHugeCirclesAreIgnored() {
        val res = r.recognize(listOf(circle(100f, 100f), circle(104f, 98f), circle(300f, 300f, rad = 200f)))
        assertEquals(1, res.vertices.size)
        assertTrue(res.notes.any { "very large" in it })
    }

    @Test fun roughHandDrawnCircleWithGapStillCounts() {
        // 85% of a wobbly circle, not closed.
        val partial = r.circleStroke(Pt(100f, 100f), 25f, points = 40, wobble = 3f).take(35)
        val res = r.recognize(listOf(partial))
        assertEquals(1, res.vertices.size)
        assertTrue(res.vertices[0].drawn)
    }

    @Test fun exampleDrawingIsAHouseWithOneArrow() {
        val res = r.recognize(r.exampleStrokes(400f, 300f))
        assertEquals(5, res.vertices.size)
        assertEquals(6, res.edges.size)
        assertEquals(1, res.edges.count { it.directed })
        assertTrue(res.notes.isEmpty(), res.notes.toString())
    }

    @Test fun resampleKeepsEndpointsAndSpacing() {
        val pts = r.resample(listOf(Pt(0f, 0f), Pt(100f, 0f)), 11)
        assertEquals(11, pts.size)
        assertEquals(10f, hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y), .01f)
        assertEquals(100f, pts.last().x, .01f)
    }
}

class DrawnGraphTest {
    private fun path(n: Int) = DrawnGraph(List(n) { DrawnVertex(it / n.toFloat(), .5f) }, (0 until n - 1).map { DrawnEdge(it, it + 1) })

    @Test fun editsKeepIndicesConsistent() {
        var g = path(4).addEdge(0, 3)
        assertEquals(4, g.edges.size)
        assertSame(g, g.addEdge(1, 0)) // duplicate in either order is ignored
        g = g.removeVertex(1)          // edges 0-1 and 1-2 vanish, 2-3 becomes 1-2, 0-3 becomes 0-2
        assertEquals(3, g.size)
        assertEquals(setOf(1 to 2, 0 to 2), g.edges.map { minOf(it.a, it.b) to maxOf(it.a, it.b) }.toSet())
        g = g.setDirection(g.edgeIndex(0, 2), 2)
        assertEquals(DrawnEdge(2, 0, 1, true), g.edges[g.edgeIndex(0, 2)])
        assertEquals(26, (0 until 40).fold(DrawnGraph()) { acc, i -> acc.addVertex(i / 40f, .5f) }.size)
    }

    @Test fun factsDescribeTheGraph() {
        val tree = analyze(path(5))
        assertTrue(tree.isTree); assertFalse(tree.hasCycle); assertTrue(tree.bipartite)
        val triangle = analyze(path(3).addEdge(0, 2))
        assertTrue(triangle.hasCycle); assertFalse(triangle.bipartite); assertFalse(triangle.isTree)
        val split = analyze(DrawnGraph(List(4) { DrawnVertex(0f, 0f) }, listOf(DrawnEdge(0, 1), DrawnEdge(2, 3))))
        assertEquals(2, split.components.size)
        assertEquals(listOf(1, 1, 1, 1), split.degrees)
    }

    @Test fun oneWayEdgesGiveStrongGroups() {
        // A→B→C→A cycle plus C→D: groups {A,B,C} and {D}.
        val g = DrawnGraph(List(4) { DrawnVertex(0f, 0f) },
            listOf(DrawnEdge(0, 1, directed = true), DrawnEdge(1, 2, directed = true), DrawnEdge(2, 0, directed = true), DrawnEdge(2, 3, directed = true)))
        assertEquals(listOf(setOf(0, 1, 2), setOf(3)), analyze(g).strongGroups)
        assertNull(cheapestRoute(g, 3, 0))
        assertEquals(listOf(0, 1, 2, 3) to 3, cheapestRoute(g, 0, 3))
    }

    @Test fun cheapestRouteUsesWeights() {
        val g = path(3).addEdge(0, 2).let { it.setWeight(it.edgeIndex(0, 2), 5) }
        assertEquals(listOf(0, 1, 2) to 2, cheapestRoute(g, 0, 2))
        assertEquals(listOf(0, 2) to 1, cheapestRoute(g.setWeight(g.edgeIndex(0, 2), 1), 0, 2))
    }

    @Test fun layoutsStayOnTheBoard() {
        val g = path(6).addEdge(0, 5).addEdge(1, 4)
        for (laid in listOf(tidyArrangement(g), circleArrangement(g), tidyArrangement(DrawnGraph(List(3) { DrawnVertex(.5f, .5f) })))) {
            assertTrue(laid.vertices.all { it.x in 0f..1f && it.y in 0f..1f && !it.x.isNaN() && !it.y.isNaN() })
        }
        // Tidy spreads vertices out: no two end up on top of each other.
        val t = tidyArrangement(g).vertices
        for (i in t.indices) for (j in i + 1 until t.size) assertTrue(hypot(t[i].x - t[j].x, t[i].y - t[j].y) > .05f)
    }

    @Test fun sketchConversionKeepsTheDrawingLayout() {
        val res = SketchRecognizer.recognize(SketchRecognizer.exampleStrokes(400f, 300f))
        val g = DrawnGraph.fromSketch(res, 400f, 300f)
        assertEquals(5, g.size)
        // Scaled to fill the board: the drawing touches at least one pair of opposite sides.
        val xs = g.vertices.map { it.x }; val ys = g.vertices.map { it.y }
        assertTrue((xs.min() < .01f && xs.max() > .99f) || (ys.min() < .01f && ys.max() > .99f))
        assertEquals(xs.min(), g.vertices[0].x)       // A is the leftmost vertex
        // Shape is kept: same aspect ratio as the original drawing (house is 0.7w × 0.73h of a 400×300 area).
        val drawnRatio = (.85f - .15f) * 400f / ((.88f - .15f) * 300f)
        assertEquals(drawnRatio, (xs.max() - xs.min()) * 400f / ((ys.max() - ys.min()) * 300f), .02f)
        // A single vertex is centred.
        val one = DrawnGraph.fromSketch(SketchRecognizer.recognize(listOf(listOf(Pt(10f, 10f)))), 400f, 300f)
        assertEquals(DrawnVertex(.5f, .5f), one.vertices.single())
    }
}
