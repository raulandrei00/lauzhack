package com.playwise.probability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProbabilityGamesScreen(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) { Text("Back to learning paths") }
    Spacer(Modifier.height(18.dp))
    Text("Probability lab", fontSize = 32.sp)
    Text("New games will appear here as the probability path grows.")
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Betting games", "Expected value", "Probability distributions", "Options").forEach { topic ->
            Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(topic, fontSize = 20.sp)
                    Text("Coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
