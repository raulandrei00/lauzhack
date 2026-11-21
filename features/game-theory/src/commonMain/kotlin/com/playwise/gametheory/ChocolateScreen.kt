package com.playwise.gametheory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Chocolate = Color(0xFF7A563B)
private val ChocolateLight = Color(0xFF9A6E4C)
private val Poison = Color(0xFFB94B64)

private fun squares(board: ChocolateBoard): Set<Pair<Int, Int>> =
    board.heights.indices.flatMap { c -> (0 until board.heights[c]).map { c to it } }.toSet()

@Composable
fun ChocolateScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(newChocolateMatch()) }
    var bar by remember { mutableStateOf(match.board) }           // the full bar, for drawing eaten spots
    var selected by remember { mutableStateOf<Bite?>(null) }
    var aiEaten by remember { mutableStateOf(emptySet<Pair<Int, Int>>()) }
    var feedback by remember { mutableStateOf("") }
    var warning by remember { mutableStateOf(false) }

    fun newBar() {
        match = newChocolateMatch(match.difficulty); bar = match.board
        selected = null; aiEaten = emptySet(); feedback = ""
    }

    fun play() {
        val bite = selected ?: return
        val afterYou = match.board.bite(bite)
        val next = match.play(bite)
        aiEaten = if (next.winner == "You") emptySet() else squares(afterYou) - squares(next.board)
        val ate = squares(match.board).size - squares(afterYou).size
        match = next
        selected = null; warning = false
        feedback = "You bit column ${bite.column + 1}, row ${bite.row + 1} and ate $ate square(s)." +
            (if (aiEaten.isNotEmpty()) " The computer then ate ${aiEaten.size} (red outlines)." else "")
        if (next.winner == "You") onWin()
    }

    LevelHeader(onBack, "Level 6 · Chocolate bar", "Bite carefully: the top-left square is poisoned.")
    HowToPlay(listOf(
        "Tap a chocolate square. It and every square to its right and below light up orange: that's your bite.",
        "Tap it again (or press the button) to eat them.",
        "Never the poison! Whoever leaves the other player only the poison wins.",
    ))
    Spacer(Modifier.height(14.dp))
    OpponentChips(match.difficulty) { match = match.copy(difficulty = it) }
    Spacer(Modifier.height(16.dp))

    if (match.finished) ResultBanner(match.winner == "You") { newBar() }
    else GameBanner("Your turn · round ${match.turns + 1}",
        selected?.let { "This bite eats the orange squares. Tap the white-edged square again to confirm." }
            ?: "Tap any chocolate square except the poison.")
    Spacer(Modifier.height(12.dp))

    val present = squares(match.board)
    val sel = selected
    val preview = if (sel == null) emptySet() else present.filter { it.first >= sel.column && it.second >= sel.row }.toSet()
    Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(Modifier.width(26.dp))
                bar.heights.indices.forEach { c ->
                    Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) { Text("${c + 1}", color = Muted, fontSize = 12.sp) }
                }
            }
            for (row in 0 until (bar.heights.maxOrNull() ?: 1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${row + 1}", color = Muted, fontSize = 12.sp, modifier = Modifier.width(26.dp))
                    bar.heights.indices.forEach { column ->
                        val spot = column to row
                        val poison = column == 0 && row == 0
                        val here = spot in present
                        val eatenByAi = spot in aiEaten
                        val inBar = row < bar.heights[column]
                        Box(
                            Modifier.padding(4.dp).size(56.dp)
                                .then(when {
                                    here && poison -> Modifier.background(Poison, RoundedCornerShape(8.dp))
                                    here && spot in preview -> Modifier.background(TakeOrange, RoundedCornerShape(8.dp))
                                    here -> Modifier.background(Chocolate, RoundedCornerShape(8.dp))
                                    eatenByAi -> Modifier.border(2.dp, AiRed, RoundedCornerShape(8.dp))
                                    inBar -> Modifier.border(1.dp, Muted.copy(alpha = .25f), RoundedCornerShape(8.dp))
                                    else -> Modifier
                                })
                                .border(if (sel == Bite(column, row)) 3.dp else 0.dp, if (sel == Bite(column, row)) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable(enabled = !match.finished && here) {
                                    when {
                                        poison -> { feedback = "That's the poison! It can never be bitten."; warning = true }
                                        selected == Bite(column, row) -> play()
                                        else -> { selected = Bite(column, row); feedback = ""; warning = false }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                here && poison -> Text("POISON", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                here && spot !in preview -> Box(Modifier.size(30.dp).background(ChocolateLight, RoundedCornerShape(6.dp)))
                                eatenByAi -> Text("AI", color = AiRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    FeedbackLine(feedback, warning)
    Spacer(Modifier.height(8.dp))
    if (!match.finished) ActionButton(
        sel?.let { "Bite: eat ${preview.size} square(s)" } ?: "Tap a square first",
        enabled = sel != null, onClick = ::play)
    OutlinedButton(onClick = ::newBar) { Text("New bar") }
    Spacer(Modifier.height(20.dp))
    if (!match.finished) {
        val (move, winning) = match.board.bestBite()
        HintCard(if (winning) "Bite column ${move.column + 1}, row ${move.row + 1}. The shape left behind has Grundy value 0, so the computer is in a losing position."
            else "Every bite leaves a winning shape for the computer. Try column ${move.column + 1}, row ${move.row + 1} and hope it slips.",
            match.board,
            onShow = { selected = move; feedback = "" })
        Spacer(Modifier.height(16.dp))
    }
    NimGuideView("Valuing any game with mex", nimGuide(6))
}
