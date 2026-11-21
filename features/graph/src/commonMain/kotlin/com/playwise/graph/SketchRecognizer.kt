package com.playwise.graph

import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Turns freehand pen strokes into a graph, using stroke geometry rather than machine learning.
 *
 * Why not a neural network: a hand-drawn graph is made of a few geometric primitives
 * (closed loops, open lines, small "V" arrowheads), so per-stroke features such as closure,
 * roundness, length and corner sharpness separate them reliably. Geometry needs no training
 * data or model download, runs instantly offline on every target (Android, desktop, WasmGC),
 * and can explain each decision to the learner. ML would only pay off for reading handwritten
 * labels or weights, which the editor handles with taps instead.
 *
 * All coordinates and thresholds are in dp, so behaviour does not depend on screen density.
 */
data class Pt(val x: Float, val y: Float) {
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
    fun dist(o: Pt) = hypot(x - o.x, y - o.y)
}

enum class StrokeKind { VERTEX, EDGE, ARROW, IGNORED }

data class SketchVertex(val center: Pt, val radius: Float, val drawn: Boolean)

/** [directed] means the edge points from [from] to [to]. */
data class SketchEdge(val from: Int, val to: Int, val directed: Boolean)

data class SketchResult(
    val vertices: List<SketchVertex>,
    val edges: List<SketchEdge>,
    /** One entry per input stroke, for colouring the live drawing. */
    val kinds: List<StrokeKind>,
    /** Plain-language notes about anything that was ignored or guessed. */
    val notes: List<String>,
)

object SketchRecognizer {
    const val DOT_SIZE = 16f          // a stroke whose box is smaller than this is a dot / tap
    const val SNAP = 30f              // how far beyond a circle an edge end may stop and still attach
    const val IMPLICIT_RADIUS = 20f   // size of a vertex created at a loose edge end
    const val MAX_VERTEX_RADIUS = 110f
    const val ARROW_MAX_LENGTH = 90f

    private data class Shape(val stroke: List<Pt>, val length: Float, val diag: Float, val gap: Float,
                             val centroid: Pt, val meanRadius: Float, val roundness: Float, val aspect: Float)

    private fun shape(s: List<Pt>): Shape {
        val length = s.zipWithNext().sumOf { (a, b) -> a.dist(b).toDouble() }.toFloat()
        val minX = s.minOf { it.x }; val maxX = s.maxOf { it.x }
        val minY = s.minOf { it.y }; val maxY = s.maxOf { it.y }
        val w = maxX - minX; val h = maxY - minY
        val centroid = Pt((minX + maxX) / 2, (minY + maxY) / 2)
        val radii = s.map { it.dist(centroid) }
        val mean = radii.average().toFloat()
        val sd = sqrt(radii.map { (it - mean) * (it - mean) }.average()).toFloat()
        return Shape(s, length, hypot(w, h), s.first().dist(s.last()), centroid, mean,
            if (mean > 0f) sd / mean else 0f, if (minOf(w, h) > 0f) maxOf(w, h) / minOf(w, h) else 99f)
    }

    private fun isDot(sh: Shape) = sh.diag < DOT_SIZE && sh.length < DOT_SIZE * 3

    private fun isLoop(sh: Shape): Boolean {
        if (sh.diag < DOT_SIZE) return false
        val closed = (sh.gap < .35f * sh.diag && sh.length > 1.5f * sh.diag) || (sh.gap < .55f * sh.diag && sh.length > 2.0f * sh.diag)
        return closed && sh.roundness < .38f && sh.aspect < 3f
    }

    /** Resamples a polyline to [n] evenly spaced points. */
    internal fun resample(s: List<Pt>, n: Int = 32): List<Pt> {
        if (s.size < 2) return List(n) { s.first() }
        val total = s.zipWithNext().sumOf { (a, b) -> a.dist(b).toDouble() }.toFloat()
        if (total == 0f) return List(n) { s.first() }
        val step = total / (n - 1)
        val out = mutableListOf(s.first())
        var carried = 0f
        for ((a, b) in s.zipWithNext()) {
            var seg = a.dist(b)
            var start = a
            while (carried + seg >= step && out.size < n) {
                val t = (step - carried) / seg
                val p = start + (b - start) * t
                out += p
                seg -= start.dist(p); start = p; carried = 0f
            }
            carried += seg
        }
        while (out.size < n) out += s.last()
        return out
    }

    /** Sharpest corner: (index in the resampled list, turning angle in degrees). */
    internal fun sharpestCorner(r: List<Pt>): Pair<Int, Float> {
        var best = 0 to 0f
        for (i in 2..r.size - 3) {
            val u = r[i] - r[i - 2]; val v = r[i + 2] - r[i]
            val nu = hypot(u.x, u.y); val nv = hypot(v.x, v.y)
            if (nu == 0f || nv == 0f) continue
            val cos = ((u.x * v.x + u.y * v.y) / (nu * nv)).coerceIn(-1f, 1f)
            val angle = (acos(cos) * 180.0 / kotlin.math.PI).toFloat()
            if (angle > best.second) best = i to angle
        }
        return best
    }

    private fun segmentDist(p: Pt, a: Pt, b: Pt): Float {
        val ab = b - a
        val len2 = ab.x * ab.x + ab.y * ab.y
        if (len2 == 0f) return p.dist(a)
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
        return p.dist(a + ab * t)
    }

    fun recognize(strokes: List<List<Pt>>): SketchResult {
        val kinds = MutableList(strokes.size) { StrokeKind.IGNORED }
        val notes = mutableListOf<String>()
        val vertices = mutableListOf<SketchVertex>()
        val shapes = strokes.map { if (it.isEmpty()) null else shape(it) }

        // 1. Vertices: dots and closed loops. Overlapping ones are merged.
        fun addVertex(v: SketchVertex): Int {
            val hit = vertices.indexOfFirst { it.center.dist(v.center) < maxOf(it.radius, v.radius) * .8f }
            if (hit >= 0) {
                val o = vertices[hit]
                if (v.drawn && !o.drawn) vertices[hit] = v
                return hit
            }
            vertices += v
            return vertices.lastIndex
        }
        var tooBig = 0
        shapes.forEachIndexed { i, sh ->
            sh ?: return@forEachIndexed
            when {
                isDot(sh) -> { addVertex(SketchVertex(sh.centroid, IMPLICIT_RADIUS, true)); kinds[i] = StrokeKind.VERTEX }
                isLoop(sh) && sh.meanRadius > MAX_VERTEX_RADIUS -> tooBig++
                isLoop(sh) -> { addVertex(SketchVertex(sh.centroid, maxOf(sh.meanRadius, 14f), true)); kinds[i] = StrokeKind.VERTEX }
            }
        }
        if (tooBig > 0) notes += "$tooBig very large circle(s) ignored. Draw vertices as small circles or dots."

        // 2. Open strokes: arrowhead candidates are short and V-shaped; everything else is a line.
        data class Line(val stroke: Int, val points: List<Pt>, val hookDirected: Boolean)
        val lines = mutableListOf<Line>()
        val arrowCandidates = mutableListOf<Pair<Int, Pt>>()
        shapes.forEachIndexed { i, sh ->
            if (sh == null || kinds[i] == StrokeKind.VERTEX || isLoop(sh)) return@forEachIndexed
            val r = resample(sh.stroke)
            val (corner, angle) = sharpestCorner(r)
            val t = corner / (r.size - 1f)
            when {
                sh.length < ARROW_MAX_LENGTH && angle > 55f && t in .2f..0.8f -> arrowCandidates += i to r[corner]
                sh.length >= ARROW_MAX_LENGTH && angle > 100f && t > .7f && sh.length * (1 - t) < 50f ->
                    lines += Line(i, r.subList(0, corner + 1), hookDirected = true)    // line ending in a hook
                else -> lines += Line(i, sh.stroke, hookDirected = false)
            }
        }

        // 3. Lines become edges. They snap to vertices at their ends (or create one there), and
        //    a line drawn straight through a circle is split into two edges.
        val drawnCount = vertices.size
        val edges = linkedMapOf<Pair<Int, Int>, SketchEdge>()
        var selfLoops = 0
        fun snap(p: Pt): Int {
            val near = vertices.indices.filter { vertices[it].center.dist(p) <= vertices[it].radius + SNAP }
                .minByOrNull { vertices[it].center.dist(p) }
            return near ?: addVertex(SketchVertex(p, IMPLICIT_RADIUS, false))
        }
        for (line in lines) {
            val pts = line.points
            if (pts.size < 2) continue
            if (pts.first().dist(pts.last()) < DOT_SIZE) { selfLoops++; continue }   // ends where it began
            val path = mutableListOf(snap(pts.first()))
            // Vertices the line passes through in the middle (only real drawn ones).
            for (p in pts) for (v in 0 until drawnCount) {
                if (vertices[v].center.dist(p) < vertices[v].radius * .9f && path.last() != v && v !in path) path += v
            }
            val end = snap(pts.last())
            if (path.last() != end) path += end
            if (path.size < 2) { selfLoops++; kinds[line.stroke] = StrokeKind.IGNORED; continue }
            kinds[line.stroke] = StrokeKind.EDGE
            path.zipWithNext().forEachIndexed { k, (a, b) ->
                val directedHere = line.hookDirected && k == path.size - 2
                val key = minOf(a, b) to maxOf(a, b)
                val old = edges[key]
                edges[key] = when {
                    old == null -> SketchEdge(a, b, directedHere)
                    directedHere -> SketchEdge(a, b, true)
                    else -> old
                }
            }
        }
        if (selfLoops > 0) notes += "$selfLoops line(s) started and ended at the same vertex, so they were ignored. Loops aren't supported."

        // 4. Arrowheads: a V whose tip sits near the end of an edge makes it point into that vertex.
        var lonelyArrows = 0
        for ((stroke, tip) in arrowCandidates) {
            val match = edges.values.flatMap { e -> listOf(e to e.to, e to e.from) }
                .filter { (e, v) ->
                    val other = if (v == e.to) e.from else e.to
                    tip.dist(vertices[v].center) <= vertices[v].radius + 40f &&
                        segmentDist(tip, vertices[other].center, vertices[v].center) <= 28f
                }
                .minByOrNull { (_, v) -> tip.dist(vertices[v].center) }
            if (match == null) {
                // Not near an edge end: treat it as a short line after all.
                val sh = shapes[stroke]!!
                val a = snap(sh.stroke.first()); val b = snap(sh.stroke.last())
                if (a != b) {
                    edges.getOrPut(minOf(a, b) to maxOf(a, b)) { SketchEdge(a, b, false) }
                    kinds[stroke] = StrokeKind.EDGE
                } else lonelyArrows++
                continue
            }
            val (e, into) = match
            val from = if (into == e.to) e.from else e.to
            edges[minOf(from, into) to maxOf(from, into)] = SketchEdge(from, into, true)
            kinds[stroke] = StrokeKind.ARROW
        }
        if (lonelyArrows > 0) notes += "$lonelyArrows small mark(s) didn't touch a line, so they were ignored."
        val implicit = vertices.count { !it.drawn }
        if (implicit > 0) notes += "$implicit line end(s) had no circle, so a vertex was added there."
        val ignored = kinds.count { it == StrokeKind.IGNORED } - selfLoops - lonelyArrows - tooBig
        if (ignored > 0) notes += "$ignored stroke(s) were not understood and were ignored."

        // 5. Label vertices left to right (A is the leftmost), like the other boards.
        val order = vertices.indices.sortedWith(compareBy({ vertices[it].center.x }, { vertices[it].center.y }))
        val newIndex = IntArray(vertices.size).also { arr -> order.forEachIndexed { n, old -> arr[old] = n } }
        return SketchResult(
            vertices = order.map { vertices[it] },
            edges = edges.values.map { SketchEdge(newIndex[it.from], newIndex[it.to], it.directed) },
            kinds = kinds,
            notes = notes,
        )
    }

    // ---- Synthetic strokes: used by "Show me an example" and by the tests ----

    fun circleStroke(c: Pt, r: Float, points: Int = 28, wobble: Float = 0f): List<Pt> = List(points + 1) { i ->
        val a = 2 * kotlin.math.PI * i / points
        val rr = r + wobble * kotlin.math.sin(a * 3).toFloat()
        Pt(c.x + rr * kotlin.math.cos(a).toFloat(), c.y + rr * kotlin.math.sin(a).toFloat())
    }

    fun lineStroke(a: Pt, b: Pt, points: Int = 16): List<Pt> = List(points) { i -> a + (b - a) * (i / (points - 1f)) }

    /** A "V" arrowhead whose tip is at [tip], for an edge arriving from [from]. */
    fun arrowStroke(from: Pt, tip: Pt, size: Float = 16f): List<Pt> {
        val d = tip - from
        val len = hypot(d.x, d.y)
        val u = Pt(d.x / len, d.y / len); val n = Pt(-u.y, u.x)
        val left = tip - u * size + n * (size * .7f)
        val right = tip - u * size - n * (size * .7f)
        return lineStroke(left, tip, 6) + lineStroke(tip, right, 6).drop(1)
    }

    /** A small house-shaped example graph inside a [w] x [h] dp area. */
    fun exampleStrokes(w: Float, h: Float): List<List<Pt>> {
        val r = minOf(w, h) * .07f
        val p = listOf(Pt(w * .15f, h * .55f), Pt(w * .5f, h * .15f), Pt(w * .85f, h * .55f), Pt(w * .3f, h * .88f), Pt(w * .7f, h * .88f))
        fun edge(a: Int, b: Int): List<Pt> {
            val d = p[b] - p[a]; val len = hypot(d.x, d.y); val u = Pt(d.x / len, d.y / len)
            return lineStroke(p[a] + u * r, p[b] - u * r)
        }
        val arrowTip = p[2] - (p[2] - p[1]) * (r / p[1].dist(p[2]))
        return p.map { circleStroke(it, r, wobble = r * .08f) } +
            listOf(edge(0, 1), edge(1, 2), edge(0, 3), edge(3, 4), edge(4, 2), edge(0, 2), arrowStroke(p[1], arrowTip))
    }
}

