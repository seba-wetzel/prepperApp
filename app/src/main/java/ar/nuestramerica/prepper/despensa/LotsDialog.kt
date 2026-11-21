package ar.nuestramerica.prepper.despensa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.nuestramerica.prepper.DateKind
import ar.nuestramerica.prepper.PrepperState
import ar.nuestramerica.prepper.Product
import ar.nuestramerica.prepper.asQuantity
import ar.nuestramerica.prepper.ui.format.dateFormat
import ar.nuestramerica.prepper.ui.theme.Bad
import ar.nuestramerica.prepper.ui.theme.EmergencyRedDark
import java.time.LocalDate

@Composable
internal fun LotsDialog(
    state: PrepperState,
    product: Product,
    onDismiss: () -> Unit,
    updateLot: (Long, Long, LocalDate?, DateKind, String) -> Unit,
    deleteLot: (Long) -> Unit,
    deleteProduct: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingLot = state.lots.firstOrNull { it.id == editingId && it.productId == product.id }
    if (editingLot != null) {
        StockEditorDialog(product, editingLot, onDismiss = { editingId = null }) { amount, date, kind, location ->
            updateLot(editingLot.id, amount, date, kind, location)
        }
        return
    }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var confirmProductDelete by remember { mutableStateOf(false) }
    val lots = state.productLots(product.id)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Existencias de ${product.name}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Total: ${state.quantityMilli(product.id).asQuantity()} ${product.unit}",
                    fontWeight = FontWeight.Bold,
                )
                if (lots.isEmpty()) {
                    Text("Todavía no hay existencias cargadas.")
                }
                lots.forEachIndexed { index, lot ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                lot.code.ifBlank { "Existencia ${index + 1}" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmergencyRedDark,
                            )
                            Text(
                                "${lot.quantityMilli.asQuantity()} ${product.unit}",
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                lot.date?.let { "${lot.dateKind.label}: ${it.format(dateFormat)}" }
                                    ?: "Fecha no informada",
                            )
                            if (lot.location.isNotBlank()) Text("Ubicación: ${lot.location}")
                            Row {
                                TextButton(onClick = { editingId = lot.id }) { Text("Editar") }
                                TextButton(onClick = { deleteId = lot.id }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (product.custom) {
                    TextButton(onClick = { confirmProductDelete = true }) {
                        Text("Quitar producto personalizado", color = Bad)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
    if (confirmProductDelete) {
        AlertDialog(
            onDismissRequest = { confirmProductDelete = false },
            title = { Text("¿Quitar este producto?") },
            text = { Text("También se eliminarán todas sus existencias. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteProduct(product.id)
                    confirmProductDelete = false
                    onDismiss()
                }) { Text("Quitar", color = Bad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmProductDelete = false }) { Text("Cancelar") }
            },
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("¿Eliminar esta existencia?") },
            text = { Text("La cantidad registrada se quitará de la despensa.") },
            confirmButton = {
                TextButton(onClick = { deleteLot(id); deleteId = null }) { Text("Eliminar", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } },
        )
    }
}
