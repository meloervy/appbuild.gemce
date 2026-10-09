package com.appbuild.gemce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.EngineStatus
import com.appbuild.gemce.ai.FakeEngine
import com.appbuild.gemce.ai.LiteRtLmEngine
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.study.ReviewState
import com.appbuild.gemce.domain.study.Sm2Scheduler
import com.appbuild.gemce.ui.theme.GemceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fakeEngine = FakeEngine()
        val liteRtLmEngine = LiteRtLmEngine(applicationContext)

        setContent {
            GemceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GemceDashboard(
                        fakeEngine = fakeEngine,
                        liteRtLmEngine = liteRtLmEngine,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GemceDashboard(
    fakeEngine: FakeEngine,
    liteRtLmEngine: LiteRtLmEngine,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var useRealModel by remember { mutableStateOf(false) }
    val currentEngine: LlmEngine = if (useRealModel) liteRtLmEngine else fakeEngine

    val engineStatus by currentEngine.status.collectAsState()
    val isModelInstalled = remember(useRealModel) { liteRtLmEngine.isModelInstalled() }

    val generatedCards = remember { mutableStateListOf<CardJsonItem>() }
    var reviewState by remember { mutableStateOf(ReviewState(cardId = "sample_card_1")) }
    var lastGradeGiven by remember { mutableStateOf<Int?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gemce",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "AI study buddy that works anywhere you go",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Engine Selection Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !useRealModel,
                onClick = {
                    coroutineScope.launch {
                        currentEngine.unload()
                        useRealModel = false
                    }
                },
                label = { Text("FakeEngine (Instant)") }
            )
            FilterChip(
                selected = useRealModel,
                onClick = {
                    coroutineScope.launch {
                        currentEngine.unload()
                        useRealModel = true
                    }
                },
                label = { Text("Gemma 4 E2B (On-Device)") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Engine: ${if (useRealModel) "LiteRT-LM (Gemma 4 E2B)" else "FakeEngine"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Status: ${
                        when (val s = engineStatus) {
                            is EngineStatus.Ready -> "Ready"
                            is EngineStatus.Loading -> "Loading: ${s.message}"
                            is EngineStatus.Unloaded -> "Unloaded"
                            is EngineStatus.Error -> "Error: ${s.throwable.message}"
                        }
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (useRealModel) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isModelInstalled) {
                            "Model file installed at: ${liteRtLmEngine.modelFile.name}"
                        } else {
                            "Model not found at: files/models/${LiteRtLmEngine.MODEL_FILENAME}.\nPush via adb: adb shell run-as com.appbuild.gemce cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isModelInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                if (engineStatus is EngineStatus.Loading || isGenerating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        if (!currentEngine.isReady) {
                            currentEngine.initialize()
                        }
                        generatedCards.clear()
                        val prompt = "Generate flashcards in JSONL format for the mitochondria section"
                        currentEngine.generateStream(prompt).collect { line ->
                            JsonlParser.parseCard(line)?.let { card ->
                                generatedCards.add(card)
                            }
                        }
                        isGenerating = false
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isGenerating) "Generating..." else "Generate Cards")
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        currentEngine.unload()
                        generatedCards.clear()
                        isGenerating = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SM-2 Spaced Repetition Interactive Tester
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SM-2 Spaced Repetition (Deterministic)",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Repetitions: ${reviewState.repetitions} | Interval: ${reviewState.intervalDays} day(s) | Ease: ${"%.2f".format(reviewState.easeFactor)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (lastGradeGiven != null) {
                    Text(
                        text = "Last rated: $lastGradeGiven/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0 to "Blackout (0)", 3 to "Hard (3)", 5 to "Easy (5)").forEach { (grade, label) ->
                        OutlinedButton(
                            onClick = {
                                lastGradeGiven = grade
                                reviewState = Sm2Scheduler.schedule(reviewState, grade)
                            }
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cards List
        Text(
            text = "Generated Cards (${generatedCards.size})",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(generatedCards) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Q: ${card.front}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A: ${card.back}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Evidence: ${card.evidence}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
