package com.playwise.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val playerColors = listOf(Color(0xFFB7F45B), Color(0xFF81A7FF), Color(0xFFFFAF74))
private fun label(node: Int) = ('A'.code+node).toChar().toString()

@Composable
fun GraphDuelScreen(kind: DuelKind) {
    var reset by remember { mutableIntStateOf(0) }
    Text(kind.title, fontSize = 28.sp)
    Text("LOCAL TWO-PLAYER • Share this screen", color = MaterialTheme.colorScheme.primary)
    Text(kind.rules, modifier = Modifier.padding(vertical = 12.dp))
    key(reset,kind) {
        if (kind == DuelKind.SPROUTS) SproutsBoard() else DuelBoard(kind)
    }
    OutlinedButton(onClick = { reset++ }) { Text("New game") }
    var explain by remember(kind) { mutableStateOf(false) }
    OutlinedButton(onClick = { explain = !explain }) { Text("${if (explain) "Hide" else "Show"} the mathematics") }
    if (explain) Text(kind.lesson, modifier = Modifier.padding(16.dp))
}

@Composable
private fun DuelBoard(kind: DuelKind) {
    var game by remember(kind) { mutableStateOf(DuelState(kind)) }
    var color by remember { mutableIntStateOf(0) }
    Text(game.result ?: when (kind) {
        DuelKind.COLORING -> if (game.turn == 0) "Builder (P1): color a vertex" else "Blocker (P2): color a vertex"
        DuelKind.SHANNON -> if (game.turn == 0) "Connector (P1): claim an edge" else "Cutter (P2): delete an edge"
        DuelKind.PATH -> if (game.routing) "Router (P2): build your route" else "Saboteur (P1): ${game.budget} budget remaining"
        else -> "Player ${game.turn+1}'s turn"
    }, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
    NetworkPicture(game)
    if (kind == DuelKind.COLORING) {
        Text("Choose a color, then a vertex. Number labels identify colors too.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { c -> Button(onClick = { color = c }, colors = ButtonDefaults.buttonColors(containerColor = playerColors[c])) { Text("${c+1}${if (color == c) " ✓" else ""}", color = Color.Black) } }
        }
    }
    if (kind in listOf(DuelKind.COLORING,DuelKind.KAYLES) || kind == DuelKind.PATH && game.routing) {
        (0 until game.graph.size).chunked(3).forEach { nodes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nodes.forEach { u ->
                    val enabled = game.result == null && when (kind) {
                        DuelKind.COLORING -> game.legalColor(u,color)
                        DuelKind.KAYLES -> u !in game.removed
                        else -> u !in game.path && game.graph.neighbors(game.path.last()).any { it.first == u }
                    }
                    OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = {
                        game = when(kind) { DuelKind.COLORING -> game.color(u,color); DuelKind.KAYLES -> game.takeNode(u); else -> game.travel(u) }
                    }) { Text(label(u) + when { u in game.colors -> " [${game.colors[u]!!+1}]"; u in game.removed -> " removed"; else -> "" }) }
                }
            }
        }
    }
    if (kind == DuelKind.PATH && game.routing) {
        Text("Route: ${game.path.joinToString(" → ") { label(it) }} | Cost: ${routeCost(game.graph,game.path)}")
        OutlinedButton(enabled = game.result == null && game.path.size > 1, onClick = { game = game.copy(path = game.path.dropLast(1)) }) { Text("Undo step") }
        Button(enabled = game.result == null && game.path.last() == 5, onClick = { game = game.submitRoute() }) { Text("Submit route") }
    }
    if (kind == DuelKind.PATH && !game.routing) Button(onClick = { game = game.lockRoads() }) { Text("Lock costs and hand to Router") }
    if (kind == DuelKind.BOXES) Text("Score — P1: ${game.boxes.values.count { it == 0 }} | P2: ${game.boxes.values.count { it == 1 }}")
    Text(if (kind in listOf(DuelKind.COLORING,DuelKind.KAYLES)) "Roads" else "Edges — choose using these controls", modifier = Modifier.padding(top = 12.dp))
    game.graph.edges.indices.chunked(2).forEach { indices ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            indices.forEach { index ->
                val e = game.graph.edges[index]
                val text = "${label(e.from)}–${label(e.to)}" + when {
                    kind == DuelKind.PATH -> " cost ${e.weight}"
                    index in game.cut -> " cut"
                    index in game.claimed -> " P${game.claimed[index]!!+1}"
                    else -> ""
                }
                if (kind in listOf(DuelKind.COLORING,DuelKind.KAYLES) || kind == DuelKind.PATH && game.routing) Text(text, modifier = Modifier.weight(1f).padding(8.dp))
                else OutlinedButton(modifier = Modifier.weight(1f), enabled = game.result == null && index !in game.claimed && index !in game.cut && (kind != DuelKind.PATH || game.budget > 0), onClick = {
                    game = if (kind == DuelKind.PATH) game.raiseCost(index) else game.takeEdge(index)
                }) { Text(text + if (kind == DuelKind.PATH) " +1" else "") }
            }
        }
    }
}

@Composable
private fun NetworkPicture(game: DuelState) {
    val positions = if (game.kind == DuelKind.BOXES) (0..8).map { InkPoint(.12f+(it%3)*.38f,.12f+(it/3)*.38f) }
        else listOf(InkPoint(.1f,.5f),InkPoint(.35f,.15f),InkPoint(.35f,.85f),InkPoint(.65f,.15f),InkPoint(.65f,.85f),InkPoint(.9f,.5f))
    BoxWithConstraints(Modifier.fillMaxWidth().height(260.dp).background(Color(0xFF172038), RoundedCornerShape(16.dp))) {
        val width = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            fun pos(i: Int) = Offset(positions[i].x*size.width,positions[i].y*size.height)
            if (game.kind == DuelKind.BOXES) game.boxes.forEach { (box, owner) ->
                val a = box/2*3+box%2
                drawRect(playerColors[owner].copy(alpha=.25f),pos(a), androidx.compose.ui.geometry.Size(size.width*.38f,size.height*.38f))
            }
            game.graph.edges.forEachIndexed { index,e ->
                if (index !in game.cut && e.from !in game.removed && e.to !in game.removed) drawLine(
                    game.claimed[index]?.let { playerColors[it] } ?: Color(0xFF71829C),pos(e.from),pos(e.to),if (index in game.claimed) 6f else 2f)
            }
        }
        positions.forEachIndexed { i,p ->
            Surface(color = game.colors[i]?.let { playerColors[it] } ?: if (i in game.removed) Color.DarkGray else Color(0xFF24304D),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.offset(width*p.x-14.dp,260.dp*p.y-16.dp)) {
                Text(label(i), color = if (i in game.colors) Color.Black else Color.White, modifier = Modifier.padding(8.dp))
            }
        }
        if (game.kind == DuelKind.BOXES) game.boxes.forEach { (box,owner) ->
            Text("P${owner+1}", color = playerColors[owner], modifier = Modifier.offset(width*(.31f+(box%2)*.38f)-10.dp,260.dp*(.31f+(box/2)*.38f)-10.dp))
        }
    }
}

@Composable
private fun SproutsBoard() {
    var game by remember { mutableStateOf(SproutsState()) }
    var draft by remember { mutableStateOf(emptyList<InkPoint>()) }
    var dotIndex by remember { mutableIntStateOf(1) }
    var message by remember { mutableStateOf("Tap a dot, add bends in empty space, then tap the destination dot. Loops may end at the starting dot.") }
    var concede by remember { mutableStateOf(false) }
    Text(game.result ?: "Player ${game.turn+1}'s turn • ${game.dots.sumOf { 3-it.degree }} lives remaining", color = MaterialTheme.colorScheme.primary)
    Text("Tap to draw a polygonal curve. The highlighted bend becomes the new dot. Dots show their remaining lives. No automatic no-move detection: use Concede if you cannot draw a legal move.")
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp).background(Color(0xFF172038),RoundedCornerShape(16.dp))) {
        val width = maxWidth
        Canvas(Modifier.fillMaxSize().pointerInput(game,draft) {
            detectTapGestures { tap ->
                if (game.result == null) {
                    val raw = InkPoint(tap.x/size.width,tap.y/size.height)
                    val nearest = game.dots.minByOrNull { it.point.distance(raw) }
                    val point = if (nearest != null && nearest.point.distance(raw) < .035f) nearest.point else raw
                    if (draft.size < 120) draft = draft + point
                }
            }
        }) {
            fun point(p: InkPoint) = Offset(p.x*size.width,p.y*size.height)
            game.edges.forEach { edge -> edge.points.zipWithNext().forEach { (a,b) -> drawLine(Color(0xFF81A7FF),point(a),point(b),3f) } }
            draft.zipWithNext().forEach { (a,b) -> drawLine(Color(0xFFB7F45B),point(a),point(b),3f) }
            draft.forEachIndexed { i,p -> drawCircle(if (i == dotIndex) Color.Yellow else Color.Gray,5f,point(p)) }
            game.dots.forEach { drawCircle(if (it.degree == 3) Color.Gray else Color.White,9f,point(it.point)) }
        }
        game.dots.forEach { dot -> Text("${3-dot.degree}", color = Color.White, modifier = Modifier.offset(width*dot.point.x+8.dp,360.dp*dot.point.y-20.dp)) }
    }
    Text(message, modifier = Modifier.padding(vertical = 10.dp))
    Text("${draft.size} curve points. New dot at bend $dotIndex.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = draft.isNotEmpty(), onClick = { draft = draft.dropLast(1); dotIndex = dotIndex.coerceAtMost((draft.size-2).coerceAtLeast(1)) }) { Text("Undo point") }
        OutlinedButton(onClick = { draft = emptyList(); dotIndex = 1 }) { Text("Clear curve") }
    }
    OutlinedButton(enabled = draft.size > 3, onClick = { dotIndex = if (dotIndex >= draft.lastIndex-1) 1 else dotIndex+1 }) { Text("Move new dot to next bend") }
    Button(enabled = game.result == null, onClick = {
        val error = game.error(draft,dotIndex)
        if (error != null) message = error else {
            game = game.play(draft,dotIndex); draft = emptyList(); dotIndex = 1
            message = "Legal move! Hand the screen to Player ${game.turn+1}."
        }
    }) { Text("Commit curve") }
    OutlinedButton(enabled = game.result == null, onClick = { concede = true }) { Text("Concede / I cannot move") }
    if (concede) AlertDialog(onDismissRequest = { concede = false }, title = { Text("Concede this game?") },
        text = { Text("This declares Player ${2-game.turn} the winner. The app does not prove that no legal curve exists.") },
        confirmButton = { Button(onClick = { game = game.copy(result = "Player ${2-game.turn} wins by concession."); concede = false }) { Text("Concede") } },
        dismissButton = { OutlinedButton(onClick = { concede = false }) { Text("Keep playing") } })
}
