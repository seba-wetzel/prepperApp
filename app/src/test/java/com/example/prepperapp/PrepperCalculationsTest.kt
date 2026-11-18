package com.example.prepperapp

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepperCalculationsTest {
    @Test
    fun targetsUseDemoRoundingRules() {
        val rice = PrepperRepository.BASE_PRODUCTS.first { it.id == "arroz" }
        val cans = PrepperRepository.BASE_PRODUCTS.first { it.id == "atun" }
        val state = PrepperState(settings = HouseholdSettings(people = 3, weeks = 2), products = listOf(rice, cans), loading = false)

        assertEquals(2_400, state.goalMilli(rice))
        assertEquals(10_000, state.goalMilli(cans))
    }

    @Test
    fun foodAndWaterOnlyCountUsableLots() {
        val rice = PrepperRepository.BASE_PRODUCTS.first { it.id == "arroz" }
        val water = PrepperRepository.BASE_PRODUCTS.first { it.id == "agua" }
        val state = PrepperState(
            settings = HouseholdSettings(people = 2, weeks = 2, dailyKcal = 1800, dailyWaterLitresMilli = 3_000),
            products = listOf(rice, water),
            lots = listOf(
                StockLot(1, rice.id, 2_000, null, DateKind.EXPIRATION, "", true),
                StockLot(2, rice.id, 20_000, null, DateKind.EXPIRATION, "", false),
                StockLot(3, rice.id, 20_000, LocalDate.now().minusDays(1), DateKind.EXPIRATION, "", true),
                StockLot(4, water.id, 18_000, null, DateKind.EXPIRATION, "", true)
            ),
            loading = false
        )

        assertEquals(2.0, state.foodDays, 0.001)
        assertEquals(3.0, state.waterDays, 0.001)
    }

    @Test
    fun unconfiguredInstallHasNoPendingTasks() {
        val task = MaintenanceTask("s", "Semanal", TaskPeriod.WEEKLY, true, emptyList())
        val state = PrepperState(
            settings = HouseholdSettings(
                configured = false,
                taskScheduleStart = LocalDate.of(2026, 11, 18),
            ),
            tasks = listOf(task),
            today = LocalDate.of(2026, 11, 18),
            loading = false,
        )

        assertTrue(state.pendingTasks.isEmpty())
    }

    @Test
    fun firstTaskDueDateStartsAfterOnboardingPeriod() {
        val start = LocalDate.of(2026, 11, 18)
        val weekly = MaintenanceTask("s", "Semanal", TaskPeriod.WEEKLY, true, emptyList())
        val state = PrepperState(
            settings = HouseholdSettings(configured = true, taskScheduleStart = start),
            tasks = listOf(weekly),
            today = start,
            loading = false,
        )

        assertEquals(LocalDate.of(2026, 11, 25), state.taskDue(weekly))
        assertFalse(weekly.isPending(start, start))
        assertTrue(weekly.isPending(LocalDate.of(2026, 11, 25), start))
    }

    @Test
    fun monthlyTasksUseCalendarPeriods() {
        val completion = TaskCompletion(1, "m", LocalDate.of(2027, 1, 31), Instant.EPOCH)
        val task = MaintenanceTask("m", "Mensual", TaskPeriod.MONTHLY, true, listOf(completion))

        assertEquals(LocalDate.of(2027, 2, 28), task.due(LocalDate.of(2027, 2, 1)))
        assertTrue(task.isPending(LocalDate.of(2027, 2, 28)))
    }
}
