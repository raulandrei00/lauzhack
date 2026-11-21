package com.playwise.gametheory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun StaircaseScreen(onBack: () -> Unit, onWin: () -> Unit = {}) {
    var match by remember { mutableStateOf(newStaircaseMatch()) }
    var selectedStep by remember { mutableIntStateOf(1) }
    var count by remember { mutableIntStateOf(1) }
    var xorSelected by remember { mutableStateOf(emptySet<Int>()) }

    OutlinedButton(onClick = onBack) { Text("All game-theory levels") }
    Spacer(Modifier.height(18.dp))
    Text("Level 2 · Staircase Nim", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Text("The coins move, but only some steps decide the strategy.", color = Muted)
    Spacer(Modifier.height(22.dp))

    BoxWithConstraints {
        if (maxWidth < 760.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StairBoard(match, selectedStep, count,
                    onSelect = { selectedStep = it; count = 1 }, onCount = { count = it },
                    onPlay = {
                        match = match.play(StairMove(selectedStep, count))
                        if (match.winner == "You") onWin()
                        selectedStep = (1..match.steps.lastIndex).firstOrNull { match.steps[it].stones > 0 } ?: 1
                        count = 1
                    },
                    onReset = {
                        match = newStaircaseMatch(match.difficulty)
                        selectedStep = 1
                        count = 1
                        xorSelected = emptySet()
                    })
                StairLesson(match.difficulty, onDifficulty = { match = match.copy(difficulty = it) })
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1.6f)) {
                    StairBoard(match, selectedStep, count,
                        onSelect = { selectedStep = it; count = 1 }, onCount = { count = it },
                        onPlay = {
                            match = match.play(StairMove(selectedStep, count))
                            if (match.winner == "You") onWin()
                            selectedStep = (1..match.steps.lastIndex).firstOrNull { match.steps[it].stones > 0 } ?: 1
                            count = 1
                        },
                        onReset = {
                            match = newStaircaseMatch(match.difficulty)
                            selectedStep = 1
                            count = 1
                            xorSelected = emptySet()
                        })
                }
                Box(Modifier.weight(1f)) {
                    StairLesson(match.difficulty, onDifficulty = { match = match.copy(difficulty = it) })
                }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    XorWorkbench(
        piles = match.steps,
        selectedPositions = xorSelected,
        onToggle = { position ->
            xorSelected = if (position in xorSelected) xorSelected - position else xorSelected + position
        },
        labelFor = { if (it.position == 0) "Floor" else "Step ${it.position}" }
    )
    Spacer(Modifier.height(20.dp))
    StairExplanation()
}

@Composable
private fun StairExplanation() {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("THINK THROUGH A MOVE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("If step 2 holds 3 coins, sliding 2 coins down leaves 1 on step 2 and adds 2 to step 1. The coins have moved; they have not disappeared.",
                color = Color.White, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            Text("Sliding coins from step 1 puts them on the floor, where they cannot move again. A game ends when every step above the floor is empty.",
                color = Muted, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            Text("Try the same XOR subset before and after several moves. Does it help you predict which player can force the final slide? Change your subset and test again.",
                color = Color.White, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun StairBoard(
    match: StaircaseMatch, selectedStep: Int, count: Int,
    onSelect: (Int) -> Unit, onCount: (Int) -> Unit,
    onPlay: () -> Unit, onReset: () -> Unit
) {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("THE STAIRCASE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(if (match.finished) "GAME COMPLETE" else "YOUR MOVE · ROUND ${match.turns + 1}",
                color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            for (step in match.steps.indices.reversed()) {
                StairRow(step, match.steps[step].stones, selectedStep == step && !match.finished) {
                    onSelect(step)
                }
                Spacer(Modifier.height(7.dp))
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = PanelLight, modifier = Modifier.fillMaxWidth()) {
                Text(match.lastMove, modifier = Modifier.padding(14.dp), color = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            if (!match.finished) {
                val available = match.steps[selectedStep].stones
                Text("SLIDE FROM STEP $selectedStep TO STEP ${selectedStep - 1}",
                    color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onCount((count - 1).coerceAtLeast(1)) }, enabled = count > 1) { Text("−") }
                    Text("$count / $available coins", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { onCount((count + 1).coerceAtMost(available)) }, enabled = count < available) { Text("+") }
                }
                if (available > 1) {
                    Slider(
                        value = count.toFloat(),
                        onValueChange = { onCount(it.roundToInt().coerceIn(1, available)) },
                        valueRange = 1f..available.toFloat(),
                        steps = (available - 2).coerceAtLeast(0)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPlay, enabled = available > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Text("Slide $count coins  →", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(if (match.winner == "You") "★  You won level 2!" else "Try another board.",
                    color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onReset) { Text("New staircase") }
        }
    }
}

@Composable
private fun StairRow(step: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    val playable = step > 0 && count > 0
    Surface(
        shape = RoundedCornerShape(12.dp), color = PanelLight,
        modifier = Modifier.fillMaxWidth().padding(start = (step * 8).dp)
            .border(2.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(enabled = playable) { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (step == 0) "Floor" else "Step $step", color = if (selected) Accent else Color.White,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(count.coerceAtMost(8)) {
                    Box(Modifier.size(16.dp).background(if (selected) Accent else Color(0xFF81A7FF), CircleShape))
                }
                if (count > 8) Text("+${count - 8}", color = Muted, fontSize = 12.sp)
            }
            Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StairLesson(difficulty: Difficulty, onDifficulty: (Difficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("HOW TO PLAY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Choose a step above the floor. Move one or more coins exactly one step down. Coins on the floor cannot move. Make the last move to win.",
                    color = Color.White, lineHeight = 21.sp)
                Spacer(Modifier.height(16.dp))
                Text("YOUR EXPERIMENT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("A move changes two neighboring steps at once. Use the XOR calculator below the board to test different groups of steps. Which group seems to predict whether you can force the last move?",
                    color = Muted, lineHeight = 21.sp)
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("OPPONENT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Difficulty.entries.forEach { option ->
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = if (option == difficulty) PanelLight else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clickable { onDifficulty(option) }) {
                        Text((if (option == difficulty) "●  " else "○  ") + option.title,
                            color = if (option == difficulty) Accent else Color.White,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}
