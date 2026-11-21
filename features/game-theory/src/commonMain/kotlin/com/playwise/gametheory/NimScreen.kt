package com.playwise.gametheory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Works out what the computer took by comparing the board before and after its reply. */
internal fun diffMove(before: List<Int>, after: List<Int>): Move? =
    before.indices.firstOrNull { before[it] != after[it] }?.let { Move(it, before[it] - after[it]) }

@Composable
fun NimScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(NimMatch()) }
    var selection by remember { mutableStateOf<Move?>(null) }
    var aiTook by remember { mutableStateOf<Move?>(null) }
    var feedback by remember { mutableStateOf("") }
    var warning by remember { mutableStateOf(false) }
    var xorSelected by remember { mutableStateOf(emptySet<Int>()) }

    fun newBoard(level: BoardLevel = match.level) {
        match = newNimMatch(level, match.difficulty)
        selection = null; aiTook = null; feedback = ""; xorSelected = emptySet()
    }

    fun play() {
        val move = selection ?: return
        val afterYou = match.piles.map { it.stones }.toMutableList().also { it[move.pile] -= move.count }
        val next = match.play(move)
        aiTook = if (next.winner == "You") null else diffMove(afterYou, next.piles.map { it.stones })
        match = next
        selection = null
        warning = false
        feedback = "You took ${move.count} from pile ${move.pile + 1}." +
            (aiTook?.let { " Then the computer took ${it.count} from pile ${it.pile + 1} (red circles)." } ?: "")
        if (next.winner == "You") onWin()
    }

    LevelHeader(onBack, "Level 1 · Classic Nim", "A tiny game with a surprisingly deep strategy.")
    HowToPlay(listOf(
        "Tap a ball. That ball and every ball to its right turn orange.",
        "Press the orange button to take them. You may only take from ONE pile per turn.",
        "Then the computer moves. Whoever takes the very last ball wins!",
    ))
    Spacer(Modifier.height(14.dp))
    ChoiceChips("BOARD", BoardLevel.entries.map { it.title }, match.level.ordinal) { newBoard(BoardLevel.entries[it]) }
    Text(match.level.description, color = Muted)
    Spacer(Modifier.height(10.dp))
    OpponentChips(match.difficulty) { match = match.copy(difficulty = it) }
    Spacer(Modifier.height(16.dp))

    if (match.finished) ResultBanner(match.winner == "You") { newBoard() }
    else GameBanner("Your turn · round ${match.turns + 1}",
        selection?.let { "Taking ${it.count} from pile ${it.pile + 1} will leave ${match.piles[it.pile].stones - it.count}. Press the button to confirm, or tap another ball." }
            ?: "Tap a ball in any pile to choose how many to take.")
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        match.piles.forEachIndexed { index, pile ->
            BallPile(
                title = "Pile ${index + 1}",
                stones = pile.stones,
                selectedCount = selection?.takeIf { it.pile == index }?.count ?: 0,
                enabled = !match.finished,
                ghosts = aiTook?.takeIf { it.pile == index }?.let { listOf(Ghost(it.count, AiRed, "Computer took ${it.count}")) } ?: emptyList(),
                onTapBall = { count ->
                    selection = if (selection == Move(index, count)) null else Move(index, count)
                    feedback = ""
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
    OutlinedButton(onClick = { newBoard() }) { Text("New board") }
    Spacer(Modifier.height(20.dp))

    XorWorkbench(
        piles = match.piles,
        selectedPositions = xorSelected,
        onToggle = { p -> xorSelected = if (p in xorSelected) xorSelected - p else xorSelected + p },
        labelFor = { "Pile ${it.position + 1}" }
    )
    Spacer(Modifier.height(16.dp))
    if (!match.finished) {
        val (move, winning) = optimalNimMove(match.piles.map { it.stones })
        HintCard(
            if (winning) "Take ${move.count} from pile ${move.pile + 1}. Then the pile sizes XOR to zero, a losing position for the computer."
            else "The XOR is already zero: no move forces a win. Take 1 from pile ${move.pile + 1} and hope the computer slips.",
            match.piles,
            onShow = { selection = move; feedback = "" })
        Spacer(Modifier.height(16.dp))
    }
    NimGuideView("Why XOR wins Nim", nimGuide(1))
}
