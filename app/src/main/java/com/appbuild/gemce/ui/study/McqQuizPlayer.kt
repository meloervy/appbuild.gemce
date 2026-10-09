package com.appbuild.gemce.ui.study

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.domain.model.McqJsonItem

@Composable
fun McqQuizPlayer(
    questions: List<McqJsonItem>,
    onFinish: (score: Int, total: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quiz questions available.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedChoice by remember { mutableStateOf<Int?>(null) }
    var isAnswerChecked by remember { mutableStateOf(false) }
    var totalScore by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    if (isCompleted) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quiz Finished!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Score: $totalScore / ${questions.size} (${(totalScore * 100) / questions.size}%)",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onFinish(totalScore, questions.size) }) {
                Text("Back to Study Hub")
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
        // Progress
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
                text = "Score: $totalScore",
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

        // Question Stem Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = currentQ.stem,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Radio Button Choices
        currentQ.choices.forEachIndexed { index, choiceText ->
            val isSelected = selectedChoice == index
            val isCorrect = isAnswerChecked && index == currentQ.answer
            val isWrongSelected = isAnswerChecked && isSelected && index != currentQ.answer

            val cardColor = when {
                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                isWrongSelected -> MaterialTheme.colorScheme.errorContainer
                isSelected -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = !isAnswerChecked) {
                        selectedChoice = index
                    },
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { if (!isAnswerChecked) selectedChoice = index },
                        enabled = !isAnswerChecked
                    )
                    Text(
                        text = choiceText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback / Explanation Card
        if (isAnswerChecked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedChoice == currentQ.answer)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (selectedChoice == currentQ.answer) "Correct! 🎉" else "Incorrect",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = currentQ.explanation, style = MaterialTheme.typography.bodyMedium)

                    if (currentQ.evidence.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Evidence: \"${currentQ.evidence}\"",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Button
        if (!isAnswerChecked) {
            Button(
                onClick = {
                    if (selectedChoice != null) {
                        isAnswerChecked = true
                        if (selectedChoice == currentQ.answer) {
                            totalScore++
                        }
                    }
                },
                enabled = selectedChoice != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Answer")
            }
        } else {
            Button(
                onClick = {
                    if (currentIndex + 1 < questions.size) {
                        currentIndex++
                        selectedChoice = null
                        isAnswerChecked = false
                    } else {
                        isCompleted = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex + 1 < questions.size) "Next Question" else "View Results")
            }
        }
    }
}
