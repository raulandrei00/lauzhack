package com.playwise.graph

import androidx.compose.ui.geometry.Offset

// =====================================================================
// Playing the existing games on a hand-made graph
// =====================================================================

/** Any graph game or lesson that can run on a user's own graph. */
sealed interface GameChoice {
    val title: String
    /** Whether the game walks or connects from a START vertex to a GOAL vertex. */
    val usesEnds: Boolean

    data class Lesson(val topic: GraphTopic) : GameChoice {
        override val title get() = topic.title
        override val usesEnds get() = topic in listOf(GraphTopic.BFS, GraphTopic.ZERO_ONE, GraphTopic.DIJKSTRA)
    }

    data class Duel(val kind: DuelKind) : GameChoice {
        override val title get() = kind.title
        override val usesEnds get() = kind == DuelKind.SHANNON || kind == DuelKind.PATH
    }
}

val allGameChoices: List<GameChoice> =
    GraphTopic.entries.map { GameChoice.Lesson(it) } + DuelKind.entries.map { GameChoice.Duel(it) }

/**
 * A board ready for a game: START is vertex 0 and GOAL is the last vertex, which is what every
 * game assumes. [positions] are normalized board coordinates.
 */
data class PlayableBoard(val graph: LessonGraph, val positions: List<Offset>, val colors: Map<Int, Int> = emptyMap())

/**
 * Renumbers vertices so [start] becomes A (index 0) and [goal] becomes the last letter; the
 * others keep their order. Returns the new graph and, for each new index, the old index.
 */
fun DrawnGraph.withEnds(start: Int, goal: Int): Pair<DrawnGraph, List<Int>> {
    require(start in 0 until size && goal in 0 until size && start != goal)
    val order = listOf(start) + (0 until size).filter { it != start && it != goal } + goal
    val newIndex = IntArray(size).also { arr -> order.forEachIndexed { n, old -> arr[old] = n } }
    return DrawnGraph(order.map { vertices[it] }, edges.map { it.copy(a = newIndex[it.a], b = newIndex[it.b]) }) to order
}

/**
 * Converts to the games' graph type. With [directed], two-way lines become two arrows and
 * one-way lines one arrow; otherwise arrows are ignored.
 */
fun DrawnGraph.toLessonGraph(directed: Boolean): LessonGraph {
    val graphEdges = if (directed) edges.flatMap { e ->
        if (e.directed) listOf(GraphEdge(e.a, e.b, e.weight)) else listOf(GraphEdge(e.a, e.b, e.weight), GraphEdge(e.b, e.a, e.weight))
    }.distinctBy { it.from to it.to }
    else edges.map { GraphEdge(it.a, it.b, it.weight) }.distinctBy { minOf(it.from, it.to) to maxOf(it.from, it.to) }
    return LessonGraph(size.coerceAtLeast(1), graphEdges, directed)
}

/** Does this game keep the arrows? Games about undirected ideas treat every line as two-way. */
fun GameChoice.keepsDirections(): Boolean = when (this) {
    is GameChoice.Lesson -> topic != GraphTopic.BICONNECTED
    is GameChoice.Duel -> kind == DuelKind.PATH
}

/**
 * Why this graph cannot be used for [choice], in words a child can act on; null if it can.
 * [start] and [goal] are only checked for games that use them.
 */
fun gameProblem(g: DrawnGraph, start: Int, goal: Int, choice: GameChoice): String? {
    if (g.size == 0) return "Add some vertices first."
    if (choice is GameChoice.Duel && choice.kind == DuelKind.BOXES)
        return "Dots and Boxes is played on a square grid of dots, so it can't use a hand-made graph."
    if (g.size < 2 && !(choice is GameChoice.Duel && (choice.kind == DuelKind.KAYLES || choice.kind == DuelKind.COLORING)))
        return "Needs at least 2 vertices."
    if (choice.usesEnds) {
        if (start == goal) return "START and GOAL must be different vertices."
        val reach = cheapestRoute(if (choice.keepsDirections()) g else g.allTwoWay(), start, goal)
        if (reach == null) return "There's no route from ${nodeLabel(start)} to ${nodeLabel(goal)}" +
            (if (g.anyDirected && choice.keepsDirections()) ". Check the one-way arrows." else ". Connect them first.")
    }
    return when (choice) {
        is GameChoice.Lesson -> when (choice.topic) {
            GraphTopic.ZERO_ONE -> g.edges.filter { it.weight > 1 }.takeIf { it.isNotEmpty() }?.let { bad ->
                "Every line needs weight 0 or 1. Change ${bad.take(3).joinToString { "${nodeLabel(it.a)}–${nodeLabel(it.b)}" }}${if (bad.size > 3) " and others" else ""}."
            }
            GraphTopic.SCC -> if (!g.anyDirected) "This lesson is about one-way streets. Make some lines one-way first." else null
            GraphTopic.BICONNECTED -> if (g.edges.isEmpty()) "Draw some lines first." else null
            else -> null
        }
        is GameChoice.Duel -> when (choice.kind) {
            DuelKind.SHANNON -> when {
                g.edges.isEmpty() -> "Draw some lines first."
                g.edgeIndex(start, goal) >= 0 -> "START and GOAL are joined directly, so Connector would win on the first move. Remove the ${nodeLabel(start)}–${nodeLabel(goal)} line."
                else -> null
            }
            DuelKind.PLANAR -> when {
                g.size > 10 -> "Planar Color & Connect works with up to 10 vertices."
                !isPlanarGraph(g.toLessonGraph(false)) -> "This graph can't be drawn without crossing lines, so it can't start a planar game."
                else -> null
            }
            DuelKind.PATH -> if (g.edges.isEmpty()) "Draw some lines first." else null
            else -> null
        }
    }
}

/** A short note about how the game will read the graph, or null. */
fun gameNote(g: DrawnGraph, choice: GameChoice): String? = when {
    g.anyDirected && !choice.keepsDirections() -> "Arrows are treated as two-way roads in this game."
    choice is GameChoice.Lesson && choice.topic == GraphTopic.BFS && g.edges.any { it.weight != 1 } -> "BFS counts roads, so every weight counts as 1."
    else -> null
}

/** Builds the board. Call only when [gameProblem] is null. */
fun playableBoard(g: DrawnGraph, start: Int, goal: Int, choice: GameChoice): PlayableBoard {
    val ordered = if (choice.usesEnds && start != goal) g.withEnds(start, goal).first else g
    val graph = ordered.toLessonGraph(choice.keepsDirections() && ordered.anyDirected || (choice is GameChoice.Lesson && choice.topic == GraphTopic.SCC))
    return PlayableBoard(graph, ordered.vertices.map { Offset(it.x, it.y) })
}

// =====================================================================
// Coloring
// =====================================================================

data class ColoringResult(val colors: List<Int>, val count: Int, val exact: Boolean)

/**
 * Colors the vertices so joined vertices differ, with as few colors as possible.
 * Small graphs get an exact backtracking search (the true chromatic number); bigger ones use the
 * DSatur greedy rule. Returns null if it would need more than [maxColors] paints.
 */
fun bestColoring(g: DrawnGraph, maxColors: Int = 6, exactLimit: Int = 14): ColoringResult? {
    val n = g.size
    if (n == 0) return ColoringResult(emptyList(), 0, true)
    val adj = List(n) { u -> g.undirectedNeighbors(u).toSet() }
    if (n <= exactLimit) {
        val order = (0 until n).sortedByDescending { adj[it].size }
        for (k in 1..maxColors) {
            val color = IntArray(n) { -1 }
            fun place(i: Int): Boolean {
                if (i == n) return true
                val u = order[i]
                // Symmetry break: never open a color beyond the next unused one.
                val limit = minOf(k, (order.take(i).maxOfOrNull { color[it] } ?: -1) + 2)
                for (c in 0 until limit) if (adj[u].none { color[it] == c }) {
                    color[u] = c
                    if (place(i + 1)) return true
                    color[u] = -1
                }
                return false
            }
            if (place(0)) return ColoringResult(color.toList(), k, true)
        }
        return null
    }
    // DSatur: repeatedly color the vertex whose neighbors already use the most different colors.
    val color = IntArray(n) { -1 }
    repeat(n) {
        val u = (0 until n).filter { color[it] == -1 }.maxWith(compareBy({ v -> adj[v].map { color[it] }.filter { it >= 0 }.toSet().size }, { adj[it].size }))
        color[u] = (0 until n).first { c -> adj[u].none { color[it] == c } }
    }
    val count = color.max() + 1
    return if (count > maxColors) null else ColoringResult(color.toList(), count, false)
}

// =====================================================================
// Text import / export
// =====================================================================

/**
 * Text form of a graph, e.g. "A-B 3, B>C, D": "-" two-way, ">" one-way, an optional weight
 * after a space, and lone letters for vertices without lines.
 */
fun graphToText(g: DrawnGraph): String {
    val used = g.edges.flatMap { listOf(it.a, it.b) }.toSet()
    val lines = g.edges.map { e -> "${nodeLabel(e.a)}${if (e.directed) ">" else "-"}${nodeLabel(e.b)}${if (e.weight != 1) " ${e.weight}" else ""}" }
    val lonely = (0 until g.size).filter { it !in used }.map { nodeLabel(it) }
    return (lines + lonely).joinToString(", ")
}

sealed interface ParseResult {
    data class Ok(val graph: DrawnGraph) : ParseResult
    data class Error(val message: String) : ParseResult
}

private val tokenPattern = Regex("""^([A-Z])\s*(?:(->|<-|-|>|<)\s*([A-Z]))?\s*(?:[:=]?\s*(\d+))?$""")

/** Parses the text form (case-insensitive; commas, semicolons or new lines between items). */
fun parseGraphText(text: String): ParseResult {
    val tokens = text.split(',', ';', '\n').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return ParseResult.Error("Type at least one vertex or line, like A-B.")
    var size = 0
    val edges = linkedMapOf<Pair<Int, Int>, DrawnEdge>()
    for (t in tokens) {
        val m = tokenPattern.matchEntire(t)
            ?: return ParseResult.Error("I don't understand \"$t\". Write lines like A-B, A>B (one-way) or A-B 5 (weight 5).")
        val a = m.groupValues[1][0] - 'A'
        size = maxOf(size, a + 1)
        val op = m.groupValues[2]
        if (op.isEmpty()) {
            if (m.groupValues[4].isNotEmpty()) return ParseResult.Error("\"$t\": a weight needs a line, like A-B ${m.groupValues[4]}.")
            continue
        }
        val b = m.groupValues[3][0] - 'A'
        if (a == b) return ParseResult.Error("\"$t\": a line must join two different letters.")
        size = maxOf(size, b + 1)
        val weight = m.groupValues[4].ifEmpty { "1" }.toInt()
        if (weight > DrawnGraph.MAX_WEIGHT) return ParseResult.Error("\"$t\": weights go up to ${DrawnGraph.MAX_WEIGHT}.")
        val edge = when (op) {
            ">", "->" -> DrawnEdge(a, b, weight, true)
            "<", "<-" -> DrawnEdge(b, a, weight, true)
            else -> DrawnEdge(a, b, weight, false)
        }
        edges[minOf(a, b) to maxOf(a, b)] = edge
    }
    val g = DrawnGraph(List(size) { DrawnVertex(0f, 0f) }, edges.values.toList())
    return ParseResult.Ok(tidyArrangement(circleArrangement(g)))
}
