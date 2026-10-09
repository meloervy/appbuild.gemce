package com.appbuild.gemce.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface EngineStatus {
    data object Unloaded : EngineStatus
    data class Loading(val progress: Float = 0f, val message: String = "") : EngineStatus
    data object Ready : EngineStatus
    data class Error(val throwable: Throwable) : EngineStatus
}

interface LlmEngine {
    val status: StateFlow<EngineStatus>
    val isReady: Boolean

    suspend fun initialize(): Result<Unit>
    suspend fun generateStream(prompt: String): Flow<String>
    suspend fun unload()
}
