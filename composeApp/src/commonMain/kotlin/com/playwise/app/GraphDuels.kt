package com.playwise.app

import kotlin.math.abs
import kotlin.math.sqrt

enum class DuelKind(val title: String, val rules: String, val lesson: String) {
    SPROUTS("Sprouts", "Take turns joining dots without crossing a line or passing through another dot. A loop uses two lives. Add a dot on each new curve; degree may never exceed three. Last legal move wins.", "Each move consumes two lives and creates one: total remaining lives drops by one. Planarity and the faces you enclose determine which lives can still meet."),
    COLORING("Graph Coloring Duel", "Builder (P1) and Blocker (P2) alternate coloring one vertex with one of three colors. Neighbors must differ. Builder wins if all vertices are colored; Blocker wins if no legal coloring move remains.", "A greedy color choice can consume a neighbor's last available color. A graph's chromatic number describes the existence of a coloring, not whether every sequence of choices succeeds."),
    SHANNON("Shannon Switching", "Connector (P1) claims a free edge; Cutter (P2) deletes a free edge. Claimed edges cannot be cut. Connector wins by joining A to F with claimed edges; Cutter wins when no possible A–F route remains.", "Every path must cross every separating cut. Protecting alternative routes can force the opponent to spend more than one move disconnecting the terminals."),
    KAYLES("Node Kayles", "Players alternate choosing an available vertex. It and all its neighbors disappear. The player who cannot move loses.", "Chosen vertices form an independent set. A large immediate removal is not always best: disconnected remaining components combine using Sprague–Grundy XOR."),
    PATH("Shortest-Path Challenge", "Saboteur (P1) spends up to five points increasing road costs, then locks the board. Router (P2) builds an A–F route. Router wins by matching the new optimal cost; otherwise Saboteur wins. This is a two-phase challenge variant.", "Dijkstra recomputes the benchmark after sabotage. Raising one road can redirect the optimal route, so a limited budget should target competing paths rather than just the original cheapest road."),
    BOXES("Dots and Boxes", "Claim a free grid edge. Every square you complete earns one point and another turn, including two squares at once. When all edges are taken, the higher score wins.", "Giving a square its third side lets the opponent finish it. Chains of nearly completed squares explain why late-game sacrifices can be stronger than taking the first available point.")
}

fun duelGraph(kind: DuelKind): LessonGraph = when (kind) {
    DuelKind.BOXES -> LessonGraph(9, buildList {
        for (y in 0..2) for (x in 0..2) {
            val u = y * 3 + x
            if (x < 2) add(GraphEdge(u, u + 1))
            if (y < 2) add(GraphEdge(u, u + 3))
        }
    })
    else -> LessonGraph(6, listOf(GraphEdge(0,1,2), GraphEdge(0,2,3), GraphEdge(1,2,1), GraphEdge(1,3,3), GraphEdge(2,4,2), GraphEdge(3,4,1), GraphEdge(3,5,2), GraphEdge(4,5,3)))
}

data class DuelState(
    val kind: DuelKind,
    val graph: LessonGraph = duelGraph(kind),
    val turn: Int = 0,
    val colors: Map<Int,Int> = emptyMap(),
    val removed: Set<Int> = emptySet(),
    val claimed: Map<Int,Int> = emptyMap(),
    val cut: Set<Int> = emptySet(),
    val boxes: Map<Int,Int> = emptyMap(),
    val budget: Int = 5,
    val routing: Boolean = false,
    val path: List<Int> = listOf(0),
    val result: String? = null
) {
    fun legalColor(node: Int, color: Int) = node in 0 until graph.size && color in 0..2 && node !in colors && graph.neighbors(node).none { colors[it.first] == color }
    fun color(node: Int, color: Int): DuelState {
        if (kind != DuelKind.COLORING || result != null || !legalColor(node, color)) return this
        val next = copy(colors = colors + (node to color), turn = 1-turn)
        return next.copy(result = when {
            next.colors.size == graph.size -> "Builder (P1) wins: every vertex is colored!"
            (0 until graph.size).none { u -> (0..2).any { next.legalColor(u,it) } } -> "Blocker (P2) wins: no legal coloring move remains."
            else -> null
        })
    }
    fun takeNode(node: Int): DuelState {
        if (kind != DuelKind.KAYLES || result != null || node !in 0 until graph.size || node in removed) return this
        val next = removed + node + graph.neighbors(node).map { it.first }
        return copy(removed = next, turn = 1-turn, result = if (next.size == graph.size) "Player ${turn+1} wins: no vertices remain." else null)
    }
    private fun connected(allowed: Set<Int>): Boolean {
        val seen = mutableSetOf(0)
        val queue = ArrayDeque<Int>(); queue.add(0)
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            graph.edges.forEachIndexed { index, edge ->
                if (index in allowed) {
                    val v = when(u) { edge.from -> edge.to; edge.to -> edge.from; else -> -1 }
                    if (v >= 0 && seen.add(v)) queue.add(v)
                }
            }
        }
        return graph.size-1 in seen
    }
    fun takeEdge(index: Int): DuelState {
        if (result != null || index !in graph.edges.indices || index in claimed || index in cut) return this
        if (kind == DuelKind.SHANNON) {
            val next = if (turn == 0) copy(claimed = claimed + (index to 0), turn = 1) else copy(cut = cut + index, turn = 0)
            return next.copy(result = when {
                next.connected(next.claimed.keys) -> "Connector (P1) wins: the claimed path joins A to F!"
                !next.connected(graph.edges.indices.toSet() - next.cut) -> "Cutter (P2) wins: A and F are disconnected."
                else -> null
            })
        }
        if (kind != DuelKind.BOXES) return this
        val edges = claimed + (index to turn)
        val newBoxes = boxes.toMutableMap()
        for (y in 0..1) for (x in 0..1) {
            val id = y*2+x; val a = y*3+x
            val sides = listOf(a to a+1, a to a+3, a+1 to a+4, a+3 to a+4)
            if (id !in boxes && sides.all { pair -> graph.edges.indexOfFirst { it.from == pair.first && it.to == pair.second } in edges }) newBoxes[id] = turn
        }
        val scores = (0..1).map { p -> newBoxes.values.count { it == p } }
        return copy(claimed = edges, boxes = newBoxes, turn = if (newBoxes.size > boxes.size) turn else 1-turn,
            result = if (edges.size != graph.edges.size) null else if (scores[0] == scores[1]) "Draw: ${scores[0]} boxes each." else "Player ${if (scores[0] > scores[1]) 1 else 2} wins ${scores[0]}–${scores[1]}." )
    }
    fun raiseCost(index: Int): DuelState {
        if (kind != DuelKind.PATH || routing || result != null || budget == 0 || index !in graph.edges.indices) return this
        return copy(budget = budget-1, graph = graph.copy(edges = graph.edges.mapIndexed { i,e -> if (i == index) e.copy(weight = e.weight+1) else e }))
    }
    fun lockRoads() = if (kind == DuelKind.PATH && !routing && result == null) copy(routing = true, turn = 1) else this
    fun travel(node: Int): DuelState {
        if (kind != DuelKind.PATH || !routing || result != null || node in path || graph.neighbors(path.last()).none { it.first == node }) return this
        return copy(path = path + node)
    }
    fun submitRoute(): DuelState {
        if (kind != DuelKind.PATH || !routing || result != null || path.last() != graph.size-1) return this
        val cost = routeCost(graph, path)!!
        val best = shortestDistances(graph, 0, GraphTopic.DIJKSTRA).last()
        return copy(result = if (cost == best) "Router (P2) wins! Cost $cost is optimal." else "Saboteur (P1) wins! Route cost $cost; optimal cost $best.")
    }
}

data class InkPoint(val x: Float, val y: Float) {
    fun distance(other: InkPoint): Float = sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y))
}
data class SproutDot(val point: InkPoint, val degree: Int = 0)
data class InkEdge(val points: List<InkPoint>)

private fun cross(a: InkPoint, b: InkPoint, c: InkPoint) = (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
private fun onSegment(p: InkPoint, a: InkPoint, b: InkPoint) = abs(cross(a,b,p)) < .000001f && p.x >= minOf(a.x,b.x)-.000001f && p.x <= maxOf(a.x,b.x)+.000001f && p.y >= minOf(a.y,b.y)-.000001f && p.y <= maxOf(a.y,b.y)+.000001f
private fun intersects(a: InkPoint,b: InkPoint,c: InkPoint,d: InkPoint): Boolean =
    (cross(a,b,c)*cross(a,b,d) < 0 && cross(c,d,a)*cross(c,d,b) < 0) || onSegment(c,a,b) || onSegment(d,a,b) || onSegment(a,c,d) || onSegment(b,c,d)

// Only an isolated common endpoint may touch. Collinear overlap is never legal.
private fun forbiddenIntersection(a: InkPoint,b: InkPoint,c: InkPoint,d: InkPoint, allowed: Set<InkPoint>): Boolean {
    if (!intersects(a,b,c,d)) return false
    val shared = listOf(a,b).filter { it == c || it == d }.toSet()
    if (shared.size != 1 || shared.first() !in allowed) return true
    val common = shared.first()
    val p = if (a == common) b else a
    val q = if (c == common) d else c
    return onSegment(p,common,q) || onSegment(q,common,p)
}

data class SproutsState(
    val dots: List<SproutDot> = listOf(SproutDot(InkPoint(.25f,.35f)), SproutDot(InkPoint(.75f,.35f)), SproutDot(InkPoint(.5f,.72f))),
    val edges: List<InkEdge> = emptyList(), val turn: Int = 0, val result: String? = null
) {
    fun error(curve: List<InkPoint>, newDot: Int): String? {
        if (result != null) return "Game finished. Restart to play again."
        if (curve.size < 3 || newDot !in 1 until curve.lastIndex) return "Add at least one bend; choose an interior bend for the new dot."
        if (curve.any { it.x !in .02f.. .98f || it.y !in .02f.. .98f }) return "Keep the curve inside the board."
        val start = dots.indexOfFirst { it.point == curve.first() }; val end = dots.indexOfFirst { it.point == curve.last() }
        if (start < 0 || end < 0) return "Start and end on existing dots. Tap close to snap to them."
        if (dots[start].degree + (if (start == end) 2 else 1) > 3 || dots[end].degree >= 3) return "That dot has too few lives remaining."
        val segments = curve.zipWithNext()
        if (segments.any { it.first.distance(it.second) < .008f }) return "Bends are too close together."
        for ((i, seg) in segments.withIndex()) {
            for (dot in dots) if (onSegment(dot.point, seg.first, seg.second)) {
                if (!(i == 0 && dot.point == curve.first()) && !(i == segments.lastIndex && dot.point == curve.last())) return "A curve cannot pass through another dot."
            }
            for (edge in edges) for ((a,b) in edge.points.zipWithNext()) {
                val allowed = buildSet { if (i == 0) add(curve.first()); if (i == segments.lastIndex) add(curve.last()) }
                if (forbiddenIntersection(seg.first,seg.second,a,b,allowed)) return "The curve touches or crosses an existing line."
            }
            for (j in 0 until i) {
                val allowed = buildSet {
                    if (j == i-1) add(seg.first)
                    if (j == 0 && i == segments.lastIndex && start == end) add(curve.first())
                }
                if (forbiddenIntersection(seg.first,seg.second,segments[j].first,segments[j].second,allowed)) return "The curve crosses or overlaps itself."
            }
        }
        if (dots.any { it.point.distance(curve[newDot]) < .035f }) return "Place the new dot farther from existing dots."
        return null
    }
    fun play(curve: List<InkPoint>, newDot: Int): SproutsState {
        if (error(curve,newDot) != null) return this
        val updated = dots.map { it.copy(degree = it.degree + (if (it.point == curve.first()) 1 else 0) + (if (it.point == curve.last()) 1 else 0)) } + SproutDot(curve[newDot],2)
        return copy(dots = updated, edges = edges + InkEdge(curve.take(newDot+1)) + InkEdge(curve.drop(newDot)), turn = 1-turn)
    }
}
