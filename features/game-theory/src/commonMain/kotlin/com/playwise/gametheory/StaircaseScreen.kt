package com.playwise.gametheory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private fun stepName(step: Int) = if (step == 0) "the floor" else "step $step"

@Composable
fun StaircaseScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(newStaircaseMatch()) }
    var selection by remember { mutableStateOf<StairMove?>(null) }
    var aiMove by remember { mutableStateOf<StairMove?>(null) }
    var feedback by remember { mutableStateOf("") }
    var xorSelected by remember { mutableStateOf(emptySet<Int>()) }

    fun newBoard() {
        match = newStaircaseMatch(match.difficulty)
        selection = null; aiMove = null; feedback = ""; xorSelected = emptySet()
    }

    fun play() {
        val move = selection ?: return
        val afterYou = match.steps.map { it.stones }.toMutableList().also { it[move.step] -= move.count; it[move.step - 1] += move.count }
        val next = match.play(move)
        val after = next.steps.map { it.stones }
        aiMove = if (next.winner == "You") null
            else (afterYou.lastIndex downTo 1).firstOrNull { after[it] < afterYou[it] }?.let { StairMove(it, afterYou[it] - after[it]) }
        match = next
        selection = null
        feedback = "You slid ${move.count} from step ${move.step} to ${stepName(move.step - 1)}." +
            (aiMove?.let { " The computer slid ${it.count} from step ${it.step} to ${stepName(it.step - 1)} (red circles)." } ?: "")
        if (next.winner == "You") onWin()
    }

    LevelHeader(onBack, "Level 2 · Staircase Nim", "The coins move, but only some steps decide the game.")
    HowToPlay(listOf(
        "Tap a coin on any step above the floor. It and every coin to its right turn orange.",
        "Press the button: those coins slide down exactly ONE step. Coins on the floor never move again.",
        "Whoever makes the last move wins!",
    ))
    Spacer(Modifier.height(14.dp))
    OpponentChips(match.difficulty) { match = match.copy(difficulty = it) }
    Spacer(Modifier.height(16.dp))

    if (match.finished) ResultBanner(match.winner == "You") { newBoard() }
    else GameBanner("Your turn · round ${match.turns + 1}",
        selection?.let { "Slide ${it.count} from step ${it.step} down to ${stepName(it.step - 1)}. The dashed orange circles show where they land." }
            ?: "Tap a coin on a step to choose how many coins slide down.")
    Spacer(Modifier.height(12.dp))
    val top = match.steps.lastIndex
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (step in top downTo 0) {
            val sel = selection
            val ghosts = buildList {
                if (sel != null && sel.step - 1 == step) add(Ghost(sel.count, TakeOrange, "+${sel.count} will land here"))
                aiMove?.takeIf { it.step == step }?.let { add(Ghost(it.count, AiRed, "Computer slid ${it.count} down from here")) }
            }
            BallPile(
                title = if (step == 0) "Floor" else "Step $step",
                subtitle = if (step == 0) "Coins here are out of the game." else null,
                stones = match.steps[step].stones,
                selectedCount = sel?.takeIf { it.step == step }?.count ?: 0,
                enabled = !match.finished && step > 0,
                ghosts = ghosts,
                indent = (step * 14).dp,
                onTapBall = { count ->
                    selection = if (selection == StairMove(step, count)) null else StairMove(step, count)
                    feedback = ""
                },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    FeedbackLine(feedback)
    Spacer(Modifier.height(8.dp))
    if (!match.finished) ActionButton(
        selection?.let { "Slide ${it.count} down to ${stepName(it.step - 1)}" } ?: "Tap a coin first",
        enabled = selection != null, onClick = ::play)
    OutlinedButton(onClick = ::newBoard) { Text("New staircase") }
    Spacer(Modifier.height(20.dp))

    XorWorkbench(
        piles = match.steps,
        selectedPositions = xorSelected,
        onToggle = { p -> xorSelected = if (p in xorSelected) xorSelected - p else xorSelected + p },
        labelFor = { if (it.position == 0) "Floor" else "Step ${it.position}" }
    )
    Text("Discovery: which group of steps has XOR 0 after every move the GTO computer makes? Test your idea before opening the hint or the lesson.",
        color = Muted, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(16.dp))
    if (!match.finished) {
        val (move, winning) = optimalStairMove(match.steps.map { it.stones })
        HintCard(
            if (winning) "Slide ${move.count} coin(s) from step ${move.step} to ${stepName(move.step - 1)}. That makes the XOR of the ODD steps zero."
            else "The odd steps already XOR to zero, so no move forces a win. Try sliding 1 coin from step ${move.step}.",
            match.steps,
            onShow = { selection = move; feedback = "" })
        Spacer(Modifier.height(16.dp))
    }
    NimGuideView("The secret of the staircase (spoiler!)", nimGuide(2))
}
