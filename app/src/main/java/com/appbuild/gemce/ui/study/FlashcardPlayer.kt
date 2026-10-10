package com.appbuild.gemce.ui.study

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.export.AnkiExporter
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import com.appbuild.gemce.domain.study.ReviewState
import com.appbuild.gemce.domain.study.Sm2Scheduler
import com.appbuild.gemce.ui.markdown.MarkdownView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardPlayer(
    cards: List<CardJsonItem>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    engine: LlmEngine? = null,
    language: LanguageMode = LanguageMode.EN,
    studentProfile: StudentProfile? = null,
    onAddCard: ((CardJsonItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Local mutable cards list for adding manual cards during session
    val localDeck = remember(cards) { mutableStateListOf(*cards.toTypedArray()) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var currentReviewState by remember { mutableStateOf(ReviewState(cardId = "card_$currentIndex")) }

    // Dialog & Sheet States
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExplainSheet by remember { mutableStateOf(false) }
    var explainText by remember { mutableStateOf("") }
    var isExplaining by remember { mutableStateOf(false) }

    // New Card Form
    var newFront by remember { mutableStateOf("") }
    var newBack by remember { mutableStateOf("") }
    var newEvidence by remember { mutableStateOf("") }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    if (localDeck.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No flashcards in this deck yet.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You can create manual flashcards without AI, or generate cards from notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAddCardDialog = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("➕ Add Manual Card", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Render Add Card Dialog even when empty
        if (showAddCardDialog) {
            AlertDialog(
                onDismissRequest = { showAddCardDialog = false },
                title = { Text("➕ Add Manual Flashcard", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newFront,
                            onValueChange = { newFront = it },
                            label = { Text("Question / Term (Front)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newBack,
                            onValueChange = { newBack = it },
                            label = { Text("Answer / Definition (Back)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newEvidence,
                            onValueChange = { newEvidence = it },
                            label = { Text("Source Evidence / Citation (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFront.isNotBlank() && newBack.isNotBlank()) {
                                val created = CardJsonItem(front = newFront.trim(), back = newBack.trim(), evidence = newEvidence.trim())
                                localDeck.add(created)
                                onAddCard?.invoke(created)
                                newFront = ""
                                newBack = ""
                                newEvidence = ""
                                showAddCardDialog = false
                            }
                        },
                        enabled = newFront.isNotBlank() && newBack.isNotBlank()
                    ) {
                        Text("Add Card")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCardDialog = false }) { Text("Cancel") }
                }
            )
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, localDeck.size - 1)
    val currentCard = localDeck[safeIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar: Actions (Add Card, Export Anki, Explain This)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Card ${safeIndex + 1} of ${localDeck.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Interval: ${currentReviewState.intervalDays}d | Ease: ${"%.2f".format(currentReviewState.easeFactor)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showAddCardDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("➕ Card", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📤 Anki", style = MaterialTheme.typography.labelSmall)
                }

                if (engine != null) {
                    Button(
                        onClick = {
                            showExplainSheet = true
                            isExplaining = true
                            explainText = ""
                            coroutineScope.launch {
                                try {
                                    val prompt = PromptBuilder.buildExplainThisPrompt(
                                        conceptOrText = "${currentCard.front} -> ${currentCard.back}",
                                        evidence = currentCard.evidence,
                                        language = language,
                                        profile = studentProfile
                                    )
                                    val sb = StringBuilder()
                                    engine.generateStream(prompt).collect { chunk ->
                                        sb.append(chunk)
                                        explainText = sb.toString()
                                    }
                                } catch (e: Exception) {
                                    explainText = "Unable to generate explanation: ${e.message}"
                                } finally {
                                    isExplaining = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("💡 Explain", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (safeIndex + 1).toFloat() / localDeck.size },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Flippable 3D Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .clickable { isFlipped = !isFlipped },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation > 90f)
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            if (rotation <= 90f) {
                // FRONT (Question)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "QUESTION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentCard.front,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Tap card to flip & reveal answer ↷",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // BACK (Answer)
                Column(
                    modifier = Modifier
                        .graphicsLayer { rotationY = 180f }
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ANSWER",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentCard.back,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Tap card to return to question ↶",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // SM-2 Spaced Repetition Buttons
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
                        if (safeIndex + 1 < localDeck.size) {
                            currentIndex = safeIndex + 1
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
                        if (safeIndex + 1 < localDeck.size) {
                            currentIndex = safeIndex + 1
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
                        if (safeIndex + 1 < localDeck.size) {
                            currentIndex = safeIndex + 1
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
                        if (safeIndex + 1 < localDeck.size) {
                            currentIndex = safeIndex + 1
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
                    if (safeIndex > 0) {
                        currentIndex = safeIndex - 1
                        isFlipped = false
                    }
                },
                enabled = safeIndex > 0
            ) {
                Text("Previous")
            }

            OutlinedButton(
                onClick = {
                    if (safeIndex + 1 < localDeck.size) {
                        currentIndex = safeIndex + 1
                        isFlipped = false
                    } else {
                        onFinish()
                    }
                }
            ) {
                Text(if (safeIndex + 1 < localDeck.size) "Skip" else "Finish")
            }
        }
    }

    // Modal 1: Add Manual Card Dialog (M14)
    if (showAddCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("➕ Add Manual Flashcard", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create cards directly for offline study with zero AI dependency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newFront,
                        onValueChange = { newFront = it },
                        label = { Text("Question / Term (Front)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBack,
                        onValueChange = { newBack = it },
                        label = { Text("Answer / Definition (Back)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEvidence,
                        onValueChange = { newEvidence = it },
                        label = { Text("Source Evidence / Citation (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFront.isNotBlank() && newBack.isNotBlank()) {
                            val created = CardJsonItem(front = newFront.trim(), back = newBack.trim(), evidence = newEvidence.trim())
                            localDeck.add(created)
                            onAddCard?.invoke(created)
                            newFront = ""
                            newBack = ""
                            newEvidence = ""
                            showAddCardDialog = false
                            Toast.makeText(context, "Card added to deck! ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newFront.isNotBlank() && newBack.isNotBlank()
                ) {
                    Text("Add Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal 2: Export to Anki CSV Modal (M15)
    if (showExportDialog) {
        val tsvData = remember(localDeck.toList()) { AnkiExporter.exportToAnkiTsv(localDeck) }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("📤 Export to Anki (CSV / TSV)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Export ${localDeck.size} cards formatted for Anki desktop and mobile apps with tab-separation and tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Text(
                            text = tsvData,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(tsvData))
                        Toast.makeText(context, "Anki TSV copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, tsvData)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Anki Deck"))
                            showExportDialog = false
                        }
                    ) {
                        Text("Share")
                    }
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Bottom Sheet: "Explain This" (M11)
    if (showExplainSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExplainSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Explain This (${language.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isExplaining) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Concept: ${currentCard.front}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentCard.evidence.isNotBlank()) {
                            Text(
                                text = "Evidence: \"${currentCard.evidence}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (explainText.isNotBlank()) {
                    MarkdownView(markdown = explainText)
                } else if (isExplaining) {
                    Text(
                        text = "Gemma 4 E2B is explaining this concept with intuitive real-world analogies...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showExplainSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It!")
                }
            }
        }
    }
}
