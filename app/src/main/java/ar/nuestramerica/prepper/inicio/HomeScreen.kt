package ar.nuestramerica.prepper.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.MaintenanceTask
import ar.nuestramerica.prepper.PrepperRepository
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.StockLot
import ar.nuestramerica.prepper.asQuantity
import ar.nuestramerica.prepper.despensa.ExpiryChip
import ar.nuestramerica.prepper.ui.components.KitCard
import ar.nuestramerica.prepper.ui.components.SectionTitle
import ar.nuestramerica.prepper.ui.components.SummaryCard
import ar.nuestramerica.prepper.ui.format.dateFormat
import java.time.LocalDate

@Composable
internal fun HomeScreen(
    state: PrepperState,
    goShopping: () -> Unit,
    goTasks: () -> Unit,
    openLot: (StockLot) -> Unit,
    dialEmergency: (String) -> Unit,
) {
    var emergencyVisible by rememberSaveable { mutableStateOf(false) }

    EmergencyAccessCard { emergencyVisible = true }
    SectionTitle("Autonomía del hogar")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AutonomyCard("Comida", state.foodDays, state.targetDays, Modifier.weight(1f))
        AutonomyCard("Agua", state.waterDays, state.targetDays, Modifier.weight(1f))
    }
    SummaryCard(
        title = "Para comprar",
        subtitle = if (state.shoppingCount == 0) "Todo el stock está completo" else "Productos por debajo de la meta",
        count = state.shoppingCount,
        onClick = goShopping
    )
    SummaryCard(
        title = "Tareas pendientes",
        subtitle = if (state.pendingTasks.isEmpty()) "Todo al día" else "Mantenimiento que toca ahora",
        count = state.pendingTasks.size,
        onClick = goTasks
    )
    SectionTitle("Vencen pronto")
    KitCard {
        if (state.expiringLots.isEmpty()) {
            Text("Nada vence en los próximos 60 días. Cargá lotes con fecha para seguirlos acá.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.expiringLots.take(6).forEachIndexed { index, (product, lot) ->
                if (index > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth()
                    .clickable(onClickLabel = "Abrir lote", role = androidx.compose.ui.semantics.Role.Button) { openLot(lot) }
                    .heightIn(min = 48.dp).padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text(listOf(lot.code, "${lot.quantityMilli.asQuantity()} ${product.unit}").filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    ExpiryChip(lot, state.today)
                    Text("›", Modifier.padding(start = 6.dp).semantics { contentDescription = "" })
                }
            }
        }
    }
    val backup = state.settings.lastBackup
    Text(
        if (backup == null) "Todavía no guardaste una copia de seguridad. Podés hacerlo desde Ajustes."
        else "Última copia confirmada: ${backup.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormat)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (emergencyVisible) {
        EmergencyNumbersDialog(
            dismiss = { emergencyVisible = false },
            dial = dialEmergency,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Inicio · teléfono",
    showBackground = true,
    widthDp = 412,
    heightDp = 850
)
@Composable
private fun HomePreview() {
    ar.nuestramerica.prepper.ui.theme.PrepperAppTheme(darkTheme = false) {
        val products = PrepperRepository.BASE_PRODUCTS
        val state = PrepperState(
            products = products,
            lots = listOf(
                StockLot(1, "agua", 48_000, null, DateKind.EXPIRATION, "Lavadero", true),
                StockLot(2, "arroz", 3_000, LocalDate.now().plusDays(20), DateKind.EXPIRATION, "Despensa", true),
                StockLot(3, "atun", 5_000, LocalDate.now().minusDays(2), DateKind.EXPIRATION, "Despensa", true)
            ),
            tasks = PrepperRepository.BASE_TASKS.map { (id, title, period) ->
                MaintenanceTask(id, title, period, true, emptyList())
            },
            today = LocalDate.now(),
            loading = false
        )
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeScreen(state, {}, {}, {}, {})
        }
    }
}
