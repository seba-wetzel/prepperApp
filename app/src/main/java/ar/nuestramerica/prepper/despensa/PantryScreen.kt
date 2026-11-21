package ar.nuestramerica.prepper.despensa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.PrepperViewModel
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.ui.components.KitCard
import ar.nuestramerica.prepper.ui.components.SectionTitle
import java.time.temporal.ChronoUnit

internal enum class PantryFilter(val label: String) {
    ALL("Todo"), SHOPPING("Para comprar"), EXPIRING("Vence pronto")
}

private data class PantrySelection(val product: Product, val action: PantryAction)

@Composable
internal fun PantryScreen(
    state: PrepperState,
    model: PrepperViewModel,
    filter: PantryFilter,
    changeFilter: (PantryFilter) -> Unit,
    shareShoppingList: () -> Unit
) {
    var selection by remember { mutableStateOf<PantrySelection?>(null) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PantryFilter.entries.forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = { changeFilter(item) },
                label = { Text(item.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val shown = state.visibleProducts.filter { product ->
        when (filter) {
            PantryFilter.ALL -> true
            PantryFilter.SHOPPING -> state.missingMilli(product) > 0
            PantryFilter.EXPIRING -> state.productLots(product.id).any {
                it.date?.let { date -> ChronoUnit.DAYS.between(state.today, date) <= 60 } == true
            }
        }
    }
    if (shown.isEmpty()) {
        val message = when (filter) {
            PantryFilter.ALL -> "No hay productos visibles. Podés elegirlos desde Ajustes."
            PantryFilter.SHOPPING -> "No falta nada: el stock cubre la meta."
            PantryFilter.EXPIRING -> "Nada vence en los próximos 60 días."
        }
        KitCard { Text(message) }
    }
    shown.groupBy { it.category }.forEach { (category, products) ->
        SectionTitle(category)
        products.forEach { product ->
            ProductCard(state, product) { action -> selection = PantrySelection(product, action) }
        }
    }
    OutlinedButton(onClick = shareShoppingList, modifier = Modifier.fillMaxWidth()) {
        Text("Compartir lista de compras")
    }
    selection?.let { selected ->
        ProductLotsDialog(
            state = state,
            product = selected.product,
            initialAction = selected.action,
            onDismiss = { selection = null },
            addLot = model::addLot,
            consume = model::consumeLot,
            updateLot = model::updateLot,
            deleteLot = model::deleteLot,
            deleteProduct = model::deleteCustomProduct,
        )
    }
}
