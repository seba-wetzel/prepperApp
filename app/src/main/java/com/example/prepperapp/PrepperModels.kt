package com.example.prepperapp

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

internal data class HouseholdSettings(
    val people: Int = 5,
    val weeks: Int = 2,
    val dailyKcal: Int = 1900,
    val dailyWaterLitresMilli: Long = 4_000,
    val lastBackup: Instant? = null,
    val configured: Boolean = false,
    val taskScheduleStart: LocalDate = LocalDate.now(),
)

internal data class Product(
    val id: String,
    val name: String,
    val unit: String,
    val category: String,
    val weeklyForFiveMilli: Long,
    val kcalPerUnit: Int,
    val stepMilli: Long,
    val note: String,
    val custom: Boolean = false,
)

internal enum class DateKind(val label: String) {
    EXPIRATION("Vencimiento"),
    BEST_BEFORE("Consumo preferente"),
}

internal data class StockLot(
    val id: Long,
    val productId: String,
    val quantityMilli: Long,
    val date: LocalDate?,
    val dateKind: DateKind,
    val location: String,
    val usable: Boolean,
)

internal enum class TaskPeriod(val label: String) {
    WEEKLY("Semanal"),
    MONTHLY("Mensual"),
    QUARTERLY("Trimestral"),
    SEMIANNUAL("Semestral"),
    ANNUAL("Anual");

    fun next(date: LocalDate): LocalDate = when (this) {
        WEEKLY -> date.plusWeeks(1)
        MONTHLY -> date.plusMonths(1)
        QUARTERLY -> date.plusMonths(3)
        SEMIANNUAL -> date.plusMonths(6)
        ANNUAL -> date.plusYears(1)
    }
}

internal data class MaintenanceTask(
    val id: String,
    val title: String,
    val period: TaskPeriod,
    val active: Boolean,
    val completions: List<TaskCompletion>,
) {
    fun due(today: LocalDate, scheduleStart: LocalDate = today): LocalDate =
        completions.firstOrNull()?.date?.let(period::next) ?: period.next(scheduleStart)
    fun daysUntil(today: LocalDate, scheduleStart: LocalDate = today): Long =
        ChronoUnit.DAYS.between(today, due(today, scheduleStart))
    fun isPending(today: LocalDate, scheduleStart: LocalDate = today): Boolean =
        daysUntil(today, scheduleStart) <= 0
}

internal data class TaskCompletion(val id: Long, val taskId: String, val date: LocalDate, val instant: Instant)

internal data class PrepperState(
    val settings: HouseholdSettings = HouseholdSettings(),
    val products: List<Product> = emptyList(),
    val lots: List<StockLot> = emptyList(),
    val tasks: List<MaintenanceTask> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
) {
    val targetDays: Int get() = settings.weeks * 7

    fun productLots(productId: String) = lots.filter { it.productId == productId }
        .sortedWith(compareBy<StockLot> { it.date == null }.thenBy { it.date })

    fun quantityMilli(productId: String, usableOnly: Boolean = false): Long =
        productLots(productId).filter { lot ->
            !usableOnly || (lot.usable &&
                !(lot.dateKind == DateKind.EXPIRATION && lot.date?.isBefore(today) == true))
        }.sumOf { it.quantityMilli }

    fun goalMilli(product: Product): Long {
        val raw = product.weeklyForFiveMilli.toDouble() * settings.people / 5.0 * settings.weeks
        val countable = product.unit.lowercase().let { it != "kg" && !it.startsWith("litro") && it != "l" }
        return if (countable) ceil(raw / 1000.0).toLong() * 1000
        else ceil(raw / 100.0).toLong() * 100
    }

    fun missingMilli(product: Product): Long = (goalMilli(product) - quantityMilli(product.id)).coerceAtLeast(0)

    val shoppingCount: Int get() = products.count { missingMilli(it) > 0 }

    val pendingTasks: List<MaintenanceTask> get() =
        if (!settings.configured) emptyList()
        else tasks.filter { it.active && it.isPending(today, settings.taskScheduleStart) }

    fun taskDue(task: MaintenanceTask): LocalDate =
        task.due(today, settings.taskScheduleStart)

    fun taskDaysUntil(task: MaintenanceTask): Long =
        task.daysUntil(today, settings.taskScheduleStart)

    val foodDays: Double get() {
        val kcal = products.filter { it.id != "agua" }.sumOf { product ->
            quantityMilli(product.id, usableOnly = true).toDouble() / 1000 * product.kcalPerUnit
        }
        return kcal / (settings.people * settings.dailyKcal).coerceAtLeast(1)
    }

    val waterDays: Double get() {
        val water = quantityMilli("agua", usableOnly = true).toDouble()
        val daily = settings.people * settings.dailyWaterLitresMilli
        return if (daily <= 0) 0.0 else water / daily
    }

    val expiringLots: List<Pair<Product, StockLot>> get() = lots.asSequence()
        .filter { it.quantityMilli > 0 && it.date != null && ChronoUnit.DAYS.between(today, it.date) <= 60 }
        .mapNotNull { lot -> products.firstOrNull { it.id == lot.productId }?.let { it to lot } }
        .sortedBy { it.second.date }
        .take(6)
        .toList()
}

internal fun Long.asQuantity(): String {
    val whole = this / 1000
    val fraction = this % 1000
    return if (fraction == 0L) whole.toString()
    else "%.2f".format(java.util.Locale.forLanguageTag("es-AR"), this / 1000.0)
        .trimEnd('0').trimEnd(',', '.')
}

internal fun Double.asDays(): String =
    "%.1f".format(java.util.Locale.forLanguageTag("es-AR"), this.coerceAtLeast(0.0))
