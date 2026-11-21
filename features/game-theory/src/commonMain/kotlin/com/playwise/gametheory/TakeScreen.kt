package com.playwise.gametheory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private fun movesText(allowed: List<Int>) = when {
    allowed == (1..allowed.size).toList() -> "take 1 to ${allowed.last()}"
    else -> "take " + allowed.dropLast(1).joinToString(", ") + " or " + allowed.last()
}

@Composable
fun TakeScreen(level: Int, onBack: () -> Unit, onWin: () -> Unit = {}) {
    var cap by remember(level) { mutableIntStateOf(3) }
    var match by remember(level) { mutableStateOf(newTakeMatch(level, cap)) }
    var selection by remember(level) { mutableStateOf<TakeMove?>(null) }
    var aiTook by remember(level) { mutableStateOf<TakeMove?>(null) }
    var feedback by remember(level) { mutableStateOf("") }
    var warning by remember(level) { mutableStateOf(false) }
    val title = when (level) { 3 -> "Take up to n"; 4 -> "Two mini-games"; else -> "Find the missing value" }

    fun newBoard() {
        match = newTakeMatch(level, cap, match.difficulty)
        selection = null; aiTook = null; feedback = ""
    }

    fun play() {
        val move = selection ?: return
        val afterYou = match.after(move).piles.map { it.stones }
        val next = match.play(move)
        aiTook = if (next.winner == "You") null else diffMove(afterYou, next.piles.map { it.stones })?.let { TakeMove(it.pile, it.count) }
        match = next
        selection = null; warning = false
        feedback = "You took ${move.count} from pile ${move.pile + 1}." +
            (aiTook?.let { " Then the computer took ${it.count} from pile ${it.pile + 1} (red circles)." } ?: "")
        if (next.winner == "You") onWin()
    }

    LevelHeader(onBack, "Level $level · $title", when (level) {
        3 -> "One pile, but you may only take a few stones at a time."
        4 -> "Two piles with different rules. Each turn, play in ONE of them."
        else -> "Two piles where you may take 1, 3 or 4. Find each pile's hidden value."
    })
    HowToPlay(listOf(
        "Tap a ball with a green ring: it shows a legal amount to take (that ball and the ones to its right).",
        "Press the orange button to take them.",
        "Whoever makes the last legal move wins!",
    ))
    Spacer(Modifier.height(14.dp))
    if (level == 3) {
        ChoiceChips("MOST YOU MAY TAKE (n)", (2..5).map { "$it" }, cap - 2) { cap = it + 2; newBoard() }
        Spacer(Modifier.height(10.dp))
    }
    OpponentChips(match.difficulty) { match = match.copy(difficulty = it) }
    Spacer(Modifier.height(16.dp))

    if (match.finished) ResultBanner(match.winner == "You") { newBoard() }
    else GameBanner("Your turn · round ${match.turns + 1}",
        selection?.let { "Take ${it.count} from pile ${it.pile + 1}, leaving ${match.piles[it.pile].stones - it.count}. Press the button to confirm." }
            ?: "Tap a ball with a green ring.")
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        match.piles.forEachIndexed { index, pile ->
            val allowed = match.allowed[index]
            BallPile(
                title = "Pile ${index + 1}",
                subtitle = "Rule: ${movesText(allowed)} stones",
                stones = pile.stones,
                selectedCount = selection?.takeIf { it.pile == index }?.count ?: 0,
                enabled = !match.finished,
                legalCounts = allowed,
                ghosts = aiTook?.takeIf { it.pile == index }?.let { listOf(Ghost(it.count, AiRed, "Computer took ${it.count}")) } ?: emptyList(),
                onTapBall = { count ->
                    if (count in allowed) {
                        selection = if (selection == TakeMove(index, count)) null else TakeMove(index, count)
                        feedback = ""; warning = false
                    } else {
                        feedback = "That would take $count. From pile ${index + 1} you may only ${movesText(allowed)}. Tap a ball with a green ring."
                        warning = true
                    }
                },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    FeedbackLine(feedback, warning)
    Spacer(Modifier.height(8.dp))
    if (!match.finished) ActionButton(
        selection?.let { "Take ${it.count} from pile ${it.pile + 1}" } ?: "Tap a ball first",
        enabled = selection != null, onClick = ::play)
    OutlinedButton(onClick = ::newBoard) { Text("New board") }
    Spacer(Modifier.height(20.dp))

    GrundyExplorer(match.piles.mapIndexed { i, p -> Triple("pile ${i + 1}", p.stones, match.allowed[i]) })
    Spacer(Modifier.height(16.dp))
    if (!match.finished) {
        val suggestion = match.bestMove()
        HintCard(
            if (suggestion.winning) "Take ${suggestion.move.count} from pile ${suggestion.move.pile + 1}. That leaves a board whose value is zero."
            else "This board's value is zero, so no move forces a win. Try taking ${suggestion.move.count} from pile ${suggestion.move.pile + 1}.",
            match.piles,
            onShow = { selection = suggestion.move; feedback = ""; warning = false })
        Spacer(Modifier.height(16.dp))
    }
    NimGuideView(when (level) { 3 -> "The repeating pattern"; 4 -> "Adding games together"; else -> "mex and Grundy values" }, nimGuide(level))
}
