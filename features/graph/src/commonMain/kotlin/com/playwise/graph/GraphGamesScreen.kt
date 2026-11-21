package com.playwise.graph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun groupName(group: Set<Int>) = group.sorted().joinToString(" · ") { nodeLabel(it) }

@Composable
fun GraphGamesScreen(onBack: () -> Unit) {
    var topic by remember { mutableStateOf<GraphTopic?>(null) }
    var duel by remember { mutableStateOf<DuelKind?>(null) }
    var completed by remember { mutableStateOf(setOf<GraphTopic>()) }
    var primer by remember { mutableStateOf(false) }
    var sketch by remember { mutableStateOf(false) }
    val atHub = topic == null && duel == null && !primer && !sketch
    OutlinedButton(onClick = { if (atHub) onBack() else { topic = null; duel = null; primer = false; sketch = false } }) {
        Text(if (atHub) "Back to learning paths" else "All graph games")
    }
    Text("Graph adventures", fontSize = 32.sp)
    Text("${completed.size} / 5 topics explored this session", color = Muted)
    Spacer(Modifier.height(16.dp))
    val active = topic
    if (sketch) {
        GraphSketchScreen()
    } else if (primer) {
        GraphPrimerScreen()
    } else if (duel != null) {
        key(duel) { GraphDuelScreen(duel!!) }
    } else if (active == null) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1C2742), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("New to graphs? Start here", fontSize = 22.sp)
                Text("Vocabulary, how programs store graphs, a traversal template, and a guide to which algorithm to use when.")
                Text("Suggested order: basics → BFS → 0–1 BFS → Dijkstra → SCC → Biconnected. Every lesson has a worked example, pseudocode, a quiz and links to further reading.", color = Muted)
                Button(onClick = { primer = true }) { Text("Open graph basics") }
            }
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF243A2A), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Draw your own graph", fontSize = 22.sp)
                Text("Sketch circles and lines with your finger. The app recognizes them and turns them into a neat graph you can move, edit and explore: cheapest routes, cycles, trees and more.")
                Button(onClick = { sketch = true }) { Text("Start drawing") }
            }
        }
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
    Text(topic.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text("Board ${level + 1}", color = Muted)
    key(level) {
        val graph = remember { randomLesson(topic) }
        var theory by remember { mutableStateOf(false) }
        val next: () -> Unit = { level++ }
        if (topic in listOf(GraphTopic.BFS, GraphTopic.DIJKSTRA, GraphTopic.ZERO_ONE))
            RouteLesson(topic, graph, onWin = { onComplete(); theory = true }, onStruggle = { theory = true }, onNext = next)
        else
            ComponentLesson(topic, graph, onWin = { onComplete(); theory = true }, onStruggle = { theory = true }, onNext = next)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { theory = !theory }) { Text(if (theory) "Hide lesson" else "Theory & implementation") }
        if (theory) {
            TeachingGuideView(topic.guide)
            Text("Try predicting the answer on the next board before you tap.", color = Muted)
        }
    }
}

/** Shortest-route puzzles: tap neighboring dots to walk from A (START) to F (GOAL). */
@Composable
private fun RouteLesson(topic: GraphTopic, graph: LessonGraph, onWin: () -> Unit, onStruggle: () -> Unit, onNext: () -> Unit) {
    val goal = graph.size - 1
    val optimum = remember { shortestDistances(graph, 0, topic).last() }
    var path by remember { mutableStateOf(listOf(0)) }
    var won by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(Tone.INFO) }
    val costWord = if (topic == GraphTopic.BFS) "roads used" else "cost"
    val cost = routeCost(graph, path) ?: 0
    val here = path.last()
    val options = if (won) emptySet() else graph.neighbors(here).map { it.first }.filter { it !in path }.toSet()

    TurnBanner(Accent,
        if (won) "You found the best route!" else "Walk from START to GOAL",
        when {
            won -> "Great job: $costWord $cost is the best possible."
            topic == GraphTopic.BFS -> "Tap a glowing dot to walk there. Use as few roads as you can."
            topic == GraphTopic.ZERO_ONE -> "Tap a glowing dot to walk there. FREE roads cost nothing, so pay as few tolls as you can."
            else -> "Tap a glowing dot to walk there. The numbers are road prices. Keep the total small."
        })

    fun tap(node: Int) {
        if (won) return
        when {
            node == here && path.size > 1 -> { path = path.dropLast(1); message = "Stepped back to ${nodeLabel(path.last())}."; tone = Tone.INFO }
            node == here -> { message = "You are standing on ${nodeLabel(node)}. Tap a glowing dot next to it."; tone = Tone.INFO }
            node in path -> { message = "You already walked through ${nodeLabel(node)}. Tap ${nodeLabel(here)} to step back."; tone = Tone.BAD }
            node !in options -> { message = "No road goes from ${nodeLabel(here)} to ${nodeLabel(node)}. Pick a glowing dot."; tone = Tone.BAD }
            else -> {
                path = path + node
                val newCost = routeCost(graph, path) ?: 0
                if (node == goal) {
                    attempts++
                    if (newCost == optimum) {
                        won = true; message = "You reached GOAL with $costWord $newCost. That's the best!"; tone = Tone.GOOD; onWin()
                    } else {
                        message = "You reached GOAL with $costWord $newCost, but $optimum is possible! Tap F to step back, or press Start over."
                        tone = Tone.BAD
                        if (attempts >= 2) onStruggle()
                    }
                } else { message = "Now at ${nodeLabel(node)}. ${costWord.replaceFirstChar { it.uppercase() }} so far: $newCost."; tone = Tone.INFO }
            }
        }
    }

    val steps = path.zipWithNext().map { (a, b) -> minOf(a, b) to maxOf(a, b) }.toSet()
    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until graph.size).map { i ->
            NodeLook(
                label = nodeLabel(i),
                fill = if (i in path) Accent else NodeIdle,
                textColor = if (i in path) Color.Black else Color.White,
                pulse = if (i in options) Accent else null,
                ring = if (i == here && !won) Color.White else null,
                badge = when (i) { 0 -> "START"; goal -> "GOAL"; else -> null },
                badgeColor = if (i == goal) Warn else Accent,
            )
        },
        edges = graph.edges.map { e ->
            val onPath = (minOf(e.from, e.to) to maxOf(e.from, e.to)) in steps
            EdgeLook(e.from, e.to,
                color = when { onPath -> Accent; topic == GraphTopic.ZERO_ONE && e.weight == 0 -> Color(0xFF4DD4C6); else -> EdgeIdle },
                width = if (onPath) 7.dp else 3.dp,
                dashed = topic == GraphTopic.ZERO_ONE && e.weight == 0 && !onPath,
                label = when (topic) { GraphTopic.BFS -> null; GraphTopic.ZERO_ONE -> if (e.weight == 0) "FREE" else "1"; else -> "${e.weight}" },
                labelColor = if (topic == GraphTopic.ZERO_ONE && e.weight == 0) Color(0xFF4DD4C6) else Color.White,
            )
        },
        onNodeTap = ::tap,
    )
    Text("Route: ${path.joinToString(" → ") { nodeLabel(it) }}   •   ${costWord.replaceFirstChar { it.uppercase() }}: $cost",
        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    Feedback(message, tone)
    if (won) ResultCard("Route complete!", "Best $costWord: $optimum. Ready for a harder board?", Accent, "Next board", onNext)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = !won && path.size > 1, onClick = { tap(here) }) { Text("Step back") }
        OutlinedButton(onClick = { path = listOf(0); won = false; message = "Back at START. Try again!"; tone = Tone.INFO }) { Text("Start over") }
        OutlinedButton(enabled = !won, onClick = {
            message = "Hint: the best route has $costWord $optimum. ${if (topic == GraphTopic.BFS) "Think in rings around START." else "Compare total cost, not number of roads."}"
            tone = Tone.INFO
        }) { Text("Hint") }
    }
    OutlinedButton(onClick = onNext) { Text("New board") }
    RoadListToggle(graph, showWeights = topic != GraphTopic.BFS)
}

/** Component puzzles: tap dots to select a group, then claim it. */
@Composable
private fun ComponentLesson(topic: GraphTopic, graph: LessonGraph, onWin: () -> Unit, onStruggle: () -> Unit, onNext: () -> Unit) {
    val answers = remember { if (topic == GraphTopic.SCC) stronglyConnectedComponents(graph) else biconnectedComponents(graph) }
    var selection by remember { mutableStateOf(setOf<Int>()) }
    var claimed by remember { mutableStateOf(listOf<Set<Int>>()) }
    var attempts by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(Tone.INFO) }
    var hint by remember { mutableStateOf<Int?>(null) }
    val won = claimed.size == answers.size

    TurnBanner(Color(0xFFFFD93D),
        if (won) "Every group found!" else "Find the groups (${claimed.size} of ${answers.size} found)",
        if (topic == GraphTopic.SCC) "Tap dots to pick a group, then press Claim. In a group, every dot can reach every other dot by following the arrows, and back."
        else "Tap dots to pick a group, then press Claim. A group stays connected even if any one of its dots is removed. A dot can be in two groups!")

    fun claim() {
        attempts++
        when {
            selection in claimed -> { message = "You already found that group. Look for another one."; tone = Tone.BAD }
            selection in answers -> {
                claimed = claimed + listOf(selection); selection = emptySet(); hint = null
                message = if (claimed.size == answers.size) "All groups found! You mapped the whole network." else "Correct! ${claimed.size} of ${answers.size} groups found."
                tone = Tone.GOOD
                if (claimed.size == answers.size) onWin()
            }
            answers.any { selection.containsAll(it) && selection != it } || answers.none { it.containsAll(selection) } ->
                { message = if (topic == GraphTopic.SCC) "Too much: some of these dots can't get back to each other." else "Too much: these dots don't all hold together without a weak point."; tone = Tone.BAD }
            else -> { message = "Almost! This group can grow. Add more dots that belong with it."; tone = Tone.BAD }
        }
        if (!won && attempts >= 2 && tone == Tone.BAD) onStruggle()
    }

    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until graph.size).map { i ->
            val groups = claimed.indices.filter { i in claimed[it] }
            val selected = i in selection
            NodeLook(
                label = nodeLabel(i),
                fill = when { selected -> Color(0xFFFFD93D); groups.isNotEmpty() -> GroupColors[groups.first() % GroupColors.size]; else -> NodeIdle },
                textColor = if (selected || groups.isNotEmpty()) Color.Black else Color.White,
                ring = when { selected -> Color.White; groups.size > 1 -> GroupColors[groups[1] % GroupColors.size]; else -> null },
                pulse = if (i == hint) Color(0xFFFFD93D) else null,
                badge = if (groups.size > 1) "shared" else if (selected) "picked" else null,
                badgeColor = if (selected) Color(0xFFFFD93D) else Muted,
            )
        },
        edges = graph.edges.map { e ->
            val group = claimed.indexOfFirst { e.from in it && e.to in it }
            val picked = e.from in selection && e.to in selection
            EdgeLook(e.from, e.to,
                color = when { picked -> Color(0xFFFFD93D); group >= 0 -> GroupColors[group % GroupColors.size]; else -> EdgeIdle },
                width = if (picked || group >= 0) 5.dp else 3.dp,
                arrow = graph.directed)
        },
        onNodeTap = { node ->
            if (!won) {
                selection = if (node in selection) selection - node else selection + node
                message = if (selection.isEmpty()) "" else "Picked: ${groupName(selection)}. Tap Claim when the group is complete."
                tone = Tone.INFO
            }
        },
    )
    if (claimed.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        claimed.forEachIndexed { i, g ->
            Surface(color = GroupColors[i % GroupColors.size], shape = RoundedCornerShape(12.dp)) {
                Text(groupName(g), color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
    Feedback(message, tone)
    if (won) ResultCard("Network mapped!", "You found all ${answers.size} groups.", Accent, "Next board", onNext)
    else Button(enabled = selection.isNotEmpty(), onClick = ::claim, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(if (selection.isEmpty()) "Tap dots to pick a group" else "Claim group ${groupName(selection)}", fontSize = 18.sp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = selection.isNotEmpty(), onClick = { selection = emptySet(); message = "" }) { Text("Clear picks") }
        OutlinedButton(onClick = { selection = emptySet(); claimed = emptyList(); attempts = 0; hint = null; message = "Board reset. Try again!"; tone = Tone.INFO }) { Text("Start over") }
        OutlinedButton(enabled = !won, onClick = {
            hint = answers.filter { it !in claimed }.minByOrNull { it.size }?.minOrNull()
            message = "Hint: the glowing dot ${hint?.let { nodeLabel(it) } ?: ""} belongs to a group you haven't found yet."
            tone = Tone.INFO
        }) { Text("Hint") }
    }
    OutlinedButton(onClick = onNext) { Text("New board") }
    RoadListToggle(graph, showWeights = false)
}
