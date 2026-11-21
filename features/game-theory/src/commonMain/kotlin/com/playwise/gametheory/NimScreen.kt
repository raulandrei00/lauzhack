package com.playwise.gametheory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
private fun StrategyPanel() {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("AFTER YOU PLAY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("The winning idea", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("XOR the pile sizes. A zero result is a losing position for the player whose turn is next, if the other player responds perfectly.",
                color = Muted, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            Text("Beginner example: 3 xor 4 xor 5 = 2. Take 2 from pile 1 to leave 1, 4, 5 — their XOR is zero.",
                color = Color.White, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            Text("In binary, 3 = 011, 4 = 100, and 5 = 101. XOR marks a bit only when that column has an odd number of 1s, so the score is 010 (2).",
                color = Muted, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text("Use the calculator to select all piles, make a move, and compare the new score. An optimal response aims to restore zero after every opponent move.",
                color = Color.White, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text("Implementation idea: calculate the XOR, then try each legal move until the remaining piles XOR to zero.",
                color = Accent, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
fun NimScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(NimMatch()) }
    var selectedPile by remember { mutableIntStateOf(0) }
    var take by remember { mutableIntStateOf(1) }
    var xorSelected by remember { mutableStateOf(emptySet<Int>()) }
    val reset: (BoardLevel) -> Unit = { level ->
        match = newNimMatch(level, match.difficulty)
        selectedPile = 0
        take = 1
        xorSelected = emptySet()
    }
    val play: () -> Unit = {
        match = match.play(Move(selectedPile, take))
        if (match.winner == "You") onWin()
        selectedPile = match.piles.indexOfFirst { it.stones > 0 }.coerceAtLeast(0)
        take = 1
    }

    Text("←  All paths", color = Muted, modifier = Modifier.clickable { onBack() })
    Spacer(Modifier.height(18.dp))
    Text("Nim", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Text("A tiny game with a surprisingly deep strategy.", color = Muted, fontSize = 16.sp)
    Spacer(Modifier.height(24.dp))

    BoxWithConstraints {
        if (maxWidth < 760.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GamePanel(match, selectedPile, take,
                    onSelect = { selectedPile = it; take = 1 },
                    onTake = { take = it },
                    onPlay = play,
                    onReset = { reset(match.level) }
                )
                SidePanel(match.level, match.difficulty,
                    onLevel = reset,
                    onDifficulty = { match = match.copy(difficulty = it) })
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1.6f)) {
                    GamePanel(match, selectedPile, take,
                        onSelect = { selectedPile = it; take = 1 },
                        onTake = { take = it },
                        onPlay = play,
                        onReset = { reset(match.level) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    SidePanel(match.level, match.difficulty,
                        onLevel = reset,
                        onDifficulty = { match = match.copy(difficulty = it) })
                }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    XorWorkbench(
        piles = match.piles,
        selectedPositions = xorSelected,
        onToggle = { position ->
            xorSelected = if (position in xorSelected) xorSelected - position else xorSelected + position
        },
        labelFor = { "Pile ${it.position + 1}" }
    )
    Spacer(Modifier.height(20.dp))
    if (!match.finished) {
        val (move, winning) = optimalNimMove(match.piles.map { it.stones })
        HintReveal(
            if (winning) "Take ${move.count} from pile ${move.pile + 1}; the remaining pile sizes XOR to zero."
            else "This position has no forced win against perfect play. A legal move is to take 1 from pile ${move.pile + 1}.",
            match.piles
        )
        Spacer(Modifier.height(20.dp))
    }
    TheoryReveal(
        "level 1 theory",
        "Leave the next player a board whose pile sizes XOR to zero. After any move they make, aim to restore zero.",
        "A move changes exactly one pile. From XOR zero, changing that pile to a smaller number changes at least one binary bit, so the XOR cannot stay zero. From nonzero XOR x, take its highest set bit. Some pile has that bit set. Replacing its size a with a xor x makes it smaller and changes the total XOR to zero. Thus every nonzero board can move to zero, and no zero board can move to zero. The empty board is zero, so induction on remaining stones proves the strategy.",
        "Compute x by XORing every pile size. If x is nonzero, find a pile a with (a xor x) < a; remove a - (a xor x) stones. If x is zero, no move forces a win against perfect play."
    )
}

@Composable
private fun GamePanel(
    match: NimMatch, selectedPile: Int, take: Int,
    onSelect: (Int) -> Unit, onTake: (Int) -> Unit, onPlay: () -> Unit, onReset: () -> Unit
) {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("THE BOARD", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${match.level.title}  ·  ROUND ${match.turns + 1}", color = Muted, fontSize = 12.sp)
            }
            if (!match.finished) {
                Spacer(Modifier.height(8.dp))
                Text("YOUR MOVE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            match.piles.forEachIndexed { index, pile ->
                PileRow(index, pile.stones, selectedPile == index && !match.finished) { onSelect(index) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = PanelLight, modifier = Modifier.fillMaxWidth()) {
                Text(match.lastMove, color = Color.White, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
            }
            Spacer(Modifier.height(20.dp))
            if (!match.finished) {
                Text("HOW MANY TO TAKE?", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val maxTake = match.piles[selectedPile].stones
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onTake((take - 1).coerceAtLeast(1)) }, enabled = take > 1) {
                        Text("−")
                    }
                    Text("$take / $maxTake", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { onTake((take + 1).coerceAtMost(maxTake)) }, enabled = take < maxTake) {
                        Text("+")
                    }
                }
                if (maxTake > 1) {
                    Slider(
                        value = take.toFloat(),
                        onValueChange = { onTake(it.roundToInt().coerceIn(1, maxTake)) },
                        valueRange = 1f..maxTake.toFloat(),
                        steps = (maxTake - 2).coerceAtLeast(0)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPlay, enabled = match.piles[selectedPile].stones > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)) {
                    Text("Take $take from pile ${selectedPile + 1}  →", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(if (match.winner == "You") "★  You won!" else "Try again — you can crack it.",
                    color = Accent, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onReset) { Text("New game") }
        }
    }
}

@Composable
private fun PileRow(index: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    val outline = if (selected) Accent else Color.Transparent
    Surface(
        color = PanelLight, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().border(2.dp, outline, RoundedCornerShape(14.dp))
            .clickable(enabled = count > 0) { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", color = if (selected) Accent else Muted, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically) {
                repeat(count.coerceAtMost(8)) { Stone(15, if (selected) Accent else Color(0xFF81A7FF)) }
                if (count > 8) Text("+${count - 8}", color = Muted, fontSize = 12.sp)
                if (count == 0) Text("Empty", color = Muted, fontSize = 13.sp)
            }
            Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Stone(size: Int, color: Color) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

@Composable
private fun SidePanel(
    level: BoardLevel,
    difficulty: Difficulty,
    onLevel: (BoardLevel) -> Unit,
    onDifficulty: (Difficulty) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("BOARD LEVEL", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                BoardLevel.entries.forEach { option ->
                    val active = option == level
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) PanelLight else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clickable { onLevel(option) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text((if (active) "●  " else "○  ") + option.title,
                                color = if (active) Accent else Color.White, fontWeight = FontWeight.Bold)
                            Text(option.description, color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Changing level starts a new board.", color = Muted, fontSize = 12.sp)
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("YOUR OPPONENT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Difficulty.entries.forEach { option ->
                    val active = option == difficulty
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) PanelLight else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clickable { onDifficulty(option) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text((if (active) "●  " else "○  ") + option.title,
                                color = if (active) Accent else Color.White, fontWeight = FontWeight.Bold)
                            Text(option.description, color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("HOW TO PLAY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("1. Select one pile.\n2. Remove one or more stones from it.\n3. Whoever takes the last stone wins.",
                    color = Color.White, lineHeight = 24.sp)
            }
        }
    }
}
