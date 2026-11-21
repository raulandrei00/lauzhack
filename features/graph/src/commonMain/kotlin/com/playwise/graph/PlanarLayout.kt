package com.playwise.graph

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.random.Random

private const val EPS = 0.00001f

private fun orientation(a: Offset, b: Offset, c: Offset): Float =
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

private fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0f) 0f else
        (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    val x = p.x - a.x - t * dx
    val y = p.y - a.y - t * dy
    return sqrt(x * x + y * y)
}

/** True when every edge can be drawn straight, without crossing another edge or a third node. */
internal fun isCrossingFree(graph: LessonGraph, positions: List<Offset>): Boolean {
    if (positions.size != graph.size) return false
    for (i in positions.indices) for (j in i + 1 until positions.size) {
        if ((positions[i] - positions[j]).getDistance() < .10f) return false
    }
    for (edge in graph.edges) {
        val a = positions[edge.from]
        val b = positions[edge.to]
        for (node in positions.indices) {
            if (node != edge.from && node != edge.to &&
                pointToSegmentDistance(positions[node], a, b) <
                (if (graph.size <= 6) .05f else .02f)) return false
        }
    }
    for (i in graph.edges.indices) for (j in i + 1 until graph.edges.size) {
        val first = graph.edges[i]
        val second = graph.edges[j]
        if (first.from == second.from || first.from == second.to ||
            first.to == second.from || first.to == second.to) continue
        val a = positions[first.from]; val b = positions[first.to]
        val c = positions[second.from]; val d = positions[second.to]
        val abC = orientation(a, b, c); val abD = orientation(a, b, d)
        val cdA = orientation(c, d, a); val cdB = orientation(c, d, b)
        if (abC * abD < -EPS && cdA * cdB < -EPS) return false
        // With distinct endpoints, a collinear touch is also an overlap.
        if ((kotlin.math.abs(abC) < EPS && pointToSegmentDistance(c, a, b) < EPS) ||
            (kotlin.math.abs(abD) < EPS && pointToSegmentDistance(d, a, b) < EPS) ||
            (kotlin.math.abs(cdA) < EPS && pointToSegmentDistance(a, c, d) < EPS) ||
            (kotlin.math.abs(cdB) < EPS && pointToSegmentDistance(b, c, d) < EPS)) return false
    }
    return true
}

private fun ring(count: Int, radius: Float, phase: Double = 0.0): List<Offset> = List(count) { i ->
    val angle = -PI / 2 + phase + 2 * PI * i / count
    Offset(.5f + radius * cos(angle).toFloat(), .5f + radius * sin(angle).toFloat())
}

/**
 * Keep an existing clear drawing. For 2–6 nodes, search fixed point layouts exactly; for larger
 * boards, search continuous placements until edges and unrelated nodes no longer overlap.
 */
internal fun planarLayout(graph: LessonGraph, previous: List<Offset> = circleLayout(graph.size)): List<Offset> {
    require(!graph.directed && graph.size in 2..10 && isPlanarGraph(graph))
    if (isCrossingFree(graph, previous)) return previous
    val n = graph.size
    if (n > 6) return searchLargeLayout(graph, previous)
    val templates = buildList {
        add(circleLayout(n))
        if (n >= 4) {
            val inside = n - 3
            for (phase in listOf(0.0, PI / 3)) {
                add(ring(3, .43f, phase) + if (inside == 1) listOf(Offset(.5f, .5f))
                    else ring(inside, .16f, phase))
            }
        }
    }
    var best: List<Offset>? = null
    var bestCost = Float.POSITIVE_INFINITY
    for (template in templates) {
        val used = BooleanArray(n)
        val candidate = Array(n) { Offset.Zero }
        fun search(node: Int, cost: Float) {
            if (cost >= bestCost) return
            if (node == n) {
                val positions = candidate.toList()
                if (isCrossingFree(graph, positions)) {
                    best = positions
                    bestCost = cost
                }
                return
            }
            for (index in template.indices) if (!used[index]) {
                used[index] = true
                candidate[node] = template[index]
                val dx = previous[node].x - template[index].x
                val dy = previous[node].y - template[index].y
                search(node + 1, cost + dx * dx + dy * dy)
                used[index] = false
            }
        }
        search(0, 0f)
    }
    return best ?: error("No clear drawing found for a planar graph with $n vertices")
}

private fun layoutEnergy(graph: LessonGraph, positions: List<Offset>): Float {
    var energy = 0f
    for (i in positions.indices) for (j in i + 1 until positions.size) {
        val d = (positions[i] - positions[j]).getDistance()
        if (d < .17f) energy += 100f * (.17f - d)
    }
    for (edge in graph.edges) {
        val a = positions[edge.from]; val b = positions[edge.to]
        energy += .08f * (a - b).getDistance()
        for (node in positions.indices) if (node != edge.from && node != edge.to) {
            val d = pointToSegmentDistance(positions[node], a, b)
            if (d < .08f) energy += 100f * (.08f - d)
        }
    }
    for (i in graph.edges.indices) for (j in i + 1 until graph.edges.size) {
        val first = graph.edges[i]; val second = graph.edges[j]
        if (first.from == second.from || first.from == second.to ||
            first.to == second.from || first.to == second.to) continue
        val a = positions[first.from]; val b = positions[first.to]
        val c = positions[second.from]; val d = positions[second.to]
        if (orientation(a, b, c) * orientation(a, b, d) < 0f &&
            orientation(c, d, a) * orientation(c, d, b) < 0f) energy += 25f
    }
    return energy
}

private fun searchLargeLayout(graph: LessonGraph, previous: List<Offset>): List<Offset> {
    val n = graph.size
    val random = Random(graph.edges.fold(n) { seed, edge -> seed * 31 + edge.from * 11 + edge.to })
    var closest = previous
    var closestEnergy = layoutEnergy(graph, previous)
    repeat(12) { restart ->
        var current = if (restart == 0) previous else List(n) { node ->
            val base = if (restart % 3 == 0) circleLayout(n)[node] else Offset(random.nextFloat(), random.nextFloat())
            Offset(base.x.coerceIn(.06f, .94f), base.y.coerceIn(.06f, .94f))
        }
        var energy = layoutEnergy(graph, current)
        repeat(3200) { iteration ->
            val mutable = current.toMutableList()
            val node = random.nextInt(n)
            if (random.nextInt(8) == 0) {
                val other = random.nextInt(n)
                mutable[node] = current[other]
                mutable[other] = current[node]
            } else {
                val scale = .36f * (1f - iteration / 3200f) + .025f
                val old = current[node]
                mutable[node] = Offset(
                    (old.x + (random.nextFloat() * 2f - 1f) * scale).coerceIn(.05f, .95f),
                    (old.y + (random.nextFloat() * 2f - 1f) * scale).coerceIn(.05f, .95f)
                )
            }
            val nextEnergy = layoutEnergy(graph, mutable)
            val temperature = 1.5f * (1f - iteration / 3200f) + .03f
            if (nextEnergy < energy || random.nextFloat() < exp(((energy - nextEnergy) / temperature).toDouble()).toFloat()) {
                current = mutable
                energy = nextEnergy
                if (energy < closestEnergy) {
                    closest = current
                    closestEnergy = energy
                }
                if (isCrossingFree(graph, current) && current.indices.all { a ->
                        (a + 1 until n).all { b -> (current[a] - current[b]).getDistance() >= .11f }
                    }) return current
            }
        }
    }
    return if (isCrossingFree(graph, closest)) closest
        else error("Could not find a clear drawing for a planar graph with $n vertices")
}
