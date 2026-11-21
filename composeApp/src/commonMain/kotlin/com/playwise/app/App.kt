package com.playwise.app

import com.playwise.gametheory.GameTheoryPathScreen
import com.playwise.graph.GraphGamesScreen
import com.playwise.probability.ProbabilityGamesScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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

private val Background = Color(0xFF101528)
private val Panel = Color(0xFF1C2540)
private val Accent = Color(0xFFB7F45B)
private val Muted = Color(0xFFADB8D0)

private enum class Screen { HOME, GAME_THEORY, GRAPHS, PROBABILITY }

@Composable
fun PlaywiseApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Background,
            background = Background,
            surface = Panel,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Header(onHome = { screen = Screen.HOME }, onGameTheory = { screen = Screen.GAME_THEORY })
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            Column(Modifier.widthIn(max = 1100.dp).fillMaxWidth()) {
                                when (screen) {
                                    Screen.HOME -> HomeScreen(
                                        onStart = { screen = Screen.GAME_THEORY },
                                        onGraphs = { screen = Screen.GRAPHS },
                                        onProbability = { screen = Screen.PROBABILITY }
                                    )
                                    Screen.GAME_THEORY -> GameTheoryPathScreen(onBack = { screen = Screen.HOME })
                                    Screen.GRAPHS -> GraphGamesScreen(onBack = { screen = Screen.HOME })
                                    Screen.PROBABILITY -> ProbabilityGamesScreen(onBack = { screen = Screen.HOME })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onHome: () -> Unit, onGameTheory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◈", color = Accent, fontSize = 27.sp, modifier = Modifier.padding(end = 8.dp))
        Text("playwise", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp,
            modifier = Modifier.clickable { onHome() })
        Spacer(Modifier.weight(1f))
        Text("Explore", color = Muted, modifier = Modifier.clickable { onHome() }.padding(8.dp))
        Text("Game theory", color = Accent, modifier = Modifier.clickable { onGameTheory() }.padding(8.dp))
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit, onGraphs: () -> Unit, onProbability: () -> Unit) {
    Text("LEARN BY PLAYING", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    Text("Big ideas start with\na small move.", color = Color.White, fontSize = 42.sp,
        fontWeight = FontWeight.Bold, lineHeight = 47.sp)
    Spacer(Modifier.height(14.dp))
    Text("Play a game, discover the strategy, then build it yourself.", color = Muted, fontSize = 17.sp)
    Spacer(Modifier.height(28.dp))
    FeatureCard(onStart)
    Spacer(Modifier.height(32.dp))
    Text("LEARNING PATHS", color = Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    BoxWithConstraints {
        if (maxWidth < 700.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PathCard("01", "Games & strategy", "Nim journey: levels 1–2", true, onStart)
                PathCard("02", "Graphs", "6 games + 5 algorithm lessons", true, onGraphs)
                PathCard("03", "Probability", "Path ready for new games", true, onProbability)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { PathCard("01", "Games & strategy", "Nim journey: levels 1–2", true, onStart) }
                Box(Modifier.weight(1f)) { PathCard("02", "Graphs", "6 games + 5 algorithm lessons", true, onGraphs) }
                Box(Modifier.weight(1f)) { PathCard("03", "Probability", "Path ready for new games", true, onProbability) }
            }
        }
    }
}

@Composable
private fun FeatureCard(onStart: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.padding(26.dp)) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column {
                    FeatureText(onStart)
                    Spacer(Modifier.height(22.dp))
                    StonePreview()
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(end = 24.dp)) { FeatureText(onStart) }
                    StonePreview()
                }
            }
        }
    }
}

@Composable
private fun FeatureText(onStart: () -> Unit) {
    Column {
        Text("FIRST CHALLENGE  ·  GAME THEORY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Nim: take the last stone", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Choose one pile. Take any number of stones. Outsmart three kinds of AI.", color = Muted)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
            Text("Explore the Nim journey  →", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StonePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(row + 3) { Stone(20, if (row == 1) Accent else Color(0xFF81A7FF)) }
            }
        }
    }
}

@Composable
private fun PathCard(number: String, title: String, subtitle: String, available: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = Panel,
        modifier = Modifier.fillMaxWidth().then(if (available) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(number, color = if (available) Accent else Muted, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Stone(size: Int, color: Color) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}
