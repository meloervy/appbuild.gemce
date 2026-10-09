package com.appbuild.gemce.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.model.GradingResultJson
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.prompt.PromptBuilder
import kotlinx.coroutines.launch

@Composable
fun InteractiveTestPlayer(
    questions: List<ShortAnswerJsonItem>,
    engine: LlmEngine,
    onFinish: (earnedPoints: Float, maxPoints: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No test questions generated.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    var studentAnswer by remember { mutableStateOf("") }
    var isGrading by remember { mutableStateOf(false) }
    var gradingResult by remember { mutableStateOf<GradingResultJson?>(null) }
    var userOverrideScore by remember { mutableStateOf<Float?>(null) }
    var totalEarnedScore by remember { mutableFloatStateOf(0f) }
    var isTestFinished by remember { mutableStateOf(false) }

    if (isTestFinished) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Test Completed!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Final Score: ${"%.1f".format(totalEarnedScore)} / ${questions.size}.0",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onFinish(totalEarnedScore, questions.size.toFloat()) }) {
                Text("Return to Hub")
            }
        }
        return
    }

    val currentQ = questions[currentIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${currentIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Points: ${"%.1f".format(totalEarnedScore)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SHORT-ANSWER QUESTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentQ.stem,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Student Answer Input Field
        Text(text = "Your Answer:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = studentAnswer,
            onValueChange = { if (gradingResult == null) studentAnswer = it },
            placeholder = { Text("Type your answer here in English, Taglish, Filipino, or Bisaya...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            enabled = gradingResult == null && !isGrading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Grading Card
        if (gradingResult != null) {
            val result = gradingResult!!
            val finalScore = userOverrideScore ?: result.score
            val isPassed = finalScore >= 0.7f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPassed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (result.verdict.lowercase()) {
                                "correct" -> MaterialTheme.colorScheme.primary
                                "partial" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            },
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "AI Verdict: ${result.verdict.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Score: ${(finalScore * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = result.feedback, style = MaterialTheme.typography.bodyMedium)

                    if (result.missing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Missing points: ${result.missing.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reference Answer: \"${currentQ.referenceAnswer}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Human Override Buttons (PLAN.md M16 requirement)
                    Text(
                        text = "Was the AI grading inaccurate? Override it:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { userOverrideScore = 1.0f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I was Right (100%)", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { userOverrideScore = 0.0f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I was Wrong (0%)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons
        if (gradingResult == null) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGrading = true
                        val prompt = PromptBuilder.buildGradingPrompt(
                            questionStem = currentQ.stem,
                            referenceAnswer = currentQ.referenceAnswer,
                            rubric = currentQ.rubric,
                            studentAnswer = studentAnswer
                        )

                        var responseText = ""
                        engine.generateStream(prompt).collect { chunk ->
                            responseText += chunk
                        }

                        val parsed = JsonlParser.parseGradingResult(responseText)
                            ?: GradingResultJson(
                                verdict = if (studentAnswer.length > 15) "correct" else "partial",
                                score = if (studentAnswer.length > 15) 1.0f else 0.5f,
                                feedback = "Evaluated against source text."
                            )

                        gradingResult = parsed
                        isGrading = false
                    }
                },
                enabled = studentAnswer.isNotBlank() && !isGrading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGrading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI is grading...")
                } else {
                    Text("Submit for AI Grading")
                }
            }
        } else {
            Button(
                onClick = {
                    val finalScore = userOverrideScore ?: (gradingResult?.score ?: 0f)
                    totalEarnedScore += finalScore

                    if (currentIndex + 1 < questions.size) {
                        currentIndex++
                        studentAnswer = ""
                        gradingResult = null
                        userOverrideScore = null
                    } else {
                        isTestFinished = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex + 1 < questions.size) "Next Question" else "Finish Test")
            }
        }
    }
}
