package com.appbuild.gemce

import com.appbuild.gemce.domain.study.ReviewState
import com.appbuild.gemce.domain.study.Sm2Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    @Test
    fun testFirstSuccessfulReview() {
        val initial = ReviewState(cardId = "c1")
        val scheduled = Sm2Scheduler.schedule(initial, grade = 4)

        assertEquals(1, scheduled.repetitions)
        assertEquals(1, scheduled.intervalDays)
        assertEquals(2.5f, scheduled.easeFactor, 0.01f)
    }

    @Test
    fun testSecondSuccessfulReview() {
        val first = ReviewState(cardId = "c1", repetitions = 1, intervalDays = 1, easeFactor = 2.5f)
        val second = Sm2Scheduler.schedule(first, grade = 4)

        assertEquals(2, second.repetitions)
        assertEquals(6, second.intervalDays)
    }

    @Test
    fun testThirdReviewAppliesEaseMultiplier() {
        val second = ReviewState(cardId = "c1", repetitions = 2, intervalDays = 6, easeFactor = 2.5f)
        val third = Sm2Scheduler.schedule(second, grade = 5)

        assertEquals(3, third.repetitions)
        // 6 * 2.5 = 15 days
        assertEquals(15, third.intervalDays)
        // Grade 5 increases ease factor
        assertTrue(third.easeFactor > 2.5f)
    }

    @Test
    fun testLapseResetsIntervalAndRepetitions() {
        val mature = ReviewState(cardId = "c1", repetitions = 5, intervalDays = 30, easeFactor = 2.4f)
        val lapsed = Sm2Scheduler.schedule(mature, grade = 1) // Failed recall

        assertEquals(0, lapsed.repetitions)
        assertEquals(1, lapsed.intervalDays)
        // In standard SM-2, ease factor is preserved upon lapse
        assertEquals(2.4f, lapsed.easeFactor, 0.01f)
    }

    @Test
    fun testEaseFactorFloor() {
        var state = ReviewState(cardId = "c1", easeFactor = 1.35f, repetitions = 2, intervalDays = 6)
        // Repeatedly give poor passing grade (3) which decreases ease factor
        for (i in 0 until 5) {
            state = Sm2Scheduler.schedule(state, grade = 3)
        }
        // Must never go below 1.3f
        assertEquals(Sm2Scheduler.MIN_EASE_FACTOR, state.easeFactor, 0.001f)
    }
}
