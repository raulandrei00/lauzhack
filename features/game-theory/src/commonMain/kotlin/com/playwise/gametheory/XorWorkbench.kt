package com.playwise.gametheory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** An exploratory calculator: the game never chooses a subset for the learner. */
@Composable
fun XorWorkbench(
    piles: List<NimPile>,
    selectedPositions: Set<Int>,
    onToggle: (Int) -> Unit,
    labelFor: (NimPile) -> String
) {
    val selected = piles.filter { it.position in selectedPositions }
    val score = xorScore(piles, selectedPositions)
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("BUILD YOUR OWN XOR", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Tap the cards below to choose piles for this calculation. Tapping balls on the board still chooses your move.",
                color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints {
                val columns = if (maxWidth < 460.dp) 3 else 5
                // BoxWithConstraints stacks children like a Box, so rows need their own Column.
                Column { piles.chunked(columns).forEach { rowPiles ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowPiles.forEach { pile ->
                            val active = pile.position in selectedPositions
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) Accent else PanelLight,
                                modifier = Modifier.weight(1f).clickable { onToggle(pile.position) }
                            ) {
                                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(labelFor(pile), color = if (active) Background else Color.White,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${pile.stones}", color = if (active) Background else Muted,
                                        fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                    Text(if (active) "Selected" else "Tap to add",
                                        color = if (active) Background else Muted, fontSize = 10.sp)
                                }
                            }
                        }
                        repeat(columns - rowPiles.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                } }
            }
            Text("XOR score: $score", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (selected.isEmpty()) "No piles selected yet."
                else selected.joinToString(" xor ") { it.stones.toString() } + " = $score  (binary ${score.toString(2)})",
                color = Color.White, fontSize = 13.sp
            )
            if (selected.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("How it's calculated, in binary:", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                BinaryBreakdown(selected.map { labelFor(it) to it.stones })
            }
        }
    }
}
