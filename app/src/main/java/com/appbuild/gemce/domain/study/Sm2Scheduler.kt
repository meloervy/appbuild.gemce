package com.appbuild.gemce.domain.study

import kotlin.math.max
import kotlin.math.roundToInt

data class ReviewState(
    val cardId: String,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val dueAtEpochMs: Long = System.currentTimeMillis()
)

object Sm2Scheduler {
    const val MIN_EASE_FACTOR = 1.3f
    const val INITIAL_EASE_FACTOR = 2.5f
    private const val ONE_DAY_MS = 86_400_000L

    /**
     * Calculates the next review state based on standard SuperMemo-2 (SM-2) algorithm.
     * @param currentState the current review state
     * @param grade quality rating from 0 (complete blackout) to 5 (perfect recall)
     * @param nowEpochMs current epoch timestamp in milliseconds
     */
    fun schedule(
        currentState: ReviewState,
        grade: Int,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReviewState {
        val clampedGrade = grade.coerceIn(0, 5)

        val newRepetitions: Int
        val newIntervalDays: Int
        val newEaseFactor: Float

        if (clampedGrade >= 3) {
            newRepetitions = currentState.repetitions + 1
            newIntervalDays = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> max(1, (currentState.intervalDays * currentState.easeFactor).roundToInt())
            }

            // Standard SM-2 ease factor formula:
            // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
            val qFactor = 5 - clampedGrade
            val delta = 0.1f - qFactor * (0.08f + qFactor * 0.02f)
            newEaseFactor = max(MIN_EASE_FACTOR, currentState.easeFactor + delta)
        } else {
            // Lapse: reset interval and repetitions
            newRepetitions = 0
            newIntervalDays = 1
            newEaseFactor = currentState.easeFactor
        }

        val nextDue = nowEpochMs + (newIntervalDays * ONE_DAY_MS)

        return currentState.copy(
            easeFactor = newEaseFactor,
            repetitions = newRepetitions,
            intervalDays = newIntervalDays,
            dueAtEpochMs = nextDue
        )
    }
}
