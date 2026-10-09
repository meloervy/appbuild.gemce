package com.appbuild.gemce.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FakeEngine(
    private val simulatedDelayMs: Long = 60L
) : LlmEngine {

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Unloaded)
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    override val isReady: Boolean
        get() = _status.value is EngineStatus.Ready

    override suspend fun initialize(): Result<Unit> {
        _status.value = EngineStatus.Loading(0.5f, "Initializing FakeEngine...")
        delay(simulatedDelayMs)
        _status.value = EngineStatus.Ready
        return Result.success(Unit)
    }

    override suspend fun generateStream(prompt: String): Flow<String> = flow {
        val sampleOutputs = when {
            prompt.contains("grader", ignoreCase = true) || prompt.contains("evaluat", ignoreCase = true) -> {
                // If student answer is short or empty
                if (prompt.contains("STUDENT'S ANSWER:\n\n") || prompt.contains("I don't know", ignoreCase = true)) {
                    listOf(
                        """{"verdict":"incorrect","score":0.0,"feedback":"Answer was missing the core concept.","missing":["cellular respiration","ATP"]}"""
                    )
                } else if (prompt.contains("ATP", ignoreCase = true) && !prompt.contains("respiration", ignoreCase = true)) {
                    listOf(
                        """{"verdict":"partial","score":0.6,"feedback":"Magaling! You identified ATP, but you forgot to mention cellular respiration.","missing":["cellular respiration"]}"""
                    )
                } else {
                    listOf(
                        """{"verdict":"correct","score":1.0,"feedback":"Tumpak! Complete explanation touching all rubric points.","missing":[]}"""
                    )
                }
            }

            prompt.contains("short-answer", ignoreCase = true) || prompt.contains("type\":\"short", ignoreCase = true) -> listOf(
                """{"type":"short","stem":"Paano gumagawa ng ATP ang mitochondria sa loob ng cell?","reference_answer":"Sa pamamagitan ng cellular respiration, kung saan kino-convert ang nutrients at oxygen upang maging ATP.","rubric":["cellular respiration","nutrients and oxygen converted to ATP"],"evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"short","stem":"Why is ATP called the cell's energy currency?","reference_answer":"Because it stores and transfers energy needed for almost all biochemical cellular activities.","rubric":["stores and transports chemical energy","fuels biochemical reactions"],"evidence":"ATP is used by cells as universal energy currency."}"""
            )

            prompt.contains("multiple-choice", ignoreCase = true) || prompt.contains("type\":\"mcq", ignoreCase = true) -> listOf(
                """{"type":"mcq","stem":"Which organelle is universally referred to as the powerhouse of the cell?","choices":["Ribosome","Mitochondrion","Endoplasmic Reticulum","Golgi Complex"],"answer":1,"explanation":"Mitochondria perform cellular respiration to produce energy.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"mcq","stem":"Ano ang pangunahing enerhiya na ginagawa ng mitochondria?","choices":["Glucose","Glycogen","ATP (Adenosine Triphosphate)","Lactic Acid"],"answer":2,"explanation":"ATP ang energy currency na ginagawa ng mitochondria.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"mcq","stem":"What biological process converts nutrients into ATP?","choices":["Photosynthesis","Fermentation","Cellular Respiration","Meiosis"],"answer":2,"explanation":"Cellular respiration uses glucose and oxygen to generate ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
            )

            prompt.contains("Ask Gemce", ignoreCase = true) || prompt.contains("cite", ignoreCase = true) -> listOf(
                "Ayon sa iyong notes [S1], ang pangunahing function ng **mitochondria** ay ang pag-produce ng ATP sa pamamagitan ng *cellular respiration*.\n\n",
                "Ito ang tinatawag na 'powerhouse of the cell' dahil ang ATP [S1] ang nagbibigay ng energy para sa mga metabolic activities.\n\n",
                "[unverified] Karagdagang info: May sariling DNA (mtDNA) ang mitochondria na namamana mula sa maternal lineage."
            )

            prompt.contains("point", ignoreCase = true) || prompt.contains("reviewer", ignoreCase = true) -> listOf(
                """{"type":"point","text":"Ang mitochondria ang responsible sa paggawa ng cellular energy (ATP).","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"point","text":"Cellular respiration converts nutrients into usable chemical energy.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"point","text":"Lahat ng vital biochemical processes sa katawan ay umaasa sa ATP.","evidence":"ATP is used by cells as universal energy currency."}"""
            )

            else -> listOf(
                """{"type":"card","front":"Ano ang pangunahing function ng mitochondria?","back":"Gumagawa ng ATP (energy currency) para sa cell sa pamamagitan ng cellular respiration.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"card","front":"What does ATP stand for and what is its role?","back":"Adenosine Triphosphate; it acts as the primary chemical energy currency of the cell.","evidence":"ATP is used by cells as universal energy currency."}""",
                """{"type":"card","front":"Anong proseso ang ginagamit upang makabuo ng ATP?","back":"Cellular respiration ang proseso na nagko-convert ng nutrients at oxygen papuntang ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
            )
        }

        for (line in sampleOutputs) {
            delay(simulatedDelayMs)
            emit(line + if (line.endsWith("\n")) "" else "\n")
        }
    }

    override suspend fun unload() {
        _status.value = EngineStatus.Unloaded
    }
}
