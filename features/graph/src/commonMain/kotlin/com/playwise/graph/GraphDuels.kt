package com.playwise.graph


/** Teaching content for each duel lives in [guide] (GraphLearning.kt). */
enum class DuelKind(val title: String, val rules: String) {
    PLANAR("Planar Color & Connect", "Start with isolated vertices. On your turn, permanently color one vertex or add one edge. Adjacent colored vertices must differ; added edges must preserve planarity. No legal move loses."),
    COLORING("Graph Coloring Duel", "Builder (P1) and Blocker (P2) alternate coloring one vertex with a selected paint. Neighbors must differ. Builder wins if all vertices are colored; Blocker wins if no legal coloring move remains."),
    SHANNON("Shannon Switching", "Connector (P1) claims a free edge; Cutter (P2) deletes a free edge. Claimed edges cannot be cut. Connector wins by joining START to GOAL with claimed edges; Cutter wins when no possible route remains."),
    KAYLES("Node Kayles", "Players alternate choosing an available vertex. It and all its neighbors disappear. The player who cannot move loses."),
    PATH("Shortest-Path Challenge", "Saboteur (P1) spends the chosen budget increasing road costs, then locks the board. Router (P2) builds a route from START to GOAL. Router wins by matching the new optimal cost; otherwise Saboteur wins. This is a two-phase challenge variant."),
    BOXES("Dots and Boxes", "Claim a free grid edge. Every square you complete earns one point and another turn, including two squares at once. When all edges are taken, the higher score wins.")
}

fun duelGraph(kind: DuelKind): LessonGraph = when (kind) {
    DuelKind.PLANAR -> LessonGraph(6, emptyList())
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
    val paletteSize: Int = 3,
    val routing: Boolean = false,
    val path: List<Int> = listOf(0),
    val result: String? = null,
    val gridColumns: Int = 3
) {
    fun legalColor(node: Int, color: Int) = node in 0 until graph.size && color in 0 until paletteSize && node !in colors && graph.neighbors(node).none { colors[it.first] == color }
    fun color(node: Int, color: Int): DuelState {
        if (kind != DuelKind.COLORING || result != null || !legalColor(node, color)) return this
        val next = copy(colors = colors + (node to color), turn = 1-turn)
        return next.copy(result = when {
            next.colors.size == graph.size -> "Builder (P1) wins: every vertex is colored!"
            (0 until graph.size).none { u -> (0 until paletteSize).any { next.legalColor(u,it) } } -> "Blocker (P2) wins: no legal coloring move remains."
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
        for (y in 0 until graph.size/gridColumns-1) for (x in 0 until gridColumns-1) {
            val id = y*(gridColumns-1)+x; val a = y*gridColumns+x
            val sides = listOf(a to a+1, a to a+gridColumns, a+1 to a+gridColumns+1, a+gridColumns to a+gridColumns+1)
            if (id !in boxes && sides.all { pair -> graph.edges.indexOfFirst { it.from == pair.first && it.to == pair.second } in edges }) newBoxes[id] = turn
        }
        val scores = (0..1).map { p -> newBoxes.values.count { it == p } }
        return copy(claimed = edges, boxes = newBoxes, turn = if (newBoxes.size > boxes.size) turn else 1-turn,
            result = if (edges.size != graph.edges.size) null else if (scores[0] == scores[1]) "Draw: ${scores[0]} boxes each." else "Player ${if (scores[0] > scores[1]) 1 else 2} wins ${scores[0]}–${scores[1]}." )
    }
    fun raiseCost(index: Int): DuelState {
        if (kind != DuelKind.PATH || routing || result != null || budget == 0 || index !in graph.edges.indices) return this
        // In a directed board a two-way road is two arrows; raising the road raises both.
        val road = graph.edges[index]
        fun sameRoad(e: GraphEdge) = e == road || (graph.directed && e.from == road.to && e.to == road.from && e.weight == road.weight)
        return copy(budget = budget-1, graph = graph.copy(edges = graph.edges.map { e -> if (sameRoad(e)) e.copy(weight = e.weight+1) else e }))
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
