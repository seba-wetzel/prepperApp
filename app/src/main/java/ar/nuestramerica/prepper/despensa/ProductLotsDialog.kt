package ar.nuestramerica.prepper.despensa

import androidx.compose.runtime.Composable
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.Product
import java.time.LocalDate

@Composable
internal fun ProductLotsDialog(
    state: PrepperState,
    product: Product,
    initialAction: PantryAction,
    onDismiss: () -> Unit,
    addLot: (String, Long, LocalDate?, DateKind, String) -> Unit,
    consume: (Long, Long) -> Unit,
    updateLot: (Long, Long, LocalDate?, DateKind, String) -> Unit,
    deleteLot: (Long) -> Unit,
    deleteProduct: (String) -> Unit,
) {
    when (initialAction) {
        PantryAction.ADD -> StockEditorDialog(product, onDismiss = onDismiss) { amount, date, kind, location ->
            addLot(product.id, amount, date, kind, location)
        }
        PantryAction.CONSUME -> ConsumeStockDialog(state, product, onDismiss, consume)
        PantryAction.LOTS -> LotsDialog(state, product, onDismiss, updateLot, deleteLot, deleteProduct)
    }
}
