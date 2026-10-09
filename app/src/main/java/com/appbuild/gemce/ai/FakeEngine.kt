package com.appbuild.gemce.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FakeEngine(
    private val simulatedDelayMs: Long = 100L
) : LlmEngine {

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Unloaded)
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    override val isReady: Boolean
        get() = _status.value is EngineStatus.Ready

    override suspend fun initialize(): Result<Unit> {
        _status.value = EngineStatus.Loading(0.5f, "Loading simulated model...")
        delay(simulatedDelayMs * 2)
        _status.value = EngineStatus.Ready
        return Result.success(Unit)
    }

    override suspend fun generateStream(prompt: String): Flow<String> = flow {
        val sampleOutputs = when {
            prompt.contains("short", ignoreCase = true) || prompt.contains("grade", ignoreCase = true) -> listOf(
                """{"verdict":"correct","score":1.0,"feedback":"Spot-on explanation! You mentioned ATP and cellular respiration.","missing":[]}"""
            )
            prompt.contains("mcq", ignoreCase = true) -> listOf(
                """{"type":"mcq","stem":"Which organelle is considered the powerhouse of the cell?","choices":["Ribosome","Mitochondrion","Endoplasmic Reticulum","Golgi Apparatus"],"answer":1,"explanation":"Mitochondria produce the majority of cellular ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"mcq","stem":"Ano ang pangunahing produkto ng cellular respiration?","choices":["Glucose","ATP","Oxygen","Lipids"],"answer":1,"explanation":"ATP ang pangunahing energy currency ng cell.","evidence":"cellular respiration produces ATP energy."}"""
            )
            else -> listOf(
                """{"type":"card","front":"Ano ang function ng mitochondria?","back":"Gumagawa ng ATP (energy) para sa cell sa pamamagitan ng cellular respiration.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"card","front":"What is the main role of ATP in cellular processes?","back":"It acts as the primary energy currency for biochemical reactions.","evidence":"ATP is used by cells as universal energy currency."}""",
                """{"type":"point","text":"Mitochondria generate most of the chemical energy needed by the cell.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
            )
        }

        for (line in sampleOutputs) {
            delay(simulatedDelayMs)
            emit(line + "\n")
        }
    }

    override suspend fun unload() {
        _status.value = EngineStatus.Unloaded
    }
}
