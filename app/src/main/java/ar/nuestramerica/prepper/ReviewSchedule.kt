package ar.nuestramerica.prepper

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val calendarFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
internal fun LocalDate.displayDate(): String = format(calendarFormat)

internal const val REVIEW_TITLE = "Revisar bidones"
internal const val REVIEW_NOTICE_DAYS = 7L

internal data class ReviewSchedule(
    val firstDue: LocalDate,
    val history: List<Completion>,
    val lastCheck: Instant?,
    val today: LocalDate,
) {
    val due: LocalDate get() = history.firstOrNull()?.date?.plusMonths(3) ?: firstDue
    val daysLeft: Long get() = ChronoUnit.DAYS.between(today, due)
    val noticeDate: LocalDate get() = due.minusDays(REVIEW_NOTICE_DAYS)
    val pending: Boolean get() = daysLeft <= 0

    fun shouldNotify(alreadyNotified: Boolean): Boolean =
        !alreadyNotified && !today.isBefore(noticeDate)
}

internal data class Completion(val id: Long, val date: LocalDate, val timestamp: Instant)
