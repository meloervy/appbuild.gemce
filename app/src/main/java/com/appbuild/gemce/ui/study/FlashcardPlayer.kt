package com.appbuild.gemce.ui.study

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.study.ReviewState
import com.appbuild.gemce.domain.study.Sm2Scheduler

@Composable
fun FlashcardPlayer(
    cards: List<CardJsonItem>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No flashcards available in this section.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var currentReviewState by remember { mutableStateOf(ReviewState(cardId = "card_$currentIndex")) }
    val currentCard = cards[currentIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Card ${currentIndex + 1} of ${cards.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Interval: ${currentReviewState.intervalDays}d | Ease: ${"%.2f".format(currentReviewState.easeFactor)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / cards.size },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Flippable Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clickable { isFlipped = !isFlipped },
            colors = CardDefaults.cardColors(
                containerColor = if (isFlipped) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFlipped) "ANSWER" else "QUESTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isFlipped) currentCard.back else currentCard.front,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isFlipped) "Tap card to see question" else "Tap card to reveal answer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Source Grounding Evidence
        AnimatedVisibility(visible = isFlipped && currentCard.evidence.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Source Evidence (Grounding)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${currentCard.evidence}\"",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SM-2 Spaced Repetition Buttons (shown when card is flipped)
        if (isFlipped) {
            Text(
                text = "Rate your recall (SM-2 Spaced Repetition):",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        currentReviewState = Sm2Scheduler.schedule(currentReviewState, grade = 1)
                        if (currentIndex + 1 < cards.size) {
                            currentIndex++
                            isFlipped = false
                        } else onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Again\n(1d)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        currentReviewState = Sm2Scheduler.schedule(currentReviewState, grade = 3)
                        if (currentIndex + 1 < cards.size) {
                            currentIndex++
                            isFlipped = false
                        } else onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hard\n(3d)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        currentReviewState = Sm2Scheduler.schedule(currentReviewState, grade = 4)
                        if (currentIndex + 1 < cards.size) {
                            currentIndex++
                            isFlipped = false
                        } else onFinish()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Good\n(4d)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        currentReviewState = Sm2Scheduler.schedule(currentReviewState, grade = 5)
                        if (currentIndex + 1 < cards.size) {
                            currentIndex++
                            isFlipped = false
                        } else onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Easy\n(6d)", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            Button(
                onClick = { isFlipped = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Flip Card")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    if (currentIndex > 0) {
                        currentIndex--
                        isFlipped = false
                    }
                },
                enabled = currentIndex > 0
            ) {
                Text("Previous")
            }

            OutlinedButton(
                onClick = {
                    if (currentIndex + 1 < cards.size) {
                        currentIndex++
                        isFlipped = false
                    } else {
                        onFinish()
                    }
                }
            ) {
                Text(if (currentIndex + 1 < cards.size) "Skip" else "Finish")
            }
        }
    }
}
