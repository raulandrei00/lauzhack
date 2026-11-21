package com.playwise.app

import kotlin.random.Random

/** Seedable generators: UI uses Random.Default; tests supply a seed. */
fun randomNetwork(random: Random, weight: () -> Int = { 1 }, colorable: Boolean = false, cycle: Boolean = false): LessonGraph {
    val middle = (1..4).shuffled(random)
    val order = if (cycle) listOf(0)+middle.take(2)+listOf(5)+middle.drop(2) else (0..5).shuffled(random)
    val edges = mutableSetOf<Pair<Int,Int>>()
    fun add(a: Int, b: Int) { edges.add(minOf(a,b) to maxOf(a,b)) }
    // A random spanning path ensures reachability and is compatible with the hidden coloring.
    order.zipWithNext().forEach { (a,b) -> add(a,b) }
    if (cycle) add(order.last(),order.first())
    val colors = order.withIndex().associate { it.value to it.index % 3 }
    for (a in 0..5) for (b in a+1..5) {
        if ((!colorable || colors[a] != colors[b]) && random.nextDouble() < .28) add(a,b)
    }
    // Avoid an immediate one-edge Shannon win.
    if (cycle) {
        edges.remove(0 to 5)
        // Terminals are opposite on the ring, retaining two independent routes.
    }
    return LessonGraph(6, edges.map { (a,b) -> GraphEdge(a,b,weight()) })
}

fun randomDuel(kind: DuelKind, random: Random = Random.Default): DuelState {
    if (kind == DuelKind.BOXES) {
        val columns = random.nextInt(3,6)
        val rows = random.nextInt(3,6)
        val graph = LessonGraph(columns*rows, buildList {
            for (y in 0 until rows) for (x in 0 until columns) {
                val u = y*columns+x
                if (x < columns-1) add(GraphEdge(u,u+1))
                if (y < rows-1) add(GraphEdge(u,u+columns))
            }
        })
        return DuelState(kind,graph,gridColumns = columns)
    }
    return DuelState(kind, randomNetwork(random,
        weight = { if (kind == DuelKind.PATH) random.nextInt(1,10) else 1 },
        colorable = kind == DuelKind.COLORING, cycle = kind == DuelKind.SHANNON))
}

fun randomSprouts(random: Random = Random.Default): SproutsState {
    // Jittered cells guarantee useful spacing without an unbounded rejection loop.
    val cells = (0..8).shuffled(random).take(random.nextInt(3,7))
    return SproutsState(dots = cells.map {
        SproutDot(InkPoint(.18f+(it%3)*.32f+random.nextFloat()*.07f,
            .16f+(it/3)*.32f+random.nextFloat()*.07f))
    })
}

fun randomLesson(topic: GraphTopic, random: Random = Random.Default): LessonGraph = when (topic) {
    GraphTopic.SCC -> {
        // Random strong groups connected only forward, so they remain distinct SCCs.
        val order = (0..5).shuffled(random)
        val split = random.nextInt(1,6)
        val groups = listOf(order.take(split),order.drop(split))
        val edges = mutableSetOf<Pair<Int,Int>>()
        groups.forEach { group ->
            if (group.size > 1) group.indices.forEach { i -> edges.add(group[i] to group[(i+1)%group.size]) }
            for (a in group) for (b in group) if (a != b && random.nextBoolean()) edges.add(a to b)
        }
        edges.add(groups[0].random(random) to groups[1].random(random))
        LessonGraph(6,edges.map { (a,b) -> GraphEdge(a,b) },true)
    }
    GraphTopic.BICONNECTED -> {
        val order = (0..5).shuffled(random)
        val split = random.nextInt(2,5)
        val edges = mutableSetOf<Pair<Int,Int>>()
        fun block(nodes: List<Int>) {
            for (i in nodes.indices) {
                val a = nodes[i]; val b = nodes[(i+1)%nodes.size]
                if (a != b) edges.add(minOf(a,b) to maxOf(a,b))
            }
        }
        block(order.take(split))
        block(listOf(order[split-1])+order.drop(split))
        LessonGraph(6,edges.map { (a,b) -> GraphEdge(a,b) })
    }
    else -> randomNetwork(random,weight = { when(topic) {
        GraphTopic.BFS -> 1
        GraphTopic.ZERO_ONE -> random.nextInt(2)
        else -> random.nextInt(1,10)
    } })
}
