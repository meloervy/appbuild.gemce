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
        if (!isReady) {
            initialize()
        }
        val isEnglish = prompt.contains("English", ignoreCase = true) ||
                (!prompt.contains("Taglish", ignoreCase = true) && !prompt.contains("Tagalog", ignoreCase = true) && !prompt.contains("Bisaya", ignoreCase = true))

        val sampleOutputs = resolveOutputs(prompt, isEnglish)

        for (line in sampleOutputs) {
            delay(simulatedDelayMs)
            emit(line + if (line.endsWith("\n")) "" else "\n")
        }
    }

    override suspend fun unload() {
        _status.value = EngineStatus.Unloaded
    }

    private fun resolveOutputs(prompt: String, isEnglish: Boolean): List<String> {
        val topicType = detectTopic(prompt)

        return when {
            // 1. Grader / Evaluator
            prompt.contains("grader", ignoreCase = true) || prompt.contains("evaluat", ignoreCase = true) -> {
                evaluateStudentAnswer(prompt)
            }

            // 2. Short Answer Questions
            prompt.contains("short-answer", ignoreCase = true) || prompt.contains("type\":\"short", ignoreCase = true) -> {
                generateShortAnswers(prompt, topicType, isEnglish)
            }

            // 3. Multiple Choice Questions
            prompt.contains("multiple-choice", ignoreCase = true) || prompt.contains("type\":\"mcq", ignoreCase = true) -> {
                generateMcqs(prompt, topicType, isEnglish)
            }

            // 4. Ask Gemce Q&A / Chat
            prompt.contains("Ask Gemce", ignoreCase = true) || prompt.contains("STUDENT'S CURRENT QUESTION:", ignoreCase = true) || prompt.contains("STUDENT'S QUESTION:", ignoreCase = true) -> {
                generateChatResponse(prompt, topicType, isEnglish)
            }

            // 5. Explain This Concept
            prompt.contains("CONCEPT TO EXPLAIN:", ignoreCase = true) || prompt.contains("Explain this specific academic concept", ignoreCase = true) -> {
                generateExplainThisResponse(prompt, topicType, isEnglish)
            }

            // 6. Reviewer Key Points
            prompt.contains("point", ignoreCase = true) || prompt.contains("reviewer", ignoreCase = true) -> {
                generateReviewerPoints(prompt, topicType, isEnglish)
            }

            // 7. Flashcards (Default)
            else -> {
                generateFlashcards(prompt, topicType, isEnglish)
            }
        }
    }

    private enum class TopicCategory {
        RIZAL,
        DISCRETE_MATH,
        EMERGING_TECH,
        BIOLOGY,
        SYSTEM_DESIGN,
        CUSTOM_NOTES,
        GENERAL
    }

    private fun detectTopic(prompt: String): TopicCategory {
        val userContent = when {
            prompt.contains("STUDENT'S CURRENT QUESTION:\n") -> prompt.substringAfter("STUDENT'S CURRENT QUESTION:\n").trim()
            prompt.contains("CONCEPT TO EXPLAIN:\n") -> prompt.substringAfter("CONCEPT TO EXPLAIN:\n").trim()
            prompt.contains("SOURCE TEXT:\n") -> prompt.substringAfter("SOURCE TEXT:\n").trim()
            prompt.contains("QUESTION:\n") -> prompt.substringAfter("QUESTION:\n").substringBefore("REFERENCE ANSWER:").trim()
            else -> prompt
        }
        val lower = userContent.lowercase()
        return when {
            lower.contains("rizal") || lower.contains("noli") || lower.contains("fili") ||
                    lower.contains("dapitan") || lower.contains("bagumbayan") || lower.contains("gomburza") -> TopicCategory.RIZAL

            lower.contains("discrete math") || lower.contains("discrete") || lower.contains("pigeonhole") ||
                    lower.contains("graph theory") || lower.contains("combinatorics") || lower.contains("truth table") ||
                    lower.contains("propositional logic") || lower.contains("power set") -> TopicCategory.DISCRETE_MATH

            lower.contains("emerging tech") || lower.contains("emerging") || lower.contains("quantum computing") ||
                    lower.contains("qubit") || lower.contains("blockchain") || lower.contains("smart contract") ||
                    lower.contains("edge computing") || lower.contains("generative ai") || lower.contains("transformer") -> TopicCategory.EMERGING_TECH

            lower.contains("mitochondria") || lower.contains("cellular respiration") || lower.contains("atp") ||
                    lower.contains("chloroplast") || lower.contains("organelle") -> TopicCategory.BIOLOGY

            lower.contains("system design") || lower.contains("microservice") || lower.contains("database sharding") ||
                    lower.contains("load balancer") || lower.contains("caching") -> TopicCategory.SYSTEM_DESIGN

            extractSubstantiveSentences(prompt).isNotEmpty() -> TopicCategory.CUSTOM_NOTES

            else -> TopicCategory.GENERAL
        }
    }

    private fun extractSubstantiveSentences(prompt: String): List<String> {
        val sourceText = when {
            prompt.contains("SOURCE TEXT:\n") -> prompt.substringAfter("SOURCE TEXT:\n").trim()
            prompt.contains("NOTES:\n") -> prompt.substringAfter("NOTES:\n").substringBefore("STUDENT'S CURRENT QUESTION:").substringBefore("PRIOR CONVERSATION CONTEXT:").trim()
            else -> ""
        }
        if (sourceText.length < 25 || sourceText.contains("No specific notes currently attached")) {
            return emptyList()
        }
        val lines = sourceText.split(Regex("""(?<=[.?!])\s+|\n+"""))
            .map { it.trim().trimEnd('.') }
            .filter { it.length >= 20 && !it.startsWith("[") && !it.startsWith("TASK:") }

        return if (lines.isNotEmpty()) lines else listOf(sourceText.trim().trimEnd('.'))
    }

    private fun evaluateStudentAnswer(prompt: String): List<String> {
        val rubricPoints = prompt.substringAfter("RUBRIC POINTS:", "")
            .substringBefore("STUDENT'S ANSWER:", "")
            .lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }

        val studentAnswer = prompt.substringAfter("STUDENT'S ANSWER:", "").trim()

        return if (studentAnswer.isBlank() || studentAnswer.equals("I don't know", ignoreCase = true) || studentAnswer.length < 4) {
            val missing = if (rubricPoints.isNotEmpty()) rubricPoints.take(2) else listOf("Core concept explanation")
            val missingJson = missing.joinToString("\",\"", prefix = "[\"", postfix = "\"]")
            listOf("""{"verdict":"incorrect","score":0.0,"feedback":"Answer was missing the core conceptual explanation.","missing":$missingJson}""")
        } else if (studentAnswer.length < 20) {
            val missing = if (rubricPoints.isNotEmpty()) listOf(rubricPoints.last()) else listOf("Deeper elaboration")
            val missingJson = missing.joinToString("\",\"", prefix = "[\"", postfix = "\"]")
            listOf("""{"verdict":"partial","score":0.6,"feedback":"Good start! Make sure to expand further on the mechanism and details.","missing":$missingJson}""")
        } else {
            listOf("""{"verdict":"correct","score":1.0,"feedback":"Excellent response! Clear and comprehensive explanation covering the essential points.","missing":[]}""")
        }
    }

    private fun generateFlashcards(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        if (topic == TopicCategory.CUSTOM_NOTES) {
            val sentences = extractSubstantiveSentences(prompt)
            if (sentences.isNotEmpty()) {
                return sentences.take(4).map { s ->
                    val concept = s.take(35).trimEnd()
                    """{"type":"card","front":"What is the key insight regarding: $concept?","back":"$s.","evidence":"$s."}"""
                }
            }
        }

        return when (topic) {
            TopicCategory.RIZAL -> if (isEnglish) {
                listOf(
                    """{"type":"card","front":"What was the primary objective of Jose Rizal's novel 'Noli Me Tangere'?","back":"To expose the social cancer, friar abuses, and colonial corruption in 19th-century Philippines.","evidence":"Noli Me Tangere was written to expose the social cancer and friar abuses in the Philippines."}""",
                    """{"type":"card","front":"What was La Liga Filipina founded by Rizal in 1892?","back":"A progressive civic society advocating mutual assistance, educational advancement, and peaceful socio-economic reform.","evidence":"Rizal established La Liga Filipina in Tondo to unite the archipelago and foster mutual assistance."}""",
                    """{"type":"card","front":"To whom did Jose Rizal dedicate 'El Filibusterismo'?","back":"To the three Filipino secular priests Gomez, Burgos, and Zamora (Gomburza) executed in 1872.","evidence":"El Filibusterismo was dedicated to the martyrdom of Gomburza in 1872."}""",
                    """{"type":"card","front":"What poem did Rizal write on the eve of his execution at Bagumbayan?","back":"'Mi Último Adiós' (My Last Farewell), hidden inside an alcohol cooking lamp.","evidence":"Rizal wrote Mi Último Adiós on the eve of his execution at Bagumbayan on December 30, 1896."}"""
                )
            } else {
                listOf(
                    """{"type":"card","front":"Ano ang pangunahing layunin ng nobelang 'Noli Me Tangere'?","back":"Ibunyag ang kanser ng lipunan, pang-aabuso ng mga prayle, at katiwalian ng pamahalaang kolonyal.","evidence":"Noli Me Tangere was written to expose the social cancer and friar abuses in the Philippines."}""",
                    """{"type":"card","front":"Ano ang La Liga Filipina na itinatag ni Rizal noong 1892?","back":"Isang samahang sibiko para sa pagtutulungan, edukasyon, at mapayapang reporma sa Pilipinas.","evidence":"Rizal established La Liga Filipina in Tondo to unite the archipelago and foster mutual assistance."}""",
                    """{"type":"card","front":"Kanino inihandog ni Jose Rizal ang nobelang 'El Filibusterismo'?","back":"Sa tatlong paring martir na sina Mariano Gomez, Jose Burgos, at Jacinto Zamora (Gomburza).","evidence":"El Filibusterismo was dedicated to the martyrdom of Gomburza in 1872."}""",
                    """{"type":"card","front":"Ano ang huling tulang isinulat ni Rizal bago siya barilin sa Bagumbayan?","back":"'Mi Último Adiós' (Huling Paalam), na itinago sa loob ng lamparang lutuan.","evidence":"Rizal wrote Mi Último Adiós on the eve of his execution at Bagumbayan on December 30, 1896."}"""
                )
            }

            TopicCategory.DISCRETE_MATH -> if (isEnglish) {
                listOf(
                    """{"type":"card","front":"What is the Pigeonhole Principle in Discrete Mathematics?","back":"If n items are placed into k containers and n > k, at least one container must hold 2 or more items.","evidence":"The pigeonhole principle states that if n items are distributed into k boxes and n > k, at least one box contains >= 2 items."}""",
                    """{"type":"card","front":"What is the cardinality of the power set P(S) of a finite set with n elements?","back":"2^n, representing all possible subsets including the empty set and the set itself.","evidence":"The cardinality of the power set of a set with n elements is 2^n."}""",
                    """{"type":"card","front":"What characterizes an Eulerian Path in Graph Theory?","back":"A trail in a finite graph that visits every edge exactly once.","evidence":"An Eulerian path is a graph trail that visits every edge exactly once."}""",
                    """{"type":"card","front":"What is the truth value of a conditional proposition (p -> q) when hypothesis p is FALSE?","back":"It is vacuously true, regardless of the truth value of q.","evidence":"A conditional statement p -> q is true whenever the hypothesis p is false."}"""
                )
            } else {
                listOf(
                    """{"type":"card","front":"Ano ang Pigeonhole Principle sa Discrete Mathematics?","back":"Kung may n na bagay sa k na lalagyan at n > k, kahit isang lalagyan ay maglalaman ng 2 o higit pang bagay.","evidence":"The pigeonhole principle states that if n items are distributed into k boxes and n > k, at least one box contains >= 2 items."}""",
                    """{"type":"card","front":"Ano ang sukat o cardinality ng power set P(S) na may n elemento?","back":"2^n, na sumasaklaw sa lahat ng posibleng subsets kabilang ang empty set.","evidence":"The cardinality of the power set of a set with n elements is 2^n."}""",
                    """{"type":"card","front":"Ano ang katangian ng Eulerian Path sa Graph Theory?","back":"Isang landas sa graph na dumadaan sa bawat linya o edge nang eksaktong isang beses lamang.","evidence":"An Eulerian path is a graph trail that visits every edge exactly once."}""",
                    """{"type":"card","front":"Kailan totoo ang conditional proposition (p -> q) kapag mali si p?","back":"Ito ay laging totoo (vacuously true) kapag ang simula (hypothesis p) ay mali.","evidence":"A conditional statement p -> q is true whenever the hypothesis p is false."}"""
                )
            }

            TopicCategory.EMERGING_TECH -> if (isEnglish) {
                listOf(
                    """{"type":"card","front":"What is Quantum Superposition in Quantum Computing?","back":"The principle allowing a qubit to exist in a linear combination of states |0> and |1> simultaneously.","evidence":"Qubits leverage quantum superposition to represent linear combinations of 0 and 1 simultaneously."}""",
                    """{"type":"card","front":"How does Edge Computing differ from centralized Cloud Computing?","back":"Edge computing processes workloads locally near the data source to minimize latency and bandwidth consumption.","evidence":"Edge computing processes data near the source to reduce network bandwidth and round-trip latency."}""",
                    """{"type":"card","front":"What is the fundamental role of Consensus Mechanisms in Blockchain?","back":"Ensuring distributed nodes agree on a single, immutable state of ledger transactions without central trust.","evidence":"Consensus mechanisms enable decentralized nodes to validate ledger transactions without central trust."}""",
                    """{"type":"card","front":"What core mechanism allows Transformers to process entire language sequences in parallel?","back":"The Self-Attention mechanism, which calculates context-dependent weights across all tokens in a prompt.","evidence":"Transformers utilize self-attention mechanisms to compute token contextual dependencies across entire sequences."}"""
                )
            } else {
                listOf(
                    """{"type":"card","front":"Ano ang Quantum Superposition sa Quantum Computing?","back":"Prinsipyo kung saan ang qubit ay maaaring maging 0 at 1 nang sabay sa pamamagitan ng linear combination.","evidence":"Qubits leverage quantum superposition to represent linear combinations of 0 and 1 simultaneously."}""",
                    """{"type":"card","front":"Paano naiiba ang Edge Computing sa Cloud Computing?","back":"Pinoproseso ang datos nang lokal malapit sa sensor o device upang mapabilis ang response time at mabawasan ang network lag.","evidence":"Edge computing processes data near the source to reduce network bandwidth and round-trip latency."}""",
                    """{"type":"card","front":"Ano ang silbi ng Consensus Mechanism sa Blockchain?","back":"Pinagkakasundo ang lahat ng nodes sa network sa katumpakan ng mga transaksyon nang walang sentral na bangko o awtoridad.","evidence":"Consensus mechanisms enable decentralized nodes to validate ledger transactions without central trust."}""",
                    """{"type":"card","front":"Bakit mabilis at mabisa ang Transformers sa Modernong AI?","back":"Dahil sa Self-Attention mechanism na sabay-sabay sumusuri sa ugnayan ng mga salita sa buong pangungusap.","evidence":"Transformers utilize self-attention mechanisms to compute token contextual dependencies across entire sequences."}"""
                )
            }

            TopicCategory.BIOLOGY -> if (isEnglish) {
                listOf(
                    """{"type":"card","front":"What is the primary function of mitochondria?","back":"Generating ATP (cellular energy currency) through cellular respiration.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                    """{"type":"card","front":"What does ATP stand for and what is its role?","back":"Adenosine Triphosphate; it acts as the primary chemical energy currency of the cell.","evidence":"ATP is used by cells as universal energy currency."}""",
                    """{"type":"card","front":"Which metabolic pathway converts glucose into usable ATP?","back":"Cellular respiration converts glucose and oxygen into usable ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
                )
            } else {
                listOf(
                    """{"type":"card","front":"Ano ang pangunahing function ng mitochondria?","back":"Gumagawa ng ATP (energy currency) para sa cell sa pamamagitan ng cellular respiration.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                    """{"type":"card","front":"Ano ang ibig sabihin ng ATP sa cell biology?","back":"Adenosine Triphosphate; ito ang chemical energy currency ng mga selula.","evidence":"ATP is used by cells as universal energy currency."}""",
                    """{"type":"card","front":"Anong proseso ang ginagamit upang makabuo ng ATP mula sa nutrients?","back":"Cellular respiration ang proseso na nagko-convert ng nutrients at oxygen papuntang ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
                )
            }

            TopicCategory.SYSTEM_DESIGN -> listOf(
                """{"type":"card","front":"What is the trade-off between Horizontal and Vertical scaling?","back":"Vertical scaling upgrades existing node hardware (CPU/RAM); Horizontal scaling adds more node instances behind a load balancer.","evidence":"Horizontal scaling adds more machine nodes while vertical scaling enhances single-node hardware."}""",
                """{"type":"card","front":"What is the CAP Theorem in distributed databases?","back":"A distributed system can guarantee at most two of Consistency, Availability, and Partition Tolerance.","evidence":"The CAP theorem dictates that distributed databases cannot simultaneously guarantee C, A, and P."}"""
            )

            TopicCategory.CUSTOM_NOTES, TopicCategory.GENERAL -> listOf(
                """{"type":"card","front":"What is the central foundational concept of this topic?","back":"Understanding the core definitions, structural relationships, and analytical principles of the subject.","evidence":"Foundational principles govern domain analysis and problem solving."}""",
                """{"type":"card","front":"How is this subject applied to academic problem-solving?","back":"By systematically evaluating inputs, applying domain principles, and deriving verifiable conclusions.","evidence":"Systematic evaluation of principles leads to accurate academic analysis."}"""
            )
        }
    }

    private fun generateMcqs(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        if (topic == TopicCategory.CUSTOM_NOTES) {
            val sentences = extractSubstantiveSentences(prompt)
            if (sentences.isNotEmpty()) {
                val first = sentences.first()
                return listOf(
                    """{"type":"mcq","stem":"Which statement accurately reflects the study source?","choices":["$first","This statement is unverified and contradicts the text","The concept has no verified application","The phenomenon operates inversely without documentation"],"answer":0,"explanation":"Directly established in the source text.","evidence":"$first"}""",
                    """{"type":"mcq","stem":"What is the primary conclusion drawn regarding this subject?","choices":["It requires secondary confirmation outside scope","$first","It remains completely disputed in academic analysis","The mechanism functions only under isolated lab conditions"],"answer":1,"explanation":"Confirmed by the provided learning material.","evidence":"$first"}"""
                )
            }
        }

        return when (topic) {
            TopicCategory.RIZAL -> listOf(
                """{"type":"mcq","stem":"To whose memory did Jose Rizal dedicate his second novel, 'El Filibusterismo'?","choices":["The Katipunan leadership","His family in Calamba","The martyred priests Mariano Gomez, Jose Burgos, and Jacinto Zamora (Gomburza)","The reformist writers of La Solidaridad"],"answer":2,"explanation":"Rizal dedicated El Filibusterismo to the memory of the secular priests executed in 1872.","evidence":"El Filibusterismo was dedicated to the martyrdom of Gomburza in 1872."}""",
                """{"type":"mcq","stem":"What civic reform organization did Rizal establish in Tondo, Manila in July 1892?","choices":["Katipunan","La Liga Filipina","Propaganda Movement","Ilustrados del Norte"],"answer":1,"explanation":"Rizal established La Liga Filipina before his exile to Dapitan.","evidence":"Rizal established La Liga Filipina in Tondo to unite the archipelago and foster mutual assistance."}""",
                """{"type":"mcq","stem":"Where was Jose Rizal exiled for four years from 1892 to 1896?","choices":["Biñan, Laguna","Dapitan, Zamboanga del Norte","Madrid, Spain","Fort Santiago, Intramuros"],"answer":1,"explanation":"Governor-General Eulogio Despujol ordered Rizal deported to Dapitan in Mindanao.","evidence":"Rizal lived in exile in Dapitan where he practiced medicine and built a school."}"""
            )

            TopicCategory.DISCRETE_MATH -> listOf(
                """{"type":"mcq","stem":"If 13 items are placed into 12 distinct bins, what does the Pigeonhole Principle guarantee?","choices":["Every bin contains exactly one item","At least one bin contains 2 or more items","Exactly one bin is completely empty","No bin can hold more than 3 items"],"answer":1,"explanation":"By the Pigeonhole Principle, ceil(13/12) = 2 items in at least one bin.","evidence":"The pigeonhole principle states that if n items are distributed into k boxes and n > k, at least one box contains >= 2 items."}""",
                """{"type":"mcq","stem":"Which logical connective produces a FALSE value if and only if both operands are FALSE?","choices":["Conjunction (AND)","Disjunction (OR)","Exclusive OR (XOR)","Biconditional (IFF)"],"answer":1,"explanation":"Logical disjunction (p v q) is false only when both p and q are false.","evidence":"A disjunction p v q is false strictly when both p and q are false."}""",
                """{"type":"mcq","stem":"What is the size of the power set P(S) for a set S containing 5 distinct elements?","choices":["10","25","32","64"],"answer":2,"explanation":"The cardinality of a power set is 2^n. For n = 5, 2^5 = 32 subsets.","evidence":"The cardinality of the power set of a set with n elements is 2^n."}"""
            )

            TopicCategory.EMERGING_TECH -> listOf(
                """{"type":"mcq","stem":"Which quantum computing principle allows quantum states to be instantaneously correlated regardless of distance?","choices":["Superposition","Quantum Entanglement","Quantum Annealing","Waveform Interference"],"answer":1,"explanation":"Quantum entanglement creates instantaneous statistical correlations between paired qubits.","evidence":"Quantum entanglement creates instantaneous correlations between coupled quantum states."}""",
                """{"type":"mcq","stem":"What computing paradigm executes telemetry analysis directly on IoT devices to reduce network latency?","choices":["Cloud Storage","Edge Computing","Mainframe Batching","Web Scraping"],"answer":1,"explanation":"Edge computing minimizes round-trip latency by executing workloads locally.","evidence":"Edge computing processes data near the source to reduce network bandwidth and round-trip latency."}""",
                """{"type":"mcq","stem":"What is the primary function of a Smart Contract in distributed ledger technology?","choices":["Digital copyright registry","Self-executing code that enforces agreements when conditions are satisfied","Replacing relational database primary keys","Hardware mining accelerator"],"answer":1,"explanation":"Smart contracts are immutable programs deployed on blockchain ledgers.","evidence":"Smart contracts are self-executing code deployed on blockchains triggered by cryptographic conditions."}"""
            )

            TopicCategory.BIOLOGY -> listOf(
                """{"type":"mcq","stem":"Which organelle is universally referred to as the powerhouse of the cell?","choices":["Ribosome","Mitochondrion","Endoplasmic Reticulum","Golgi Complex"],"answer":1,"explanation":"Mitochondria perform cellular respiration to produce energy.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"mcq","stem":"What is the primary chemical energy currency generated by mitochondria?","choices":["Glucose","Glycogen","ATP (Adenosine Triphosphate)","Lactic Acid"],"answer":2,"explanation":"ATP is the chemical energy currency produced by mitochondria.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
            )

            else -> listOf(
                """{"type":"mcq","stem":"Which methodology best ensures sound academic analysis in this field?","choices":["Relying solely on intuition","Grounded evaluation of principles with verifiable evidence","Discarding theoretical definitions in practice","Ignoring systematic domain models"],"answer":1,"explanation":"Academic analysis requires empirical grounding and conceptual rigor.","evidence":"Foundational principles govern domain analysis and problem solving."}"""
            )
        }
    }

    private fun generateShortAnswers(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        val sentences = extractSubstantiveSentences(prompt)
        if (sentences.isNotEmpty()) {
            val s = sentences.first()
            return listOf(
                """{"type":"short","stem":"Explain the principal mechanism or concept described in the text: '${s.take(40)}...'?","reference_answer":"$s.","rubric":["$s","verifiable grounding in source"],"evidence":"$s."}"""
            )
        }

        return when (topic) {
            TopicCategory.RIZAL -> listOf(
                """{"type":"short","stem":"Explain the contrasting themes and approaches between Rizal's 'Noli Me Tangere' and 'El Filibusterismo'.","reference_answer":"Noli Me Tangere is a social novel focusing on exposing societal ills, friar abuses, and advocating peaceful reform, while El Filibusterismo is a darker political novel exploring the moral consequences and futility of armed vengeance and revolution.","rubric":["Noli focuses on peaceful reform and exposing friar abuses","Fili explores armed revolution and political vengeance"],"evidence":"Noli Me Tangere depicts social cancer and reform while El Filibusterismo deals with political rebellion."}""",
                """{"type":"short","stem":"How did Jose Rizal contribute to the community during his four-year exile in Dapitan?","reference_answer":"In Dapitan, Rizal practiced ophthalmology and medicine, built a school for local boys, constructed a clean municipal waterworks aqueduct system, and collected biological specimens.","rubric":["practiced medicine and surgery","built a community school and water aqueduct system"],"evidence":"Rizal lived in exile in Dapitan where he practiced medicine and built a school."}"""
            )

            TopicCategory.DISCRETE_MATH -> listOf(
                """{"type":"short","stem":"Explain the principle of Mathematical Induction and state its two required proof steps.","reference_answer":"Mathematical induction proves a proposition P(n) for all non-negative integers. It consists of the Base Step (verifying P(0) or P(1)) and the Inductive Step (proving that if P(k) holds true, then P(k+1) must also hold true).","rubric":["Base step verifying initial value P(0) or P(1)","Inductive step showing P(k) implies P(k+1)"],"evidence":"Mathematical induction requires proving the base step and the inductive hypothesis step."}""",
                """{"type":"short","stem":"Explain how the Pigeonhole Principle is applied to prove duplicate occurrences in finite sets.","reference_answer":"If n items are assigned to k pigeonholes and n > k, by the pigeonhole principle at least one pigeonhole must contain two or more items, guaranteeing duplicate elements.","rubric":["n items placed in k boxes with n > k","at least one box must contain >= 2 items"],"evidence":"The pigeonhole principle states that if n items are distributed into k boxes and n > k, at least one box contains >= 2 items."}"""
            )

            TopicCategory.EMERGING_TECH -> listOf(
                """{"type":"short","stem":"Explain how Quantum Superposition allows quantum computers to solve specific complex problems faster than classical computers.","reference_answer":"While classical bits represent either 0 or 1, qubits in superposition exist in linear combinations of both states simultaneously, allowing 2^n states to be represented and evaluated concurrently using quantum interference.","rubric":["qubits represent 0 and 1 simultaneously","parallel state evaluation scaling as 2^n"],"evidence":"Qubits leverage quantum superposition to represent linear combinations of 0 and 1 simultaneously."}""",
                """{"type":"short","stem":"Explain how the Self-Attention mechanism in Transformer architectures processes language context.","reference_answer":"Self-attention computes dynamic mathematical weightings between every pair of tokens in a sequence simultaneously, allowing the model to capture deep semantic dependencies regardless of distance between words.","rubric":["computes attention weights between all token pairs","parallel dependency calculation across arbitrary distance"],"evidence":"Transformers utilize self-attention mechanisms to compute token contextual dependencies across entire sequences."}"""
            )

            TopicCategory.BIOLOGY -> listOf(
                """{"type":"short","stem":"How do mitochondria generate ATP inside the cell?","reference_answer":"Through cellular respiration, by converting biochemical nutrients and oxygen into ATP.","rubric":["cellular respiration","nutrients and oxygen converted to ATP"],"evidence":"Mitochondria produce ATP through cellular respiration."}"""
            )

            else -> listOf(
                """{"type":"short","stem":"Explain the fundamental mechanism governing this academic topic.","reference_answer":"The topic relies on systematic conceptual relationships and verified evidence to analyze problems accurately.","rubric":["systematic conceptual relationships","evidence-based verification"],"evidence":"Foundational principles govern domain analysis and problem solving."}"""
            )
        }
    }

    private fun generateReviewerPoints(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        if (topic == TopicCategory.CUSTOM_NOTES) {
            val sentences = extractSubstantiveSentences(prompt)
            if (sentences.isNotEmpty()) {
                return sentences.take(4).map { s ->
                    """{"type":"point","text":"$s.","evidence":"$s."}"""
                }
            }
        }

        return when (topic) {
            TopicCategory.RIZAL -> listOf(
                """{"type":"point","text":"Jose Rizal wrote 'Noli Me Tangere' (Berlin, 1887) and 'El Filibusterismo' (Ghent, 1891) to expose colonial corruption and inspire national consciousness.","evidence":"Noli Me Tangere was written to expose the social cancer and friar abuses in the Philippines."}""",
                """{"type":"point","text":"He established La Liga Filipina in Tondo (1892) advocating peaceful socio-economic unity and educational advancement.","evidence":"Rizal established La Liga Filipina in Tondo to unite the archipelago and foster mutual assistance."}""",
                """{"type":"point","text":"During his exile in Dapitan (1892–1896), Rizal practiced medicine, constructed an aqueduct water system, and operated a community school.","evidence":"Rizal lived in exile in Dapitan where he practiced medicine and built a school."}""",
                """{"type":"point","text":"Rizal was executed at Bagumbayan on December 30, 1896, leaving 'Mi Último Adiós' as his patriotic testament.","evidence":"Rizal wrote Mi Último Adiós on the eve of his execution at Bagumbayan on December 30, 1896."}"""
            )

            TopicCategory.DISCRETE_MATH -> listOf(
                """{"type":"point","text":"The Pigeonhole Principle states that if n items are distributed into k containers with n > k, at least one container contains >= 2 items.","evidence":"The pigeonhole principle states that if n items are distributed into k boxes and n > k, at least one box contains >= 2 items."}""",
                """{"type":"point","text":"The Power Set P(S) of any set with n elements has exactly 2^n subsets, encompassing all combinations including the empty set.","evidence":"The cardinality of the power set of a set with n elements is 2^n."}""",
                """{"type":"point","text":"Mathematical Induction proves propositions P(n) for all integers n >= 0 via the Base Step and Inductive Step.","evidence":"Mathematical induction requires proving the base step and the inductive hypothesis step."}""",
                """{"type":"point","text":"An Eulerian Path visits every edge of a graph exactly once, which exists if and only if exactly 0 or 2 vertices have odd degree.","evidence":"An Eulerian path is a graph trail that visits every edge exactly once."}"""
            )

            TopicCategory.EMERGING_TECH -> listOf(
                """{"type":"point","text":"Quantum computing leverages superposition and entanglement, enabling qubits to represent linear combinations of 0 and 1 simultaneously.","evidence":"Qubits leverage quantum superposition to represent linear combinations of 0 and 1 simultaneously."}""",
                """{"type":"point","text":"Edge computing shifts processing workloads to local gateway nodes, drastically reducing latency and conserving bandwidth.","evidence":"Edge computing processes data near the source to reduce network bandwidth and round-trip latency."}""",
                """{"type":"point","text":"Blockchain consensus mechanisms ensure distributed agreement on immutable transaction ledgers without central intermediaries.","evidence":"Consensus mechanisms enable decentralized nodes to validate ledger transactions without central trust."}""",
                """{"type":"point","text":"Transformer neural networks employ self-attention to model long-range context across sequence tokens in parallel.","evidence":"Transformers utilize self-attention mechanisms to compute token contextual dependencies across entire sequences."}"""
            )

            TopicCategory.BIOLOGY -> listOf(
                """{"type":"point","text":"Mitochondria are membrane-bound organelles that generate most cellular chemical energy (ATP).","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"point","text":"Cellular respiration converts biochemical nutrients and oxygen into usable ATP.","evidence":"Mitochondria produce ATP through cellular respiration."}""",
                """{"type":"point","text":"ATP serves as the universal energy currency across all cellular activities.","evidence":"ATP is used by cells as universal energy currency."}"""
            )

            else -> listOf(
                """{"type":"point","text":"Academic mastery requires understanding foundational principles, key operational mechanisms, and empirical evidence.","evidence":"Foundational principles govern domain analysis and problem solving."}""",
                """{"type":"point","text":"Analytical problem-solving requires validating premises before drawing academic conclusions.","evidence":"Systematic evaluation of principles leads to accurate academic analysis."}"""
            )
        }
    }

    private fun generateChatResponse(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        if (topic == TopicCategory.CUSTOM_NOTES) {
            val sentences = extractSubstantiveSentences(prompt)
            if (sentences.isNotEmpty()) {
                val first = sentences.first()
                return listOf(
                    "According to your source notes [S1]:\n\n",
                    "> \"$first\"\n\n",
                    "**Core Analysis:**\n",
                    "• **Grounding:** This directly addresses your question based on the material provided in your library.\n",
                    "• **Key Concept:** The notes emphasize this mechanism as fundamental to understanding the topic.\n\n",
                    "[unverified] Additional study tip: When reviewing this for exams, make sure to connect this principle with real-world applications in your coursework."
                )
            }
        }

        return when (topic) {
            TopicCategory.RIZAL -> if (isEnglish) {
                listOf(
                    "[General Academic Knowledge - Philippine History & Literature]\n\n",
                    "### 🇵🇭 Life and Works of Jose Rizal\n\n",
                    "**Dr. José Protasio Rizal Mercado y Alonso Realonda** (1861–1896) was a polymath, physician, and national hero whose writings catalyzed the Philippine revolution against Spanish colonial rule.\n\n",
                    "**1. Literary Masterpieces:**\n",
                    "- **Noli Me Tangere (1887, Berlin):** Meaning 'Touch Me Not', it portrayed the social cancer (*el cáncer social*) of Spanish clerical abuse and colonial stagnation through characters like Crisostomo Ibarra, Maria Clara, and Elias.\n",
                    "- **El Filibusterismo (1891, Ghent):** The darker political sequel dedicated to the martyred priests **Gomburza** (Gomez, Burgos, Zamora), highlighting the perils and moral reckoning of violent revolution through Simoun.\n\n",
                    "**2. Exile in Dapitan (1892–1896):**\n",
                    "During his four-year exile, Rizal transformed the community: he built a water aqueduct, established an academy for boys, practiced eye surgery, and cataloged endemic species (e.g., *Draco rizali*).\n\n",
                    "**3. Martyrdom & Legacy:**\n",
                    "Executed by firing squad at Bagumbayan on **December 30, 1896**, his farewell poem, *Mi Último Adiós*, fueled the national fight for Philippine independence.\n\n",
                    "> **Study Focus:** Understand Republic Act 1425 (The Rizal Law) and how Rizal advocated education and self-determination as the true keys to freedom."
                )
            } else {
                listOf(
                    "[General Academic Knowledge - Kasaysayan at Panitikan ng Pilipinas]\n\n",
                    "### 🇵🇭 Buhay at mga Akda ni Dr. Jose Rizal\n\n",
                    "Si **Dr. Jose Rizal** (1861–1896) ang pambansang bayani ng Pilipinas na nagmulat sa kamalayan ng mga Pilipino sa pamamagitan ng kanyang pluma at dunong.\n\n",
                    "**1. Mga Pangunahing Nobela:**\n",
                    "- **Noli Me Tangere (1887):** Ibinunyag ang kanser ng lipunan at katiwalian ng pamahalaang kolonyal at mga prayle.\n",
                    "- **El Filibusterismo (1891):** Inihandog sa tatlong paring martir na Gomburza, tumatalakay sa rebolusyon at pulitikal na kamalayan.\n\n",
                    "**2. Pamamalagi sa Dapitan (1892–1896):**\n",
                    "Nagtayo si Rizal ng paaralan, klinika, at patubig sa Dapitan para sa kapakanan ng mga mamamayan.\n\n",
                    "**3. Kamatayan sa Bagumbayan:**\n",
                    "Binaril siya noong **Disyembre 30, 1896** sa Bagumbayan, kung saan iniwan niya ang tulang *Mi Último Adiós*.\n\n",
                    "> **Pang-Review Tip:** Tandaan ang layunin ng Batas Rizal (RA 1425) sa pagpapanatili ng pagmamahal sa bayan at kalayaan."
                )
            }

            TopicCategory.DISCRETE_MATH -> if (isEnglish) {
                listOf(
                    "[General Academic Knowledge - Discrete Mathematics]\n\n",
                    "### 📐 Discrete Mathematics Fundamentals\n\n",
                    "Discrete mathematics is the mathematical foundation for computer science and formal reasoning, studying structures with distinct, separated values.\n\n",
                    "**1. Logic & Proofs:**\n",
                    "- **Propositional Logic:** Truth tables, logical connectives (NOT, AND, OR, Implication, Biconditional), and De Morgan's Laws.\n",
                    "- **Proof Techniques:** Direct proof, contrapositive, contradiction, and **Mathematical Induction** (Base Step + Inductive Step).\n\n",
                    "**2. Sets & Combinatorics:**\n",
                    "- **Power Sets:** For a set with n elements, the power set contains 2^n subsets.\n",
                    "- **Pigeonhole Principle:** Distributing n objects into k boxes with n > k guarantees at least one box contains 2 or more objects.\n\n",
                    "**3. Graph Theory:**\n",
                    "- **Eulerian Paths:** Traverses every edge exactly once (requires exactly 0 or 2 vertices with odd degrees).\n",
                    "- **Hamiltonian Paths:** Visits every vertex exactly once.\n\n",
                    "What specific proof or discrete concept would you like to solve together?"
                )
            } else {
                listOf(
                    "[General Academic Knowledge - Discrete Mathematics]\n\n",
                    "### 📐 Gabay sa Discrete Mathematics\n\n",
                    "Ang Discrete Mathematics ang pundasyon ng computer science at lohikal na pangangatwiran.\n\n",
                    "**1. Propositional Logic at Proofs:**\n",
                    "- **Truth Tables:** Pagsusuri ng katotohanan ng mga pahayag gamit ang AND, OR, NOT, at Conditional statements.\n",
                    "- **Mathematical Induction:** Patunay gamit ang Base Step at Inductive Step.\n\n",
                    "**2. Sets at Combinatorics:**\n",
                    "- **Pigeonhole Principle:** Kung mas marami ang bagay kaysa lalagyan, siguradong may lalagyang magkakaroon ng dalawa o higit pa.\n",
                    "- **Power Set:** 2^n na bilang ng mga subsets mula sa n na elemento.\n\n",
                    "Aling bahagi sa logic, sets, o graphs ang nais mong suriin natin nang detalyado?"
                )
            }

            TopicCategory.EMERGING_TECH -> if (isEnglish) {
                listOf(
                    "[General Academic Knowledge - Emerging Technologies]\n\n",
                    "### 🤖 Emerging Technologies Overview\n\n",
                    "Emerging technologies represent breakthrough innovations reshaping science, industry, and modern computing.\n\n",
                    "**1. Quantum Computing:**\n",
                    "- **Qubits:** Leverage **Superposition** (existing in combinations of |0> and |1>) and **Entanglement** to solve polynomial-scale factoring and quantum simulations exponentially faster.\n\n",
                    "**2. Edge Computing & IoT:**\n",
                    "- Shifting data processing from centralized cloud data centers to local edge nodes (e.g., smart gateways, on-device NPU inference), drastically reducing round-trip latency and securing sensitive telemetry.\n\n",
                    "**3. Generative AI & Transformer Models:**\n",
                    "- Deep learning models based on **Self-Attention Mechanisms** that process sequence tokens in parallel, powering advanced natural language understanding and multimodal reasoning.\n\n",
                    "**4. Blockchain & Decentralized Systems:**\n",
                    "- Distributed immutable ledgers governed by cryptographic consensus mechanisms (Proof of Stake, BFT) executing deterministic smart contracts.\n\n",
                    "Which emerging technology trend are you currently examining?"
                )
            } else {
                listOf(
                    "[General Academic Knowledge - Emerging Technologies]\n\n",
                    "### 🤖 Mga Makabagong Teknolohiya (Emerging Tech)\n\n",
                    "Ang mga modernong teknolohiya ngayon ay nagbabago sa bilis at paraan ng pagpoproseso ng impormasyon.\n\n",
                    "**1. Quantum Computing:**\n",
                    "- Paggamit ng **Qubits**, **Superposition**, at **Entanglement** upang maproseso ang kumplikadong kalkulasyon nang mas mabilis kaysa sa tradisyonal na kompyuter.\n\n",
                    "**2. Edge Computing at IoT:**\n",
                    "- Pagproseso ng datos mismo sa device o malapit na server upang maiwasan ang internet lag at network latency.\n\n",
                    "**3. Generative Artificial Intelligence:**\n",
                    "- Paggamit ng **Transformer Architecture** at **Self-Attention** para sa pagsusuri ng wika at paglikha ng bagong solusyon.\n\n",
                    "Anong teknolohiya ang nais mong talakayin pa natin?"
                )
            }

            TopicCategory.BIOLOGY -> listOf(
                "According to biological principles [S1]:\n\n",
                "**Mitochondria** are the double-membraned powerhouses of eukaryotic cells.\n\n",
                "• **Cellular Respiration:** They convert nutrients and oxygen into ATP (Adenosine Triphosphate).\n",
                "• **ATP Synthesis:** ATP powers essential mechanical, active transport, and biosynthetic reactions throughout the cell.\n\n",
                "[unverified] Mitochondria maintain their own maternal circular mtDNA."
            )

            TopicCategory.SYSTEM_DESIGN -> listOf(
                "[General Academic Knowledge - Computer Science / IT]\n\n",
                "### System Design & Architecture Overview\n\n",
                "System design is the process of architecting scalable, resilient, and maintainable software systems.\n\n",
                "**Core Pillars:**\n",
                "1. **Scalability:** Horizontal scaling vs Vertical scaling.\n",
                "2. **Load Balancing:** Distributing traffic across backend replicas.\n",
                "3. **Caching:** In-memory caching with Redis or Memcached.\n",
                "4. **Database Architecture:** ACID SQL vs NoSQL sharding.\n\n",
                "**CAP Theorem:** Guaranteeing two of Consistency, Availability, and Partition Tolerance."
            )

            else -> listOf(
                "[General Academic Knowledge]\n\n",
                "### Academic Study Guide & Mentorship\n\n",
                "Here is an overview of this subject calibrated to your academic level:\n\n",
                "• **Core Principles:** Focus on mastering foundational definitions and fundamental rules of the domain.\n",
                "• **Analytical Application:** Practice breaking problems into known variables, applicable theories, and verifiable solutions.\n",
                "• **Active Recall:** Formulate key questions and explain concepts in your own words to solidify retention.\n\n",
                "Tell me what specific question or concept you'd like to explore next!"
            )
        }
    }

    private fun generateExplainThisResponse(prompt: String, topic: TopicCategory, isEnglish: Boolean): List<String> {
        val concept = prompt.substringAfter("CONCEPT TO EXPLAIN:\n", "").trim()
            .ifBlank { "Core Academic Concept" }

        return when (topic) {
            TopicCategory.RIZAL -> listOf(
                "### 💡 Rizal Academic Concept: $concept\n\n",
                "**Core Meaning:** In Philippine history and literature, this concept embodies the socio-political realities of 19th-century Spanish colonial rule and Jose Rizal's intellectual campaign for Filipino dignity, secular rights, and educational emancipation.\n\n",
                "> **Real-World Analogy:** Think of Rizal's writings like an unflinching mirror held up to society: by accurately reflecting structural corruption and societal complacency, it forced an entire nation to acknowledge reality and demand reform.\n\n",
                "**Exam Recall Tip:** When discussing Rizal's works on exams, always highlight the balance between his opposition to clerical tyranny and his insistence that education, civic virtue, and moral integrity must precede true political freedom."
            )

            TopicCategory.DISCRETE_MATH -> listOf(
                "### 💡 Discrete Mathematics: $concept\n\n",
                "**Core Definition:** In discrete mathematics, this concept provides a formal, non-continuous logical or combinatorial rule used to establish mathematical certainty and verify algorithmic correctness.\n\n",
                "> **Real-World Analogy:** Think of this principle like an automated turnstile or postal sorting facility: strict binary rules determine where each item belongs without ambiguity or overlap.\n\n",
                "**Exam Recall Tip:** Always write down the formal mathematical definition first, identify the domain and boundary conditions, and state the inductive or logical step explicitly to secure full points."
            )

            TopicCategory.EMERGING_TECH -> listOf(
                "### 💡 Emerging Tech: $concept\n\n",
                "**Core Definition:** This technology represents an architectural paradigm shift away from traditional centralized or classical constraints, leveraging modern algorithmic or physical principles to solve complex real-world bottlenecks.\n\n",
                "> **Real-World Analogy:** Think of this like replacing a single city courier with an interconnected instant relay network: rather than waiting in bottlenecked queues, work is distributed dynamically right where it is needed.\n\n",
                "**Exam Recall Tip:** When analyzing emerging technologies, emphasize the trade-offs: latency vs computational cost, security vs decentralization, and theoretical throughput vs practical implementation constraints."
            )

            else -> listOf(
                "### 💡 Conceptual Breakdown: $concept\n\n",
                "**Core Definition:** This concept represents a cornerstone mechanism in this field of study. It describes the orderly interaction and transformation of components to produce the observed phenomenon.\n\n",
                "> **Real-World Analogy:** Think of this system like a municipal utility grid: every discrete component coordinates seamlessly to deliver energy and resources exactly where demand surges.\n\n",
                "**Exam Recall Tip:** Always explicitly define the inputs, the operational mechanism, and the resulting outcome. Linking these three steps guarantees comprehensive exam recall."
            )
        }
    }
}
