package com.playwise.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val muted = Color(0xFFADB8D0)

/** Standalone "graph basics" page reachable from the Graphs hub. */
@Composable
fun GraphPrimerScreen() {
    Text("Graph basics", fontSize = 28.sp)
    Text("Read this first, or come back whenever a lesson uses a word you don't know.", color = muted)
    Spacer(Modifier.height(12.dp))
    TeachingGuideView(graphPrimer, openByDefault = true)
}

/**
 * Renders a [TeachingGuide] as collapsible sections. The idea and pseudocode are open by
 * default; [openByDefault] opens everything (used by the primer page).
 */
@Composable
fun TeachingGuideView(guide: TeachingGuide, openByDefault: Boolean = false) {
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(guide.summary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            GuideSection("The idea", open = true) {
                guide.intuition.forEach { Text(it) }
            }
            GuideSection("Vocabulary", openByDefault) {
                guide.terms.forEach { (term, meaning) ->
                    Column {
                        Text(term, fontWeight = FontWeight.SemiBold)
                        Text(meaning, color = muted)
                    }
                }
            }
            GuideSection("Worked example: ${guide.exampleTitle}", openByDefault) {
                guide.exampleSteps.forEachIndexed { i, step -> Text("${i + 1}. $step") }
            }
            GuideSection("Pseudocode", open = true) {
                CodeBlock(guide.pseudocode)
            }
            GuideSection("Running time and memory", openByDefault) {
                guide.complexity.forEach { Text("• $it") }
            }
            GuideSection("Why it works", openByDefault) {
                Text(guide.whyItWorks)
            }
            GuideSection("Common mistakes", openByDefault) {
                guide.pitfalls.forEach { Text("• $it") }
            }
            GuideSection("Check yourself (${guide.quiz.size} questions)", openByDefault) {
                guide.quiz.forEach { QuizCard(it) }
            }
            GuideSection("Go deeper: resources", openByDefault) {
                guide.resources.forEach { ResourceRow(it) }
            }
            GuideSection("In this app's source code", openByDefault) {
                Text(guide.inThisApp)
                Text("Folder: features/graph/src/commonMain/kotlin/com/playwise/graph/", color = muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, open: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember(title) { mutableStateOf(open) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
            Text("${if (expanded) "–" else "+"}  $title", fontSize = 18.sp)
        }
        if (expanded) Column(
            Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    // Horizontal scroll keeps indentation intact on narrow screens instead of wrapping lines.
    Box(
        Modifier.fillMaxWidth()
            .background(Color(0xFF0E1428), RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp, softWrap = false, color = Color(0xFFDCE6FF))
    }
}

@Composable
private fun QuizCard(item: QuizItem) {
    var revealed by remember(item) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1C2742), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Q: ${item.question}")
            if (revealed) Text("A: ${item.answer}", color = Color(0xFFB7F45B))
            OutlinedButton(onClick = { revealed = !revealed }) { Text(if (revealed) "Hide answer" else "Think first, then reveal") }
        }
    }
}

@Composable
private fun ResourceRow(resource: LearningResource) {
    val uriHandler = LocalUriHandler.current
    var failed by remember(resource) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(resource.title, fontWeight = FontWeight.SemiBold)
        Text(resource.note, color = muted)
        if (resource.url.isNotEmpty()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    failed = runCatching { uriHandler.openUri(resource.url) }.isFailure
                }) { Text("Open link") }
                Text(resource.url, fontSize = 12.sp, color = muted, modifier = Modifier.weight(1f))
            }
            if (failed) Text("Could not open a browser here. Copy the address above.", fontSize = 12.sp, color = Color(0xFFFFAF74))
        } else {
            Text("Book: find it in a library or bookshop.", fontSize = 12.sp, color = muted)
        }
    }
}
