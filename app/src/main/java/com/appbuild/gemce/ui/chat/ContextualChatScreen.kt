package com.appbuild.gemce.ui.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import com.appbuild.gemce.ui.study.ChatMessage
import kotlinx.coroutines.launch
import org.json.JSONObject

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
        LanguageMode.EN -> "Hello! I'm Gemce, your offline AI study buddy. Ask me anything about your loaded notes, or any academic topic like System Design, Computer Science, Math, or Science!"
        LanguageMode.FIL -> "Kumusta! Ako si Gemce, ang iyong offline AI study buddy. Magtanong tungkol sa iyong notes o anumang aralin tulad ng System Design, IT, Math, o Science!"
        LanguageMode.TAGLISH -> "Kumusta! Ako si Gemce. Pwede kang magtanong tungkol sa notes mo, or kahit anong academic subjects like System Design, Programming, Math, or Science!"
        LanguageMode.CEB -> "Kumusta! Ako si Gemce, ang imong offline AI study buddy. Pangutana bahin sa imong notes o bisan unsang academic subject sama sa System Design, IT, Math, o Science!"
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

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isReplying) return

        messages.add(ChatMessage(isUser = true, text = trimmed))
        inputText = ""
        isReplying = true

        coroutineScope.launch {
            val labeledSections = activeSections.take(4).mapIndexed { idx, sec ->
                "${sec.documentTitle} > S${idx + 1}" to sec.text
            }

            val prompt = PromptBuilder.buildAskGemcePrompt(
                sectionsWithLabels = labeledSections,
                question = trimmed,
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
                messages[botMsgIndex] = ChatMessage(isUser = false, text = "AI notice: ${e.localizedMessage ?: e.message}")
            } finally {
                isReplying = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Active Scope Banner
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (activeDocs.isNotEmpty()) "📚" else "💡", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (activeDocs.isNotEmpty()) "Study Notes Active (${activeDocs.size})" else "General Academic Tutor Mode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (activeDocs.isNotEmpty()) activeDocs.joinToString(", ") { it.title } else "Ready to teach any subject: System Design, IT, Math, Science",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message List
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                val bubbleColor = if (msg.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val align = if (msg.isUser) Alignment.End else Alignment.Start

                Column(
                    horizontalAlignment = align,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        ),
                        color = bubbleColor,
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (msg.isUser) "👤 You" else "🤖 Gemce AI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val displayText = if (!msg.isUser) {
                                unwrapAiResponse(msg.text)
                            } else {
                                msg.text
                            }

                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Tag badge for outside knowledge or notes citation
                            if (!msg.isUser) {
                                if (displayText.contains("[General Academic Knowledge]", ignoreCase = true) || displayText.contains("[General Academic Study Knowledge]", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "🎓 Broad Academic Subject Tutoring",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else if (displayText.contains("[S1]", ignoreCase = true) || displayText.contains("[S2]", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "📖 Grounded in Your Source Notes",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "💻 System Design & Architecture",
                "🏛️ Microservices vs Monolith",
                "🗄️ Caching & Database Sharding",
                "🧬 Cellular Respiration & ATP"
            )
            suggestions.forEach { promptSuggestion ->
                val cleanText = promptSuggestion.substringAfter(" ")
                SuggestionChip(
                    onClick = { sendMessage(cleanText) },
                    label = { Text(promptSuggestion, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    enabled = !isReplying
                )
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
                placeholder = {
                    Text(
                        text = if (activeDocs.isNotEmpty()) "Ask about notes or any topic..." else "Ask any subject (e.g. System Design)...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isReplying,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { sendMessage(inputText) },
                enabled = inputText.isNotBlank() && !isReplying,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(52.dp)
            ) {
                if (isReplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun unwrapAiResponse(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{")) {
        try {
            val json = org.json.JSONObject(trimmed)
            if (json.has("response")) return json.getString("response")
            if (json.has("answer")) return json.getString("answer")
        } catch (_: Exception) {
            for (key in listOf("\"response\"", "\"answer\"")) {
                val idx = trimmed.indexOf(key)
                if (idx != -1) {
                    val colon = trimmed.indexOf(":", idx)
                    if (colon != -1) {
                        var content = trimmed.substring(colon + 1).trim()
                        if (content.startsWith("\"")) content = content.substring(1)
                        if (content.endsWith("\"}")) content = content.substring(0, content.length - 2)
                        else if (content.endsWith("\"")) content = content.substring(0, content.length - 1)
                        return content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                    }
                }
            }
        }
    }
    return raw
}

