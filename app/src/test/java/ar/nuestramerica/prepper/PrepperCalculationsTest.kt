package ar.nuestramerica.prepper

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun argentineDatesAreFormattedAndParsed() {
        assertEquals("31/12/25", "311225".toArgentineDateInput())
        assertEquals("31/12/2025", "31122025".toArgentineDateInput())
        assertEquals(LocalDate.of(2025, 12, 31), "31/12/25".parseDateOrNull())
        assertEquals(LocalDate.of(2025, 12, 31), "31/12/2025".parseDateOrNull())
        assertNull("31/02/25".parseDateOrNull())
    }

    @Test
    fun waterDefaultsToThreeMonthRotation() {
        val today = LocalDate.of(2026, 11, 18)
        val water = PrepperRepository.BASE_PRODUCTS.first { it.id == "agua" }
        val rice = PrepperRepository.BASE_PRODUCTS.first { it.id == "arroz" }

        assertEquals(LocalDate.of(2027, 2, 18), water.defaultLotDate(today))
        assertEquals(DateKind.ROTATION, water.defaultDateKind())
        assertNull(rice.defaultLotDate(today))
        assertEquals(DateKind.EXPIRATION, rice.defaultDateKind())
    }

    @Test
    fun expiredWaterRotationIsNotCountedAsUsable() {
        val today = LocalDate.of(2026, 11, 18)
        val water = PrepperRepository.BASE_PRODUCTS.first { it.id == "agua" }
        val state = PrepperState(
            settings = HouseholdSettings(people = 1, dailyWaterLitresMilli = 3_000),
            products = listOf(water),
            lots = listOf(
                StockLot(1, water.id, 9_000, today.minusDays(1), DateKind.ROTATION, "", true),
                StockLot(2, water.id, 6_000, today.plusDays(1), DateKind.ROTATION, "", true),
            ),
            today = today,
            loading = false,
        )

        assertEquals(2.0, state.waterDays, 0.001)
    }

    @Test
    fun consumptionSuggestsTheNearestUsableLot() {
        val today = LocalDate.of(2026, 11, 18)
        val rice = PrepperRepository.BASE_PRODUCTS.first { it.id == "arroz" }
        val state = PrepperState(
            products = listOf(rice),
            lots = listOf(
                StockLot(1, rice.id, 1_000, today.minusDays(1), DateKind.EXPIRATION, "", true),
                StockLot(2, rice.id, 1_000, today.plusDays(5), DateKind.EXPIRATION, "", true),
                StockLot(3, rice.id, 1_000, today.plusDays(2), DateKind.EXPIRATION, "", true),
                StockLot(4, rice.id, 1_000, null, DateKind.EXPIRATION, "", true),
            ),
            today = today,
            loading = false,
        )

        assertEquals(3L, state.suggestedLot(rice.id)?.id)
    }

    @Test
    fun lotCodesUseProductRelatedPrefixesAndIncrement() {
        assertEquals("LP", lotCodePrefix("leche", "Leche en polvo"))
        assertEquals("LA", lotCodePrefix("atun", "Atún o caballa en lata"))
        assertEquals("AR", lotCodePrefix("choclo", "Choclo o arvejas en lata"))
        assertEquals("GC", lotCodePrefix("custom", "Garbanzos cocidos"))
        assertEquals("LP03", nextLotCode("LP", listOf("LP01", "LP02", "AR01")))
    }

    @Test
    fun hiddenProductsDoNotAffectShoppingAutonomyOrExpiry() {
        val today = LocalDate.of(2026, 11, 18)
        val rice = PrepperRepository.BASE_PRODUCTS.first { it.id == "arroz" }.copy(visible = false)
        val state = PrepperState(
            settings = HouseholdSettings(people = 1, weeks = 2, dailyKcal = 1800),
            products = listOf(rice),
            lots = listOf(
                StockLot(1, rice.id, 2_000, today.plusDays(10), DateKind.EXPIRATION, "", true, "AR01"),
            ),
            today = today,
            loading = false,
        )

        assertEquals(0, state.shoppingCount)
        assertEquals(0.0, state.foodDays, 0.001)
        assertTrue(state.expiringLots.isEmpty())
    }

    @Test
    fun reviewTargetsIdentifyEachLotAndEachTaskOccurrence() {
        val today = LocalDate.of(2026, 11, 20)
        val product = PrepperRepository.BASE_PRODUCTS.first { it.id == "atun" }
        val task = MaintenanceTask("kit", "Revisar botiquín", TaskPeriod.WEEKLY, true, emptyList())
        val lots = (1L..7L).map { id ->
            StockLot(id, product.id, 2000, today.plusDays(id), DateKind.EXPIRATION, "", true, "LA0$id")
        }
        val state = PrepperState(HouseholdSettings(configured = true, taskScheduleStart = today.minusWeeks(1)),
            listOf(product), lots, listOf(task), today)
        val alerts = state.reviewAlerts()
        assertEquals(8, alerts.distinctBy { it.key }.size)
        assertEquals(task.title, alerts.first().title)
        assertTrue(alerts.last().title.contains("LA07"))
        assertTrue(alerts.last().detail.contains("27/11/2026"))
        val completed = task.copy(completions = listOf(TaskCompletion(1, task.id, today, Instant.EPOCH)))
        val nextWeek = state.copy(lots = lots.dropLast(1), tasks = listOf(completed), today = today.plusWeeks(1))
        assertTrue(nextWeek.reviewAlerts().none { it.key in setOf(alerts.first().key, alerts.last().key) })
    }

    @Test
    fun monthlyTasksUseCalendarPeriods() {
        val completion = TaskCompletion(1, "m", LocalDate.of(2027, 1, 31), Instant.EPOCH)
        val task = MaintenanceTask("m", "Mensual", TaskPeriod.MONTHLY, true, listOf(completion))

        assertEquals(LocalDate.of(2027, 2, 28), task.due(LocalDate.of(2027, 2, 1)))
        assertTrue(task.isPending(LocalDate.of(2027, 2, 28)))
    }
}
