package ar.nuestramerica.prepper

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewScheduleTest {
    private val firstDue = LocalDate.of(2026, 11, 17)

    @Test
    fun completionUsesCalendarMonthsAndUndoRestoresFirstDate() {
        val initial = ReviewSchedule(firstDue, emptyList(), null, firstDue)
        val completed = initial.copy(history = listOf(
            Completion(1, LocalDate.of(2026, 11, 30), Instant.EPOCH)
        ))
        assertEquals(LocalDate.of(2027, 2, 28), completed.due)
        assertEquals(firstDue, completed.copy(history = emptyList()).due)
    }

    @Test
    fun noticeStartsSevenCalendarDaysBeforeDueAndRemainsEligibleWhenOverdue() {
        val review = ReviewSchedule(firstDue, emptyList(), null, firstDue.minusDays(8))
        assertFalse(review.shouldNotify(alreadyNotified = false))
        assertTrue(review.copy(today = firstDue.minusDays(7)).shouldNotify(alreadyNotified = false))
        assertTrue(review.copy(today = firstDue.plusDays(1)).shouldNotify(alreadyNotified = false))
        assertFalse(review.copy(today = firstDue).shouldNotify(alreadyNotified = true))
        assertTrue(review.copy(today = firstDue).pending)
    }
}
