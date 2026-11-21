package com.playwise.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ---------- Shared palette ----------

internal val BoardBackground = Color(0xFF172038)
internal val NodeIdle = Color(0xFF2B3A5E)
internal val EdgeIdle = Color(0xFF71829C)
internal val Accent = Color(0xFFB7F45B)
internal val Muted = Color(0xFFADB8D0)
internal val Danger = Color(0xFFFF6B6B)
internal val Warn = Color(0xFFFFAF74)
internal val PlayerColors = listOf(Color(0xFFB7F45B), Color(0xFF81A7FF))

/** Paints for the coloring games. Names are shown too, so color is never the only cue. */
internal val Paints = listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF4D96FF), Color(0xFFC77DFF))
internal val PaintNames = listOf("Red", "Yellow", "Blue", "Purple")

/** Group colors for claimed components. */
internal val GroupColors = listOf(Color(0xFF4DD4C6), Color(0xFFFF8FB1), Color(0xFFFFAF74), Color(0xFFB39DFF), Color(0xFF81A7FF), Color(0xFFFFD93D))

internal fun nodeLabel(node: Int) = ('A'.code + node).toChar().toString()

// ---------- Layouts (normalized 0..1 coordinates) ----------

/**
 * Convex hexagon: A on the left, F on the right. Convexity means no straight edge passes through
 * another vertex; the slight skew stops the three long diagonals meeting in one point, where
 * their cost labels would pile up.
 */
internal val sixNodeLayout = listOf(
    Offset(0f, .42f), Offset(.3f, 0f), Offset(.24f, 1f), Offset(.76f, 0f), Offset(.7f, 1f), Offset(1f, .58f)
)

internal fun circleLayout(n: Int): List<Offset> = List(n) { i ->
    val angle = PI + 2 * PI * i / n
    Offset((.5 + .5 * cos(angle)).toFloat(), (.5 + .5 * sin(angle)).toFloat())
}

internal fun gridLayout(columns: Int, rows: Int): List<Offset> = List(columns * rows) { i ->
    Offset((i % columns) / (columns - 1f), (i / columns) / (rows - 1f).coerceAtLeast(1f))
}

internal fun layoutFor(size: Int) = if (size == 6) sixNodeLayout else circleLayout(size)

// ---------- Board model ----------

internal data class NodeLook(
    val label: String,
    val fill: Color = NodeIdle,
    val textColor: Color = Color.White,
    /** Pulsing ring meaning "you can tap me". */
    val pulse: Color? = null,
    /** Steady ring, e.g. the selected vertex or a second group membership. */
    val ring: Color? = null,
    /** Small caption below the vertex (START, GOAL, a paint name...). */
    val badge: String? = null,
    val badgeColor: Color = Muted,
    /** Red cross: this vertex is about to disappear, or is stuck. */
    val crossed: Boolean = false,
    /** Removed vertex: drawn as a faint dashed outline. */
    val ghost: Boolean = false,
)

internal data class EdgeLook(
    val from: Int,
    val to: Int,
    val color: Color = EdgeIdle,
    val width: Dp = 3.dp,
    val dashed: Boolean = false,
    val arrow: Boolean = false,
    val label: String? = null,
    val labelColor: Color = Color.White,
    val hidden: Boolean = false,
    val tappable: Boolean = false,
)

internal data class BoxLook(val topLeft: Int, val bottomRight: Int, val color: Color, val label: String)

/**
 * An interactive graph drawing. Tap a vertex to call [onNodeTap] and tap near an edge to call
 * [onEdgeTap]. Vertex hits win over edge hits. Only edges marked tappable respond.
 */
@Composable
internal fun TapBoard(
    positions: List<Offset>,
    nodes: List<NodeLook>,
    edges: List<EdgeLook>,
    boxes: List<BoxLook> = emptyList(),
    aspectRatio: Float = 1.45f,
    nodeRadius: Dp = 26.dp,
    showLabels: Boolean = true,
    onNodeTap: ((Int) -> Unit)? = null,
    onEdgeTap: ((Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val pulse by rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val nodeTap by rememberUpdatedState(onNodeTap)
    val edgeTap by rememberUpdatedState(onEdgeTap)
    val currentEdges by rememberUpdatedState(edges)
    val currentNodes by rememberUpdatedState(nodes)
    val currentPositions by rememberUpdatedState(positions)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().aspectRatio(aspectRatio)
                .background(BoardBackground, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val pad = nodeRadius.toPx() + 26.dp.toPx()
                        val pts = currentPositions.map { toPx(it, size.width.toFloat(), size.height.toFloat(), pad) }
                        val nodeHandler = nodeTap
                        if (nodeHandler != null) {
                            val hitRadius = maxOf(nodeRadius.toPx() + 12.dp.toPx(), 26.dp.toPx())
                            val hit = pts.indices.filter { (pts[it] - tap).getDistance() <= hitRadius }
                                .minByOrNull { (pts[it] - tap).getDistance() }
                            if (hit != null && hit < currentNodes.size) { nodeHandler(hit); return@detectTapGestures }
                        }
                        val edgeHandler = edgeTap ?: return@detectTapGestures
                        val hit = currentEdges.indices.filter { currentEdges[it].tappable && !currentEdges[it].hidden }
                            .map { it to segmentDistance(tap, pts[currentEdges[it].from], pts[currentEdges[it].to]) }
                            .filter { it.second <= 22.dp.toPx() }
                            .minByOrNull { it.second }
                        if (hit != null) edgeHandler(hit.first)
                    }
                }
        ) {
            val r = nodeRadius.toPx()
            val pad = r + 26.dp.toPx()
            val pts = positions.map { toPx(it, size.width, size.height, pad) }

            for (box in boxes) {
                val a = pts[box.topLeft]; val b = pts[box.bottomRight]
                drawRoundRect(box.color.copy(alpha = .35f), a, Size(b.x - a.x, b.y - a.y), CornerRadius(6.dp.toPx()))
                centeredText(measurer, box.label, (a + b) / 2f, TextStyle(color = box.color, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }

            val placedLabels = mutableListOf<Offset>()
            val labelJobs = mutableListOf<Triple<Offset, String, Color>>()
            for (edge in edges) {
                if (edge.hidden) continue
                val a = pts[edge.from]; val b = pts[edge.to]
                val effect = if (edge.dashed) PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) else null
                if (edge.arrow) {
                    val delta = b - a
                    val unit = delta / delta.getDistance()
                    val normal = Offset(-unit.y, unit.x)
                    val tip = b - unit * (r + 3.dp.toPx())
                    val len = 16.dp.toPx(); val half = 8.dp.toPx()
                    drawLine(edge.color, a + unit * r, tip - unit * (len * .6f), edge.width.toPx(), pathEffect = effect)
                    drawPath(Path().apply {
                        moveTo(tip.x, tip.y)
                        val base = tip - unit * len
                        lineTo(base.x + normal.x * half, base.y + normal.y * half)
                        lineTo(base.x - normal.x * half, base.y - normal.y * half)
                        close()
                    }, edge.color)
                } else {
                    drawLine(edge.color, a, b, edge.width.toPx(), pathEffect = effect)
                }
                edge.label?.let { text ->
                    // Greedy placement: avoid other labels and, if possible, other roads, so each
                    // price clearly belongs to one road.
                    val candidates = listOf(.5f, .4f, .6f, .32f, .68f, .26f, .74f).map { a + (b - a) * it }
                    val free = candidates.filter { c -> placedLabels.none { (it - c).getDistance() < 30.dp.toPx() } }
                    val others = edges.filter { it !== edge && !it.hidden }
                    val spot = free.firstOrNull { c -> others.none { o -> segmentDistance(c, pts[o.from], pts[o.to]) < 13.dp.toPx() } }
                        ?: free.firstOrNull() ?: (a + b) / 2f
                    placedLabels += spot
                    labelJobs += Triple(spot, text, edge.labelColor)
                }
            }
            for ((spot, text, color) in labelJobs) {
                val layout = measurer.measure(text, TextStyle(color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                val w = layout.size.width + 14.dp.toPx(); val h = layout.size.height + 6.dp.toPx()
                drawRoundRect(Color(0xFF0E1428), Offset(spot.x - w / 2, spot.y - h / 2), Size(w, h), CornerRadius(h / 2))
                drawRoundRect(color.copy(alpha = .6f), Offset(spot.x - w / 2, spot.y - h / 2), Size(w, h), CornerRadius(h / 2), style = Stroke(1.5.dp.toPx()))
                drawText(layout, topLeft = Offset(spot.x - layout.size.width / 2f, spot.y - layout.size.height / 2f))
            }

            nodes.forEachIndexed { i, node ->
                val c = pts[i]
                if (node.ghost) {
                    drawCircle(Muted.copy(alpha = .35f), r * .8f, c, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
                    return@forEachIndexed
                }
                node.pulse?.let {
                    drawCircle(it.copy(alpha = .25f + .45f * pulse), r + 5.dp.toPx() + 5.dp.toPx() * pulse, c, style = Stroke(4.dp.toPx()))
                }
                drawCircle(node.fill, r, c)
                drawCircle(Color.White.copy(alpha = .25f), r, c, style = Stroke(2.dp.toPx()))
                node.ring?.let { drawCircle(it, r + 3.dp.toPx(), c, style = Stroke(4.dp.toPx())) }
                if (showLabels && node.label.isNotEmpty())
                    centeredText(measurer, node.label, c, TextStyle(color = node.textColor, fontSize = (r / density / 1.3f).coerceIn(10f, 22f).sp, fontWeight = FontWeight.Bold))
                if (node.crossed) {
                    val k = r * .75f
                    drawLine(Danger, c + Offset(-k, -k), c + Offset(k, k), 5.dp.toPx())
                    drawLine(Danger, c + Offset(-k, k), c + Offset(k, -k), 5.dp.toPx())
                }
                node.badge?.let {
                    centeredText(measurer, it, c + Offset(0f, r + 13.dp.toPx()), TextStyle(color = node.badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

private fun toPx(p: Offset, w: Float, h: Float, pad: Float) = Offset(pad + p.x * (w - 2 * pad), pad + p.y * (h - 2 * pad))

private fun segmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / len2).coerceIn(0f, 1f)
    val q = a + ab * t
    val d = p - q
    return sqrt(d.x * d.x + d.y * d.y)
}

private fun DrawScope.centeredText(measurer: androidx.compose.ui.text.TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

// ---------- Shared UI pieces ----------

/** Big colored "whose turn / what to do" strip. */
@Composable
internal fun TurnBanner(color: Color, title: String, instruction: String) {
    Surface(color = color.copy(alpha = .18f), shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).border(2.dp, color, RoundedCornerShape(16.dp))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).background(color, CircleShape))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                Text(instruction, fontSize = 17.sp, color = Color.White)
            }
        }
    }
}

internal enum class Tone { INFO, GOOD, BAD }

/** One line of feedback after each tap. */
@Composable
internal fun Feedback(text: String, tone: Tone) {
    if (text.isEmpty()) return
    val color = when (tone) { Tone.INFO -> Muted; Tone.GOOD -> Accent; Tone.BAD -> Warn }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text, color = color, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

/** Shown when a game or puzzle is finished. */
@Composable
internal fun ResultCard(title: String, detail: String, color: Color, buttonText: String, onAgain: () -> Unit) {
    Surface(color = color.copy(alpha = .2f), shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).border(3.dp, color, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(detail, fontSize = 17.sp, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = onAgain, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text(buttonText, color = Color.Black, fontSize = 18.sp)
            }
        }
    }
}

/** Round paint buttons with the color name written underneath. */
@Composable
internal fun PaintPicker(count: Int, selected: Int?, enabled: (Int) -> Boolean = { true }, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        (0 until count).forEach { c ->
            val on = enabled(c)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPick(c) }) {
                Box(Modifier.size(54.dp)
                    .background(Paints[c].copy(alpha = if (on) 1f else .3f), CircleShape)
                    .border(if (selected == c) 5.dp else 2.dp, if (selected == c) Color.White else Color.White.copy(alpha = .3f), CircleShape))
                Text(PaintNames[c] + if (selected == c) " ✓" else "", fontSize = 14.sp, color = if (on) Color.White else Muted)
            }
        }
    }
}

/** Accessible text version of the board, hidden by default. */
@Composable
internal fun RoadListToggle(graph: LessonGraph, showWeights: Boolean) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide road list" else "Show roads as text") }
    if (open) Text(graph.edges.joinToString("   |   ") {
        "${nodeLabel(it.from)} ${if (graph.directed) "→" else "—"} ${nodeLabel(it.to)}${if (showWeights) " (${it.weight})" else ""}"
    }, fontSize = 14.sp, color = Muted)
}
