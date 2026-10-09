package com.appbuild.gemce.ui.chat

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import com.appbuild.gemce.ui.study.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun ContextualChatScreen(
    engine: LlmEngine,
    language: LanguageMode = LanguageMode.EN,
    studentProfile: StudentProfile? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allDocuments by DocumentRepository.documents.collectAsState()
    val activeDocs = allDocuments.filter { it.isIncludedInContext }
    val activeSections = activeDocs.flatMap { it.sections }

    val welcomeGreeting = when (language) {
        LanguageMode.EN -> "Hello! I'm Gemce, your offline AI study buddy. Your local notes are loaded (${activeDocs.size} documents, ${activeSections.size} sections). Ask me anything about your study materials!"
        LanguageMode.FIL -> "Kumusta! Ako si Gemce, ang iyong offline AI study buddy. Naka-load ang iyong mga tala (${activeDocs.size} documents, ${activeSections.size} sections). Magtanong tungkol sa iyong aralin!"
        LanguageMode.TAGLISH -> "Kumusta! Ako si Gemce. Naka-load ang iyong local notes (${activeDocs.size} documents, ${activeSections.size} sections). Magtanong ka lang tungkol sa mga aralin mo!"
        LanguageMode.CEB -> "Kumusta! Ako si Gemce, ang imong offline AI study buddy. Naka-load ang imong mga notes (${activeDocs.size} documents, ${activeSections.size} sections). Pangutana bahin sa imong leksyon!"
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = welcomeGreeting
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Active Context Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Context Scope (${activeDocs.size} files active)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    studentProfile?.let {
                        Text(
                            text = "🎓 ${it.getBadgeText()} • ${language.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (activeDocs.isEmpty()) {
                    Text(
                        text = "No documents active! Open Library tab and toggle 'Include in AI Context' to enable file search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = activeDocs.joinToString(", ") { "${it.fileType.icon} ${it.title}" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message List
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                val bubbleColor = if (msg.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val align = if (msg.isUser) Alignment.End else Alignment.Start

                Column(
                    horizontalAlignment = align,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = bubbleColor,
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (msg.isUser) "You" else "Gemce AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayText = if (!msg.isUser) {
                                try {
                                    val trimmed = msg.text.trim()
                                    if (trimmed.startsWith("{") && trimmed.contains("\"answer\"")) {
                                        org.json.JSONObject(trimmed).optString("answer", msg.text)
                                    } else {
                                        msg.text
                                    }
                                } catch (_: Exception) {
                                    msg.text
                                }
                            } else {
                                msg.text
                            }
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Unverified warning badge for outside knowledge
                            if (!msg.isUser && msg.text.contains("[unverified]", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "Contains unverified outside knowledge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about your notes...") },
                modifier = Modifier.weight(1f),
                enabled = !isReplying && activeDocs.isNotEmpty()
            )

            Button(
                onClick = {
                    val query = inputText.trim()
                    if (query.isNotBlank()) {
                        messages.add(ChatMessage(isUser = true, text = query))
                        inputText = ""
                        isReplying = true

                        coroutineScope.launch {
                            val labeledSections = activeSections.take(4).mapIndexed { idx, sec ->
                                "${sec.documentTitle} > S${idx + 1}" to sec.text
                            }

                            val prompt = PromptBuilder.buildAskGemcePrompt(
                                sectionsWithLabels = labeledSections,
                                question = query,
                                language = language,
                                profile = studentProfile
                            )

                            var streamedResponse = ""
                            val botMsgIndex = messages.size
                            messages.add(ChatMessage(isUser = false, text = "..."))

                            try {
                                engine.generateStream(prompt).collect { chunk ->
                                    streamedResponse += chunk
                                    messages[botMsgIndex] = ChatMessage(isUser = false, text = streamedResponse)
                                }
                            } catch (e: Exception) {
                                messages[botMsgIndex] = ChatMessage(isUser = false, text = "AI response notice: ${e.localizedMessage ?: e.message}")
                            } finally {
                                isReplying = false
                            }
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isReplying && activeDocs.isNotEmpty(),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (isReplying) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send")
                }
            }
        }
    }
}
