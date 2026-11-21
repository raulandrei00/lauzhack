package com.playwise.gametheory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class PathPage { JOURNEY, NIM, STAIRCASE }

@Composable
fun GameTheoryPathScreen(onBack: () -> Unit) {
    var page by remember { mutableStateOf(PathPage.JOURNEY) }
    var completed by remember { mutableStateOf(emptySet<Int>()) }

    when (page) {
        PathPage.NIM -> NimScreen(onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 1 })
        PathPage.STAIRCASE -> StaircaseScreen(onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 2 })
        PathPage.JOURNEY -> {
            OutlinedButton(onClick = onBack) { Text("Back to learning paths") }
            Spacer(Modifier.height(20.dp))
            Text("The Nim journey", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("One new idea at a time. Play, discover the pattern, then climb to the next game.", color = Muted)
            Spacer(Modifier.height(26.dp))

            PathLevel(1, "Classic Nim", "Take stones from one pile. Learn to leave XOR zero.",
                completed = 1 in completed, playable = true, onClick = { page = PathPage.NIM })
            LevelExplanation(
                title = "How level 1 works",
                paragraphs = listOf(
                    "On your turn, choose exactly one pile and take at least one stone from it. The player who takes the last stone wins. A position is just the number of stones in each pile.",
                    "XOR compares the binary digits of those numbers without carrying. For example, 3 xor 4 xor 5 = 2. Try changing one pile so the score of all piles becomes zero; then watch how the AI changes it again.",
                    "Use the calculator in the game to select any piles and see their XOR score. For Classic Nim, selecting every pile shows the score of the full position."
                )
            )
            PathConnector()
            PathLevel(2, "Staircase Nim", "Slide coins down one step. Test which groups of steps matter.",
                completed = 2 in completed, playable = true, onClick = { page = PathPage.STAIRCASE })
            LevelExplanation(
                title = "How level 2 works",
                paragraphs = listOf(
                    "Each step holds coins. Choose a step above the floor and slide one or more coins to the step immediately below it. Step 0 is the floor: coins there stay, but cannot be moved again. The last player who can move wins.",
                    "A slide changes two neighboring steps at once. That makes the full-board XOR less straightforward than in Classic Nim. Before moving, try different groups in the calculator and compare their scores after the AI replies.",
                    "Your challenge is to discover a repeatable subset of steps that helps predict the game. The calculator reports only what you select; it does not choose a subset or reveal a winning move."
                )
            )
            PathConnector()
            PathLevel(3, "Take 1 or 2", "Find a repeating pattern when moves are restricted.", false, false, {})
            PathConnector()
            PathLevel(4, "Two mini-games", "Combine independent positions and compare their values.", false, false, {})
            PathConnector()
            PathLevel(5, "Find the missing value", "Meet mex and calculate small Grundy values.", false, false, {})
            PathConnector()
            PathLevel(6, "Chocolate bar SG", "Use the ideas from earlier levels on a more complex board.", false, false, {})
        }
    }
}

@Composable
private fun LevelExplanation(title: String, paragraphs: List<String>) {
    Surface(shape = RoundedCornerShape(16.dp), color = PanelLight, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            paragraphs.forEach { paragraph ->
                Spacer(Modifier.height(10.dp))
                Text(paragraph, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun PathLevel(
    number: Int, title: String, description: String,
    completed: Boolean, playable: Boolean, onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (playable) Panel else PanelLight,
        modifier = Modifier.fillMaxWidth().then(if (playable) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (playable) Accent else Muted,
                modifier = Modifier.size(44.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(if (completed) "✓" else "$number", color = Background, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Muted, fontSize = 13.sp)
                Text(when {
                    completed -> "Completed · replay"
                    playable -> "Play now"
                    else -> "Coming later"
                }, color = if (playable) Accent else Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PathConnector() {
    Surface(color = PanelLight, modifier = Modifier.padding(start = 39.dp).size(width = 3.dp, height = 18.dp)) {}
}
