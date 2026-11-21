package com.playwise.app

import com.playwise.gametheory.GameTheoryPathScreen
import com.playwise.graph.GraphGamesScreen
import com.playwise.probability.ProbabilityGamesScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
private val GraphBlue = Color(0xFF81A7FF)
private val ProbabilityPurple = Color(0xFFC59BFF)
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
private fun HomeScreen(onStart: () -> Unit, onGraphs: () -> Unit, onProbability: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 32.dp)) {
        Text("PLAYWISE", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("Choose a subject", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("Three collections of playable lessons.", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        BoxWithConstraints {
            if (maxWidth < 700.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SubjectButton("01", "Game theory", "Strategy and impartial games", Accent, Background, onStart)
                    SubjectButton("02", "Graphs", "Algorithms and graph games", GraphBlue, Background, onGraphs)
                    SubjectButton("03", "Probability", "Chance and expected value", ProbabilityPurple, Background, onProbability)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SubjectButton("01", "Game theory", "Strategy and impartial games", Accent, Background, onStart, Modifier.weight(1f))
                    SubjectButton("02", "Graphs", "Algorithms and graph games", GraphBlue, Background, onGraphs, Modifier.weight(1f))
                    SubjectButton("03", "Probability", "Chance and expected value", ProbabilityPurple, Background, onProbability, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectButton(
    number: String,
    title: String,
    description: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(168.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = .65f))
            Column {
                Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 14.sp, color = contentColor.copy(alpha = .72f))
            }
        }
    }
}
