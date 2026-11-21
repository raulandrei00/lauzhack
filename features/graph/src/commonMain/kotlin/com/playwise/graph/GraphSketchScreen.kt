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
}

/** Draw a graph by hand, let the app recognize it, then edit and explore the clean version. */
@Composable
fun GraphSketchScreen() {
    var strokes by remember { mutableStateOf(listOf<List<Pt>>()) }
    var boardDp by remember { mutableStateOf(400f to 300f) }
    var graph by remember { mutableStateOf<DrawnGraph?>(null) }
    val current = graph
    Text("Draw your own graph", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    if (current == null) {
        SketchPad(strokes, onStrokes = { strokes = it }, onSize = { boardDp = it },
            onConvert = { result -> graph = DrawnGraph.fromSketch(result, boardDp.first, boardDp.second) },
            onStartEmpty = { graph = DrawnGraph() })
    } else {
        GraphEditor(current, onChange = { graph = it }, onBackToDrawing = { graph = null })
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
private fun GraphEditor(graph: DrawnGraph, onChange: (DrawnGraph) -> Unit, onBackToDrawing: () -> Unit) {
    var history by remember { mutableStateOf(listOf<DrawnGraph>()) }
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
        history = (history + graph).takeLast(50)
        onChange(next)
    }
    fun clearSelection() { selVertex = null; selEdge = null; dragging = null }
    fun forgetVertex() { routeFrom = null; routeTo = null; clearSelection() }

    TurnBanner(Accent, "Your graph, cleaned up", tool.help)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tool.entries.forEach { t ->
            FilterChip(selected = tool == t, onClick = { tool = t; clearSelection(); message = "" }, label = { Text(t.title) })
        }
    }

    val route = if (routeFrom != null && routeTo != null) cheapestRoute(graph, routeFrom!!, routeTo!!) else null
    val routeSteps = route?.first?.zipWithNext()?.map { (a, b) -> minOf(a, b) to maxOf(a, b) }?.toSet() ?: emptySet()
    val weightsMatter = showWeights || graph.edges.any { it.weight != 1 }
    val many = graph.size > 12

    TapBoard(
        positions = graph.vertices.map { Offset(it.x, it.y) },
        nodes = graph.vertices.indices.map { i ->
            val canConnect = tool != Tool.ERASE && selVertex != null && selVertex != i && graph.edgeIndex(selVertex!!, i) < 0
            NodeLook(
                label = nodeLabel(i),
                fill = when { i == selVertex -> Accent; route?.first?.contains(i) == true -> Color(0xFF4DD4C6); else -> NodeIdle },
                textColor = if (i == selVertex || route?.first?.contains(i) == true) Color.Black else Color.White,
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
                color = when { k == selEdge -> Color.White; onRoute -> Color(0xFF4DD4C6); else -> EdgeIdle },
                width = if (k == selEdge || onRoute) 6.dp else 3.dp,
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
                selVertex == null -> { selVertex = i; selEdge = null; message = "Picked ${nodeLabel(i)}. Tap a glowing vertex to connect it, or drag to move." }
                selVertex == i -> selVertex = null
                else -> {
                    val a = selVertex!!
                    val existing = graph.edgeIndex(a, i)
                    if (existing >= 0) { selEdge = existing; selVertex = null }
                    else { commit(graph.addEdge(a, i)); message = "Connected ${nodeLabel(a)} and ${nodeLabel(i)}."; selVertex = null }
                }
            }
        },
        onEdgeTap = { k ->
            dragging = null
            if (tool == Tool.ERASE) { val e = graph.edges[k]; commit(graph.removeEdge(k)); clearSelection(); message = "Removed the line ${nodeLabel(e.a)}–${nodeLabel(e.b)}." }
            else { selEdge = k; selVertex = null; message = "" }
        },
        onNodeDrag = if (tool == Tool.ERASE) null else { i, p ->
            if (dragging != i) { history = (history + graph).takeLast(50); dragging = i }
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
        OutlinedButton(enabled = history.isNotEmpty(), onClick = {
            onChange(history.last()); history = history.dropLast(1); forgetVertex(); message = "Undone."
        }) { Text("Undo") }
        OutlinedButton(enabled = graph.size > 1, onClick = { commit(tidyArrangement(graph)); message = "Tidied: lines pull like springs, vertices push apart." }) { Text("Tidy up") }
        OutlinedButton(enabled = graph.size > 2, onClick = { commit(circleArrangement(graph)) }) { Text("Circle") }
        OutlinedButton(onClick = { showWeights = !showWeights }) { Text(if (weightsMatter) "Hide weights" else "Show weights") }
        OutlinedButton(onClick = onBackToDrawing) { Text("New drawing") }
    }
    if (weightsMatter && !showWeights && graph.edges.any { it.weight != 1 })
        Text("Weights stay visible while some line costs more or less than 1.", color = Muted, fontSize = 13.sp)

    RouteFinder(graph, routeFrom, routeTo, route, onFrom = { routeFrom = it }, onTo = { routeTo = it })
    FactsPanel(graph)
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
    val f = remember(graph.size, graph.edges) { analyze(graph) }
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
