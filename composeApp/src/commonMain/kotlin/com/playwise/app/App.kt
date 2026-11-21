package com.playwise.app

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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

private val Background = Color(0xFF101528)
private val Panel = Color(0xFF1C2540)
private val PanelLight = Color(0xFF24304D)
private val Accent = Color(0xFFB7F45B)
private val Muted = Color(0xFFADB8D0)

private enum class Screen { HOME, NIM }

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
                Header(onHome = { screen = Screen.HOME }, onNim = { screen = Screen.NIM })
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            Column(Modifier.widthIn(max = 1100.dp).fillMaxWidth()) {
                                if (screen == Screen.HOME) HomeScreen(onStart = { screen = Screen.NIM })
                                else NimScreen(onBack = { screen = Screen.HOME })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onHome: () -> Unit, onNim: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◈", color = Accent, fontSize = 27.sp, modifier = Modifier.padding(end = 8.dp))
        Text("playwise", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp,
            modifier = Modifier.clickable { onHome() })
        Spacer(Modifier.weight(1f))
        Text("Explore", color = Muted, modifier = Modifier.clickable { onHome() }.padding(8.dp))
        Text("Play Nim", color = Accent, modifier = Modifier.clickable { onNim() }.padding(8.dp))
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit) {
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
                PathCard("01", "Games & strategy", "Nim is ready to play", true, onStart)
                PathCard("02", "Graphs", "Coming soon", false, {})
                PathCard("03", "Probability", "Coming soon", false, {})
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { PathCard("01", "Games & strategy", "Nim is ready to play", true, onStart) }
                Box(Modifier.weight(1f)) { PathCard("02", "Graphs", "Coming soon", false, {}) }
                Box(Modifier.weight(1f)) { PathCard("03", "Probability", "Coming soon", false, {}) }
            }
        }
    }
}

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
            Text("Opening board: 3 xor 4 xor 5 = 2. Take 2 from pile 1 to leave 1, 4, 5 — their XOR is zero.",
                color = Color.White, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text("Implementation idea: calculate the XOR, then try each legal move until the remaining piles XOR to zero.",
                color = Accent, fontSize = 13.sp, lineHeight = 19.sp)
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
                    Box(Modifier.weight(1f)) { FeatureText(onStart) }
                    StonePreview()
                }
            }
        }
    }
}

@Composable
private fun FeatureText(onStart: () -> Unit) {
    Text("FIRST CHALLENGE  ·  GAME THEORY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text("Nim: take the last stone", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text("Choose one pile. Take any number of stones. Outsmart three kinds of AI.", color = Muted)
    Spacer(Modifier.height(22.dp))
    Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
        Text("Play Nim  →", fontWeight = FontWeight.Bold)
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
private fun NimScreen(onBack: () -> Unit) {
    var match by remember { mutableStateOf(NimMatch()) }
    var selectedPile by remember { mutableIntStateOf(0) }
    var take by remember { mutableIntStateOf(1) }

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
                    onPlay = {
                        match = match.play(Move(selectedPile, take))
                        selectedPile = match.piles.indexOfFirst { it > 0 }.coerceAtLeast(0)
                        take = 1
                    },
                    onReset = { match = NimMatch(difficulty = match.difficulty); selectedPile = 0; take = 1 }
                )
                SidePanel(match.difficulty, onDifficulty = { match = match.copy(difficulty = it) })
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1.6f)) {
                    GamePanel(match, selectedPile, take,
                        onSelect = { selectedPile = it; take = 1 },
                        onTake = { take = it },
                        onPlay = {
                            match = match.play(Move(selectedPile, take))
                            selectedPile = match.piles.indexOfFirst { it > 0 }.coerceAtLeast(0)
                            take = 1
                        },
                        onReset = { match = NimMatch(difficulty = match.difficulty); selectedPile = 0; take = 1 }
                    )
                }
                Box(Modifier.weight(1f)) { SidePanel(match.difficulty, onDifficulty = { match = match.copy(difficulty = it) }) }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    StrategyPanel()
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
                Text("ROUND ${match.turns + 1}", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            match.piles.forEachIndexed { index, count ->
                PileRow(index, count, selectedPile == index && !match.finished) { onSelect(index) }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..match.piles[selectedPile].coerceAtLeast(1)).forEach { amount ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (take == amount) Accent else PanelLight,
                            modifier = Modifier.size(46.dp).clickable { onTake(amount) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$amount", color = if (take == amount) Background else Color.White,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPlay, enabled = match.piles[selectedPile] > 0,
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
        modifier = Modifier.fillMaxWidth().border(2.dp, outline, RoundedCornerShape(14.dp)).clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", color = if (selected) Accent else Muted, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(count) { Stone(20, if (selected) Accent else Color(0xFF81A7FF)) }
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
private fun SidePanel(difficulty: Difficulty, onDifficulty: (Difficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Spacer(Modifier.height(16.dp))
                Text("The secret: if you can leave the bitwise XOR of the pile sizes at zero, your opponent has no winning reply.",
                    color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}
