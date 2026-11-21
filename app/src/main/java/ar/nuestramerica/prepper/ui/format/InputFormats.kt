package ar.nuestramerica.prepper.ui.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun String.toMilli():Long =
    (replace(',','.').toDoubleOrNull()?.times(1000)?.toLong() ?: 0L).coerceAtLeast(0)

internal fun String.toArgentineDateInput(): String {
    val digits = filter(Char::isDigit).take(8)
    return buildString {
        append(digits.take(2))
        if (digits.length > 2) append('/').append(digits.substring(2, minOf(4, digits.length)))
        if (digits.length > 4) append('/').append(digits.substring(4))
    }
}

internal fun String.parseDateOrNull(): LocalDate? {
    if (isBlank()) return null
    val digits = filter(Char::isDigit)
    if (digits.length != 6 && digits.length != 8) return null
    val day = digits.substring(0, 2).toInt()
    val month = digits.substring(2, 4).toInt()
    val rawYear = digits.substring(4).toInt()
    val year = if (digits.length == 6) 2000 + rawYear else rawYear
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}
