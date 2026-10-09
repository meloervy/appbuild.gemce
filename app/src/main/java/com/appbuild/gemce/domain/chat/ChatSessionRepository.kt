package com.appbuild.gemce.domain.chat

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class ChatMessageData(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<ChatMessageData> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

object ChatSessionRepository {
    private const val FILE_NAME = "chat_sessions.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var storageFile: File? = null

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun initialize(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        storageFile = file
        if (file.exists()) {
            try {
                val content = file.readText()
                val loaded = json.decodeFromString<List<ChatSession>>(content)
                _sessions.value = loaded
                _activeSessionId.value = loaded.firstOrNull()?.id
            } catch (_: Exception) {
                createNewSession()
            }
        }
        if (_sessions.value.isEmpty()) {
            createNewSession()
        }
    }

    fun createNewSession(title: String = "New Chat"): ChatSession {
        val newSession = ChatSession(title = title)
        _sessions.update { listOf(newSession) + it }
        _activeSessionId.value = newSession.id
        save()
        return newSession
    }

    fun selectSession(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        _sessions.update { list -> list.filterNot { it.id == sessionId } }
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = _sessions.value.firstOrNull()?.id
            if (_sessions.value.isEmpty()) {
                createNewSession()
            }
        }
        save()
    }

    fun addMessage(sessionId: String, message: ChatMessageData) {
        _sessions.update { list ->
            list.map { session ->
                if (session.id == sessionId) {
                    val updatedMessages = session.messages + message
                    val updatedTitle = if (session.title == "New Chat" && message.isUser) {
                        val clean = message.text.trim().take(30)
                        if (message.text.length > 30) "$clean..." else clean
                    } else {
                        session.title
                    }
                    session.copy(
                        title = updatedTitle,
                        messages = updatedMessages,
                        updatedAt = System.currentTimeMillis()
                    )
                } else session
            }
        }
        save()
    }

    fun updateLastMessage(sessionId: String, updatedText: String) {
        _sessions.update { list ->
            list.map { session ->
                if (session.id == sessionId && session.messages.isNotEmpty()) {
                    val updatedMessages = session.messages.toMutableList()
                    val last = updatedMessages.last()
                    updatedMessages[updatedMessages.lastIndex] = last.copy(text = updatedText)
                    session.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
                } else session
            }
        }
        save()
    }

    private fun save() {
        try {
            storageFile?.let { file ->
                file.writeText(json.encodeToString(_sessions.value))
            }
        } catch (_: Exception) {
        }
    }
}
