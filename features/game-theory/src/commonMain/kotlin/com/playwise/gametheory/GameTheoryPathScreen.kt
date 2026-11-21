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

private enum class PathPage { JOURNEY, PRIMER, NIM, STAIRCASE, TAKE, TWO_GAMES, MEX, CHOCOLATE }

@Composable
fun GameTheoryPathScreen(onBack: () -> Unit) {
    var page by remember { mutableStateOf(PathPage.JOURNEY) }
    var completed by remember { mutableStateOf(emptySet<Int>()) }

    when (page) {
        PathPage.NIM -> NimScreen(onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 1 })
        PathPage.STAIRCASE -> StaircaseScreen(onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 2 })
        PathPage.TAKE -> TakeScreen(3, onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 3 })
        PathPage.TWO_GAMES -> TakeScreen(4, onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 4 })
        PathPage.MEX -> TakeScreen(5, onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 5 })
        PathPage.CHOCOLATE -> ChocolateScreen(onBack = { page = PathPage.JOURNEY }, onWin = { completed = completed + 6 })
        PathPage.PRIMER -> NimPrimerScreen(onBack = { page = PathPage.JOURNEY })
        PathPage.JOURNEY -> {
            OutlinedButton(onClick = onBack) { Text("Back to learning paths") }
            Spacer(Modifier.height(20.dp))
            Text("The Nim journey", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("One new idea at a time. Play, discover the pattern, then climb to the next game.", color = Muted)
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = PanelLight,
                modifier = Modifier.fillMaxWidth().clickable { page = PathPage.PRIMER }) {
                Column(Modifier.padding(18.dp)) {
                    Text("START HERE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Winning and losing positions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("The one idea behind every level: a short lesson, a worked example, a quiz and links to learn more.", color = Muted, fontSize = 13.sp)
                    Text("Open the basics →", color = Accent, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            PathLevel(1, "Classic Nim", "Take stones from one pile. Learn to leave XOR zero.",
                completed = 1 in completed, playable = true, onClick = { page = PathPage.NIM })
            PathConnector()
            PathLevel(2, "Staircase Nim", "Slide coins down one step. Test which groups of steps matter.",
                completed = 2 in completed, playable = true, onClick = { page = PathPage.STAIRCASE })
            PathConnector()
            PathLevel(3, "Take up to n", "Find a repeating pattern when moves are restricted.", 3 in completed, true, { page = PathPage.TAKE })
            PathConnector()
            PathLevel(4, "Two mini-games", "Combine independent positions and compare their values.", 4 in completed, true, { page = PathPage.TWO_GAMES })
            PathConnector()
            PathLevel(5, "Find the missing value", "Meet mex and calculate small Grundy values.", 5 in completed, true, { page = PathPage.MEX })
            PathConnector()
            PathLevel(6, "Chocolate bar SG", "Use the ideas from earlier levels on a more complex board.", 6 in completed, true, { page = PathPage.CHOCOLATE })
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
