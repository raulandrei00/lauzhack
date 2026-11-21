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
fun GraphDuelScreen(kind: DuelKind, board: PlayableBoard? = null) {
    var reset by remember { mutableIntStateOf(0) }
    var options by remember(kind) { mutableStateOf(DuelOptions()) }
    var draft by remember(kind) { mutableStateOf(options) }
    var settingsOpen by remember(kind) { mutableStateOf(false) }
    Text(kind.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text("TWO PLAYERS • Share this screen and take turns", color = MaterialTheme.colorScheme.primary)
    var rules by remember(kind) { mutableStateOf(false) }
    TextButton(onClick = { rules = !rules }) { Text(if (rules) "Hide rules" else "How to play") }
    if (rules) Text(kind.rules, modifier = Modifier.padding(bottom = 8.dp))
    TextButton(onClick = { settingsOpen = !settingsOpen }) {
        Text(if (settingsOpen) "Hide settings" else "Settings")
    }
    if (settingsOpen) {
        @Composable fun StepSetting(label: String, value: Int, min: Int, max: Int, change: (Int) -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$label: $value", modifier = Modifier.width(115.dp))
                OutlinedButton(onClick = { change((value - 1).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
                OutlinedButton(onClick = { change((value + 1).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
            }
        }
        if (kind != DuelKind.BOXES && board == null) StepSetting("Dots", draft.nodes,
            if (kind == DuelKind.SHANNON || kind == DuelKind.PATH) 4 else 2, 10) { draft = draft.copy(nodes = it) }
        if (kind == DuelKind.COLORING || kind == DuelKind.PLANAR)
            StepSetting("Paints", draft.paints, 1, 6) { draft = draft.copy(paints = it) }
        if (kind == DuelKind.PATH)
            StepSetting("Budget", draft.budget, 1, 10) { draft = draft.copy(budget = it) }
        if (kind == DuelKind.BOXES) {
            StepSetting("Columns", draft.columns, 2, 6) { draft = draft.copy(columns = it) }
            StepSetting("Rows", draft.rows, 2, 6) { draft = draft.copy(rows = it) }
        }
        Button(onClick = { options = draft; reset++; settingsOpen = false }) { Text("Start with these settings") }
    }
    val again: () -> Unit = { reset++ }
    key(reset, kind) {
        when (kind) {
            DuelKind.PLANAR -> PlanarDuel(options, again, board)
            DuelKind.COLORING -> ColoringDuel(options, again, board)
            DuelKind.SHANNON -> ShannonDuel(options, again, board)
            DuelKind.KAYLES -> KaylesDuel(options, again, board)
            DuelKind.PATH -> PathDuel(options, again, board)
            DuelKind.BOXES -> BoxesDuel(options, again)
        }
    }
    OutlinedButton(onClick = again) { Text(if (board != null) "Play again" else "New random game") }
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
private fun ColoringDuel(options: DuelOptions, onAgain: () -> Unit, board: PlayableBoard? = null) {
    var game by remember { mutableStateOf(board?.let { DuelState(DuelKind.COLORING, it.graph, budget = options.budget, paletteSize = options.paints) } ?: randomDuel(DuelKind.COLORING, options = options)) }
    var paint by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    fun stuck(u: Int) = u !in game.colors && (0 until game.paletteSize).none { game.legalColor(u, it) }
    val result = game.result
    if (result == null) TurnBanner(PlayerColors[game.turn],
        if (game.turn == 0) "${playerName(0)} · Builder" else "${playerName(1)} · Blocker",
        if (game.turn == 0) "Pick a paint, then tap a glowing dot. Try to paint every dot!"
        else "Pick a paint, then tap a glowing dot. Try to trap a dot so it can't be painted!")
    PaintPicker(game.paletteSize, paint) { paint = it; message = "" }
    TapBoard(
        positions = board?.positions ?: layoutFor(g.size),
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
        aspectRatio = if (board != null) 1.3f else if (g.size > 6) 1f else 1.45f,
        nodeRadius = if (g.size > 6) 18.dp else 26.dp,
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
private fun ShannonDuel(options: DuelOptions, onAgain: () -> Unit, board: PlayableBoard? = null) {
    var game by remember { mutableStateOf(board?.let { DuelState(DuelKind.SHANNON, it.graph, budget = options.budget, paletteSize = options.paints) } ?: randomDuel(DuelKind.SHANNON, options = options)) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val result = game.result
    if (result == null) TurnBanner(PlayerColors[game.turn],
        if (game.turn == 0) "${playerName(0)} · Connector" else "${playerName(1)} · Cutter",
        if (game.turn == 0) "Tap a road to lock it (it turns green). Build a locked path from START to GOAL!"
        else "Tap a road to cut it. Stop every path from START to GOAL!")
    TapBoard(
        positions = board?.positions ?: layoutFor(g.size),
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
        aspectRatio = if (board != null) 1.3f else if (g.size > 6) 1f else 1.45f,
        nodeRadius = if (g.size > 6) 18.dp else 26.dp,
        onEdgeTap = { i ->
            game = game.takeEdge(i); message = ""
        })
    Text("Tip: tap right on a grey road.", color = Muted, fontSize = 14.sp)
    Feedback(message, Tone.BAD)
    result?.let { GameOver(it, onAgain) }
}

// ---------------- Node Kayles ----------------

@Composable
private fun KaylesDuel(options: DuelOptions, onAgain: () -> Unit, board: PlayableBoard? = null) {
    var game by remember { mutableStateOf(board?.let { DuelState(DuelKind.KAYLES, it.graph, budget = options.budget, paletteSize = options.paints) } ?: randomDuel(DuelKind.KAYLES, options = options)) }
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
        positions = board?.positions ?: layoutFor(g.size),
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
        aspectRatio = if (board != null) 1.3f else if (g.size > 6) 1f else 1.45f,
        nodeRadius = if (g.size > 6) 18.dp else 26.dp,
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
private fun PathDuel(options: DuelOptions, onAgain: () -> Unit, board: PlayableBoard? = null) {
    var game by remember { mutableStateOf(board?.let { DuelState(DuelKind.PATH, it.graph, budget = options.budget, paletteSize = options.paints) } ?: randomDuel(DuelKind.PATH, options = options)) }
    // Keyed by road (unordered pair), so both arrows of a two-way road share one count.
    var raised by remember { mutableStateOf(mapOf<Pair<Int, Int>, Int>()) }
    var message by remember { mutableStateOf("") }
    val g = game.graph
    val goal = g.size - 1
    val result = game.result
    val here = game.path.last()
    val reachable = if (game.routing && result == null) g.neighbors(here).map { it.first }.filter { it !in game.path }.toSet() else emptySet()
    if (result == null) {
        if (!game.routing) TurnBanner(PlayerColors[0], "${playerName(0)} · Saboteur",
            "Tap a road to make it 1 more expensive. ${game.budget} of ${options.budget} points left. Router, look away!")
        else TurnBanner(PlayerColors[1], "${playerName(1)} · Router",
            "Tap glowing dots to walk from START to GOAL. Find the cheapest route, then press Submit.")
    }
    val steps = game.path.zipWithNext().map { (a, b) -> minOf(a, b) to maxOf(a, b) }.toSet()
    TapBoard(
        positions = board?.positions ?: layoutFor(g.size),
        nodes = (0 until g.size).map { u ->
            val walked = game.routing && u in game.path
            NodeLook(nodeLabel(u), fill = if (walked) PlayerColors[1] else NodeIdle, textColor = if (walked) Color.Black else Color.White,
                pulse = if (u in reachable) PlayerColors[1] else null,
                ring = if (game.routing && u == here && result == null) Color.White else null,
                badge = when (u) { 0 -> "START"; goal -> "GOAL"; else -> null }, badgeColor = Warn)
        },
        edges = g.edges.mapIndexed { i, e ->
            val onPath = game.routing && (minOf(e.from, e.to) to maxOf(e.from, e.to)) in steps
            val extra = raised[minOf(e.from, e.to) to maxOf(e.from, e.to)] ?: 0
            val twinLabel = g.directed && e.from > e.to && g.edges.any { it.from == e.to && it.to == e.from }
            EdgeLook(e.from, e.to, arrow = g.directed, color = when { onPath -> PlayerColors[1]; extra > 0 -> Warn; else -> EdgeIdle },
                width = if (onPath) 7.dp else 3.dp,
                label = if (twinLabel) null else if (extra > 0) "${e.weight} (+$extra)" else "${e.weight}",
                labelColor = if (extra > 0) Warn else Color.White,
                tappable = !game.routing && game.budget > 0 && result == null)
        },
        aspectRatio = if (board != null) 1.3f else if (g.size > 6) 1f else 1.45f,
        nodeRadius = if (g.size > 6) 18.dp else 26.dp,
        onNodeTap = if (!game.routing || result != null) null else { u ->
            message = when {
                u == here && game.path.size > 1 -> { game = game.copy(path = game.path.dropLast(1)); "" }
                u in game.path -> "You already walked through ${nodeLabel(u)}. Tap ${nodeLabel(here)} to step back."
                u !in reachable -> "No road from ${nodeLabel(here)} to ${nodeLabel(u)}. Pick a glowing dot."
                else -> { game = game.travel(u); "" }
            }
        },
        onEdgeTap = if (game.routing) null else { i ->
            if (game.budget > 0) {
                val e = g.edges[i]; val road = minOf(e.from, e.to) to maxOf(e.from, e.to)
                game = game.raiseCost(i); raised = raised + (road to (raised[road] ?: 0) + 1)
            }
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
private fun BoxesDuel(options: DuelOptions, onAgain: () -> Unit) {
    var game by remember { mutableStateOf(randomDuel(DuelKind.BOXES, options = options)) }
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
private fun PlanarDuel(options: DuelOptions, onAgain: () -> Unit, board: PlayableBoard? = null) {
    var game by remember { mutableStateOf(board?.let { PlanarGame(graph = it.graph, paletteSize = options.paints) } ?: PlanarGame(graph = LessonGraph(options.nodes, emptyList()), paletteSize = options.paints)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    // A custom board starts from the learner's drawing, rearranged only if lines cross.
    var positions by remember { mutableStateOf(board?.let { b -> runCatching { planarLayout(game.graph, b.positions) }.getOrDefault(b.positions) } ?: circleLayout(game.graph.size)) }
    val g = game.graph
    val winner = game.winner

    if (winner == null) TurnBanner(PlayerColors[game.turn], playerName(game.turn),
        if (selected == null) "Tap a dot to choose it. Then paint it, or tap a second dot to draw a line."
        else "Picked ${nodeLabel(selected!!)}. Tap a paint below, or tap another glowing dot to connect them.")

    fun reasonCantAdd(a: Int, b: Int): String {
        val line = "${nodeLabel(a)}–${nodeLabel(b)}"
        return when (game.edgeRejection(a, b)) {
            EdgeRejection.ALREADY_CONNECTED -> "$line already exists. Each pair of dots can have only one line."
            EdgeRejection.SAME_COLOR -> "$line is blocked: both dots are ${PaintNames[game.colors[a]!!]}. Connected dots must have different paints."
            EdgeRejection.K5 -> "$line would create a K5 pattern: five dots become pairwise connected by separate paths. That graph cannot be drawn without crossings."
            EdgeRejection.K33 -> "$line would create a K3,3 pattern: two groups of three dots become fully connected. That graph cannot be drawn without crossings."
            EdgeRejection.EDGE_LIMIT -> "$line would give the graph ${g.edges.size + 1} lines. A planar graph with ${g.size} dots can have at most ${3 * g.size - 6}."
            EdgeRejection.LOOP -> "A line must join two different dots."
            EdgeRejection.GAME_OVER -> "The game is over. Start a new game to add lines."
            EdgeRejection.INVALID_NODE -> "Choose two dots on the board."
            null -> "This line is allowed."
        }
    }

    TapBoard(
        positions = positions,
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
                badge = c?.let { if (g.size >= 8) PaintNames[it].take(1) else PaintNames[it] },
                badgeColor = c?.let { Paints[it] } ?: Muted)
        },
        edges = g.edges.map { e -> EdgeLook(e.from, e.to, EdgeIdle, 4.dp) },
        aspectRatio = 1f,
        nodeRadius = when { g.size >= 9 -> 12.dp; g.size >= 7 -> 16.dp; else -> 22.dp },
        onNodeTap = { u ->
            if (winner != null) return@TapBoard
            val s = selected
            message = ""
            when {
                s == null -> selected = u
                s == u -> { selected = null; message = "A line must join two different dots. Selection cleared." }
                game.canAdd(s, u) -> {
                    val next = game.add(s, u)
                    val arranged = runCatching { planarLayout(next.graph, positions) }.getOrNull()
                    if (arranged == null) message = "This line is legal, but the board could not arrange it clearly. Try another line."
                    else {
                        positions = arranged
                        game = next
                        selected = null
                    }
                }
                else -> { message = reasonCantAdd(s, u); selected = u }
            }
        })
    Text("After each new line, the dots move when needed so the drawing stays crossing-free.", fontSize = 13.sp, color = Muted)
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
