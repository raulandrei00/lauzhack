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

data class PlanarGame(
    val graph: LessonGraph = LessonGraph(6, emptyList()),
    val paletteSize: Int = 3,
    val colors: Map<Int,Int> = emptyMap(),
    val turn: Int = 0,
    val winner: Int? = null
) {
    init {
        require(graph.size in 2..6 && !graph.directed && paletteSize in 1..4)
        require(colors.all { (node,color) -> node in 0 until graph.size && color in 0 until paletteSize })
        require(graph.edges.none { colors[it.from] != null && colors[it.from] == colors[it.to] })
        require(isSmallPlanar(graph))
    }
    fun canColor(node: Int, color: Int) = winner == null && node in 0 until graph.size && color in 0 until paletteSize && node !in colors && graph.neighbors(node).none { colors[it.first] == color }
    fun canAdd(a: Int, b: Int): Boolean {
        if (winner != null || a !in 0 until graph.size || b !in 0 until graph.size || a == b) return false
        if (graph.neighbors(a).any { it.first == b } || colors[a] != null && colors[a] == colors[b]) return false
        return isSmallPlanar(graph.copy(edges = graph.edges+GraphEdge(a,b)))
    }
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
