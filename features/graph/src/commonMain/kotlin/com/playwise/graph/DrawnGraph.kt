package com.playwise.graph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A vertex of a user-made graph, at normalized board coordinates (0..1 on both axes), with an optional paint index. */
data class DrawnVertex(val x: Float, val y: Float, val color: Int? = null)

/** An edge of a user-made graph. If [directed], it points from [a] to [b]. */
data class DrawnEdge(val a: Int, val b: Int, val weight: Int = 1, val directed: Boolean = false) {
    fun joins(u: Int, v: Int) = (a == u && b == v) || (a == v && b == u)
}

/** Immutable, so every edit is one value and undo is just a stack of earlier values. */
data class DrawnGraph(val vertices: List<DrawnVertex> = emptyList(), val edges: List<DrawnEdge> = emptyList()) {
    val size get() = vertices.size
    val anyDirected get() = edges.any { it.directed }

    fun edgeIndex(u: Int, v: Int) = edges.indexOfFirst { it.joins(u, v) }

    fun addVertex(x: Float, y: Float): DrawnGraph =
        if (size >= MAX_VERTICES) this else copy(vertices = vertices + DrawnVertex(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))

    fun moveVertex(i: Int, x: Float, y: Float): DrawnGraph =
        copy(vertices = vertices.mapIndexed { k, v -> if (k == i) v.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)) else v })

    /** Removes a vertex and its edges; later vertices shift down one letter. */
    fun removeVertex(i: Int): DrawnGraph = copy(
        vertices = vertices.filterIndexed { k, _ -> k != i },
        edges = edges.filter { it.a != i && it.b != i }.map {
            it.copy(a = if (it.a > i) it.a - 1 else it.a, b = if (it.b > i) it.b - 1 else it.b)
        })

    /** Adds u-v; with [oneWay] it points from u to v. */
    fun addEdge(u: Int, v: Int, oneWay: Boolean = false): DrawnGraph =
        if (u == v || u !in vertices.indices || v !in vertices.indices || edgeIndex(u, v) >= 0) this
        else copy(edges = edges + DrawnEdge(u, v, directed = oneWay))

    fun allOneWay(): DrawnGraph = copy(edges = edges.map { it.copy(directed = true) })
    fun allTwoWay(): DrawnGraph = copy(edges = edges.map { it.copy(directed = false) })

    /** Paints vertex [i]; null removes the paint. */
    fun paint(i: Int, color: Int?): DrawnGraph = copy(vertices = vertices.mapIndexed { k, v -> if (k == i) v.copy(color = color) else v })
    fun clearPaint(): DrawnGraph = copy(vertices = vertices.map { it.copy(color = null) })
    fun withColoring(colors: List<Int>): DrawnGraph = copy(vertices = vertices.mapIndexed { k, v -> v.copy(color = colors.getOrNull(k)) })

    /** Indices of edges whose two ends have the same paint (direction doesn't matter for coloring). */
    fun colorClashes(): List<Int> = edges.indices.filter { k ->
        val c = vertices[edges[k].a].color
        c != null && c == vertices[edges[k].b].color
    }

    fun removeEdge(i: Int): DrawnGraph = copy(edges = edges.filterIndexed { k, _ -> k != i })

    fun setWeight(i: Int, w: Int): DrawnGraph = copy(edges = edges.mapIndexed { k, e -> if (k == i) e.copy(weight = w.coerceIn(0, MAX_WEIGHT)) else e })

    /** Two-way, a → b, b → a. */
    fun setDirection(i: Int, direction: Int): DrawnGraph = copy(edges = edges.mapIndexed { k, e ->
        if (k != i) e else {
            val lo = minOf(e.a, e.b); val hi = maxOf(e.a, e.b)
            when (direction) { 1 -> DrawnEdge(lo, hi, e.weight, true); 2 -> DrawnEdge(hi, lo, e.weight, true); else -> DrawnEdge(lo, hi, e.weight, false) }
        }
    })

    /** Neighbours you can travel to from [u] (two-way edges count both ways), with the cost. */
    fun out(u: Int): List<Pair<Int, Int>> = edges.mapNotNull { e ->
        when {
            e.a == u -> e.b to e.weight
            e.b == u && !e.directed -> e.a to e.weight
            else -> null
        }
    }

    /** Neighbours ignoring direction. */
    fun undirectedNeighbors(u: Int): List<Int> = edges.mapNotNull { e -> when (u) { e.a -> e.b; e.b -> e.a; else -> null } }

    companion object {
        const val MAX_VERTICES = 26
        const val MAX_WEIGHT = 99

        /**
         * Keeps the shape the learner drew, but scales it up to fill the board without stretching
         * (same factor on both axes) and centres it.
         */
        fun fromSketch(r: SketchResult, widthDp: Float, heightDp: Float): DrawnGraph {
            val pts = r.vertices.take(MAX_VERTICES).map { it.center }
            if (pts.isEmpty()) return DrawnGraph()
            val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
            val bw = maxX - minX; val bh = maxY - minY
            val scale = minOf(if (bw > 1f) widthDp / bw else Float.MAX_VALUE, if (bh > 1f) heightDp / bh else Float.MAX_VALUE)
                .takeIf { it != Float.MAX_VALUE } ?: 1f
            val offX = (widthDp - bw * scale) / 2; val offY = (heightDp - bh * scale) / 2
            return DrawnGraph(
                vertices = pts.map { DrawnVertex((((it.x - minX) * scale + offX) / widthDp).coerceIn(0f, 1f), (((it.y - minY) * scale + offY) / heightDp).coerceIn(0f, 1f)) },
                edges = r.edges.filter { it.from < MAX_VERTICES && it.to < MAX_VERTICES }.map { DrawnEdge(it.from, it.to, 1, it.directed) },
            )
        }
    }
}

// ---------------- Analysis ----------------

data class GraphFacts(
    val vertices: Int,
    val edges: Int,
    val degrees: List<Int>,
    val components: List<Set<Int>>,
    val hasCycle: Boolean,
    val isTree: Boolean,
    val bipartite: Boolean,
    val directed: Boolean,
    /** Strongly connected groups, only meaningful when some edge is one-way. */
    val strongGroups: List<Set<Int>>,
)

fun analyze(g: DrawnGraph): GraphFacts {
    val n = g.size
    val component = IntArray(n) { -1 }
    val components = mutableListOf<Set<Int>>()
    val side = IntArray(n) { -1 }
    var bipartite = true
    for (s in 0 until n) if (component[s] == -1) {
        val group = mutableSetOf(s)
        component[s] = components.size; side[s] = 0
        val queue = ArrayDeque(listOf(s))
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            for (v in g.undirectedNeighbors(u)) {
                if (component[v] == -1) { component[v] = components.size; side[v] = 1 - side[u]; group += v; queue += v }
                else if (side[v] == side[u]) bipartite = false
            }
        }
        components += group
    }
    // A simple undirected graph has a cycle exactly when it has more edges than a forest could.
    val hasCycle = g.edges.size > n - components.size
    return GraphFacts(
        vertices = n,
        edges = g.edges.size,
        degrees = List(n) { v -> g.edges.count { it.a == v || it.b == v } },
        components = components,
        hasCycle = hasCycle,
        isTree = n > 0 && components.size == 1 && !hasCycle,
        bipartite = bipartite,
        directed = g.anyDirected,
        strongGroups = if (g.anyDirected && n > 0) strongGroups(g) else emptyList(),
    )
}

private fun strongGroups(g: DrawnGraph): List<Set<Int>> {
    val arcs = g.edges.flatMap { e -> if (e.directed) listOf(e.a to e.b) else listOf(e.a to e.b, e.b to e.a) }
    val lesson = LessonGraph(g.size, arcs.distinct().map { GraphEdge(it.first, it.second) }, directed = true)
    return stronglyConnectedComponents(lesson).sortedBy { it.min() }
}

/** Cheapest route by total weight (Dijkstra), respecting one-way edges. Null if unreachable. */
fun cheapestRoute(g: DrawnGraph, from: Int, to: Int): Pair<List<Int>, Int>? {
    if (from !in 0 until g.size || to !in 0 until g.size) return null
    val dist = IntArray(g.size) { Int.MAX_VALUE }
    val parent = IntArray(g.size) { -1 }
    val done = BooleanArray(g.size)
    dist[from] = 0
    while (true) {
        val u = (0 until g.size).filter { !done[it] && dist[it] != Int.MAX_VALUE }.minByOrNull { dist[it] } ?: break
        done[u] = true
        for ((v, w) in g.out(u)) if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; parent[v] = u }
    }
    if (dist[to] == Int.MAX_VALUE) return null
    val path = generateSequence(to) { if (it == from) null else parent[it] }.toList().reversed()
    return path to dist[to]
}

// ---------------- Layouts ----------------

/** Vertices evenly spaced on a circle. */
fun circleArrangement(g: DrawnGraph): DrawnGraph = g.copy(vertices = List(g.size) { i ->
    val a = PI + 2 * PI * i / g.size.coerceAtLeast(1)
    g.vertices[i].copy(x = (.5 + .5 * cos(a)).toFloat(), y = (.5 + .5 * sin(a)).toFloat())
})

/**
 * Force-directed "tidy": edges pull like springs and all vertices push apart
 * (Fruchterman-Reingold), starting from the current drawing so the shape stays familiar.
 */
fun tidyArrangement(g: DrawnGraph, iterations: Int = 200): DrawnGraph {
    val n = g.size
    if (n < 2) return g
    val x = FloatArray(n) { g.vertices[it].x }
    val y = FloatArray(n) { g.vertices[it].y }
    // Separate exactly overlapping points so forces have a direction.
    for (i in 0 until n) { x[i] += (i % 3 - 1) * 1e-3f; y[i] += (i % 5 - 2) * 1e-3f }
    val k = sqrt(1f / n)
    var temperature = .1f
    repeat(iterations) {
        val dx = FloatArray(n); val dy = FloatArray(n)
        for (i in 0 until n) for (j in 0 until n) if (i != j) {
            val ex = x[i] - x[j]; val ey = y[i] - y[j]
            val d = sqrt(ex * ex + ey * ey).coerceAtLeast(1e-3f)
            val f = k * k / d
            dx[i] += ex / d * f; dy[i] += ey / d * f
        }
        for (e in g.edges) {
            val ex = x[e.a] - x[e.b]; val ey = y[e.a] - y[e.b]
            val d = sqrt(ex * ex + ey * ey).coerceAtLeast(1e-3f)
            val f = d * d / k
            dx[e.a] -= ex / d * f; dy[e.a] -= ey / d * f
            dx[e.b] += ex / d * f; dy[e.b] += ey / d * f
        }
        for (i in 0 until n) {
            val d = sqrt(dx[i] * dx[i] + dy[i] * dy[i]).coerceAtLeast(1e-6f)
            val step = minOf(d, temperature)
            x[i] += dx[i] / d * step; y[i] += dy[i] / d * step
        }
        temperature = maxOf(temperature * .97f, .002f)
    }
    // Stretch to fill the board.
    val minX = x.min(); val maxX = x.max(); val minY = y.min(); val maxY = y.max()
    val sx = (maxX - minX).takeIf { it > 1e-4f } ?: 1f
    val sy = (maxY - minY).takeIf { it > 1e-4f } ?: 1f
    return g.copy(vertices = List(n) { g.vertices[it].copy(x = ((x[it] - minX) / sx).coerceIn(0f, 1f), y = ((y[it] - minY) / sy).coerceIn(0f, 1f)) })
}
