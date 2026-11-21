package com.playwise.gametheory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TheoryReveal(title: String, idea: String, proof: String, optimal: String) {
    var open by remember(title) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            OutlinedButton(onClick = { open = !open }) { Text(if (open) "Hide $title" else "Show $title") }
            if (open) {
                Spacer(Modifier.height(12.dp))
                Text("The idea", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(idea, color = Color.White, lineHeight = 21.sp)
                Spacer(Modifier.height(12.dp))
                Text("Why it works", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(proof, color = Color.White, lineHeight = 21.sp)
                Spacer(Modifier.height(12.dp))
                Text("How to find the best move", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(optimal, color = Color.White, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
fun HintReveal(hint: String, key: Any) {
    var open by remember(key) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            OutlinedButton(onClick = { open = !open }) { Text(if (open) "Hide hint" else "Hint: show an optimal move") }
            if (open) {
                Spacer(Modifier.height(12.dp))
                Text(hint, color = Accent, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                Spacer(Modifier.height(10.dp))
                Text("XOR means exclusive OR. Write each number in binary, then compare one column at a time. A column is 1 when an odd number of inputs have a 1 there; otherwise it is 0. For example, 3 (011) xor 5 (101) = 6 (110).",
                    color = Color.White, lineHeight = 21.sp)
            }
        }
    }
}
