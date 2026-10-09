package com.appbuild.gemce.ui.study

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
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.prompt.PromptBuilder
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AskGemceChat(
    sections: List<Section>,
    engine: LlmEngine,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "Hello! Ako si Gemce. Tanungin mo ako tungkol sa notes mo at sasagutin ko gamit ang iyong mga pinag-aralan with section citations [S1]!"
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
        // Chat Header
        Text(
            text = "Ask Gemce (Q&A)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Offline Q&A with strict note citations [S1]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Message List
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                val bubbleColor = if (msg.isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant

                val alignment = if (msg.isUser) Alignment.End else Alignment.Start

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bubbleColor),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (msg.isUser) "You" else "Gemce",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (!msg.isUser && msg.text.contains("[unverified]")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "Contains unverified outside knowledge",
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

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask a question about your notes...") },
                modifier = Modifier.weight(1f),
                enabled = !isReplying
            )

            Button(
                onClick = {
                    val query = inputText.trim()
                    if (query.isNotBlank()) {
                        messages.add(ChatMessage(isUser = true, text = query))
                        inputText = ""
                        isReplying = true

                        coroutineScope.launch {
                            val labeledSections = sections.take(3).mapIndexed { idx, sec ->
                                "S${idx + 1}" to sec.text
                            }

                            val prompt = PromptBuilder.buildAskGemcePrompt(
                                sectionsWithLabels = labeledSections,
                                question = query
                            )

                            var streamedResponse = ""
                            val botMsgIndex = messages.size
                            messages.add(ChatMessage(isUser = false, text = "..."))

                            engine.generateStream(prompt).collect { chunk ->
                                streamedResponse += chunk
                                messages[botMsgIndex] = ChatMessage(isUser = false, text = streamedResponse)
                            }
                            isReplying = false
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isReplying,
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
