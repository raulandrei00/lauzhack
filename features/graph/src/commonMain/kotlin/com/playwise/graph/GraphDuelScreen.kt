package com.playwise.graph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun playerName(p: Int) = "Player ${p + 1}"

@Composable
fun GraphDuelScreen(kind: DuelKind) {
    var reset by remember { mutableIntStateOf(0) }
    Text(kind.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text("TWO PLAYERS • Share this screen and take turns", color = MaterialTheme.colorScheme.primary)
    var rules by remember(kind) { mutableStateOf(false) }
    TextButton(onClick = { rules = !rules }) { Text(if (rules) "Hide rules" else "How to play") }
    if (rules) Text(kind.rules, modifier = Modifier.padding(bottom = 8.dp))
    val again: () -> Unit = { reset++ }
    key(reset, kind) {
        when (kind) {
            DuelKind.PLANAR -> PlanarDuel(again)
            DuelKind.COLORING -> ColoringDuel(again)
            DuelKind.SHANNON -> ShannonDuel(again)
            DuelKind.KAYLES -> KaylesDuel(again)
            DuelKind.PATH -> PathDuel(again)
            DuelKind.BOXES -> BoxesDuel(again)
        }
    }
    OutlinedButton(onClick = again) { Text("New random game") }
    var explain by remember(kind) { mutableStateOf(false) }
    OutlinedButton(onClick = { explain = !explain }) { Text("${if (explain) "Hide" else "Show"} the mathematics & strategy") }
    if (explain) TeachingGuideView(kind.guide)
}

/** Parses a DuelState result line ("Player 2 wins...", "Builder (P1) wins...") into a winner index, or null for a draw. */
private fun winnerOf(result: String): Int? = when {
    result.startsWith("Draw") -> null
    "P1" in result || result.startsWith("Player 1") -> 0
    else -> 1
}

@Composable
private fun GameOver(result: String, onAgain: () -> Unit) {
    val w = winnerOf(result)
    ResultCard(if (w == null) "It's a draw!" else "${playerName(w)} wins!", result, if (w == null) Warn else PlayerColors[w], "Play again", onAgain)
}

// ---------------- Graph Coloring Duel ----------------

@Composable
private fun ColoringDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.COLORING)) }
    var paint by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    fun stuck(u: Int) = u !in game.colors && (0..2).none { game.legalColor(u, it) }
    val result = game.result
    if (result == null) TurnBanner(PlayerColors[game.turn],
        if (game.turn == 0) "${playerName(0)} · Builder" else "${playerName(1)} · Blocker",
        if (game.turn == 0) "Pick a paint, then tap a glowing dot. Try to paint every dot!"
        else "Pick a paint, then tap a glowing dot. Try to trap a dot so it can't be painted!")
    PaintPicker(3, paint) { paint = it; message = "" }
    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until g.size).map { u ->
            val c = game.colors[u]
            NodeLook(nodeLabel(u),
                fill = c?.let { Paints[it] } ?: NodeIdle,
                textColor = if (c != null) Color.Black else Color.White,
                pulse = if (result == null && game.legalColor(u, paint)) Paints[paint] else null,
                badge = c?.let { PaintNames[it] } ?: if (stuck(u)) "stuck!" else null,
                badgeColor = c?.let { Paints[it] } ?: Danger,
                crossed = stuck(u))
        },
        edges = g.edges.map { EdgeLook(it.from, it.to) },
        onNodeTap = { u ->
            if (result != null) return@TapBoard
            message = when {
                u in game.colors -> "${nodeLabel(u)} is already ${PaintNames[game.colors[u]!!]}. Paint can't be changed."
                !game.legalColor(u, paint) -> {
                    val clash = g.neighbors(u).map { it.first }.first { game.colors[it] == paint }
                    "${nodeLabel(u)} can't be ${PaintNames[paint]}: its neighbor ${nodeLabel(clash)} is already ${PaintNames[paint]}."
                }
                else -> { game = game.color(u, paint); "" }
            }
        })
    Feedback(message, Tone.BAD)
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Shannon Switching ----------------

@Composable
private fun ShannonDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.SHANNON)) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val result = game.result
    if (result == null) TurnBanner(PlayerColors[game.turn],
        if (game.turn == 0) "${playerName(0)} · Connector" else "${playerName(1)} · Cutter",
        if (game.turn == 0) "Tap a road to lock it (it turns green). Build a locked path from START to GOAL!"
        else "Tap a road to cut it. Stop every path from START to GOAL!")
    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until g.size).map { u ->
            val terminal = u == 0 || u == g.size - 1
            NodeLook(nodeLabel(u), fill = if (terminal) Warn else NodeIdle, textColor = if (terminal) Color.Black else Color.White,
                badge = when (u) { 0 -> "START"; g.size - 1 -> "GOAL"; else -> null }, badgeColor = Warn)
        },
        edges = g.edges.mapIndexed { i, e ->
            when {
                i in game.claimed -> EdgeLook(e.from, e.to, PlayerColors[0], 8.dp, label = "locked", labelColor = PlayerColors[0])
                i in game.cut -> EdgeLook(e.from, e.to, Danger.copy(alpha = .55f), 2.dp, dashed = true, label = "cut", labelColor = Danger)
                else -> EdgeLook(e.from, e.to, EdgeIdle, 4.dp, tappable = result == null)
            }
        },
        onEdgeTap = { i ->
            game = game.takeEdge(i); message = ""
        })
    Text("Tip: tap right on a grey road.", color = Muted, fontSize = 14.sp)
    Feedback(message, Tone.BAD)
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Node Kayles ----------------

@Composable
private fun KaylesDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.KAYLES)) }
    var pending by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val result = game.result
    val doomed = pending?.let { p -> (g.neighbors(p).map { it.first } + p).filter { it !in game.removed }.toSet() } ?: emptySet()
    if (result == null) TurnBanner(PlayerColors[game.turn], playerName(game.turn),
        if (pending == null) "Tap a dot. It and all its neighbors will vanish. Whoever makes the last move wins!"
        else "The crossed dots will vanish. Tap ${nodeLabel(pending!!)} again (or press Remove) to confirm.")
    fun confirm() { pending?.let { game = game.takeNode(it) }; pending = null }
    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until g.size).map { u ->
            NodeLook(nodeLabel(u),
                fill = if (u == pending) PlayerColors[game.turn] else NodeIdle,
                textColor = if (u == pending) Color.Black else Color.White,
                ghost = u in game.removed,
                pulse = if (result == null && pending == null && u !in game.removed) PlayerColors[game.turn] else null,
                ring = if (u == pending) Color.White else null,
                crossed = u in doomed && u != pending)
        },
        edges = g.edges.map { e ->
            EdgeLook(e.from, e.to, if (e.from == pending || e.to == pending) Danger else EdgeIdle,
                hidden = e.from in game.removed || e.to in game.removed)
        },
        onNodeTap = { u ->
            if (result != null) return@TapBoard
            message = ""
            when {
                u in game.removed -> message = "${nodeLabel(u)} is already gone. Pick a dot that is still on the board."
                u == pending -> confirm()
                else -> pending = u
            }
        })
    if (pending != null && result == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Button(onClick = ::confirm, colors = ButtonDefaults.buttonColors(containerColor = PlayerColors[game.turn])) {
            Text("Remove ${doomed.sorted().joinToString(", ") { nodeLabel(it) }}", color = Color.Black, fontSize = 17.sp)
        }
        OutlinedButton(onClick = { pending = null }) { Text("Cancel") }
    }
    Feedback(message, Tone.BAD)
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Shortest-Path Challenge ----------------

@Composable
private fun PathDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.PATH)) }
    var raised by remember { mutableStateOf(mapOf<Int, Int>()) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val goal = g.size - 1
    val result = game.result
    val here = game.path.last()
    val options = if (game.routing && result == null) g.neighbors(here).map { it.first }.filter { it !in game.path }.toSet() else emptySet()
    if (result == null) {
        if (!game.routing) TurnBanner(PlayerColors[0], "${playerName(0)} · Saboteur",
            "Tap a road to make it 1 more expensive. ${game.budget} of 5 points left. Router, look away!")
        else TurnBanner(PlayerColors[1], "${playerName(1)} · Router",
            "Tap glowing dots to walk from START to GOAL. Find the cheapest route, then press Submit.")
    }
    val steps = game.path.zipWithNext().map { (a, b) -> minOf(a, b) to maxOf(a, b) }.toSet()
    TapBoard(
        positions = sixNodeLayout,
        nodes = (0 until g.size).map { u ->
            val walked = game.routing && u in game.path
            NodeLook(nodeLabel(u), fill = if (walked) PlayerColors[1] else NodeIdle, textColor = if (walked) Color.Black else Color.White,
                pulse = if (u in options) PlayerColors[1] else null,
                ring = if (game.routing && u == here && result == null) Color.White else null,
                badge = when (u) { 0 -> "START"; goal -> "GOAL"; else -> null }, badgeColor = Warn)
        },
        edges = g.edges.mapIndexed { i, e ->
            val onPath = game.routing && (minOf(e.from, e.to) to maxOf(e.from, e.to)) in steps
            val extra = raised[i] ?: 0
            EdgeLook(e.from, e.to, color = when { onPath -> PlayerColors[1]; extra > 0 -> Warn; else -> EdgeIdle },
                width = if (onPath) 7.dp else 3.dp,
                label = if (extra > 0) "${e.weight} (+$extra)" else "${e.weight}",
                labelColor = if (extra > 0) Warn else Color.White,
                tappable = !game.routing && game.budget > 0 && result == null)
        },
        onNodeTap = if (!game.routing || result != null) null else { u ->
            message = when {
                u == here && game.path.size > 1 -> { game = game.copy(path = game.path.dropLast(1)); "" }
                u in game.path -> "You already walked through ${nodeLabel(u)}. Tap ${nodeLabel(here)} to step back."
                u !in options -> "No road from ${nodeLabel(here)} to ${nodeLabel(u)}. Pick a glowing dot."
                else -> { game = game.travel(u); "" }
            }
        },
        onEdgeTap = if (game.routing) null else { i ->
            if (game.budget > 0) { game = game.raiseCost(i); raised = raised + (i to (raised[i] ?: 0) + 1) }
        })
    if (game.routing) Text("Route: ${game.path.joinToString(" → ") { nodeLabel(it) }}   •   Cost: ${routeCost(g, game.path)}",
        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    Feedback(message, Tone.BAD)
    if (result == null) {
        if (!game.routing) Button(onClick = { game = game.lockRoads(); message = "" }, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlayerColors[0])) {
            Text("Done! Hand the screen to ${playerName(1)}", color = Color.Black, fontSize = 18.sp)
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = here == goal, onClick = { game = game.submitRoute() }, colors = ButtonDefaults.buttonColors(containerColor = PlayerColors[1])) {
                Text(if (here == goal) "Submit route" else "Reach GOAL to submit", color = Color.Black, fontSize = 17.sp)
            }
            OutlinedButton(enabled = game.path.size > 1, onClick = { game = game.copy(path = game.path.dropLast(1)) }) { Text("Step back") }
        }
    }
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Dots and Boxes ----------------

@Composable
private fun BoxesDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.BOXES)) }
    val g = game.graph
    val columns = game.gridColumns
    val rows = g.size / columns
    val result = game.result
    val scores = (0..1).map { p -> game.boxes.values.count { it == p } }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        (0..1).forEach { p ->
            Surface(color = PlayerColors[p].copy(alpha = if (game.turn == p && result == null) .35f else .12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(playerName(p) + if (game.turn == p && result == null) "  ← your turn" else "", color = PlayerColors[p], fontWeight = FontWeight.Bold)
                    Text("${scores[p]}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
    if (result == null) TurnBanner(PlayerColors[game.turn], playerName(game.turn),
        "Tap a dotted line between two dots. Close a box to win it and go again!")
    TapBoard(
        positions = gridLayout(columns, rows),
        nodes = (0 until g.size).map { NodeLook("", fill = Color.White) },
        edges = g.edges.mapIndexed { i, e ->
            val owner = game.claimed[i]
            if (owner != null) EdgeLook(e.from, e.to, PlayerColors[owner], 6.dp)
            else EdgeLook(e.from, e.to, Muted.copy(alpha = .45f), 2.dp, dashed = true, tappable = result == null)
        },
        boxes = game.boxes.map { (box, owner) ->
            val a = box / (columns - 1) * columns + box % (columns - 1)
            BoxLook(a, a + columns + 1, PlayerColors[owner], "P${owner + 1}")
        },
        aspectRatio = ((columns - 1f) / (rows - 1f)).coerceIn(.8f, 1.6f),
        nodeRadius = 7.dp,
        showLabels = false,
        onEdgeTap = { i -> game = game.takeEdge(i) })
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Planar Color & Connect ----------------

@Composable
private fun PlanarDuel(onAgain: () -> Unit) {
    var game by remember { mutableStateOf(PlanarGame(graph = randomDuel(DuelKind.PLANAR).graph)) }
    var n by remember { mutableIntStateOf(game.graph.size) }
    var k by remember { mutableIntStateOf(game.paletteSize) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val winner = game.winner

    var settings by remember { mutableStateOf(false) }
    TextButton(onClick = { settings = !settings }) { Text(if (settings) "Hide settings" else "Settings: ${g.size} dots, ${game.paletteSize} paints") }
    if (settings) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { n = if (n == 6) 2 else n + 1 }) { Text("Dots: $n") }
            OutlinedButton(onClick = { k = if (k == 4) 1 else k + 1 }) { Text("Paints: $k") }
            Button(onClick = { game = PlanarGame(LessonGraph(n, emptyList()), k); selected = null; message = "" }) { Text("Start") }
        }
    }

    if (winner == null) TurnBanner(PlayerColors[game.turn], playerName(game.turn),
        if (selected == null) "Tap a dot to choose it. Then paint it, or tap a second dot to draw a line."
        else "Picked ${nodeLabel(selected!!)}. Tap a paint below, or tap another glowing dot to connect them.")

    fun reasonCantAdd(a: Int, b: Int) = when {
        g.neighbors(a).any { it.first == b } -> "${nodeLabel(a)} and ${nodeLabel(b)} are already connected."
        game.colors[a] != null && game.colors[a] == game.colors[b] -> "${nodeLabel(a)} and ${nodeLabel(b)} are both ${PaintNames[game.colors[a]!!]}. Same colors can't be joined."
        else -> "That line would make crossings impossible to avoid (the graph would stop being planar)."
    }

    TapBoard(
        positions = circleLayout(g.size),
        nodes = (0 until g.size).map { u ->
            val c = game.colors[u]
            val target = winner == null && selected != null && u != selected && game.canAdd(selected!!, u)
            NodeLook(nodeLabel(u),
                fill = c?.let { Paints[it] } ?: NodeIdle,
                textColor = if (c != null) Color.Black else Color.White,
                ring = if (u == selected) Color.White else null,
                pulse = when {
                    winner != null -> null
                    selected == null -> PlayerColors[game.turn]
                    target -> PlayerColors[game.turn]
                    else -> null
                },
                badge = c?.let { PaintNames[it] }, badgeColor = c?.let { Paints[it] } ?: Muted)
        },
        edges = g.edges.map { e -> EdgeLook(e.from, e.to, EdgeIdle, 4.dp) },
        aspectRatio = 1.25f,
        onNodeTap = { u ->
            if (winner != null) return@TapBoard
            val s = selected
            message = ""
            when {
                s == null -> selected = u
                s == u -> selected = null
                game.canAdd(s, u) -> { game = game.add(s, u); selected = null }
                else -> { message = reasonCantAdd(s, u); selected = u }
            }
        })
    Text("The picture may show crossing lines. That's fine: a line is allowed if the dots could be rearranged so nothing crosses.", fontSize = 13.sp, color = Muted)
    selected?.let { s ->
        if (winner == null) {
            Text("Paint ${nodeLabel(s)}:", fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
            PaintPicker(game.paletteSize, null, enabled = { game.canColor(s, it) }) { c ->
                message = when {
                    s in game.colors -> "${nodeLabel(s)} is already ${PaintNames[game.colors[s]!!]}. Paint can't be changed."
                    !game.canColor(s, c) -> {
                        val clash = g.neighbors(s).map { it.first }.first { game.colors[it] == c }
                        "${nodeLabel(s)} can't be ${PaintNames[c]}: it's connected to ${nodeLabel(clash)}, which is ${PaintNames[c]}."
                    }
                    else -> { game = game.color(s, c); selected = null; "" }
                }
            }
        }
    }
    Feedback(message, Tone.BAD)
    winner?.let { ResultCard("${playerName(it)} wins!", "${playerName(1 - it)} has no legal move left.", PlayerColors[it], "Play again", onAgain) }
}
