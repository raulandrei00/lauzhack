package com.playwise.gametheory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------- Lesson guide ----------------

/** Collapsible lesson. Closed by default so levels that are about discovery are not spoiled. */
@Composable
fun NimGuideView(title: String, guide: NimGuide, startOpen: Boolean = false) {
    var open by remember(title) { mutableStateOf(startOpen) }
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LEARN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { open = !open }, colors = ButtonDefaults.buttonColors(containerColor = PanelLight)) {
                Text(if (open) "Close the lesson" else "Open the lesson", color = Color.White)
            }
            if (open) {
                Text(guide.summary, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp,
                    modifier = Modifier.padding(vertical = 8.dp))
                Section("The idea", true) { guide.intuition.forEach { Body(it) } }
                Section("Words to know", false) {
                    guide.terms.forEach { (t, d) ->
                        Column { Text(t, color = Color.White, fontWeight = FontWeight.Bold); Text(d, color = Muted, lineHeight = 20.sp) }
                    }
                }
                Section("Worked example: ${guide.exampleTitle}", true) {
                    guide.exampleSteps.forEachIndexed { i, s -> Body("${i + 1}. $s") }
                }
                Section("Try this on the board", false) { guide.tryThis.forEach { Body("• $it") } }
                Section("Pseudocode", false) { Code(guide.pseudocode) }
                Section("Why it works", false) { Body(guide.whyItWorks) }
                Section("Common mistakes", false) { guide.pitfalls.forEach { Body("• $it") } }
                Section("Check yourself (${guide.quiz.size} questions)", false) { guide.quiz.forEach { Quiz(it) } }
                Section("Learn more", false) { guide.resources.forEach { ResourceLink(it) } }
                Section("Where this is in the code", false) { Body(guide.inThisApp) }
            }
        }
    }
}

@Composable
private fun Section(title: String, startOpen: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var open by remember(title) { mutableStateOf(startOpen) }
    TextButton(onClick = { open = !open }, contentPadding = PaddingValues(0.dp)) {
        Text("${if (open) "–" else "+"}  $title", color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    if (open) Column(Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun Body(text: String) = Text(text, color = Color.White, lineHeight = 22.sp)

@Composable
private fun Code(code: String) {
    Box(Modifier.fillMaxWidth().background(Background, RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState()).padding(14.dp)) {
        Text(code, color = Color(0xFFDCE6FF), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp, softWrap = false)
    }
}

@Composable
private fun Quiz(item: NimQuiz) {
    var shown by remember(item) { mutableStateOf(false) }
    Surface(color = PanelLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Q: ${item.question}", color = Color.White, lineHeight = 21.sp)
            if (shown) Text("A: ${item.answer}", color = Accent, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = { shown = !shown }) { Text(if (shown) "Hide answer" else "Think first, then reveal") }
        }
    }
}

@Composable
private fun ResourceLink(r: NimResource) {
    val uri = LocalUriHandler.current
    var failed by remember(r) { mutableStateOf(false) }
    Column {
        Text(r.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(r.note, color = Muted, lineHeight = 20.sp)
        if (r.url.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { failed = runCatching { uri.openUri(r.url) }.isFailure }) { Text("Open") }
                Text(r.url, color = Muted, fontSize = 12.sp)
            }
            if (failed) Text("Could not open a browser. Copy the address above.", color = TakeOrange, fontSize = 12.sp)
        } else Text("Book or paper: look for it in a library.", color = Muted, fontSize = 12.sp)
    }
}

/** "Start here" page on the journey: winning and losing positions. */
@Composable
fun NimPrimerScreen(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) { Text("← Back to the journey") }
    Spacer(Modifier.height(16.dp))
    Text("Winning and losing positions", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Text("The one idea behind every game in this journey.", color = Muted)
    Spacer(Modifier.height(16.dp))
    NimGuideView("Game theory basics", nimPrimer, startOpen = true)
}

// ---------------- Binary XOR breakdown ----------------

/** Shows numbers in binary, one under another, and their XOR, so the column rule is visible. */
@Composable
internal fun BinaryBreakdown(rows: List<Pair<String, Int>>) {
    if (rows.isEmpty()) return
    val result = rows.fold(0) { acc, r -> acc xor r.second }
    val width = maxOf(1, (rows.map { it.second } + result).maxOf { 32 - it.countLeadingZeroBits() })
    val labelWidth = rows.maxOf { it.first.length }.coerceAtLeast(6)
    fun bits(n: Int) = n.toString(2).padStart(width, '0')
    val oddColumns = (0 until width).filter { c -> rows.count { bits(it.second)[c] == '1' } % 2 == 1 }.toSet()
    Box(Modifier.fillMaxWidth().background(Background, RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        Column {
            rows.forEach { (label, n) ->
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Muted)) { append(label.padEnd(labelWidth + 2)) }
                    bits(n).forEach { ch -> withStyle(SpanStyle(color = if (ch == '1') StoneBlue else Muted.copy(alpha = .5f))) { append(ch); append(' ') } }
                    withStyle(SpanStyle(color = Muted)) { append("  = $n") }
                }, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            }
            Text("-".repeat(labelWidth + 2 + width * 2 + 6), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) { append("XOR".padEnd(labelWidth + 2)) }
                bits(result).forEachIndexed { c, ch ->
                    withStyle(SpanStyle(color = if (c in oddColumns) TakeOrange else Muted, fontWeight = FontWeight.Bold)) { append(ch); append(' ') }
                }
                withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) { append("  = $result") }
            }, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        }
    }
    Text("Each column of the answer is 1 when that column has an odd number of 1s (orange), and 0 when it has an even number.",
        color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
}

// ---------------- Grundy table explorer ----------------

/**
 * Builds g(0), g(1), ... one value at a time for each pile rule, explaining every mex.
 * [piles] pairs each pile's label and current size with its allowed takes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GrundyExplorer(piles: List<Triple<String, Int, List<Int>>>, maxSize: Int = 16) {
    val rules = piles.map { it.third }.distinct()
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GRUNDY TABLE: WORK IT OUT STEP BY STEP", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("g(size) = the smallest number NOT among the values you can reach in one move (mex). Predict each value before you reveal it!",
                color = Muted, fontSize = 14.sp, lineHeight = 19.sp)
            val revealed = remember(rules) { mutableStateListOf(*Array(rules.size) { -1 }) }
            rules.forEachIndexed { r, allowed ->
                val here = piles.filter { it.third == allowed }
                Text("Moves: take ${allowed.joinToString(", ")}" + "   (${here.joinToString { it.first }})",
                    color = Color.White, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..maxSize).forEach { s ->
                        val known = s <= revealed[r]
                        val g = if (known) grundy(s, allowed) else -1
                        val isPile = here.any { it.second == s }
                        Column(
                            Modifier.width(38.dp)
                                .background(if (known && g == 0) AiRed.copy(alpha = .25f) else PanelLight, RoundedCornerShape(8.dp))
                                .border(if (isPile) 2.dp else 0.dp, if (isPile) TakeOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$s", color = Muted, fontSize = 11.sp)
                            Text(if (known) "$g" else "?", color = if (known) Color.White else Muted, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    }
                }
                val last = revealed[r]
                if (last >= 1) {
                    val moves = allowed.filter { it <= last }
                    val reach = moves.map { grundy(last - it, allowed) }
                    Text("g($last): you can reach sizes ${moves.joinToString { "${last - it}" }} with values {${reach.distinct().sorted().joinToString()}}, so mex = ${grundy(last, allowed)}.",
                        color = Accent, fontSize = 14.sp, lineHeight = 19.sp)
                } else if (last == 0) Text("g(0) = 0: no moves are possible, so the set is empty and mex{} = 0.", color = Accent, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = revealed[r] < maxSize, onClick = { revealed[r] = revealed[r] + 1 }) {
                        Text(if (revealed[r] < 0) "Start: g(0)" else "Next: g(${revealed[r] + 1})")
                    }
                    OutlinedButton(onClick = { revealed[r] = maxSize }) { Text("Fill all") }
                    OutlinedButton(onClick = { revealed[r] = -1 }) { Text("Reset") }
                }
            }
            Text("Red boxes have value 0: those sizes are losing. Orange borders mark the pile sizes on the board right now.",
                color = Muted, fontSize = 13.sp)
            val allKnown = piles.all { p -> p.second <= revealed[rules.indexOf(p.third)] }
            if (allKnown && piles.size > 1) {
                val values = piles.map { grundy(it.second, it.third) }
                val x = values.fold(0, Int::xor)
                Text("Board value: ${values.joinToString(" xor ")} = $x  →  ${if (x == 0) "losing for the player to move" else "winning for the player to move"}",
                    color = Color.White, fontWeight = FontWeight.Bold)
            } else if (allKnown) {
                val v = grundy(piles[0].second, piles[0].third)
                Text("Board value: $v  →  ${if (v == 0) "losing for the player to move" else "winning for the player to move"}",
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
