package com.playwise.gametheory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChocolateScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(newChocolateMatch()) }
    var selected by remember { mutableStateOf<Bite?>(null) }
    OutlinedButton(onClick = onBack) { Text("All game-theory levels") }
    Spacer(Modifier.height(18.dp))
    Text("Level 6 · Chocolate bar SG", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Text("Choose a safe square to bite. The top-left square is poisoned.", color = Muted)
    Spacer(Modifier.height(20.dp))
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (match.finished) "GAME COMPLETE" else "YOUR MOVE · ROUND ${match.turns + 1}", color = Accent)
            val rows = match.board.heights.maxOrNull() ?: 1
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    match.board.heights.indices.forEach { column ->
                        val bite = Bite(column, row)
                        val present = row < match.board.heights[column]
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = when {
                                !present -> PanelLight
                                column == 0 && row == 0 -> Color(0xFFB94B64)
                                selected == bite -> Accent
                                else -> Color(0xFF7A563B)
                            }, modifier = Modifier.size(56.dp).clickable(enabled = !match.finished && bite in match.board.legalMoves()) {
                                selected = bite
                            }) {
                            Text(when { !present -> ""; column == 0 && row == 0 -> "☠"; else -> "■" },
                                color = Color.White, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            Text(match.lastMove, color = Color.White)
            if (!match.finished) {
                Button(onClick = {
                    val bite = selected ?: return@Button
                    match = match.play(bite)
                    if (match.winner == "You") onWin()
                    selected = null
                }, enabled = selected in match.board.legalMoves()) {
                    Text(if (selected == null) "Select a safe square" else "Bite selected square")
                }
            } else Text(if (match.winner == "You") "You won level 6!" else "Try another bar.", color = Accent)
            OutlinedButton(onClick = { match = newChocolateMatch(match.difficulty); selected = null }) { Text("New bar") }
        }
    }
    Spacer(Modifier.height(16.dp))
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("OPPONENT", color = Accent, fontWeight = FontWeight.Bold)
            Difficulty.entries.forEach { option ->
                OutlinedButton(onClick = { match = match.copy(difficulty = option) }) {
                    Text(if (match.difficulty == option) "● ${option.title}" else option.title)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (!match.finished) {
        val (move, winning) = match.board.bestBite()
        HintReveal(if (winning) "Bite column ${move.column + 1}, row ${move.row + 1}. Its remaining board has Grundy value zero."
            else "No forced win is available against perfect play. Try column ${move.column + 1}, row ${move.row + 1}.", match.board)
        Spacer(Modifier.height(16.dp))
    }
    TheoryReveal(
        "level 6 theory",
        "Each bite leaves a smaller chocolate shape. Give each shape a value based on the shapes reachable in one bite.",
        "The poisoned square cannot be chosen, so the board with only that square has no legal moves and value 0. For every other shape B, define g(B) as the smallest nonnegative integer absent from the values of its legal successor shapes. If g(B) = 0, no move reaches 0. If g(B) > 0, 0 is among the successors. Each bite removes at least one square, so induction on the number of safe squares proves that value 0 means a losing position. This is the Sprague-Grundy recursion; it also works when this board is combined with another game by XORing their values.",
        "List each safe bite, compute the resulting shape's Grundy value recursively with a cache, and choose a bite leading to value 0. If no such bite exists, perfect play can defeat any move."
    )
}
