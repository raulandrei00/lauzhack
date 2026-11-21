package com.playwise.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val BOARD_ASPECT = 1.3f
private val EdgeInk = Color(0xFF81A7FF)

private fun kindColor(k: StrokeKind) = when (k) {
    StrokeKind.VERTEX -> Accent
    StrokeKind.EDGE -> EdgeInk
    StrokeKind.ARROW -> Warn
    StrokeKind.IGNORED -> Muted.copy(alpha = .45f)
}

private enum class Tool(val title: String, val help: String) {
    MOVE("Move & connect", "Drag a vertex to move it. Tap two vertices to connect them. Tap a line to change it."),
    ADD("Add vertex", "Tap an empty spot to add a vertex there."),
    ERASE("Eraser", "Tap a vertex or a line to remove it."),
    PAINT("Paint", "Pick a paint below, then tap vertices to color them. Joined vertices should get different colors."),
}

/** The learner's graph and drawing. Lives above the screen so it survives leaving to play a game. */
class SketchSession {
    var strokes by mutableStateOf(listOf<List<Pt>>())
    var boardDp by mutableStateOf(400f to 300f)
    var graph by mutableStateOf<DrawnGraph?>(null)
    var history by mutableStateOf(listOf<DrawnGraph>())
    var start by mutableStateOf(0)
    var goal by mutableStateOf<Int?>(null)
}

/** Draw a graph by hand, let the app recognize it, then edit and explore the clean version. */
@Composable
fun GraphSketchScreen(session: SketchSession, onPlay: (GameChoice, PlayableBoard) -> Unit) {
    val current = session.graph
    fun open(g: DrawnGraph) { session.history = emptyList(); session.start = 0; session.goal = null; session.graph = g }
    Text("Draw your own graph", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    if (current == null) {
        SketchPad(session.strokes, onStrokes = { session.strokes = it }, onSize = { session.boardDp = it },
            onConvert = { result -> open(DrawnGraph.fromSketch(result, session.boardDp.first, session.boardDp.second)) },
            onStartEmpty = { open(DrawnGraph()) })
        ImportBox(onImport = ::open)
    } else {
        GraphEditor(session, current, onChange = { session.graph = it }, onBackToDrawing = { session.graph = null }, onPlay = onPlay)
    }
}

/** Type or paste a graph as text, e.g. "A-B 3, B>C". */
@Composable
private fun ImportBox(onImport: (DrawnGraph) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide text import" else "Import a graph from text") }
    if (open) Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Write lines between letters: A-B is a two-way line, A>B is one-way, and a number after it is the weight. A lone letter is a vertex with no lines.", color = Muted)
            OutlinedTextField(value = text, onValueChange = { text = it; error = "" }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("A-B 3, B>C, C-A, D") }, minLines = 2)
            if (error.isNotEmpty()) Text(error, color = Warn)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (val r = parseGraphText(text)) {
                        is ParseResult.Ok -> onImport(r.graph)
                        is ParseResult.Error -> error = r.message
                    }
                }) { Text("Import") }
                OutlinedButton(onClick = { text = "A-B 4, A-C 1, C-B 2, B-D 1, C-D 6, D>E 3" ; error = "" }) { Text("Fill in an example") }
            }
        }
    }
}

// ---------------- Drawing pad ----------------

@Composable
private fun SketchPad(
    strokes: List<List<Pt>>,
    onStrokes: (List<List<Pt>>) -> Unit,
    onSize: (Pair<Float, Float>) -> Unit,
    onConvert: (SketchResult) -> Unit,
    onStartEmpty: () -> Unit,
) {
    val density = LocalDensity.current.density
    val result = remember(strokes) { SketchRecognizer.recognize(strokes) }
    var live by remember { mutableStateOf(listOf<Offset>()) }
    var size by remember { mutableStateOf(400f to 300f) }
    val latestStrokes by rememberUpdatedState(strokes)
    val measurer = rememberTextMeasurer()

    TurnBanner(Accent, "Draw with your finger or mouse",
        "Circles or dots are vertices. Lines between them are edges. Add a small V at the end of a line to make it one-way.")
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        Legend(Accent, "${result.vertices.size} vertices")
        Legend(EdgeInk, "${result.edges.size} edges")
        Legend(Warn, "${result.edges.count { it.directed }} one-way")
        Legend(Muted, "${result.kinds.count { it == StrokeKind.IGNORED }} ignored")
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().aspectRatio(BOARD_ASPECT)
                .background(BoardBackground, RoundedCornerShape(20.dp))
                .onSizeChanged {
                    val s = it.width / density to it.height / density
                    size = s; onSize(s)
                }
                .pointerInput(Unit) {
                    // Every touch on the pad draws, so it must not scroll the page.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val points = mutableListOf(down.position)
                        live = points.toList()
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            if ((change.position - points.last()).getDistance() > 1.5f) { points += change.position; live = points.toList() }
                        }
                        live = emptyList()
                        onStrokes(latestStrokes + listOf(points.map { Pt(it.x / density, it.y / density) }))
                    }
                }
        ) {
            val dp = density
            // Dotted paper.
            val step = 28f * dp
            var gx = step
            while (gx < this.size.width) {
                var gy = step
                while (gy < this.size.height) { drawCircle(Color.White.copy(alpha = .06f), 1.5f * dp, Offset(gx, gy)); gy += step }
                gx += step
            }
            // What the recognizer sees, drawn faintly under the ink.
            fun at(p: Pt) = Offset(p.x * dp, p.y * dp)
            result.edges.forEach { e ->
                drawLine(EdgeInk.copy(alpha = .25f), at(result.vertices[e.from].center), at(result.vertices[e.to].center), 10f * dp, cap = StrokeCap.Round)
            }
            result.vertices.forEachIndexed { i, v ->
                drawCircle((if (v.drawn) Accent else Warn).copy(alpha = .8f), v.radius * dp + 6 * dp, at(v.center),
                    style = Stroke(2f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * dp, 6f * dp))))
                val layout = measurer.measure(nodeLabel(i), TextStyle(color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                drawText(layout, topLeft = at(v.center) + Offset(v.radius * dp + 6 * dp, -v.radius * dp - 14 * dp))
            }
            // The ink, coloured by what each stroke was recognized as.
            strokes.forEachIndexed { i, s ->
                val color = kindColor(result.kinds.getOrElse(i) { StrokeKind.IGNORED })
                if (s.size == 1) drawCircle(color, 6f * dp, at(s[0]))
                else drawPath(Path().apply { moveTo(s[0].x * dp, s[0].y * dp); s.drop(1).forEach { lineTo(it.x * dp, it.y * dp) } },
                    color, style = Stroke(4f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (live.size > 1) drawPath(Path().apply { moveTo(live[0].x, live[0].y); live.drop(1).forEach { lineTo(it.x, it.y) } },
                Color.White, style = Stroke(4f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            else if (live.size == 1) drawCircle(Color.White, 6f * dp, live[0])
        }
    }
    result.notes.forEach { Feedback(it, Tone.INFO) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(enabled = strokes.isNotEmpty(), onClick = { onStrokes(strokes.dropLast(1)) }) { Text("Undo stroke") }
        OutlinedButton(enabled = strokes.isNotEmpty(), onClick = { onStrokes(emptyList()) }) { Text("Clear") }
        OutlinedButton(onClick = { onStrokes(SketchRecognizer.exampleStrokes(size.first, size.second)) }) { Text("Show an example") }
    }
    Button(enabled = result.vertices.isNotEmpty(), onClick = { onConvert(result) },
        modifier = Modifier.fillMaxWidth().height(58.dp).padding(top = 6.dp)) {
        Text(if (result.vertices.isEmpty()) "Draw something first" else "Make it neat →", fontSize = 18.sp)
    }
    TextButton(onClick = onStartEmpty) { Text("Or skip drawing and build it by tapping") }
    HowItWorks()
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(6.dp)))
        Text(" $text", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun HowItWorks() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide: how does the app read my drawing?" else "How does the app read my drawing?") }
    if (open) Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Each pen stroke is measured, not guessed by a neural network:", fontWeight = FontWeight.Bold)
            Text("• Closed and round (the end comes back near the start, and every point is about the same distance from the middle) → a vertex. A tiny tap → a dot vertex.")
            Text("• Open stroke → an edge. Each end snaps to the closest vertex. With no vertex nearby, one is created there (dashed orange). A line drawn through a circle becomes two edges.")
            Text("• A short stroke with one sharp corner, touching the end of a line → an arrowhead, so that edge becomes one-way. A hook at the end of a line works too.")
            Text("• Vertices are named A, B, C... from left to right.", color = Muted)
            Text("Why not AI? Shapes like these are easy to measure exactly, so the app can explain every decision, works offline, and needs no training data. Reading handwritten numbers would need machine learning, so you set weights by tapping instead.", color = Muted)
        }
    }
}

// ---------------- Editor ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GraphEditor(session: SketchSession, graph: DrawnGraph, onChange: (DrawnGraph) -> Unit, onBackToDrawing: () -> Unit,
                        onPlay: (GameChoice, PlayableBoard) -> Unit) {
    var oneWayMode by remember { mutableStateOf(false) }
    var paint by remember { mutableStateOf<Int?>(0) }
    var tool by remember { mutableStateOf(Tool.MOVE) }
    var selVertex by remember { mutableStateOf<Int?>(null) }
    var selEdge by remember { mutableStateOf<Int?>(null) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var showWeights by remember { mutableStateOf(false) }
    var routeFrom by remember { mutableStateOf<Int?>(null) }
    var routeTo by remember { mutableStateOf<Int?>(null) }

    fun commit(next: DrawnGraph) {
        if (next == graph) return
        session.history = (session.history + graph).takeLast(50)
        onChange(next)
    }
    fun clearSelection() { selVertex = null; selEdge = null; dragging = null }
    fun forgetVertex() { routeFrom = null; routeTo = null; clearSelection() }

    TurnBanner(Accent, "Your graph, cleaned up", tool.help)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tool.entries.forEach { t ->
            FilterChip(selected = tool == t, onClick = { tool = t; clearSelection(); message = "" }, label = { Text(t.title) })
        }
    }
    if (tool == Tool.PAINT) {
        PaintPicker(Paints.size, paint) { paint = it; message = "" }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = paint == null, onClick = { paint = null }, label = { Text("No paint (eraser)") })
            OutlinedButton(enabled = graph.size > 0, onClick = {
                val best = bestColoring(graph)
                if (best == null) message = "This graph needs more than ${Paints.size} paints. Try removing some lines."
                else {
                    commit(graph.withColoring(best.colors))
                    message = if (best.exact) "Colored with ${best.count} paint(s). That's the fewest possible: the chromatic number is ${best.count}."
                        else "Colored with ${best.count} paints using a quick rule (DSatur). Fewer might be possible."
                }
            }) { Text("Auto-color (fewest paints)") }
            OutlinedButton(enabled = graph.vertices.any { it.color != null }, onClick = { commit(graph.clearPaint()); message = "Paint removed." }) { Text("Clear paint") }
        }
    } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = oneWayMode, onCheckedChange = { oneWayMode = it })
            Text("  One-way mode: new lines point from the first vertex you tap to the second")
        }
        OutlinedButton(enabled = graph.edges.any { !it.directed }, onClick = { commit(graph.allOneWay()); oneWayMode = true; message = "Every line is now one-way (pointing the way it was drawn). Tap a line to flip it." }) { Text("Make all one-way") }
        OutlinedButton(enabled = graph.anyDirected, onClick = { commit(graph.allTwoWay()); oneWayMode = false; message = "Every line is now two-way." }) { Text("Make all two-way") }
    }

    val route = if (routeFrom != null && routeTo != null) cheapestRoute(graph, routeFrom!!, routeTo!!) else null
    val routeSteps = route?.first?.zipWithNext()?.map { (a, b) -> minOf(a, b) to maxOf(a, b) }?.toSet() ?: emptySet()
    val weightsMatter = showWeights || graph.edges.any { it.weight != 1 }
    val many = graph.size > 12
    val clashes = graph.colorClashes().toSet()

    TapBoard(
        positions = graph.vertices.map { Offset(it.x, it.y) },
        nodes = graph.vertices.indices.map { i ->
            val canConnect = tool != Tool.ERASE && tool != Tool.PAINT && selVertex != null && selVertex != i && graph.edgeIndex(selVertex!!, i) < 0
            NodeLook(
                label = nodeLabel(i),
                fill = when { i == selVertex -> Accent; route?.first?.contains(i) == true -> Color(0xFF4DD4C6); else -> graph.vertices[i].color?.let { Paints[it] } ?: NodeIdle },
                textColor = if (i == selVertex || route?.first?.contains(i) == true || graph.vertices[i].color != null) Color.Black else Color.White,
                ring = if (i == selVertex) Color.White else null,
                pulse = if (canConnect) Accent else null,
                crossed = false,
                badge = when (i) { routeFrom -> "FROM"; routeTo -> "TO"; else -> null },
                badgeColor = Warn,
            )
        },
        edges = graph.edges.mapIndexed { k, e ->
            val onRoute = (minOf(e.a, e.b) to maxOf(e.a, e.b)) in routeSteps
            EdgeLook(e.a, e.b,
                color = when { k == selEdge -> Color.White; onRoute -> Color(0xFF4DD4C6); k in clashes -> Danger; else -> EdgeIdle },
                width = if (k == selEdge || onRoute || k in clashes) 6.dp else 3.dp,
                dashed = k in clashes,
                arrow = e.directed,
                label = if (weightsMatter) "${e.weight}" else null,
                tappable = true)
        },
        aspectRatio = BOARD_ASPECT,
        nodeRadius = if (many) 17.dp else 24.dp,
        onNodeTap = { i ->
            dragging = null
            message = ""
            when {
                tool == Tool.ERASE -> { commit(graph.removeVertex(i)); forgetVertex(); message = "Removed ${nodeLabel(i)}. Later letters moved up one." }
                tool == Tool.PAINT -> {
                    val next = graph.paint(i, paint)
                    commit(next)
                    val clash = paint?.let { p -> graph.undirectedNeighbors(i).firstOrNull { graph.vertices[it].color == p } }
                    message = when {
                        paint == null -> "Removed the paint from ${nodeLabel(i)}."
                        clash != null -> "Careful: ${nodeLabel(i)} and ${nodeLabel(clash)} are joined and both ${PaintNames[paint!!]}. The dashed red line shows the clash."
                        next.vertices.all { it.color != null } && next.colorClashes().isEmpty() -> "Every vertex is painted and no joined vertices share a color: a proper coloring with ${next.vertices.mapNotNull { it.color }.toSet().size} paints!"
                        else -> ""
                    }
                }
                selVertex == null -> { selVertex = i; selEdge = null; message = "Picked ${nodeLabel(i)}. Tap a glowing vertex to connect it, or drag to move." }
                selVertex == i -> selVertex = null
                else -> {
                    val a = selVertex!!
                    val existing = graph.edgeIndex(a, i)
                    if (existing >= 0) { selEdge = existing; selVertex = null }
                    else {
                        commit(graph.addEdge(a, i, oneWay = oneWayMode))
                        message = if (oneWayMode) "Added a one-way line ${nodeLabel(a)} → ${nodeLabel(i)}." else "Connected ${nodeLabel(a)} and ${nodeLabel(i)}."
                        selVertex = null
                    }
                }
            }
        },
        onEdgeTap = { k ->
            dragging = null
            if (tool == Tool.ERASE) { val e = graph.edges[k]; commit(graph.removeEdge(k)); clearSelection(); message = "Removed the line ${nodeLabel(e.a)}–${nodeLabel(e.b)}." }
            else { selEdge = k; selVertex = null; message = "" }
        },
        onNodeDrag = if (tool == Tool.ERASE) null else { i, p ->
            if (dragging != i) { session.history = (session.history + graph).takeLast(50); dragging = i }
            onChange(graph.moveVertex(i, p.x, p.y))
        },
        onEmptyTap = { p ->
            dragging = null
            when {
                tool == Tool.ADD && graph.size >= DrawnGraph.MAX_VERTICES -> message = "That's the most vertices (A to Z)."
                tool == Tool.ADD -> { commit(graph.addVertex(p.x, p.y)); message = "Added ${nodeLabel(graph.size)}." }
                else -> { clearSelection(); message = "" }
            }
        },
    )
    if (message.isNotEmpty()) Feedback(message, Tone.INFO)

    // Panel for the selected vertex or edge.
    selVertex?.takeIf { it < graph.size }?.let { v ->
        Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("Vertex ${nodeLabel(v)} · ${graph.undirectedNeighbors(v).size} edge(s)", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { routeFrom = v; clearSelection() }) { Text("Route from here") }
                    OutlinedButton(onClick = { routeTo = v; clearSelection() }) { Text("Route to here") }
                    OutlinedButton(onClick = { commit(graph.removeVertex(v)); forgetVertex() }) { Text("Delete vertex", color = Danger) }
                }
            }
        }
    }
    selEdge?.takeIf { it < graph.edges.size }?.let { k ->
        val e = graph.edges[k]
        val lo = minOf(e.a, e.b); val hi = maxOf(e.a, e.b)
        val direction = when { !e.directed -> 0; e.a == lo -> 1; else -> 2 }
        Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Line ${nodeLabel(lo)}–${nodeLabel(hi)}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weight (cost):")
                    OutlinedButton(enabled = e.weight > 0, onClick = { commit(graph.setWeight(k, e.weight - 1)) }) { Text("−") }
                    Text("${e.weight}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(enabled = e.weight < DrawnGraph.MAX_WEIGHT, onClick = { commit(graph.setWeight(k, e.weight + 1)) }) { Text("+") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Both ways", "${nodeLabel(lo)} → ${nodeLabel(hi)}", "${nodeLabel(hi)} → ${nodeLabel(lo)}").forEachIndexed { d, label ->
                        FilterChip(selected = direction == d, onClick = { commit(graph.setDirection(k, d)) }, label = { Text(label) })
                    }
                }
                OutlinedButton(onClick = { commit(graph.removeEdge(k)); clearSelection() }) { Text("Delete line", color = Danger) }
            }
        }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(enabled = session.history.isNotEmpty(), onClick = {
            onChange(session.history.last()); session.history = session.history.dropLast(1); forgetVertex(); message = "Undone."
        }) { Text("Undo") }
        OutlinedButton(enabled = graph.size > 1, onClick = { commit(tidyArrangement(graph)); message = "Tidied: lines pull like springs, vertices push apart." }) { Text("Tidy up") }
        OutlinedButton(enabled = graph.size > 2, onClick = { commit(circleArrangement(graph)) }) { Text("Circle") }
        OutlinedButton(onClick = { showWeights = !showWeights }) { Text(if (weightsMatter) "Hide weights" else "Show weights") }
        OutlinedButton(onClick = onBackToDrawing) { Text("New drawing") }
    }
    if (weightsMatter && !showWeights && graph.edges.any { it.weight != 1 })
        Text("Weights stay visible while some line costs more or less than 1.", color = Muted, fontSize = 13.sp)

    PlayPanel(session, graph, onPlay)
    RouteFinder(graph, routeFrom, routeTo, route, onFrom = { routeFrom = it }, onTo = { routeTo = it })
    FactsPanel(graph)
    ShareBox(graph)
    var textList by remember { mutableStateOf(false) }
    TextButton(onClick = { textList = !textList }) { Text(if (textList) "Hide edge list" else "Show edges as text") }
    if (textList) Text(if (graph.edges.isEmpty()) "No edges yet." else graph.edges.joinToString("   |   ") {
        "${nodeLabel(it.a)} ${if (it.directed) "→" else "—"} ${nodeLabel(it.b)} (${it.weight})"
    }, color = Muted, fontSize = 14.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteFinder(graph: DrawnGraph, from: Int?, to: Int?, route: Pair<List<Int>, Int>?, onFrom: (Int?) -> Unit, onTo: (Int?) -> Unit) {
    if (graph.size < 2) return
    Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CHEAPEST ROUTE (Dijkstra)", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("From:", color = Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until graph.size).forEach { v -> FilterChip(selected = from == v, onClick = { onFrom(if (from == v) null else v) }, label = { Text(nodeLabel(v)) }) }
            }
            Text("To:", color = Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until graph.size).forEach { v -> FilterChip(selected = to == v, onClick = { onTo(if (to == v) null else v) }, label = { Text(nodeLabel(v)) }) }
            }
            when {
                from == null || to == null -> Text("Pick a start and an end to light up the cheapest route.", color = Muted)
                route == null -> Text("There's no way from ${nodeLabel(from)} to ${nodeLabel(to)}${if (graph.anyDirected) ". Check the one-way arrows!" else ": they are in separate pieces."}", color = Warn)
                else -> Text("Cheapest route: ${route.first.joinToString(" → ") { nodeLabel(it) }}  ·  total cost ${route.second}", color = Color(0xFF4DD4C6), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FactsPanel(graph: DrawnGraph) {
    val f = remember(graph) { analyze(graph) }
    fun group(s: Set<Int>) = s.sorted().joinToString("") { nodeLabel(it) }
    Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("WHAT KIND OF GRAPH IS THIS?", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (f.vertices == 0) { Text("Empty so far. Switch to \"Add vertex\" and tap the board."); return@Column }
            Fact("Size", "${f.vertices} vertices and ${f.edges} edges.")
            Fact("Degrees", f.degrees.mapIndexed { i, d -> "${nodeLabel(i)} $d" }.joinToString(", ") +
                ". They add up to ${f.degrees.sum()} = 2 × ${f.edges} edges, because every edge has two ends.")
            Fact("Connected?", if (f.components.size == 1) "Yes: you can get from every vertex to every other (ignoring arrows)."
                else "No: it is in ${f.components.size} separate pieces: ${f.components.joinToString("  ") { "{${group(it)}}" }}.")
            Fact("Cycles", when {
                f.isTree -> "None. It's a TREE: connected with no cycles, so it has exactly ${f.vertices} − 1 = ${f.edges} edges."
                f.hasCycle -> "Yes, there is at least one round trip (cycle)."
                else -> "None. It's a FOREST (several trees)."
            })
            Fact("Two colors enough?", if (f.bipartite) "Yes: you can color the vertices with 2 colors so every edge joins different colors (bipartite)."
                else "No: there is a cycle of odd length, so neighbors would clash with only 2 colors.")
            if (f.directed) Fact("One-way groups", if (f.strongGroups.size == 1) "Every vertex can reach every other following the arrows: strongly connected."
                else "Groups where everyone can reach everyone (SCC): ${f.strongGroups.joinToString("  ") { "{${group(it)}}" }}.")
            val painted = graph.vertices.count { it.color != null }
            if (painted > 0) {
                val clashes = graph.colorClashes()
                val used = graph.vertices.mapNotNull { it.color }.toSet().size
                Fact("Your coloring", when {
                    clashes.isNotEmpty() -> "${clashes.size} clash(es): " + clashes.joinToString { "${nodeLabel(graph.edges[it].a)}–${nodeLabel(graph.edges[it].b)}" } + " join vertices of the same color."
                    painted < graph.size -> "$painted of ${graph.size} vertices painted, no clashes so far."
                    else -> "Proper: no joined vertices share a color. It uses $used paint(s)."
                })
            }
        }
    }
}

// ---------------- Play a game on this graph ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayPanel(session: SketchSession, graph: DrawnGraph, onPlay: (GameChoice, PlayableBoard) -> Unit) {
    if (graph.size == 0) return
    val start = session.start.coerceIn(0, graph.size - 1)
    val goal = (session.goal ?: (graph.size - 1)).coerceIn(0, graph.size - 1)
    var open by remember { mutableStateOf(true) }
    Surface(color = Color(0xFF243A2A), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PLAY A GAME ON YOUR GRAPH", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { open = !open }) { Text(if (open) "Hide" else "Show") }
            }
            if (!open) return@Column
            if (graph.size >= 2) {
                Text("Route games go from START to GOAL:", color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("START", color = Warn, modifier = Modifier.align(Alignment.CenterVertically))
                    (0 until graph.size).forEach { v -> FilterChip(selected = start == v, onClick = { session.start = v }, label = { Text(nodeLabel(v)) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("GOAL ", color = Warn, modifier = Modifier.align(Alignment.CenterVertically))
                    (0 until graph.size).forEach { v -> FilterChip(selected = goal == v, onClick = { session.goal = v }, label = { Text(nodeLabel(v)) }) }
                }
                if (start != 0 || goal != graph.size - 1) Text(
                    "In route games the letters are renumbered so START is A and GOAL is ${nodeLabel(graph.size - 1)}, like on every other board.",
                    color = Muted, fontSize = 13.sp)
            }
            allGameChoices.forEach { choice ->
                val problem = gameProblem(graph, start, goal, choice)
                val note = if (problem == null) gameNote(graph, choice) else null
                Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(choice.title, fontWeight = FontWeight.Bold, color = if (problem == null) Color.White else Muted)
                            Text(if (choice is GameChoice.Lesson) "Lesson · solo" else "Two players", color = Muted, fontSize = 12.sp)
                            problem?.let { Text(it, color = Warn, fontSize = 13.sp) }
                            note?.let { Text(it, color = Muted, fontSize = 13.sp) }
                        }
                        Button(enabled = problem == null, onClick = { onPlay(choice, playableBoard(graph, start, goal, choice)) }) { Text("Play") }
                    }
                }
            }
        }
    }
}

// ---------------- Share as text ----------------

@Composable
private fun ShareBox(graph: DrawnGraph) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide text version" else "Share as text (copy it, import it later)") }
    if (open) Surface(color = Color(0xFF1C2742), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Select and copy this, then use \"Import a graph from text\" on the drawing page:", color = Muted, fontSize = 13.sp)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(if (graph.size == 0) "(empty)" else graphToText(graph), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Fact(title: String, text: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text, color = Muted, lineHeight = 20.sp)
    }
}
