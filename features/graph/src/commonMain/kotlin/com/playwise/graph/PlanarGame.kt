package com.playwise.graph

/** Exact minor test for simple undirected graphs of at most six vertices.
 * Wagner: nonplanar iff there is a K5 or K3,3 minor. With <=6 vertices,
 * K3,3 must be a subgraph; K5 is a subgraph or requires one contraction.
 */
fun isSmallPlanar(graph: LessonGraph): Boolean {
    require(!graph.directed && graph.size <= 6)
    val n = graph.size
    val adjacent = Array(n) { BooleanArray(n) }
    graph.edges.forEach { adjacent[it.from][it.to] = true; adjacent[it.to][it.from] = true }
    fun clique(groups: List<List<Int>>) = groups.indices.all { i ->
        (i+1 until groups.size).all { j -> groups[i].any { a -> groups[j].any { b -> adjacent[a][b] } } }
    }
    if (n < 5) return true
    if (n == 5) return !clique((0 until n).map { listOf(it) })
    // Delete one vertex: K5 subgraph.
    for (omit in 0 until n) if (clique((0 until n).filter { it != omit }.map { listOf(it) })) return false
    // Contract one edge: the only possible nonsingleton K5 branch set.
    for (edge in graph.edges) {
        val groups = listOf(listOf(edge.from,edge.to)) + (0 until n).filter { it != edge.from && it != edge.to }.map { listOf(it) }
        if (clique(groups)) return false
    }
    for (mask in 0 until (1 shl n)) if (mask.countOneBits() == 3) {
        val left = (0 until n).filter { mask and (1 shl it) != 0 }
        val right = (0 until n).filter { it !in left }
        if (left.all { a -> right.all { b -> adjacent[a][b] } }) return false
    }
    return true
}

enum class PlanarityObstruction { EDGE_LIMIT, K5, K33 }

/** Exact Kuratowski subdivision test for the game's graphs of at most ten vertices. */
fun planarityObstruction(graph: LessonGraph): PlanarityObstruction? {
    require(!graph.directed && graph.size in 2..10)
    if (graph.size <= 6 && isSmallPlanar(graph)) return null
    val n = graph.size
    val m = graph.edges.size
    if (n == 5 && m == 10) return PlanarityObstruction.K5
    if (m > 3 * n - 6) return PlanarityObstruction.EDGE_LIMIT
    val neighbors = IntArray(n)
    graph.edges.forEach { edge ->
        neighbors[edge.from] = neighbors[edge.from] or (1 shl edge.to)
        neighbors[edge.to] = neighbors[edge.to] or (1 shl edge.from)
    }

    fun hasSubdivision(branches: List<Int>, requiredPairs: List<Pair<Int, Int>>): Boolean {
        val branchMask = branches.fold(0) { mask, v -> mask or (1 shl v) }
        val spare = ((1 shl n) - 1) and branchMask.inv()
        val missing = requiredPairs.filter { (a, b) -> neighbors[a] and (1 shl b) == 0 }
        if (missing.size > spare.countOneBits()) return false
        if (missing.isEmpty()) return true
        val routes = missing.map { (start, goal) ->
            val found = mutableSetOf<Int>()
            fun walk(current: Int, used: Int) {
                var next = neighbors[current] and spare and used.inv()
                if (neighbors[current] and (1 shl goal) != 0) found.add(used)
                while (next != 0) {
                    val bit = next and -next
                    next -= bit
                    walk(bit.countTrailingZeroBits(), used or bit)
                }
            }
            walk(start, 0)
            found.toList().sortedBy { it.countOneBits() }
        }.sortedBy { it.size }
        if (routes.any { it.isEmpty() }) return false
        fun assign(index: Int, used: Int): Boolean {
            if (index == routes.size) return true
            return routes[index].any { mask -> mask and used == 0 && assign(index + 1, used or mask) }
        }
        return assign(0, 0)
    }

    fun choose(size: Int, visit: (List<Int>) -> Boolean): Boolean {
        fun recurse(start: Int, chosen: List<Int>): Boolean {
            if (chosen.size == size) return visit(chosen)
            for (v in start..n - (size - chosen.size)) {
                if (recurse(v + 1, chosen + v)) return true
            }
            return false
        }
        return recurse(0, emptyList())
    }
    if (choose(5) { branch ->
            branch.all { neighbors[it].countOneBits() >= 4 } &&
                hasSubdivision(branch, branch.indices.flatMap { i ->
                    (i + 1 until 5).map { j -> branch[i] to branch[j] }
                })
        }) return PlanarityObstruction.K5
    if (choose(6) { branch ->
            if (branch.any { neighbors[it].countOneBits() < 3 }) return@choose false
            for (left1 in 1..4) for (left2 in left1 + 1..5) {
                val left = listOf(branch[0], branch[left1], branch[left2])
                val right = branch.filter { it !in left }
                if (hasSubdivision(branch, left.flatMap { a -> right.map { b -> a to b } })) return@choose true
            }
            false
        }) return PlanarityObstruction.K33
    return null
}

fun isPlanarGraph(graph: LessonGraph): Boolean = planarityObstruction(graph) == null

enum class EdgeRejection { GAME_OVER, INVALID_NODE, LOOP, ALREADY_CONNECTED, SAME_COLOR, EDGE_LIMIT, K5, K33 }

data class PlanarGame(
    val graph: LessonGraph = LessonGraph(6, emptyList()),
    val paletteSize: Int = 3,
    val colors: Map<Int,Int> = emptyMap(),
    val turn: Int = 0,
    val winner: Int? = null
) {
    init {
        require(graph.size in 2..10 && !graph.directed && paletteSize in 1..6)
        require(colors.all { (node,color) -> node in 0 until graph.size && color in 0 until paletteSize })
        require(graph.edges.none { colors[it.from] != null && colors[it.from] == colors[it.to] })
        require(isPlanarGraph(graph))
    }
    fun canColor(node: Int, color: Int) = winner == null && node in 0 until graph.size && color in 0 until paletteSize && node !in colors && graph.neighbors(node).none { colors[it.first] == color }
    fun edgeRejection(a: Int, b: Int): EdgeRejection? {
        if (winner != null) return EdgeRejection.GAME_OVER
        if (a !in 0 until graph.size || b !in 0 until graph.size) return EdgeRejection.INVALID_NODE
        if (a == b) return EdgeRejection.LOOP
        if (graph.neighbors(a).any { it.first == b }) return EdgeRejection.ALREADY_CONNECTED
        if (colors[a] != null && colors[a] == colors[b]) return EdgeRejection.SAME_COLOR
        return when (planarityObstruction(graph.copy(edges = graph.edges + GraphEdge(a, b)))) {
            PlanarityObstruction.EDGE_LIMIT -> EdgeRejection.EDGE_LIMIT
            PlanarityObstruction.K5 -> EdgeRejection.K5
            PlanarityObstruction.K33 -> EdgeRejection.K33
            null -> null
        }
    }
    fun canAdd(a: Int, b: Int): Boolean = edgeRejection(a, b) == null
    fun hasMove(): Boolean = (0 until graph.size).any { u ->
        (0 until paletteSize).any { canColor(u,it) } || (u+1 until graph.size).any { canAdd(u,it) }
    }
    private fun finishMove(): PlanarGame {
        val next = copy(turn = 1-turn)
        return if (next.hasMove()) next else next.copy(winner = turn)
    }
    fun color(node: Int, color: Int): PlanarGame = if (!canColor(node,color)) this else copy(colors = colors+(node to color)).finishMove()
    fun add(a: Int,b: Int): PlanarGame = if (!canAdd(a,b)) this else copy(graph = graph.copy(edges = graph.edges+GraphEdge(a,b))).finishMove()
}
