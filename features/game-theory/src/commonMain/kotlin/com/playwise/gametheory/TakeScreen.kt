package com.playwise.gametheory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TakeScreen(level: Int, onBack: () -> Unit, onWin: () -> Unit = {}) {
    var cap by remember(level) { mutableIntStateOf(3) }
    var match by remember(level) { mutableStateOf(newTakeMatch(level, cap)) }
    var selectedPile by remember(level) { mutableIntStateOf(0) }
    var selectedCount by remember(level) { mutableIntStateOf(1) }
    val title = when (level) { 3 -> "Take up to n"; 4 -> "Two mini-games"; else -> "Find the missing value" }

    OutlinedButton(onClick = onBack) { Text("All game-theory levels") }
    Spacer(Modifier.height(18.dp))
    Text("Level $level · $title", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(when (level) {
        3 -> "Take 1 through n stones from a single pile."
        4 -> "Choose one of two independent take-away games each turn."
        else -> "Work out the missing value from the moves 1, 3, and 4."
    }, color = Muted)
    Spacer(Modifier.height(20.dp))

    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (level == 3) {
                Text("Maximum take: $cap", color = Accent, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..5).forEach { n ->
                        OutlinedButton(onClick = {
                            cap = n; match = newTakeMatch(level, cap, match.difficulty)
                            selectedPile = 0; selectedCount = 1
                        }) { Text("$n") }
                    }
                }
            }
            Text(if (match.finished) "GAME COMPLETE" else "YOUR MOVE · ROUND ${match.turns + 1}", color = Accent)
            match.piles.forEachIndexed { index, pile ->
                val legal = match.allowed[index].filter { it <= pile.stones }
                Surface(shape = RoundedCornerShape(12.dp), color = if (selectedPile == index) PanelLight else Panel,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !match.finished && legal.isNotEmpty()) {
                        selectedPile = index; selectedCount = legal.first()
                    }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pile ${index + 1}: ${pile.stones} stones", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("You may take ${match.allowed[index].joinToString(", ")}", color = Muted)
                    }
                }
            }
            Text(match.lastMove, color = Color.White)
            if (!match.finished) {
                Text("Take from pile ${selectedPile + 1}", color = Accent)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    match.allowed[selectedPile].filter { it <= match.piles[selectedPile].stones }.forEach { amount ->
                        OutlinedButton(onClick = { selectedCount = amount }) {
                            Text(if (selectedCount == amount) "● $amount" else "$amount")
                        }
                    }
                }
                Button(onClick = {
                    match = match.play(TakeMove(selectedPile, selectedCount))
                    if (match.winner == "You") onWin()
                    selectedPile = match.piles.indices.firstOrNull { i -> match.allowed[i].any { it <= match.piles[i].stones } } ?: 0
                    selectedCount = match.allowed[selectedPile].first()
                }, enabled = TakeMove(selectedPile, selectedCount) in match.legalMoves()) {
                    Text("Take $selectedCount")
                }
            } else Text(if (match.winner == "You") "You won level $level!" else "Try another board.", color = Accent)
            OutlinedButton(onClick = {
                match = newTakeMatch(level, cap, match.difficulty); selectedPile = 0; selectedCount = 1
            }) { Text("New board") }
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
        val suggestion = match.bestMove()
        HintReveal(
            if (suggestion.winning) "Take ${suggestion.move.count} from pile ${suggestion.move.pile + 1}. This leaves the combined Grundy XOR at zero."
            else "No move forces a win against perfect play. Try taking ${suggestion.move.count} from pile ${suggestion.move.pile + 1}.",
            match.piles
        )
        Spacer(Modifier.height(16.dp))
    }
    val theory = takeTheory(level)
    TheoryReveal("level $level theory", theory.first, theory.second, theory.third)
}

private fun takeTheory(level: Int): Triple<String, String, String> = when (level) {
    3 -> Triple(
        "Losing pile sizes repeat every n + 1 stones: 0, n + 1, 2(n + 1), and so on.",
        "At a multiple of n + 1, removing 1 through n lands on a different remainder. From any other remainder r, removing r lands on a multiple. By induction from the empty pile, multiples are losing and all other sizes are winning. The Grundy value is size mod (n + 1): the reachable values are all other residues 0 through n, so their smallest missing value is the current residue.",
        "Divide the pile size by n + 1. If the remainder r is positive, take r. If it is zero, no forced winning move exists."
    )
    4 -> Triple(
        "Each mini-game has its own value. XOR the two values to find the value of the combined game.",
        "Each pile's Grundy value is its size mod (cap + 1), by the previous level. A move changes exactly one mini-game. From combined XOR zero, changing one value cannot leave zero. From nonzero XOR x, the highest set bit of x identifies a component whose value can be lowered to value xor x. Grundy values have moves to every smaller value, so that move exists. Induction on remaining stones proves zero is losing.",
        "Compute each pile's remainder, then XOR them. Try every legal move on either pile and choose one that makes the new XOR zero. If none does, the position has no forced win."
    )
    else -> Triple(
        "A pile can have a value other than its size. Find the smallest nonnegative value missing from its possible next positions: mex.",
        "Let g(0) = 0. For s > 0, g(s) = mex of g(s - 1), g(s - 3), and g(s - 4), using only legal removals. If g(s) = 0, no move reaches zero; if g(s) > 0, zero is among the reachable values. This proves the single-pile winning rule by induction. For two independent piles, the Sprague-Grundy XOR rule gives their combined value.",
        "Build a table from g(0) upward. For each pile and each legal removal, calculate the next Grundy values and XOR them. Play a move that leaves XOR zero when one exists."
    )
}
