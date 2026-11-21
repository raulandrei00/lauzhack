package com.playwise.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun nodeName(node: Int) = ('A'.code + node).toChar().toString()
private fun groupName(group: Set<Int>) = group.sorted().joinToString(" · ") { nodeName(it) }

@Composable
fun GraphGamesScreen(onBack: () -> Unit) {
    var topic by remember { mutableStateOf<GraphTopic?>(null) }
    var duel by remember { mutableStateOf<DuelKind?>(null) }
    var completed by remember { mutableStateOf(setOf<GraphTopic>()) }
    OutlinedButton(onClick = { if (topic == null && duel == null) onBack() else { topic = null; duel = null } }) {
        Text(if (topic == null && duel == null) "Back to learning paths" else "All graph games")
    }
    Text("Graph adventures", fontSize = 32.sp)
    Text("${completed.size} / 5 topics explored this session", color = Color(0xFFADB8D0))
    Spacer(Modifier.height(16.dp))
    val active = topic
    if (duel != null) {
        key(duel) { GraphDuelScreen(duel!!) }
    } else if (active == null) {
        Text("Two-player games", fontSize = 24.sp)
        DuelKind.entries.forEach { game ->
            Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(game.title, fontSize = 22.sp)
                    Text(game.rules)
                    Button(onClick = { duel = game }) { Text("Play together") }
                }
            }
        }
        Text("Algorithm lessons", fontSize = 24.sp)
        GraphTopic.entries.forEach { lesson ->
            Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(lesson.title, fontSize = 22.sp)
                    Text(lesson.mission)
                    Button(onClick = { topic = lesson }) { Text(if (lesson in completed) "Play again" else "Start challenge") }
                }
            }
        }
    } else key(active) {
        GraphLesson(active, onComplete = { completed = completed + active })
    }
}

@Composable
private fun GraphLesson(topic: GraphTopic, onComplete: () -> Unit) {
    var level by remember { mutableIntStateOf(0) }
    key(level) {
        val graph = remember { randomLesson(topic) }
        val isRoute = topic in listOf(GraphTopic.BFS, GraphTopic.DIJKSTRA, GraphTopic.ZERO_ONE)
        val answers = remember { when (topic) {
            GraphTopic.SCC -> stronglyConnectedComponents(graph)
            GraphTopic.BICONNECTED -> biconnectedComponents(graph)
            else -> emptySet()
        } }
        var path by remember { mutableStateOf(listOf(0)) }
        var selection by remember { mutableStateOf(setOf<Int>()) }
        var claimed by remember { mutableStateOf(setOf<Set<Int>>()) }
        var message by remember { mutableStateOf("Explore the board, then submit your answer.") }
        var won by remember { mutableStateOf(false) }
        var attempts by remember { mutableIntStateOf(0) }
        var theory by remember { mutableStateOf(false) }
        val optimum = remember { if (isRoute) shortestDistances(graph, 0, topic).last() else 0 }
        Text(topic.title, fontSize = 25.sp)
        Text("Random board ${level + 1} • ${topic.mission}")
        OutlinedButton(onClick = { level++ }) { Text("New random board") }
        Text(if (graph.directed) "Arrows are one-way. Select nodes with the buttons below." else "Roads work both ways. Select nodes with the buttons below.")
        GraphBoard(graph, if (isRoute) path.toSet() else selection)
        // Text road list keeps direction and cost readable without relying on the drawing or color.
        Text(graph.edges.joinToString("   |   ") {
            "${nodeName(it.from)} ${if (graph.directed) "→" else "—"} ${nodeName(it.to)}${if (isRoute) " (${it.weight})" else ""}"
        }, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        (0 until graph.size).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { node ->
                    val selected = if (isRoute) node in path else node in selection
                    OutlinedButton(
                        enabled = !won && (!isRoute || (node !in path && graph.neighbors(path.last()).any { it.first == node })),
                        onClick = {
                            if (isRoute) path = path + node
                            else selection = if (node in selection) selection - node else selection + node
                            message = "Ready when you are."
                        }, modifier = Modifier.weight(1f)
                    ) { Text("${nodeName(node)}${if (selected) " ✓" else ""}") }
                }
            }
        }
        if (isRoute) Text("Route: ${path.joinToString(" → ") { nodeName(it) }} • Cost: ${routeCost(graph, path)}")
        else {
            Text("Selected: ${if (selection.isEmpty()) "none" else groupName(selection)}")
            Text("Claimed ${claimed.size} / ${answers.size}: ${claimed.joinToString(" | ") { groupName(it) }}")
        }
        Text(message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
        Text("Checks: $attempts")
        Button(enabled = !won, onClick = {
            attempts++
            if (isRoute) {
                val cost = routeCost(graph, path)
                won = path.last() == graph.size - 1 && cost == optimum
                message = when {
                    won -> "Delivery complete! Your cost of $cost is optimal."
                    path.last() != graph.size - 1 -> "Keep going: the destination is F. Undo if you reached a dead end."
                    else -> "You reached F for $cost, but a route costing $optimum exists. Try a different route."
                }
            } else {
                message = when {
                    selection in claimed -> "You already found that group. Look for another."
                    selection in answers -> {
                        claimed = claimed + setOf(selection.toSet()); selection = emptySet()
                        "Correct! ${claimed.size} of ${answers.size} groups discovered."
                    }
                    topic == GraphTopic.SCC -> "Not a maximal SCC. Check that every selected node can get back to every other, and include the entire group."
                    else -> "Not a maximal block. Look for cycles, shared articulation vertices, bridges, or isolated nodes."
                }
                won = claimed == answers
                if (won) message = "All groups found! You mapped the whole network."
            }
            if (won) { onComplete(); theory = true }
            else if (attempts >= 2) theory = true
        }) { Text(if (isRoute) "Check delivery" else "Claim component") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !won, onClick = {
                if (isRoute && path.size > 1) path = path.dropLast(1) else selection = emptySet()
                message = "Try another choice."
            }) { Text(if (isRoute) "Undo" else "Clear") }
            OutlinedButton(onClick = {
                path = listOf(0); selection = emptySet(); claimed = emptySet(); won = false; attempts = 0
                message = "Board reset. Try again!"
            }) { Text("Restart") }
            OutlinedButton(onClick = {
                theory = true
                message = if (isRoute) "Aim for cost $optimum. ${if (topic == GraphTopic.BFS) "Think in layers." else "Compare total cost, not road count."}"
                else "Try building a group around ${nodeName((answers - claimed).firstOrNull()?.minOrNull() ?: 0)}. Read the rule below."
            }) { Text("Hint") }
        }
        if (won) Button(onClick = { level++ }) { Text("Next random board") }
        OutlinedButton(onClick = { theory = !theory }) { Text(if (theory) "Hide lesson" else "Theory & implementation") }
        if (theory) Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The idea", fontSize = 22.sp)
                Text(topic.theory)
                Text("Build it yourself", fontSize = 20.sp)
                Text(topic.implementation)
                Text("Try predicting the answer on the next board before choosing your moves.")
            }
        }
    }
}

@Composable
private fun GraphBoard(graph: LessonGraph, selected: Set<Int>) {
    val positions = listOf(.12f to .2f, .5f to .12f, .88f to .2f, .12f to .8f, .5f to .88f, .88f to .8f)
    BoxWithConstraints(Modifier.fillMaxWidth().height(240.dp).background(Color(0xFF172038), RoundedCornerShape(16.dp))) {
        val width = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            fun point(i: Int) = Offset(positions[i].first * size.width, positions[i].second * size.height)
            for (edge in graph.edges) {
                val a = point(edge.from); val b = point(edge.to)
                drawLine(Color(0xFF81A7FF), a, b, 3f)
                if (graph.directed) {
                    val delta = b - a
                    val unit = delta / delta.getDistance()
                    val tip = a + delta * .65f
                    val normal = Offset(-unit.y, unit.x)
                    drawLine(Color.White, tip, tip - unit * 12f + normal * 6f, 3f)
                    drawLine(Color.White, tip, tip - unit * 12f - normal * 6f, 3f)
                }
            }
        }
        positions.forEachIndexed { i, (x, y) ->
            Surface(color = if (i in selected) Color(0xFFB7F45B) else Color(0xFF24304D),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.offset(width * x - 16.dp, 240.dp * y - 16.dp)) {
                Text(nodeName(i), color = if (i in selected) Color.Black else Color.White, modifier = Modifier.padding(10.dp))
            }
        }
    }
}
