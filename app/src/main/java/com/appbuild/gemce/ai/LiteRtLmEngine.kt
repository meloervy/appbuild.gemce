package com.appbuild.gemce.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Production implementation of LlmEngine using LiteRT-LM Kotlin API
 * and Gemma 4 E2B on-device model as per PLAN.md.
 */
class LiteRtLmEngine(
    private val context: Context,
    private val preferredBackend: Backend = Backend.CPU()
) : LlmEngine {

    companion object {
        const val MODEL_FILENAME = ModelStorageManager.MODEL_FILENAME
    }

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Unloaded)
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private var engine: Engine? = null

    override val isReady: Boolean
        get() = _status.value is EngineStatus.Ready && engine != null

    val modelFile: File
        get() = ModelStorageManager.resolveModelFile(context)

    fun isModelInstalled(): Boolean = ModelStorageManager.isModelInstalled(context)

    fun getStorageDescription(): String = ModelStorageManager.getModelLocationDescription(context)

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        val resolvedFile = modelFile
        if (!isModelInstalled()) {
            val err = IllegalStateException("Model file not found at ${resolvedFile.absolutePath}. Please download or push via adb.")
            _status.value = EngineStatus.Error(err)
            return@withContext Result.failure(err)
        }

        try {
            _status.value = EngineStatus.Loading(0.1f, "Initializing Gemma 4 E2B engine...")

            val cacheDirectory = File(context.cacheDir, "litert_cache").apply { mkdirs() }
            val config = EngineConfig(
                modelPath = resolvedFile.absolutePath,
                backend = preferredBackend,
                cacheDir = cacheDirectory.absolutePath
            )

            val newEngine = Engine(config)
            newEngine.initialize()

            engine = newEngine
            _status.value = EngineStatus.Ready
            Result.success(Unit)
        } catch (t: Throwable) {
            _status.value = EngineStatus.Error(t)
            Result.failure(t)
        }
    }

    override suspend fun generateStream(prompt: String): Flow<String> = flow {
        if (!isReady) {
            val initRes = initialize()
            if (initRes.isFailure) {
                emit("Error initializing Gemma 4 E2B engine: ${initRes.exceptionOrNull()?.message}")
                return@flow
            }
        }
        val currentEngine = engine ?: error("Engine is not initialized")
        val conversation: Conversation = currentEngine.createConversation(
            ConversationConfig()
        )

        try {
            conversation.sendMessageAsync(prompt).collect { message ->
                emit(message.toString())
            }
        } finally {
            // Short-lived conversation per task as specified in PLAN.md 5.6
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun unload() {
        withContext(Dispatchers.IO) {
            try {
                engine?.close()
            } catch (_: Exception) {
            } finally {
                engine = null
                _status.value = EngineStatus.Unloaded
            }
        }
    }
}
