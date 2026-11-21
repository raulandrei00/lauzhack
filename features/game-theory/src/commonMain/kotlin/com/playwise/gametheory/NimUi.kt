package com.playwise.gametheory

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val StoneBlue = Color(0xFF81A7FF)
internal val TakeOrange = Color(0xFFFFAF74)
internal val AiRed = Color(0xFFFF8F8F)
internal val Good = Color(0xFFB7F45B)

/** A faded group of balls drawn after the real ones: what the computer took, or where coins will land. */
internal data class Ghost(val count: Int, val color: Color, val label: String)

/**
 * A pile of tappable balls. Tapping ball i selects it and every ball to its right
 * (stones - i balls); the caller decides whether that amount is legal.
 * [legalCounts] = null means any positive amount; otherwise balls where a legal take
 * would start get a green ring.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BallPile(
    title: String,
    stones: Int,
    selectedCount: Int,
    enabled: Boolean,
    onTapBall: (count: Int) -> Unit,
    subtitle: String? = null,
    legalCounts: Collection<Int>? = null,
    ghosts: List<Ghost> = emptyList(),
    indent: Dp = 0.dp,
    emptyText: String = "Empty",
) {
    val active = selectedCount > 0
    val ballSize = if (stones > 24) 26.dp else 38.dp
    Surface(
        color = if (active) PanelLight else Panel, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(start = indent)
            .border(2.dp, if (active) TakeOrange else Color.Transparent, RoundedCornerShape(18.dp))
            .semantics { contentDescription = "$title: $stones" }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = if (active) TakeOrange else Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("$stones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            subtitle?.let { Text(it, color = Muted, fontSize = 13.sp) }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(stones) { i ->
                    val count = stones - i
                    Ball(
                        size = ballSize,
                        selected = i >= stones - selectedCount,
                        legalStart = enabled && legalCounts != null && count in legalCounts,
                        enabled = enabled,
                        onClick = { onTapBall(count) },
                    )
                }
                ghosts.forEach { ghost ->
                    repeat(ghost.count) {
                        Box(Modifier.size(ballSize).border(2.dp, ghost.color.copy(alpha = .7f), CircleShape))
                    }
                }
                if (stones == 0 && ghosts.isEmpty()) Text(emptyText, color = Muted, modifier = Modifier.padding(vertical = 8.dp))
            }
            ghosts.forEach { Text(it.label, color = it.color, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun Ball(size: Dp, selected: Boolean, legalStart: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) TakeOrange else StoneBlue)
    val lift by animateDpAsState(if (selected) (-5).dp else 0.dp)
    Box(
        Modifier.size(size).offset(y = lift)
            .border(if (legalStart && !selected) 3.dp else 0.dp, if (legalStart && !selected) Good else Color.Transparent, CircleShape)
            .padding(if (legalStart && !selected) 3.dp else 0.dp)
            .background(color, CircleShape)
            .border(if (selected) 2.dp else 0.dp, if (selected) Color.White else Color.Transparent, CircleShape)
            .clickable(enabled = enabled) { onClick() }
    ) {
        // A small shine makes the circle read as a ball.
        Box(Modifier.padding(start = size / 5, top = size / 6).size(size / 4).background(Color.White.copy(alpha = .35f), CircleShape))
    }
}

/** Big status strip at the top of a board. */
@Composable
internal fun GameBanner(title: String, text: String, color: Color = Accent) {
    Surface(color = color.copy(alpha = .15f), shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(2.dp, color, RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

/** One line explaining what just happened, or why a tap was not allowed. */
@Composable
internal fun FeedbackLine(text: String, warning: Boolean = false) {
    if (text.isEmpty()) return
    Surface(color = (if (warning) TakeOrange else Muted).copy(alpha = .12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = if (warning) TakeOrange else Color.White, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
internal fun ResultBanner(youWon: Boolean, onAgain: () -> Unit) {
    val color = if (youWon) Good else AiRed
    Surface(color = color.copy(alpha = .18f), shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().border(3.dp, color, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (youWon) "You won!" else "The computer won", color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(if (youWon) "Great play. Can you do it again against a stronger opponent?"
                else "Every board here can be won from the start. Open the lesson below, then try again.",
                color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = onAgain, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text("Play again", color = Background, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

/** Big confirm button under the board. */
@Composable
internal fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakeOrange)) {
        Text(text, color = Background, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
internal fun ChoiceChips(label: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Column {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, option ->
                val on = i == selected
                Surface(color = if (on) Accent else PanelLight, shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onPick(i) }) {
                    Text((if (on) "✓ " else "") + option, color = if (on) Background else Color.White,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
    }
}

@Composable
internal fun OpponentChips(difficulty: Difficulty, onPick: (Difficulty) -> Unit) {
    ChoiceChips("OPPONENT", Difficulty.entries.map { it.title }, difficulty.ordinal) { onPick(Difficulty.entries[it]) }
    Text(difficulty.description, color = Muted, fontSize = 13.sp)
}

/** Short numbered "how to play" list. */
@Composable
internal fun HowToPlay(steps: List<String>) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("HOW TO PLAY", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Background, fontWeight = FontWeight.Bold)
                    }
                    Text(s, color = Color.White, modifier = Modifier.padding(start = 10.dp, top = 2.dp))
                }
            }
        }
    }
}

/** A hidden hint that can also highlight the suggested move on the board. */
@Composable
internal fun HintCard(hint: String, key: Any, onShow: (() -> Unit)?) {
    var open by remember(key) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(18.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            OutlinedButton(onClick = { open = !open }) { Text(if (open) "Hide hint" else "Stuck? Show a hint") }
            if (open) {
                Spacer(Modifier.height(10.dp))
                Text(hint, color = Accent, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                if (onShow != null) TextButton(onClick = onShow) { Text("Select this move for me") }
            }
        }
    }
}

@Composable
internal fun LevelHeader(onBack: () -> Unit, title: String, subtitle: String) {
    OutlinedButton(onClick = onBack) { Text("← All game-theory levels") }
    Spacer(Modifier.height(16.dp))
    Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Muted)
    Spacer(Modifier.height(16.dp))
}
