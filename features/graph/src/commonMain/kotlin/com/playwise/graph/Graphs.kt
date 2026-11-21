package com.playwise.graph

data class GraphEdge(val from: Int, val to: Int, val weight: Int = 1)

data class LessonGraph(val size: Int, val edges: List<GraphEdge>, val directed: Boolean = false) {
    init {
        require(size > 0)
        require(edges.all { it.from in 0 until size && it.to in 0 until size && it.from != it.to && it.weight in 0..1000 })
        require(edges.map { if (directed) it.from to it.to else minOf(it.from, it.to) to maxOf(it.from, it.to) }.distinct().size == edges.size)
    }
    private val adjacency = List(size) { mutableListOf<Pair<Int, Int>>() }.also { lists ->
        edges.forEach {
            lists[it.from].add(it.to to it.weight)
            if (!directed) lists[it.to].add(it.from to it.weight)
        }
    }
    fun neighbors(node: Int): List<Pair<Int, Int>> = adjacency[node]
}

/** Lessons, listed in the suggested learning order. Teaching content lives in [guide] (GraphLearning.kt). */
enum class GraphTopic(val title: String, val mission: String) {
    BFS("BFS: rescue relay", "Reach F from A using the fewest roads. Every road takes one turn."),
    ZERO_ONE("0–1 BFS: portal courier", "Reach F from A paying as few tolls as possible. Zero-cost roads are free portals."),
    DIJKSTRA("Dijkstra: delivery run", "Deliver from A to F with the lowest total road cost."),
    SCC("SCC: return-ticket islands", "Select a maximal group where every vertex can reach every other following the arrows. Find all groups."),
    BICONNECTED("Biconnected: resilient districts", "Select every vertex in one maximal resilient block, then claim it. Find all blocks.")
}

fun shortestDistances(graph: LessonGraph, start: Int, topic: GraphTopic): List<Int> {
    require(start in 0 until graph.size)
    require(topic in listOf(GraphTopic.DIJKSTRA, GraphTopic.BFS, GraphTopic.ZERO_ONE))
    if (topic == GraphTopic.ZERO_ONE) require(graph.edges.all { it.weight in 0..1 })
    val distance = MutableList(graph.size) { Int.MAX_VALUE }
    distance[start] = 0
    if (topic == GraphTopic.DIJKSTRA) {
        val used = mutableSetOf<Int>()
        repeat(graph.size) {
            val u = (0 until graph.size).filter { it !in used }.minByOrNull { distance[it] } ?: return distance
            if (distance[u] == Int.MAX_VALUE) return distance
            used.add(u)
            for ((v, w) in graph.neighbors(u)) distance[v] = minOf(distance[v], distance[u] + w)
        }
    } else {
        val queue = ArrayDeque<Int>()
        queue.addLast(start)
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            for ((v, weight) in graph.neighbors(u)) {
                val w = if (topic == GraphTopic.BFS) 1 else weight
                if (distance[u] + w < distance[v]) {
                    distance[v] = distance[u] + w
                    if (w == 0) queue.addFirst(v) else queue.addLast(v)
                }
            }
        }
    }
    return distance
}

fun stronglyConnectedComponents(graph: LessonGraph): Set<Set<Int>> {
    require(graph.directed)
    val seen = mutableSetOf<Int>()
    val order = mutableListOf<Int>()
    fun visit(u: Int) {
        if (!seen.add(u)) return
        graph.neighbors(u).forEach { visit(it.first) }
        order.add(u)
    }
    (0 until graph.size).forEach { visit(it) }
    val reverse = LessonGraph(graph.size, graph.edges.map { GraphEdge(it.to, it.from, it.weight) }, true)
    seen.clear()
    fun collect(u: Int, group: MutableSet<Int>) {
        if (!seen.add(u)) return
        group.add(u)
        reverse.neighbors(u).forEach { collect(it.first, group) }
    }
    return buildSet {
        order.asReversed().forEach { u ->
            if (u !in seen) add(mutableSetOf<Int>().also { collect(u, it) }.toSet())
        }
    }
}

fun biconnectedComponents(graph: LessonGraph): Set<Set<Int>> {
    require(!graph.directed)
    val discovery = IntArray(graph.size) { -1 }
    val low = IntArray(graph.size)
    var time = 0
    val stack = mutableListOf<Pair<Int, Int>>()
    val blocks = mutableSetOf<Set<Int>>()
    fun visit(u: Int, parent: Int) {
        discovery[u] = time++; low[u] = discovery[u]
        for ((v, _) in graph.neighbors(u)) {
            if (discovery[v] == -1) {
                stack.add(u to v); visit(v, u)
                low[u] = minOf(low[u], low[v])
                if (low[v] >= discovery[u]) {
                    val block = mutableSetOf<Int>()
                    do {
                        val edge = stack.removeAt(stack.lastIndex)
                        block.add(edge.first); block.add(edge.second)
                    } while (edge != u to v)
                    blocks.add(block.toSet())
                }
            } else if (v != parent && discovery[v] < discovery[u]) {
                stack.add(u to v); low[u] = minOf(low[u], discovery[v])
            }
        }
    }
    for (u in 0 until graph.size) if (discovery[u] == -1) {
        if (graph.neighbors(u).isEmpty()) blocks.add(setOf(u))
        visit(u, -1)
    }
    return blocks
}

fun graphLevel(topic: GraphTopic, level: Int): LessonGraph {
    val variant = level.mod(3)
    fun edges(vararg pairs: Pair<Int, Int>) = pairs.map { GraphEdge(it.first, it.second) }
    return when (topic) {
        GraphTopic.BICONNECTED -> LessonGraph(6, when (variant) {
            0 -> edges(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 2, 4 to 5)
            1 -> edges(0 to 1, 1 to 2, 2 to 3, 3 to 0, 1 to 4, 4 to 5)
            else -> edges(0 to 1, 1 to 2, 2 to 0, 3 to 4)
        })
        GraphTopic.SCC -> LessonGraph(6, when (variant) {
            0 -> edges(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 5, 5 to 3)
            1 -> edges(0 to 1, 1 to 0, 1 to 2, 2 to 3, 3 to 2, 3 to 4, 4 to 5)
            else -> edges(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 5, 5 to 2)
        }, true)
        else -> {
            val pairs = when (variant) {
                0 -> listOf(0 to 1, 0 to 2, 1 to 3, 2 to 3, 2 to 4, 3 to 5, 4 to 5)
                1 -> listOf(0 to 1, 1 to 5, 0 to 2, 2 to 3, 3 to 4, 4 to 5, 1 to 3)
                else -> listOf(0 to 5, 0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 1 to 4)
            }
            val weights = when (topic) {
                GraphTopic.BFS -> List(7) { 1 }
                GraphTopic.ZERO_ONE -> listOf(1, 1, 0, 0, 0, 1, 0)
                else -> listOf(8, 2, 2, 1, 3, 8, 1)
            }
            LessonGraph(6, pairs.mapIndexed { i, pair -> GraphEdge(pair.first, pair.second, weights[i]) })
        }
    }
}

fun routeCost(graph: LessonGraph, route: List<Int>): Int? {
    if (route.isEmpty() || route.any { it !in 0 until graph.size }) return null
    var cost = 0
    for ((u, v) in route.zipWithNext()) cost += graph.neighbors(u).firstOrNull { it.first == v }?.second ?: return null
    return cost
}
